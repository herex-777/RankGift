package me.herex.rankgift.gift;

import java.util.UUID;

public class GiftRequest {
  private final UUID sender;
  private final UUID target;
  private final String rankKey;
  private final String duration;
  private final long cost;
  private final long createdAt;

  public GiftRequest(UUID sender, UUID target, String rankKey, String duration, long cost, long createdAt) {
    this.sender = sender;
    this.target = target;
    this.rankKey = rankKey;
    this.duration = duration;
    this.cost = cost;
    this.createdAt = createdAt;
  }

  public UUID getSender() { return sender; }
  public UUID getTarget() { return target; }
  public String getRankKey() { return rankKey; }
  public String getDuration() { return duration; }
  public long getCost() { return cost; }
  public long getCreatedAt() { return createdAt; }
}
