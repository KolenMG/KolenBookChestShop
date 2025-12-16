package com.bookchest.shops.utils;

import com.bookchest.shops.BookChestShops;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class ConfigManager {
    
    private final BookChestShops plugin;
    private FileConfiguration config;
    
    public ConfigManager(BookChestShops plugin) {
        this.plugin = plugin;
        reload();
    }
    
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public String getBookName() {
        return config.getString("shop.book-name", "Shop");
    }
    
    // Legacy method for backwards compatibility
    @Deprecated
    public String getBookTitle() {
        return getBookName();
    }
    
    public int getMaxShopsPerPlayer() {
        return config.getInt("shop.max-per-player", -1);
    }
    
    public boolean allowDoubleChests() {
        return config.getBoolean("shop.allow-double-chests", false);
    }
    
    public void sendMessage(CommandSender sender, String messageKey) {
        sendMessage(sender, messageKey, Map.of());
    }
    
    public void sendMessage(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        String prefix = config.getString("messages.prefix", "&8[&6BookShop&8]&r ");
        String message = config.getString("messages." + messageKey, "Message not found: " + messageKey);
        
        // Replace placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        // Parse color codes
        String fullMessage = prefix + message;
        fullMessage = fullMessage.replace("&", "§");
        
        // Send as legacy text (Bedrock compatible)
        sender.sendMessage(fullMessage);
    }
    
    public void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(message.replace("&", "§"));
    }
}