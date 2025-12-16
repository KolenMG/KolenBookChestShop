package com.bookchest.shops.data;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ShopData {
    
    private final Location location;
    private final UUID owner;
    private final Map<Integer, ShopSlot> slots;
    private final long createdAt;
    
    public ShopData(Location location, UUID owner) {
        this.location = location;
        this.owner = owner;
        this.slots = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }
    
    public Location getLocation() {
        return location;
    }
    
    public UUID getOwner() {
        return owner;
    }
    
    public Map<Integer, ShopSlot> getSlots() {
        return slots;
    }
    
    public void addSlot(int slot, Material item, int amount) {
        slots.put(slot, new ShopSlot(slot, item, amount));
    }
    
    public void removeSlot(int slot) {
        slots.remove(slot);
    }
    
    public ShopSlot getSlot(int slot) {
        return slots.get(slot);
    }
    
    public boolean hasSlot(int slot) {
        return slots.containsKey(slot);
    }
    
    public void clearSlots() {
        slots.clear();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public static class ShopSlot {
        private final int slot;
        private final Material requiredItem;
        private final int requiredAmount;
        
        public ShopSlot(int slot, Material requiredItem, int requiredAmount) {
            this.slot = slot;
            this.requiredItem = requiredItem;
            this.requiredAmount = requiredAmount;
        }
        
        public int getSlot() {
            return slot;
        }
        
        public Material getRequiredItem() {
            return requiredItem;
        }
        
        public int getRequiredAmount() {
            return requiredAmount;
        }
        
        public boolean isValidPayment(ItemStack item) {
            if (item == null || item.getType() != requiredItem) {
                return false;
            }
            return item.getAmount() == requiredAmount;
        }
    }
}