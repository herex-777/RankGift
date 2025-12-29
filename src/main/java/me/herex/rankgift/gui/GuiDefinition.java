package me.herex.rankgift.gui;

import java.util.LinkedHashMap;
import java.util.Map;

public class GuiDefinition {
  public final String id;
  public final String title;
  public final int size;
  public final Map<Integer, GuiItemDef> itemsBySlot = new LinkedHashMap<Integer, GuiItemDef>();

  public GuiDefinition(String id, String title, int size) {
    this.id = id;
    this.title = title;
    this.size = size;
  }
}
