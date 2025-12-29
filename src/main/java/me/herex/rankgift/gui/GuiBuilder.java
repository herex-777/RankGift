package me.herex.rankgift.gui;

import me.herex.rankgift.RankGiftPlugin;
import me.herex.rankgift.util.MaterialUtil;
import me.herex.rankgift.util.SkullUtil;
import me.herex.rankgift.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class GuiBuilder {
    private GuiBuilder() {}

    public static Inventory build(RankGiftPlugin plugin, GuiManager gm, Player viewer, GuiDefinition def) {
        GuiHolder holder = new GuiHolder(def.id, def);
        me.herex.rankgift.gui.GuiManager.PendingSelection pending = "confirm".equalsIgnoreCase(def.id) ? gm.getPending(viewer) : null;
        Inventory inv = Bukkit.createInventory(holder, def.size, Text.color(plugin.applyTokens(viewer, def.title, null, pending)));
        for (GuiItemDef item : def.itemsBySlot.values()) {
            ItemStack stack = buildItem(plugin, gm, viewer, item, pending);
            inv.setItem(item.slot, stack);
        }
        return inv;
    }

    private static ItemStack buildItem(RankGiftPlugin plugin, GuiManager gm, Player viewer, GuiItemDef item, me.herex.rankgift.gui.GuiManager.PendingSelection pending) {
        String targetName = gm.getTargetName(viewer);
        Player target = gm.getTargetPlayer(viewer);

        Material mat = MaterialUtil.matchMaterial(item.material);
        ItemStack stack;

        boolean wantsSkull = (mat == Material.SKULL_ITEM) || "PLAYER_HEAD".equalsIgnoreCase(item.material);
        if (wantsSkull) {
            String owner = token(plugin, viewer, item.skull, targetName);
            String texture = item.texture;
            if (texture != null && texture.trim().length() == 0) texture = null;
            // For player head entries in rank_select, use target skin if skull is {target_name}
            if (owner != null && owner.contains("{target_name}") && target != null) owner = target.getName();
            stack = SkullUtil.makeSkull(owner, texture);
        } else {
            stack = new ItemStack(mat == null ? Material.AIR : mat, 1, (short) item.data);
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = token(plugin, viewer, item.displayName, targetName);
            meta.setDisplayName(Text.color(plugin.applyTokens(viewer, name, item, pending)));

            List<String> loreOut = new ArrayList<String>();
            if (item.lore != null) {
                for (String line : item.lore) {
                    loreOut.add(Text.color(plugin.applyTokens(viewer, token(plugin, viewer, line, targetName), item, pending)));
                }
            }
            meta.setLore(loreOut);
            stack.setItemMeta(meta);
        }

        if (item.enchanted) {
            stack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            ItemMeta m = stack.getItemMeta();
            try { m.addItemFlags(ItemFlag.HIDE_ENCHANTS); } catch (Throwable ignored) {}
            stack.setItemMeta(m);
        }
        return stack;
    }

    private static String token(RankGiftPlugin plugin, Player viewer, String s, String targetName) {
        if (s == null) return "";
        return s.replace("{target_name}", targetName == null ? "" : targetName)
                .replace("{sender_name}", viewer == null ? "" : viewer.getName());
    }
}