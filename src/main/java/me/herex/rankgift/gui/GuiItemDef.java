package me.herex.rankgift.gui;

import java.util.List;

public class GuiItemDef {
  public String key;
  public int slot;
  public String material;
  public int data;
  public String displayName;
  public List<String> lore;
  public boolean enchanted;

  public String skull;     // owner
  public String texture;   // base64

  public GuiAction action;
  public String opens;     // for open_gui
  public String rank;      // for select_rank
  public long cost;        // for select_rank
  public String duration;  // for select_rank

  public String as;        // PLAYER/CONSOLE for run_command
  public List<String> commands;
}
