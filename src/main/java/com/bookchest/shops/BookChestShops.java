package com.bookchest.shops;

import com.bookchest.shops.commands.ShopCommand;
import com.bookchest.shops.listeners.ChestListener;
import com.bookchest.shops.listeners.ProtectionListener;
import com.bookchest.shops.listeners.ShopInteractionListener;
import com.bookchest.shops.managers.ShopManager;
import com.bookchest.shops.managers.TransactionManager;
import com.bookchest.shops.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BookChestShops extends JavaPlugin {
    
    private static BookChestShops instance;
    private ConfigManager configManager;
    private ShopManager shopManager;
    private TransactionManager transactionManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        configManager = new ConfigManager(this);
        shopManager = new ShopManager(this);
        transactionManager = new TransactionManager(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChestListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopInteractionListener(this), this);
        
        // Register commands
        getCommand("bookshop").setExecutor(new ShopCommand(this));
        
        // Load data
        shopManager.loadShops();
        
        getLogger().info("BookChestShops v1.0.0 enabled!");
        getLogger().info("Shops loaded: " + shopManager.getShopCount());
        getLogger().info("Use Shift+Right-Click with a price book to create shops!");
    }
    
    @Override
    public void onDisable() {
        // Save all data
        if (shopManager != null) {
            shopManager.saveShops();
        }
        
        // Clear active transactions
        if (transactionManager != null) {
            transactionManager.clearAll();
        }
        
        getLogger().info("BookChestShops disabled!");
    }
    
    public static BookChestShops getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ShopManager getShopManager() {
        return shopManager;
    }
    
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
    
    public void reload() {
        configManager.reload();
        shopManager.loadShops();
        getLogger().info("Configuration reloaded!");
    }
}