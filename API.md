# Project EXFIL API 文档

## 概述

Project EXFIL 提供了公开的API接口，供外部插件和bot使用。API基于Java标准，易于集成。

## 快速开始

```java
import org.nmo.project_exfil.api.ExfilAPI;

// 获取API实例
ExfilAPI api = ExfilAPI.getInstance();

// 检查玩家是否在游戏中
if (api.isPlayerInGame(player)) {
    // 获取玩家统计数据
    ExfilAPI.PlayerStats stats = api.getPlayerStats(player);
    System.out.println("击杀数: " + stats.kills);
    System.out.println("撤离次数: " + stats.extracts);
}
```

## API方法

### 玩家状态查询

#### `isPlayerInGame(Player player)`
检查玩家是否在游戏中。

**参数:**
- `player` - 玩家对象

**返回:** `boolean` - 是否在游戏中

#### `isPlayerInGame(UUID uuid)`
通过UUID检查玩家是否在游戏中。

**参数:**
- `uuid` - 玩家UUID

**返回:** `boolean` - 是否在游戏中

#### `getPlayerMap(Player player)`
获取玩家当前所在的地图名称。

**参数:**
- `player` - 玩家对象

**返回:** `String` - 地图名称，如果不在游戏中则返回`null`

### 统计数据

#### `getPlayerStats(Player player)`
获取玩家的完整统计数据。

**参数:**
- `player` - 玩家对象

**返回:** `PlayerStats` 对象，包含：
- `kills` - 击杀数
- `deaths` - 死亡数
- `extracts` - 撤离次数
- `totalValue` - 总价值
- `playTime` - 游戏时长（毫秒）

### 任务和成就

#### `getCompletedTasks(Player player)`
获取玩家已完成的任务列表。

**返回:** `Set<String>` - 已完成任务的名称集合

#### `getCompletedAchievements(Player player)`
获取玩家已完成的成就列表。

**返回:** `Set<String>` - 已完成成就的ID集合

### 小队系统

#### `isPlayerInParty(Player player)`
检查玩家是否在小队中。

**返回:** `boolean` - 是否在小队中

#### `getPartyMembers(Player player)`
获取玩家的小队成员列表。

**返回:** `List<UUID>` - 小队成员的UUID列表

### 排行榜

#### `getLeaderboard(LeaderboardType type, int limit)`
获取排行榜数据。

**参数:**
- `type` - 排行榜类型（KILLS, EXTRACTS, VALUE, PLAY_TIME）
- `limit` - 返回的最大条目数

**返回:** `List<LeaderboardEntry>` - 排行榜条目列表

#### `getPlayerRank(Player player, LeaderboardType type)`
获取玩家在排行榜中的排名。

**返回:** `int` - 排名（从1开始），如果未上榜返回-1

### 地图信息

#### `getAvailableMaps()`
获取所有可用地图列表。

**返回:** `List<String>` - 地图显示名称列表

## 数据类

### PlayerStats
玩家统计数据类。

```java
public class PlayerStats {
    public final int kills;        // 击杀数
    public final int deaths;       // 死亡数
    public final int extracts;     // 撤离次数
    public final double totalValue; // 总价值
    public final long playTime;    // 游戏时长（毫秒）
}
```

### LeaderboardEntry
排行榜条目类。

```java
public class LeaderboardEntry {
    public final UUID playerUuid; // 玩家UUID
    public final double value;     // 数值
    public final int rank;         // 排名
}
```

## 使用示例

### 示例1：检查玩家状态
```java
ExfilAPI api = ExfilAPI.getInstance();
Player player = Bukkit.getPlayer("PlayerName");

if (api.isPlayerInGame(player)) {
    String mapName = api.getPlayerMap(player);
    System.out.println("玩家正在 " + mapName + " 地图中");
}
```

### 示例2：获取统计数据
```java
ExfilAPI api = ExfilAPI.getInstance();
ExfilAPI.PlayerStats stats = api.getPlayerStats(player);

double kdr = stats.deaths > 0 ? (double) stats.kills / stats.deaths : stats.kills;
System.out.println("K/D: " + kdr);
```

### 示例3：查询排行榜
```java
ExfilAPI api = ExfilAPI.getInstance();
List<ExfilAPI.LeaderboardEntry> top10 = 
    api.getLeaderboard(LeaderboardManager.LeaderboardType.KILLS, 10);

for (ExfilAPI.LeaderboardEntry entry : top10) {
    Player player = Bukkit.getPlayer(entry.playerUuid);
    System.out.println("#" + entry.rank + " " + player.getName() + 
                       ": " + entry.value + " 击杀");
}
```

## 注意事项

1. API方法可能返回`null`，请在使用前检查
2. 某些方法需要玩家在线才能正常工作
3. API是线程安全的，可以在异步环境中使用
4. 如果插件未启用或功能不可用，某些方法可能返回空集合或默认值

## 版本兼容性

- 最低Minecraft版本: 1.21.8
- 最低Java版本: 21
- 依赖: ProtocolLib 5.4.0+

