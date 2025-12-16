package com.bookchest.shops.listeners;

import com.bookchest.shops.BookChestShops;
import com.bookchest.shops.data.ShopData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class ShopInteractionListener implements Listener {
    
    private final BookChestShops plugin;
    
    public ShopInteractionListener(BookChestShops plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onChestInteract(PlayerInteractEvent event) {
        // Only handle main hand to avoid double triggers
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        // Must be right-clicking a block
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Must be sneaking (shift)
        if (!player.isSneaking()) {
            return;
        }
        
        // Must be holding a book and quill
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.WRITABLE_BOOK) {
            return;
        }
        
        // Check if book is renamed to "Shop"
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            player.sendMessage("§cRename your Book and Quill to §e'Shop' §cin an anvil!");
            return;
        }
        
        String bookName = item.getItemMeta().getDisplayName();
        String requiredName = plugin.getConfigManager().getBookName();
        
        // Strip color codes for comparison
        String cleanBookName = org.bukkit.ChatColor.stripColor(bookName);
        
        if (!cleanBookName.equalsIgnoreCase(requiredName)) {
            player.sendMessage("§cBook must be renamed to: §e" + requiredName);
            return;
        }
        
        // Cancel the normal chest opening
        event.setCancelled(true);
        
        Chest chest = (Chest) block.getState();
        
        // Check if already a shop
        if (plugin.getShopManager().isShop(chest.getLocation())) {
            ShopData existingShop = plugin.getShopManager().getShop(chest.getLocation());
            
            // Check ownership
            if (!existingShop.getOwner().equals(player.getUniqueId()) && 
                !player.hasPermission("bookshop.admin")) {
                player.sendMessage("§cThis shop belongs to someone else!");
                return;
            }
            
            // Update existing shop
            boolean updated = updateShop(chest, player, item);
            if (updated) {
                player.sendMessage("§a§lShop Updated!");
                player.sendMessage("§7Your shop prices have been refreshed.");
            } else {
                player.sendMessage("§cInvalid book format! No valid price entries found.");
                player.sendMessage("§7Format: §eSLOT <number> COST <amount> <item>");
            }
            return;
        }
        
        // Check permission to create
        if (!player.hasPermission("bookshop.create")) {
            plugin.getConfigManager().sendMessage(player, "no-permission");
            return;
        }
        
        // Check max shops limit
        int maxShops = plugin.getConfigManager().getMaxShopsPerPlayer();
        if (maxShops > 0) {
            int currentShops = plugin.getShopManager().getPlayerShopCount(player.getUniqueId());
            if (currentShops >= maxShops) {
                player.sendMessage("§cYou've reached the maximum number of shops (" + maxShops + ")!");
                return;
            }
        }
        
        // Create new shop
        boolean created = createShop(chest, player, item);
        
        if (created) {
            player.sendMessage("§a§l§m                                    ");
            player.sendMessage("§a§lSHOP CREATED!");
            player.sendMessage("");
            player.sendMessage("§7Your chest is now a shop!");
            player.sendMessage("§7Players can buy items at your set prices.");
            player.sendMessage("");
            player.sendMessage("§eTo update prices: §fShift + Right-Click with the book again");
            player.sendMessage("§a§l§m                                    ");
        } else {
            player.sendMessage("§c§lShop Creation Failed!");
            player.sendMessage("");
            player.sendMessage("§cInvalid book format! Check your price book.");
            player.sendMessage("§7Format: §eSLOT <number> COST <amount> <item>");
            player.sendMessage("§7Example: §eSLOT 0 COST 5 DIAMOND");
        }
    }
    
    private boolean createShop(Chest chest, Player player, ItemStack bookItem) {
        // Parse shop from book and quill
        ShopData shop = plugin.getShopManager().parseShopFromBookAndQuill(
            chest.getLocation(), 
            player.getUniqueId(), 
            bookItem
        );
        
        if (shop == null || shop.getSlots().isEmpty()) {
            return false;
        }
        
        // Check if chest has items in the defined slots
        ItemStack[] contents = chest.getInventory().getContents();
        boolean hasItems = false;
        
        for (int slotNum : shop.getSlots().keySet()) {
            if (slotNum < contents.length && contents[slotNum] != null && 
                contents[slotNum].getType() != Material.AIR) {
                hasItems = true;
                break;
            }
        }
        
        if (!hasItems) {
            player.sendMessage("§eWarning: §7No items found in your shop slots!");
            player.sendMessage("§7Add items to the chest so players can buy them.");
        }
        
        // Register the shop
        plugin.getShopManager().registerShop(chest.getLocation(), shop);
        return true;
    }
    
    private boolean updateShop(Chest chest, Player player, ItemStack bookItem) {
        // Parse new shop configuration from book and quill
        ShopData shop = plugin.getShopManager().parseShopFromBookAndQuill(
            chest.getLocation(), 
            player.getUniqueId(), 
            bookItem
        );
        
        if (shop == null || shop.getSlots().isEmpty()) {
            return false;
        }
        
        // Update the shop
        plugin.getShopManager().registerShop(chest.getLocation(), shop);
        return true;
    }
}