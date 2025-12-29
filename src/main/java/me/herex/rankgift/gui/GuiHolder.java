package me.herex.rankgift.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiHolder implements InventoryHolder {
  public final String guiId;
  public final GuiDefinition def;

  public GuiHolder(String guiId, GuiDefinition def) {
    this.guiId = guiId;
    this.def = def;
  }

  @Override public Inventory getInventory() { return null; }
}
