package me.herex.rankgift.storage;

import me.herex.rankgift.RankGiftPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileDataStore implements DataStore {
  private final RankGiftPlugin plugin;
  private final File file;
  private YamlConfiguration yml;

  public FileDataStore(RankGiftPlugin plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "data.yml");
    plugin.getDataFolder().mkdirs();
    this.yml = YamlConfiguration.loadConfiguration(file);
  }

  private String path(UUID u, String key) { return "players." + u.toString() + "." + key; }

  @Override public long getInternalGold(UUID uuid) {
    if (uuid == null) return 0L;
    return yml.getLong(path(uuid,"gold"), 0L);
  }

  @Override public void setInternalGold(UUID uuid, long gold) {
    if (uuid == null) return;
    yml.set(path(uuid,"gold"), Math.max(0L, gold));
    save();
  }

  @Override public long getRanksGifted(UUID uuid) {
    if (uuid == null) return 0L;
    return yml.getLong(path(uuid,"ranks_gifted"), 0L);
  }

  @Override public void setRanksGifted(UUID uuid, long ranksGifted) {
    if (uuid == null) return;
    yml.set(path(uuid,"ranks_gifted"), Math.max(0L, ranksGifted));
    save();
  }

  private void save() {
    try { yml.save(file); } catch (IOException e) { plugin.getLogger().warning("Failed to save data.yml: " + e.getMessage()); }
  }

  @Override public void close() { save(); }
}
