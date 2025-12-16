package com.bookchest.shops.listeners;

import com.bookchest.shops.BookChestShops;
import com.bookchest.shops.data.ShopData;
import com.bookchest.shops.managers.TransactionManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class ChestListener implements Listener {
    
    private final BookChestShops plugin;
    
    public ChestListener(BookChestShops plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onContainerOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        if (!player.hasPermission("bookshop.use")) {
            return;
        }
        
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        
        if (!(holder instanceof Container container)) {
            return;
        }
        
        ShopData shop = plugin.getShopManager().getShop(container.getLocation());

        
        // Not a shop - normal container
        if (shop == null) {
            return;
        }
        
        // Owner opens their own shop - normal management access
        if (shop.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§7[Shop Owner Mode] You can freely manage your shop.");
            player.sendMessage("§7To update prices: Shift+Left-Click with your price book");
            return;
        }
        
        // Customer opens shop - start transaction and highlight items
        if (!player.hasPermission("bookshop.use")) {
            event.setCancelled(true);
            plugin.getConfigManager().sendMessage(player, "no-permission");
            return;
        }
        
        // Start transaction tracking
        plugin.getTransactionManager().startTransaction(player, inv);
        
        // Highlight items for sale with glow effect
        highlightShopItems(inv, shop);
        
        // Send shop info
        String ownerName = plugin.getServer().getOfflinePlayer(shop.getOwner()).getName();
        plugin.getConfigManager().sendMessage(player, "shop-owner", 
            Map.of("owner", ownerName != null ? ownerName : "Unknown"));
        
        player.sendMessage("§e§lHow to buy: §7Take a §b§lGLOWING§7 item, then place payment in the empty slot!");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (!player.hasPermission("bookshop.use")) {
            return;
        }
        
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        
        if (!(holder instanceof Container container)) {
            return;
        }
        
        ShopData shop = plugin.getShopManager().getShop(container.getLocation());
        if (shop == null) {
            return;
        }
        
        // OWNER MODE - Allow full management access
        if (shop.getOwner().equals(player.getUniqueId())) {
            return;
        }
        
        // CUSTOMER MODE - Handle buying transactions
        
        // Block dangerous click types that could mess up the transaction
        ClickType click = event.getClick();
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT ||
            click == ClickType.DOUBLE_CLICK || click == ClickType.NUMBER_KEY ||
            click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            event.setCancelled(true);
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Clicking in player's own inventory
        if (slot >= inv.getSize()) {
            // Allow normal interactions in player inventory
            return;
        }
        
        // Clicking in chest inventory (shop)
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // Check if this slot is for sale
        if (!shop.hasSlot(slot)) {
            // NOT a shop slot - block everything
            event.setCancelled(true);
            player.sendMessage("§cThis slot is not for sale!");
            return;
        }
        
        // This IS a shop slot - allow transactions
        TransactionManager.ActiveTransaction transaction = 
            plugin.getTransactionManager().getTransaction(player.getUniqueId());
        
        if (transaction != null) {
            transaction.addModifiedSlot(slot);
        }
        
        // Allow all valid interactions:
        // 1. Taking the item (pickup)
        // 2. Placing payment (place)
        // The validation happens when they close the chest
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
    
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
    
        if (!(holder instanceof Container container)) {
            return;
        }
    
        ShopData shop = plugin.getShopManager().getShop(container.getLocation());
        if (shop == null) {
            return;
        }
    
        // Owner can do anything
        if (shop.getOwner().equals(player.getUniqueId())) {
            return;
        }
    
        // Customers cannot drag at all
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        
        if (!(holder instanceof Container container)) {
            return;
        }
        
        ShopData shop = plugin.getShopManager().getShop(container.getLocation());
        if (shop == null) {
            return;
        }
        
        // Owner closed - just normal management
        if (shop.getOwner().equals(player.getUniqueId())) {
            return;
        }
        
        // Customer closed - validate transaction
        TransactionManager.ActiveTransaction transaction = 
            plugin.getTransactionManager().getTransaction(player.getUniqueId());
        
        if (transaction == null) {
            return;
        }
        
        // Remove glow effects before validation
        removeGlowEffects(inv, shop);
        
        // Check if any purchases were made
        if (transaction.getModifiedSlots().isEmpty()) {
            // No slots modified - just browsing
            plugin.getTransactionManager().endTransaction(player.getUniqueId(), true);
            return;
        }
        
        boolean allValid = validateTransaction(shop, transaction, inv, player);
        
        if (allValid) {
            plugin.getConfigManager().sendMessage(player, "purchase-success");
            plugin.getTransactionManager().endTransaction(player.getUniqueId(), true);
        } else {
            plugin.getConfigManager().sendMessage(player, "transaction-failed");
            plugin.getTransactionManager().endTransaction(player.getUniqueId(), false);
        }
    }
    
    // Transaction interruption handlers
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cancel and restore transaction on disconnect
        UUID playerId = event.getPlayer().getUniqueId();
        if (plugin.getTransactionManager().hasActiveTransaction(playerId)) {
            plugin.getTransactionManager().endTransaction(playerId, false);
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
    
        if (plugin.getTransactionManager().hasActiveTransaction(playerId)) {
            plugin.getTransactionManager().endTransaction(playerId, false);
            player.sendMessage("§cTransaction cancelled due to death.");
        }
    }
    
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Cancel and restore transaction if teleported away
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (plugin.getTransactionManager().hasActiveTransaction(playerId)) {
            // Check if they're teleporting away from the shop
            TransactionManager.ActiveTransaction transaction = 
                plugin.getTransactionManager().getTransaction(playerId);
            
            if (transaction != null) {
                // Close their inventory first
                player.closeInventory();
                
                // Transaction will be cancelled on inventory close
                player.sendMessage("§cTransaction cancelled due to teleport.");
            }
        }
    }
    
    private boolean validateTransaction(ShopData shop, TransactionManager.ActiveTransaction transaction, 
                                       Inventory currentInv, Player player) {
        ItemStack[] original = transaction.getChestSnapshot();
        ItemStack[] current = currentInv.getContents();
        
        boolean allValid = true;
        
        // Check each modified slot
        for (int slot : transaction.getModifiedSlots()) {
            ItemStack originalItem = original[slot];
            ItemStack currentItem = current[slot];
            
            // Get shop slot definition
            ShopData.ShopSlot shopSlot = shop.getSlot(slot);
            if (shopSlot == null) {
                allValid = false;
                continue;
            }
            
            // Expected transaction:
            // BEFORE: Slot has the item for sale (originalItem)
            // AFTER: Slot has the payment (currentItem)
            
            if (originalItem == null || originalItem.getType() == Material.AIR) {
                // Slot was empty - customer can't buy nothing
                plugin.getConfigManager().sendMessage(player, "out-of-stock", 
                    Map.of("slot", String.valueOf(slot)));
                allValid = false;
                continue;
            }
            
            if (currentItem == null || currentItem.getType() == Material.AIR) {
                // Customer took item but didn't pay
                player.sendMessage("§cYou took an item from slot " + slot + " but didn't pay!");
                allValid = false;
                continue;
            }
            
            // Validate payment
            if (!shopSlot.isValidPayment(currentItem)) {
                plugin.getConfigManager().sendMessage(player, "invalid-payment",
                    Map.of(
                        "slot", String.valueOf(slot),
                        "amount", String.valueOf(shopSlot.getRequiredAmount()),
                        "item", shopSlot.getRequiredItem().name()
                    ));
                allValid = false;
                continue;
            }
            
            // Valid purchase for this slot!
            player.sendMessage("§a✓ Purchased item from slot " + slot);
        }
        
        return allValid;
    }
    
    /**
     * Add glowing enchantment effect to items that are for sale
     * Only highlights items that are NOT the payment type
     */
    private void highlightShopItems(Inventory inv, ShopData shop) {
        for (int slot : shop.getSlots().keySet()) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                ShopData.ShopSlot shopSlot = shop.getSlot(slot);
                
                // CRITICAL: Don't highlight payment items!
                // If the item IS the payment type, it's money from a previous sale
                if (item.getType() == shopSlot.getRequiredItem() && 
                    item.getAmount() == shopSlot.getRequiredAmount()) {
                    // This is payment, not a shop item - skip highlighting
                    continue;
                }
                
                ItemStack glowingItem = item.clone();
                
                // Add enchantment glow
                org.bukkit.inventory.ItemFlag[] flags = new org.bukkit.inventory.ItemFlag[]{
                    org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS
                };
                
                org.bukkit.inventory.meta.ItemMeta meta = glowingItem.getItemMeta();
                if (meta != null) {
                    // Add a harmless enchantment for glow effect
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                    meta.addItemFlags(flags);
                    
                    // Add lore showing price
                    java.util.List<String> lore = meta.hasLore() ? meta.getLore() : new java.util.ArrayList<>();
                    lore.add("");
                    lore.add("§6§l✦ FOR SALE ✦");
                    lore.add("§7Price: §e" + shopSlot.getRequiredAmount() + "x §f" + 
                            formatMaterialName(shopSlot.getRequiredItem()));
                    meta.setLore(lore);
                    
                    glowingItem.setItemMeta(meta);
                }
                
                inv.setItem(slot, glowingItem);
            }
        }
    }
    
    /**
     * Format material name to be more readable
     */
    private String formatMaterialName(Material material) {
        String name = material.name().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            formatted.append(word.substring(0, 1).toUpperCase())
                     .append(word.substring(1).toLowerCase());
        }
        
        return formatted.toString();
    }
    
    /**
     * Remove glowing effects from items (restore to original)
     */
    private void removeGlowEffects(Inventory inv, ShopData shop) {
        for (int slot : shop.getSlots().keySet()) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                ItemStack cleanItem = item.clone();
                org.bukkit.inventory.meta.ItemMeta meta = cleanItem.getItemMeta();
                
                if (meta != null) {
                    // Remove the lure enchantment
                    meta.removeEnchant(org.bukkit.enchantments.Enchantment.LURE);
                    
                    // Remove the price lore we added
                    if (meta.hasLore()) {
                        java.util.List<String> lore = meta.getLore();
                        // Remove last 3 lines (empty, FOR SALE, Price)
                        if (lore.size() >= 3 && lore.get(lore.size() - 2).contains("FOR SALE")) {
                            lore = lore.subList(0, lore.size() - 3);
                            if (lore.isEmpty()) {
                                meta.setLore(null);
                            } else {
                                meta.setLore(lore);
                            }
                        }
                    }
                    
                    cleanItem.setItemMeta(meta);
                }
                
                inv.setItem(slot, cleanItem);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onContainerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        
        if (type != Material.CHEST && 
            type != Material.BARREL && 
            type != Material.TRAPPED_CHEST) {
            return;
        }
        
        ShopData shop = plugin.getShopManager().getShop(block.getLocation());
        if (shop == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Only owner or admin can break
        if (!shop.getOwner().equals(player.getUniqueId()) && 
            !player.hasPermission("bookshop.admin")) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break someone else's shop!");
            return;
        }
        
        // Remove shop
        plugin.getShopManager().removeShop(block.getLocation());
        plugin.getConfigManager().sendMessage(player, "shop-removed");
    }
}