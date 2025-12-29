package me.herex.rankgift.gui;

public enum GuiAction {
  OPEN_GUI,
  SELECT_RANK,
  CONFIRM_ACCEPT,
  CONFIRM_DENY,
  RUN_COMMAND,
  CLOSE,
  NONE;

  public static GuiAction fromString(String s) {
    if (s == null) return NONE;
    String n = s.trim().toUpperCase();
    if (n.equals("OPEN_GUI")) return OPEN_GUI;
    if (n.equals("SELECT_RANK") || n.equals("GIFT_RANK")) return SELECT_RANK;
    if (n.equals("CONFIRM_ACCEPT")) return CONFIRM_ACCEPT;
    if (n.equals("CONFIRM_DENY")) return CONFIRM_DENY;
    if (n.equals("RUN_COMMAND")) return RUN_COMMAND;
    if (n.equals("CLOSE")) return CLOSE;
    return NONE;
  }
}
