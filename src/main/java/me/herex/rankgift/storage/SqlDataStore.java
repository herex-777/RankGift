package me.herex.rankgift.storage;

import me.herex.rankgift.RankGiftPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class SqlDataStore implements DataStore {
  private final RankGiftPlugin plugin;
  private final StorageType type;
  private final String jdbcUrl;
  private final String user;
  private final String pass;

  public SqlDataStore(RankGiftPlugin plugin, StorageType type) {
    this.plugin = plugin;
    this.type = type;

    if (type == StorageType.SQLITE) {
      String fileName = plugin.getConfig().getString("database.sqlite.file", "rankgift.db");
      File dbFile = new File(plugin.getDataFolder(), fileName);
      plugin.getDataFolder().mkdirs();
      this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
      this.user = null;
      this.pass = null;
    } else {
      String host = plugin.getConfig().getString("database.mysql.host", "localhost");
      int port = plugin.getConfig().getInt("database.mysql.port", 3306);
      String database = plugin.getConfig().getString("database.mysql.database", "rankgift");
      this.user = plugin.getConfig().getString("database.mysql.username", "root");
      this.pass = plugin.getConfig().getString("database.mysql.password", "");
      this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
    }

    initTables();
  }

  private Connection conn() throws SQLException {
    if (type == StorageType.SQLITE) return DriverManager.getConnection(jdbcUrl);
    return DriverManager.getConnection(jdbcUrl, user, pass);
  }

  private void initTables() {
    String ddl = "CREATE TABLE IF NOT EXISTS rg_player_data (" +
      "uuid VARCHAR(36) PRIMARY KEY," +
      "gold BIGINT NOT NULL DEFAULT 0," +
      "ranks_gifted BIGINT NOT NULL DEFAULT 0" +
      ")";
    try (Connection c = conn(); Statement st = c.createStatement()) {
      st.executeUpdate(ddl);
    } catch (SQLException e) {
      plugin.getLogger().severe("Failed to init database: " + e.getMessage());
    }
  }

  private long query(UUID u, String col) {
    String sql = "SELECT " + col + " FROM rg_player_data WHERE uuid=?";
    try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, u.toString());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getLong(1);
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("DB read failed: " + e.getMessage());
    }
    return 0L;
  }

  private void upsert(UUID u, long gold, long ranks) {
    String sql;
    if (type == StorageType.MYSQL) {
      sql = "INSERT INTO rg_player_data (uuid,gold,ranks_gifted) VALUES (?,?,?) " +
            "ON DUPLICATE KEY UPDATE gold=VALUES(gold), ranks_gifted=VALUES(ranks_gifted)";
    } else {
      sql = "INSERT INTO rg_player_data (uuid,gold,ranks_gifted) VALUES (?,?,?) " +
            "ON CONFLICT(uuid) DO UPDATE SET gold=excluded.gold, ranks_gifted=excluded.ranks_gifted";
    }
    try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, u.toString());
      ps.setLong(2, Math.max(0L, gold));
      ps.setLong(3, Math.max(0L, ranks));
      ps.executeUpdate();
    } catch (SQLException e) {
      plugin.getLogger().warning("DB write failed: " + e.getMessage());
    }
  }

  @Override public long getInternalGold(UUID uuid) { return uuid==null?0L:query(uuid,"gold"); }
  @Override public void setInternalGold(UUID uuid, long gold) { if(uuid!=null) upsert(uuid,gold,getRanksGifted(uuid)); }
  @Override public long getRanksGifted(UUID uuid) { return uuid==null?0L:query(uuid,"ranks_gifted"); }
  @Override public void setRanksGifted(UUID uuid, long ranksGifted) { if(uuid!=null) upsert(uuid,getInternalGold(uuid),ranksGifted); }
  @Override public void close() {}
}
