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

    // Keep ONE connection (much safer for GUIs + prevents spam/crashes)
    private Connection connection;

    // Prevent spam re-init loops
    private volatile boolean triedInit = false;

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
            loadDriverSafe();
        } else {
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "rankgift");
            this.user = plugin.getConfig().getString("database.mysql.username", "root");
            this.pass = plugin.getConfig().getString("database.mysql.password", "");

            // IMPORTANT: safer params
            this.jdbcUrl =
                    "jdbc:mysql://" + host + ":" + port + "/" + database +
                            "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=utf8";

            loadDriverSafe();
        }

        // Connect + create table
        ensureConnected();
        initTables();
    }

    private void loadDriverSafe() {
        try {
            if (type == StorageType.SQLITE) {
                // Works whether shaded/relocated OR not
                try { Class.forName("org.sqlite.JDBC"); }
                catch (ClassNotFoundException ignored) { Class.forName("me.herex.rankgift.libs.sqlite.JDBC"); }
            } else {
                // Old + new driver names
                try { Class.forName("com.mysql.cj.jdbc.Driver"); }
                catch (ClassNotFoundException ignored) { Class.forName("com.mysql.jdbc.Driver"); }
            }
        } catch (Throwable t) {
            plugin.getLogger().severe("[RankGift] DB driver missing for " + type +
                    ". If MySQL: you MUST have mysql-connector on server or shade it into the jar.");
        }
    }

    private synchronized void ensureConnected() {
        try {
            if (connection != null && !connection.isClosed()) return;

            if (type == StorageType.SQLITE) {
                connection = DriverManager.getConnection(jdbcUrl);
                // Safe sqlite settings
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA journal_mode=WAL;");
                    st.execute("PRAGMA synchronous=NORMAL;");
                } catch (Throwable ignored) {}
            } else {
                connection = DriverManager.getConnection(jdbcUrl, user, pass);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[RankGift] Failed to connect to " + type + " database: " + e.getMessage());
        }
    }

    private synchronized void initTables() {
        if (triedInit) return;
        triedInit = true;

        ensureConnected();
        if (connection == null) return;

        String ddl =
                "CREATE TABLE IF NOT EXISTS rg_player_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "gold BIGINT NOT NULL DEFAULT 0," +
                        "ranks_gifted BIGINT NOT NULL DEFAULT 0" +
                        ")";

        try (Statement st = connection.createStatement()) {
            st.executeUpdate(ddl);
        } catch (SQLException e) {
            plugin.getLogger().severe("[RankGift] Failed to init tables: " + e.getMessage());
        }
    }

    private boolean isMissingTable(SQLException e) {
        if (e == null || e.getMessage() == null) return false;
        String msg = e.getMessage().toLowerCase();
        return msg.contains("no such table") || msg.contains("doesn't exist");
    }

    private long query(UUID u, String col) {
        if (u == null) return 0L;
        ensureConnected();
        initTables();

        String sql = "SELECT " + col + " FROM rg_player_data WHERE uuid=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, u.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            // Fix SQLite "no such table" automatically one time
            if (isMissingTable(e)) {
                triedInit = false;
                initTables();
            }
            plugin.getLogger().warning("[RankGift] DB read failed: " + e.getMessage());
        }
        return 0L;
    }

    private void upsert(UUID u, long gold, long ranks) {
        if (u == null) return;
        ensureConnected();
        initTables();

        String sql;
        if (type == StorageType.MYSQL) {
            sql = "INSERT INTO rg_player_data (uuid,gold,ranks_gifted) VALUES (?,?,?) " +
                    "ON DUPLICATE KEY UPDATE gold=VALUES(gold), ranks_gifted=VALUES(ranks_gifted)";
        } else {
            // SQLite 3.24+ (your shaded sqlite-jdbc supports this)
            sql = "INSERT INTO rg_player_data (uuid,gold,ranks_gifted) VALUES (?,?,?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET gold=excluded.gold, ranks_gifted=excluded.ranks_gifted";
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, u.toString());
            ps.setLong(2, Math.max(0L, gold));
            ps.setLong(3, Math.max(0L, ranks));
            ps.executeUpdate();
        } catch (SQLException e) {
            if (isMissingTable(e)) {
                triedInit = false;
                initTables();
            }
            plugin.getLogger().warning("[RankGift] DB write failed: " + e.getMessage());
        }
    }

    @Override
    public long getInternalGold(UUID uuid) {
        return query(uuid, "gold");
    }

    @Override
    public void setInternalGold(UUID uuid, long gold) {
        // Avoid extra DB queries (don’t call getRanksGifted() inside)
        long ranks = getRanksGifted(uuid);
        upsert(uuid, gold, ranks);
    }

    @Override
    public long getRanksGifted(UUID uuid) {
        return query(uuid, "ranks_gifted");
    }

    @Override
    public void setRanksGifted(UUID uuid, long ranksGifted) {
        long gold = getInternalGold(uuid);
        upsert(uuid, gold, ranksGifted);
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }
}
