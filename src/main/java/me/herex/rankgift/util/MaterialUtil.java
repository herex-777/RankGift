package me.herex.rankgift.util;

import org.bukkit.Material;

public final class MaterialUtil {
  private MaterialUtil() {}

  public static Material matchMaterial(String name) {
    if (name == null) return Material.AIR;
    String n = name.trim().toUpperCase();
    if ("PLAYER_HEAD".equals(n) || "PLAYER_SKULL".equals(n)) return Material.SKULL_ITEM;
    try { return Material.valueOf(n); } catch (Exception ignored) { return Material.AIR; }
  }
}
