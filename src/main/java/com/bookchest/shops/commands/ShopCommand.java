package com.bookchest.shops.commands;

import com.bookchest.shops.BookChestShops;
import com.bookchest.shops.utils.BookParser;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {
    
    private final BookChestShops plugin;
    
    public ShopCommand(BookChestShops plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help" -> {
                if (!sender.hasPermission("bookshop.help")) {
                    plugin.getConfigManager().sendMessage(sender, "no-permission");
                    return true;
                }
                sendHelp(sender);
                return true;
            }
            
            case "reload" -> {
                if (!sender.hasPermission("bookshop.admin")) {
                    plugin.getConfigManager().sendMessage(sender, "no-permission");
                    return true;
                }
                
                plugin.reload();
                plugin.getConfigManager().sendRawMessage(sender, 
                    "&aConfiguration reloaded! Shops: " + plugin.getShopManager().getShopCount());
                return true;
            }
            
            case "info" -> {
                if (!sender.hasPermission("bookshop.info")) {
                    plugin.getConfigManager().sendMessage(sender, "no-permission");
                    return true;
                }
                
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command is for players only.");
                    return true;
                }
                
                int totalShops = plugin.getShopManager().getShopCount();
                int playerShops = plugin.getShopManager().getPlayerShopCount(player.getUniqueId());
                
                plugin.getConfigManager().sendRawMessage(sender, "&6=== BookChestShops Info ===");
                plugin.getConfigManager().sendRawMessage(sender, "&eTotal shops: &f" + totalShops);
                plugin.getConfigManager().sendRawMessage(sender, "&eYour shops: &f" + playerShops);
                return true;
            }
            
            case "template" -> {
                if (!sender.hasPermission("bookshop.template")) {
                    plugin.getConfigManager().sendMessage(sender, "no-permission");
                    return true;
                }
                
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command is for players only.");
                    return true;
                }
                
                giveTemplateBook(player);
                return true;
            }
            
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }
    
    private void sendHelp(CommandSender sender) {
        plugin.getConfigManager().sendMessage(sender, "help-header");
        plugin.getConfigManager().sendMessage(sender, "help-create");
        plugin.getConfigManager().sendMessage(sender, "help-items");
        plugin.getConfigManager().sendMessage(sender, "help-book");
        plugin.getConfigManager().sendMessage(sender, "help-format");
        plugin.getConfigManager().sendMessage(sender, "help-example");
        
        sender.sendMessage("");
        plugin.getConfigManager().sendRawMessage(sender, "&e/bookshop template &7- Get a pre-made price book");
        plugin.getConfigManager().sendRawMessage(sender, "&e/bookshop info &7- View shop statistics");
        
        if (sender.hasPermission("bookshop.admin")) {
            plugin.getConfigManager().sendRawMessage(sender, "&e/bookshop reload &7- Reload config");
        }
    }
    
    private void giveTemplateBook(Player player) {
        // Create Book and Quill
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        
        // Set display name
        meta.setDisplayName("§6§lShop Template");
        
        // Generate template content
        String templateContent = generateTemplateContent();
        
        // Split into pages (Book and Quill has page limits)
        List<String> pages = splitIntoPages(templateContent);
        meta.setPages(pages);
        
        book.setItemMeta(meta);
        
        // Give to player
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), book);
            player.sendMessage("§a§l✓ Template Book Created!");
            player.sendMessage("§7Your inventory is full, so the book was dropped.");
        } else {
            player.getInventory().addItem(book);
            player.sendMessage("§a§l✓ Template Book Created!");
        }
        
        player.sendMessage("");
        player.sendMessage("§e§lHow to use:");
        player.sendMessage("§71. Edit the prices in the book");
        player.sendMessage("§72. Rename to §e'Shop' §7in an anvil");
        player.sendMessage("§73. Shift+Left-Click a chest/barrel");
        player.sendMessage("");
        player.sendMessage("§7Format: §eslot amount item");
        player.sendMessage("§7Example: §e0 10 DIAMOND §7(slot 0 costs 10 diamonds)");
    }
    
    private String generateTemplateContent() {
        StringBuilder content = new StringBuilder();
        
        // Page 1: Instructions
        content.append("§6§lShop Template§r\n\n");
        content.append("§7Format:\n");
        content.append("§eslot amount item§r\n\n");
        content.append("§7Example:\n");
        content.append("§e0 10 DIAMOND§r\n");
        content.append("§7= Slot 0 costs\n");
        content.append("§7  10 diamonds\n\n");
        content.append("§7Delete lines you\n");
        content.append("§7don't need →");
        
        content.append("\n---PAGE---\n");
        
        // Pages 2-4: Template slots (9 per page)
        for (int page = 0; page < 3; page++) {
            content.append("§6§lSlots ").append(page * 9).append("-").append((page * 9) + 8).append("§r\n\n");
            
            for (int i = 0; i < 9; i++) {
                int slot = (page * 9) + i;
                
                // Vary the prices for examples
                if (slot < 9) {
                    content.append(slot).append(" 10 DIAMOND\n");
                } else if (slot < 18) {
                    content.append(slot).append(" 5 EMERALD\n");
                } else {
                    content.append(slot).append(" 1 GOLD_INGOT\n");
                }
            }
            
            if (page < 2) {
                content.append("\n---PAGE---\n");
            }
        }
        
        return content.toString();
    }
    
    private List<String> splitIntoPages(String content) {
        String[] rawPages = content.split("\n---PAGE---\n");
        List<String> pages = new ArrayList<>();
        
        for (String page : rawPages) {
            // Each page can hold ~256 characters
            if (page.length() > 256) {
                // Split long pages
                int start = 0;
                while (start < page.length()) {
                    int end = Math.min(start + 256, page.length());
                    pages.add(page.substring(start, end));
                    start = end;
                }
            } else {
                pages.add(page);
            }
        }
        
        return pages;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("bookshop.help")) {
                completions.add("help");
            }
            if (sender.hasPermission("bookshop.info")) {
                completions.add("info");
            }
            if (sender.hasPermission("bookshop.template")) {
                completions.add("template");
            }
            if (sender.hasPermission("bookshop.admin")) {
                completions.add("reload");
            }
        }
        
        return completions;
    }
}