package me.herex.rankgift.gift;

import java.util.List;

public class RankDef {
  private final String key;
  private final String displayName;
  private final List<String> commands;

  public RankDef(String key, String displayName, List<String> commands) {
    this.key = key;
    this.displayName = displayName;
    this.commands = commands;
  }
  public String getKey() { return key; }
  public String getDisplayName() { return displayName; }
  public List<String> getCommands() { return commands; }
}
