package com.bookchest.shops.listeners;

import com.bookchest.shops.BookChestShops;
import com.bookchest.shops.data.ShopData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

public class ProtectionListener implements Listener {
    
    private final BookChestShops plugin;
    
    public ProtectionListener(BookChestShops plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("protection.block-hoppers", true)) {
            return;
        }
        
        Location sourceLoc = event.getSource().getLocation();
        Location destLoc = event.getDestination().getLocation();
        
        if (sourceLoc != null && plugin.getShopManager().isShop(sourceLoc)) {
            event.setCancelled(true);
            return;
        }
        
        if (destLoc != null && plugin.getShopManager().isShop(destLoc)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("protection.block-pistons", true)) {
            return;
        }
        
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.CHEST) {
                if (plugin.getShopManager().isShop(block.getLocation())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("protection.block-pistons", true)) {
            return;
        }
        
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.CHEST) {
                if (plugin.getShopManager().isShop(block.getLocation())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("protection.block-explosions", true)) {
            return;
        }
        
        event.blockList().removeIf(block -> {
            if (block.getType() == Material.CHEST) {
                return plugin.getShopManager().isShop(block.getLocation());
            }
            return false;
        });
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("protection.block-explosions", true)) {
            return;
        }
        
        event.blockList().removeIf(block -> {
            if (block.getType() == Material.CHEST) {
                return plugin.getShopManager().isShop(block.getLocation());
            }
            return false;
        });
    }
}