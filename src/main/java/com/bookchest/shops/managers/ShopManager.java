package com.bookchest.shops.managers;

import com.bookchest.shops.BookChestShops;
import com.bookchest.shops.data.ShopData;
import com.bookchest.shops.utils.BookParser;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopManager {
    
    private final BookChestShops plugin;
    private final Map<Location, ShopData> shops;
    private final File dataFile;
    
    public ShopManager(BookChestShops plugin) {
        this.plugin = plugin;
        this.shops = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "shops.yml");
    }
    
    public boolean isShop(Location location) {
        return shops.containsKey(location);
    }
    
    public ShopData getShop(Location location) {
        return shops.get(location);
    }
    
    /**
     * Register a shop (new or update existing)
     */
    public void registerShop(Location location, ShopData shop) {
        shops.put(location, shop);
        saveShops();
    }
    
    /**
     * Parse shop from a Book and Quill (WRITABLE_BOOK)
     */
    public ShopData parseShopFromBookAndQuill(Location location, UUID owner, ItemStack bookItem) {
        if (bookItem.getType() != Material.WRITABLE_BOOK) {
            return null;
        }
        
        BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();
        if (bookMeta == null) {
            return null;
        }
        
        Map<Integer, ShopData.ShopSlot> parsedSlots = BookParser.parseBook(bookMeta);
        
        if (parsedSlots.isEmpty()) {
            return null;
        }
        
        // Create shop data
        ShopData shop = new ShopData(location, owner);
        for (Map.Entry<Integer, ShopData.ShopSlot> entry : parsedSlots.entrySet()) {
            shop.addSlot(entry.getKey(), 
                        entry.getValue().getRequiredItem(), 
                        entry.getValue().getRequiredAmount());
        }
        
        return shop;
    }
    
    /**
     * Parse shop from a book (without needing the book to be in the chest)
     */
    public ShopData parseShopFromBook(Location location, UUID owner, BookMeta bookMeta) {
        Map<Integer, ShopData.ShopSlot> parsedSlots = BookParser.parseBook(bookMeta);
        
        if (parsedSlots.isEmpty()) {
            return null;
        }
        
        // Create shop data
        ShopData shop = new ShopData(location, owner);
        for (Map.Entry<Integer, ShopData.ShopSlot> entry : parsedSlots.entrySet()) {
            shop.addSlot(entry.getKey(), 
                        entry.getValue().getRequiredItem(), 
                        entry.getValue().getRequiredAmount());
        }
        
        return shop;
    }
    
    /**
     * Legacy method: Create shop by detecting book in chest
     * This is now deprecated in favor of Shift+Right-Click
     */
    @Deprecated
    public boolean createShop(Chest chest, UUID owner) {
        Location loc = chest.getLocation();
        
        // Parse shop from chest inventory
        ShopData shop = parseShopFromChest(chest, owner);
        if (shop == null) {
            return false;
        }
        
        shops.put(loc, shop);
        saveShops();
        return true;
    }
    
    public void removeShop(Location location) {
        shops.remove(location);
        saveShops();
    }
    
    /**
     * Legacy method: Parse shop by finding book inside chest
     */
    @Deprecated
    public ShopData parseShopFromChest(Chest chest, UUID owner) {
        Inventory inv = chest.getInventory();
        ItemStack bookItem = null;
        
        // Find written book
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == Material.WRITTEN_BOOK) {
                BookMeta meta = (BookMeta) item.getItemMeta();
                String title = meta.getTitle();
                String requiredTitle = plugin.getConfigManager().getBookTitle();
                
                if (title != null && title.equalsIgnoreCase(requiredTitle)) {
                    bookItem = item;
                    break;
                }
            }
        }
        
        // No valid book found
        if (bookItem == null) {
            return null;
        }
        
        // Parse book
        BookMeta meta = (BookMeta) bookItem.getItemMeta();
        return parseShopFromBook(chest.getLocation(), owner, meta);
    }
    
    public int getPlayerShopCount(UUID player) {
        return (int) shops.values().stream()
            .filter(shop -> shop.getOwner().equals(player))
            .count();
    }
    
    public int getShopCount() {
        return shops.size();
    }
    
    public void saveShops() {
        YamlConfiguration config = new YamlConfiguration();
        
        int index = 0;
        for (Map.Entry<Location, ShopData> entry : shops.entrySet()) {
            Location loc = entry.getKey();
            ShopData shop = entry.getValue();
            
            String path = "shops." + index;
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".owner", shop.getOwner().toString());
            config.set(path + ".created", shop.getCreatedAt());
            
            // Save slots
            int slotIndex = 0;
            for (ShopData.ShopSlot slot : shop.getSlots().values()) {
                String slotPath = path + ".slots." + slotIndex;
                config.set(slotPath + ".slot", slot.getSlot());
                config.set(slotPath + ".item", slot.getRequiredItem().name());
                config.set(slotPath + ".amount", slot.getRequiredAmount());
                slotIndex++;
            }
            
            index++;
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shops: " + e.getMessage());
        }
    }
    
    public void loadShops() {
        shops.clear();
        
        if (!dataFile.exists()) {
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        
        if (!config.contains("shops")) {
            return;
        }
        
        for (String key : config.getConfigurationSection("shops").getKeys(false)) {
            String path = "shops." + key;
            
            try {
                String worldName = config.getString(path + ".world");
                int x = config.getInt(path + ".x");
                int y = config.getInt(path + ".y");
                int z = config.getInt(path + ".z");
                UUID owner = UUID.fromString(config.getString(path + ".owner"));
                
                Location loc = new Location(plugin.getServer().getWorld(worldName), x, y, z);
                ShopData shop = new ShopData(loc, owner);
                
                // Load slots
                if (config.contains(path + ".slots")) {
                    for (String slotKey : config.getConfigurationSection(path + ".slots").getKeys(false)) {
                        String slotPath = path + ".slots." + slotKey;
                        int slot = config.getInt(slotPath + ".slot");
                        Material item = Material.valueOf(config.getString(slotPath + ".item"));
                        int amount = config.getInt(slotPath + ".amount");
                        
                        shop.addSlot(slot, item, amount);
                    }
                }
                
                shops.put(loc, shop);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load shop at " + path + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + shops.size() + " shops");
    }
}