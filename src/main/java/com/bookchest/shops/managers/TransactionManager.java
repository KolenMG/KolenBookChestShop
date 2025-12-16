package com.bookchest.shops.managers;

import com.bookchest.shops.BookChestShops;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TransactionManager {
    
    private final BookChestShops plugin;
    private final Map<UUID, ActiveTransaction> activeTransactions;
    
    public TransactionManager(BookChestShops plugin) {
        this.plugin = plugin;
        this.activeTransactions = new HashMap<>();
    }
    
    public void startTransaction(Player player, Inventory chestInventory) {
        UUID playerId = player.getUniqueId();
        
        // Snapshot both inventories
        ItemStack[] chestSnapshot = snapshotInventory(chestInventory);
        ItemStack[] playerSnapshot = snapshotInventory(player.getInventory());
        
        ActiveTransaction transaction = new ActiveTransaction(
            player,
            chestInventory,
            chestSnapshot,
            playerSnapshot,
            System.currentTimeMillis()
        );
        
        activeTransactions.put(playerId, transaction);
    }
    
    public void endTransaction(UUID playerId, boolean success) {
        ActiveTransaction transaction = activeTransactions.remove(playerId);
        
        if (transaction == null) {
            return;
        }
        
        if (!success) {
            // Restore both inventories
            restoreInventory(transaction.getChestInventory(), transaction.getChestSnapshot());
            restoreInventory(transaction.getPlayer().getInventory(), transaction.getPlayerSnapshot());
            
            // Update player view
            transaction.getPlayer().updateInventory();
        }
    }
    
    public boolean hasActiveTransaction(UUID playerId) {
        return activeTransactions.containsKey(playerId);
    }
    
    public ActiveTransaction getTransaction(UUID playerId) {
        return activeTransactions.get(playerId);
    }
    
    public void clearAll() {
        // Restore all active transactions on shutdown
        for (ActiveTransaction transaction : activeTransactions.values()) {
            restoreInventory(transaction.getChestInventory(), transaction.getChestSnapshot());
            restoreInventory(transaction.getPlayer().getInventory(), transaction.getPlayerSnapshot());
            transaction.getPlayer().updateInventory();
        }
        activeTransactions.clear();
    }
    
    private ItemStack[] snapshotInventory(Inventory inventory) {
        ItemStack[] snapshot = new ItemStack[inventory.getSize()];
        ItemStack[] contents = inventory.getContents();
        
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                snapshot[i] = contents[i].clone();
            }
        }
        
        return snapshot;
    }
    
    private void restoreInventory(Inventory inventory, ItemStack[] snapshot) {
        inventory.clear();
        
        for (int i = 0; i < snapshot.length && i < inventory.getSize(); i++) {
            if (snapshot[i] != null) {
                inventory.setItem(i, snapshot[i].clone());
            }
        }
    }
    
    public static class ActiveTransaction {
        private final Player player;
        private final Inventory chestInventory;
        private final ItemStack[] chestSnapshot;
        private final ItemStack[] playerSnapshot;
        private final long startTime;
        private final Set<Integer> modifiedSlots;
        
        public ActiveTransaction(Player player, Inventory chestInventory, 
                                ItemStack[] chestSnapshot, ItemStack[] playerSnapshot,
                                long startTime) {
            this.player = player;
            this.chestInventory = chestInventory;
            this.chestSnapshot = chestSnapshot;
            this.playerSnapshot = playerSnapshot;
            this.startTime = startTime;
            this.modifiedSlots = new HashSet<>();
        }
        
        public Player getPlayer() {
            return player;
        }
        
        public Inventory getChestInventory() {
            return chestInventory;
        }
        
        public ItemStack[] getChestSnapshot() {
            return chestSnapshot;
        }
        
        public ItemStack[] getPlayerSnapshot() {
            return playerSnapshot;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public Set<Integer> getModifiedSlots() {
            return modifiedSlots;
        }
        
        public void addModifiedSlot(int slot) {
            modifiedSlots.add(slot);
        }
        
        public boolean isSlotModified(int slot) {
            return modifiedSlots.contains(slot);
        }
    }
}