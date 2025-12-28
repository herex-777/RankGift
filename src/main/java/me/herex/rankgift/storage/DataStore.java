package me.herex.rankgift.storage;

import java.util.UUID;

public interface DataStore {
  long getInternalGold(UUID uuid);
  void setInternalGold(UUID uuid, long gold);
  long getRanksGifted(UUID uuid);
  void setRanksGifted(UUID uuid, long ranksGifted);
  void close();
}
