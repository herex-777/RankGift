package me.herex.rankgift.gui;

import me.herex.rankgift.RankGiftPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public final class GuiConfigLoader {
  private GuiConfigLoader() {}

  public static Map<String, GuiDefinition> load(RankGiftPlugin plugin) {
    Map<String, GuiDefinition> out = new HashMap<String, GuiDefinition>();
    ConfigurationSection root = plugin.getConfig().getConfigurationSection("guis");
    if (root == null) return out;

    for (String guiId : root.getKeys(false)) {
      ConfigurationSection gs = root.getConfigurationSection(guiId);
      if (gs == null) continue;
      String title = gs.getString("title", guiId);
      int size = gs.getInt("size", 54);
      GuiDefinition def = new GuiDefinition(guiId.toLowerCase(Locale.ENGLISH), title, size);

      ConfigurationSection items = gs.getConfigurationSection("items");
      if (items != null) {
        for (String itemKey : items.getKeys(false)) {
          ConfigurationSection is = items.getConfigurationSection(itemKey);
          if (is == null) continue;
          GuiItemDef it = new GuiItemDef();
          it.key = itemKey;
          it.slot = is.getInt("slot", -1);
          it.material = is.getString("material", "STONE");
          it.data = is.getInt("data", 0);
          it.displayName = is.getString("display-name", "");
          it.lore = is.getStringList("lore");
          it.enchanted = is.getBoolean("enchanted", false);
          it.skull = is.getString("skull", null);
          it.texture = is.getString("texture", null);

          it.action = GuiAction.fromString(is.getString("action", "none"));
          it.opens = is.getString("opens", null);
          it.rank = is.getString("rank", null);
          it.cost = is.getLong("cost", 0L);
          it.duration = is.getString("duration", null);
          it.as = is.getString("as", "CONSOLE");
          it.commands = is.getStringList("commands");

          if (it.slot >= 0 && it.slot < size) {
            def.itemsBySlot.put(it.slot, it);
          }
        }
      }
      out.put(def.id, def);
    }
    return out;
  }
}
