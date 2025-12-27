# Project EXFIL 使用手册

> 详细的使用说明和配置指南

## 安装与依赖

### 必需
- **AdvancedSlimePaper (ASP)**: 地图实例化
- **QualityArmory**: 武器系统
- **Citizens + Sentinel**: NPC系统
- **ItemsAdder**: 自定义物品
- **SlimeWorldManager**: 地图管理

### 可选
- PlaceholderAPI, ProtocolLib, DecentHolograms, Parties, Vault/XConomy, TAB, LuckPerms, GSit

## 管理员命令

### 地图管理
- `/exfil importmap <文件名> <显示名称>` - 导入地图（放入 `plugins/Project_EXFIL/import_maps/`）

### 区域设置（需进入地图模板世界）
- `/exfil set spawn <半径>` - 设置出生区域
- `/exfil set extract <名称>` - 设置撤离点（需用WorldEdit框选）
- `/exfil set npc <名称> <数量>` - 设置NPC生成区域（需用WorldEdit框选）
- `/exfil set loot <名称> <数量>` - 设置战利品生成区域（需用WorldEdit框选）

### 战利品管理
- `/exfil loot` - 打开战利品编辑器
- `/exfil loot preset` - 管理战利品预设

### 查看与删除
- `/exfil list maps|extracts|npc|loot` - 查看列表
- `/exfil delete map|extract|npc|loot <名称>` - 删除配置

## 玩家命令

- `/exfil` - 打开主菜单
- `/exfil stash` - 打开仓库
- `/exfil secure` - 打开安全箱

## 配置说明

### config.yml
- `armor-protection.enabled` - 护甲防弹开关
- `armor-protection.durability-damage-multiplier` - 护甲耐久消耗倍率
- `armor-protection.reduction` - 各护甲类型减伤百分比
- `footstep-interval` - 脚步声音间隔

### 战利品预设
- 预设保存在 `loot_presets.yml`
- 可通过GUI创建、编辑、删除预设
- 设置默认预设后，战利品箱将使用该预设生成

## 游戏机制

### 撤离
- 进入撤离点区域开始倒计时（5秒）
- 倒计时期间离开区域会取消
- 倒计时结束后传送到大厅

### 濒死状态
- 生命值降至1.0时进入濒死状态
- 移动速度大幅降低，无法攻击
- 队友可按住潜行键救援（10秒）
- 60秒内未救援将死亡

### 护甲系统
- 穿戴护甲可抵挡子弹伤害
- 不同护甲类型减伤不同（8%-30%）
- 被子弹击中会消耗护甲耐久
- 耐久耗尽护甲会被移除
