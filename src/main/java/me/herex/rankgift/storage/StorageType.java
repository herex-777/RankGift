package me.herex.rankgift.storage;

public enum StorageType { FILE, SQLITE, MYSQL;
  public static StorageType fromString(String s) {
    if (s == null) return FILE;
    String n = s.trim().toUpperCase();
    if ("SQLITE".equals(n)) return SQLITE;
    if ("MYSQL".equals(n)) return MYSQL;
    return FILE;
  }
}
