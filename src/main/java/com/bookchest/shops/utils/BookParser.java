package com.bookchest.shops.utils;

import com.bookchest.shops.data.ShopData;
import org.bukkit.Material;
import org.bukkit.inventory.meta.BookMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookParser {
    
    // New simplified pattern: slot amount item
    // Example: 0 5 DIAMOND or 27 1 EMERALD
    private static final Pattern SIMPLE_PATTERN = Pattern.compile(
        "^\\s*(\\d+)\\s+(\\d+)\\s+([A-Z_0-9]+)\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    
    // Legacy pattern for backwards compatibility: SLOT <number> COST <amount> <item>
    private static final Pattern LEGACY_PATTERN = Pattern.compile(
        "(?i)SLOT\\s+(\\d+)\\s+COST\\s+(\\d+)\\s+([A-Z_0-9]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    public static Map<Integer, ShopData.ShopSlot> parseBook(BookMeta bookMeta) {
        Map<Integer, ShopData.ShopSlot> slots = new HashMap<>();
        
        // Get all pages
        List<String> pages = bookMeta.getPages();
        
        for (String pageText : pages) {
            // Split into lines
            String[] lines = pageText.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    // Skip empty lines and comments
                    continue;
                }
                
                // Try simple format first (preferred)
                Matcher simpleMatcher = SIMPLE_PATTERN.matcher(line);
                if (simpleMatcher.matches()) {
                    try {
                        int slotNum = Integer.parseInt(simpleMatcher.group(1));
                        int amount = Integer.parseInt(simpleMatcher.group(2));
                        String itemName = simpleMatcher.group(3).toUpperCase();
                        
                        ShopData.ShopSlot slot = createSlot(slotNum, amount, itemName);
                        if (slot != null) {
                            slots.put(slotNum, slot);
                        }
                        
                    } catch (NumberFormatException e) {
                        // Invalid number format, skip line
                    }
                    continue;
                }
                
                // Try legacy format for backwards compatibility
                Matcher legacyMatcher = LEGACY_PATTERN.matcher(line);
                if (legacyMatcher.find()) {
                    try {
                        int slotNum = Integer.parseInt(legacyMatcher.group(1));
                        int amount = Integer.parseInt(legacyMatcher.group(2));
                        String itemName = legacyMatcher.group(3).toUpperCase();
                        
                        ShopData.ShopSlot slot = createSlot(slotNum, amount, itemName);
                        if (slot != null) {
                            slots.put(slotNum, slot);
                        }
                        
                    } catch (NumberFormatException e) {
                        // Invalid number format, skip line
                    }
                }
            }
        }
        
        return slots;
    }
    
    private static ShopData.ShopSlot createSlot(int slotNum, int amount, String itemName) {
        // Validate slot number (0-53 for double chest, 0-26 for single chest/barrel)
        if (slotNum < 0 || slotNum > 53) {
            return null;
        }
        
        // Validate amount
        if (amount <= 0 || amount > 64) {
            return null;
        }
        
        // Parse material - supports both vanilla and custom items
        Material material = parseMaterial(itemName);
        if (material == null) {
            return null;
        }
        
        return new ShopData.ShopSlot(slotNum, material, amount);
    }
    
    /**
     * Parse material name - supports custom items via resource packs
     * Custom items are identified by their Material type (e.g., PAPER, STICK)
     * The CustomModelData is preserved in the actual ItemStack
     */
    private static Material parseMaterial(String itemName) {
        try {
            // Try standard Material enum
            return Material.valueOf(itemName);
        } catch (IllegalArgumentException e) {
            // Not a standard material
            // For custom items, players should use the base material
            // Example: CUSTOM_SWORD -> Use DIAMOND_SWORD with CustomModelData
            // The custom model data is preserved in the actual item
            return null;
        }
    }
    
    /**
     * Generate a template book with all 27 slots pre-filled
     */
    public static String generateTemplate() {
        StringBuilder template = new StringBuilder();
        template.append("§6Shop Price Template\n");
        template.append("§7Edit prices below:\n");
        template.append("§7Format: slot amount item\n");
        template.append("\n");
        template.append("§e# Single Chest (27 slots)\n");
        template.append("§7# Delete lines you don't need\n");
        template.append("\n");
        
        // Generate 27 slots with example prices
        for (int i = 0; i < 27; i++) {
            if (i < 9) {
                // Top row - expensive items
                template.append(i).append(" 10 DIAMOND\n");
            } else if (i < 18) {
                // Middle row - medium items
                template.append(i).append(" 5 EMERALD\n");
            } else {
                // Bottom row - cheap items
                template.append(i).append(" 1 GOLD_INGOT\n");
            }
        }
        
        return template.toString();
    }
    
    public static boolean isValidBookFormat(BookMeta bookMeta) {
        if (bookMeta == null) {
            return false;
        }
        
        List<String> pages = bookMeta.getPages();
        if (pages.isEmpty()) {
            return false;
        }
        
        // Check if at least one valid line exists
        for (String pageText : pages) {
            String[] lines = pageText.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (SIMPLE_PATTERN.matcher(line).matches() || 
                    LEGACY_PATTERN.matcher(line).find()) {
                    return true;
                }
            }
        }
        
        return false;
    }
}