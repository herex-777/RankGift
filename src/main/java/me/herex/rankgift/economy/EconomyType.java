package me.herex.rankgift.economy;

public enum EconomyType {
  INTERNAL,
  PLACEHOLDER,
  COMMAND,
  PLACEHOLDER_COMMAND;

  public static EconomyType fromString(String s) {
    if (s == null) return PLACEHOLDER_COMMAND;
    String n = s.trim().toUpperCase().replace("-", "_");
    if ("PLACEHOLDER+COMMAND".equalsIgnoreCase(s)) return PLACEHOLDER_COMMAND;
    try { return EconomyType.valueOf(n); } catch (Exception ignored) { return PLACEHOLDER_COMMAND; }
  }
}
