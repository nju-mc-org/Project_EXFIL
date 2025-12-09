# Project EXFIL 使用文档

Project EXFIL 是一个基于 Minecraft Paper (AdvancedSlimePaper) 的塔科夫/三角洲行动风格的 FPS 撤离类游戏插件。

## 1. 安装与依赖 (Installation)

### 服务端核心
*   **AdvancedSlimePaper (ASP)**: 必须。本插件深度依赖 ASP 的 SlimeWorldManager API 来实现高性能的地图实例化和回滚。

### 依赖插件
*   **PlaceholderAPI**: (必须) 用于变量替换。
*   **FastAsyncWorldEdit (FAWE) / WorldEdit**: (必须) 用于管理员框选区域（撤离点）。
*   **DecentHolograms**: (必须) 用于显示撤离点的倒计时全息图。
*   **Parties**: (推荐) 用于组队系统。如果不安装，组队功能将不可用。
*   **Vault**: (推荐) 用于经济系统显示。
*   **XConomy**: (推荐) 经济插件，本插件已对其进行适配。
*   **TAB**: (推荐) 用于计分板和 Tab 列表显示。
*   **LuckPerms**: (推荐) 用于权限管理。
*   **GSit**: (可选) 玩家交互动作支持。
*   **Citizens**: (可选) NPC 支持。

### 内置库
*   **Inventory Framework**: 用于构建 GUI 界面。

---

## 2. 管理员指南 (Admin Guide)

### 地图管理 (Map Management)

本插件采用“导入-转换-注册”的流程来管理地图。

1.  **准备地图文件**:
    *   支持 **普通世界文件夹** (Vanilla World Folder)。
    *   支持 **ZIP 压缩包** (推荐)。
    *   支持 **.slime 文件**。
2.  **上传**: 将文件放入 `plugins/Project_EXFIL/import_maps/` 文件夹。
3.  **导入命令**:
    *   `/exfil importmap <文件名> <显示名称>`
    *   *示例*: `/exfil importmap KritCity.zip "Krit City"`
    *   *注意*: 插件会自动解压、过滤垃圾文件、将普通地图转换为 Slime 格式，并存入 `slime_worlds` 库中。

### 区域设置 (Region Setup)

在导入地图后，您需要进入该地图的模板世界进行设置。

1.  **进入地图**: 使用 ASP 或 SWM 的命令进入模板世界（通常在 `slime_worlds` 中）。
2.  **设置撤离点 (Extraction Points)**:
    *   使用 WorldEdit 木斧 (`//wand`) 框选撤离区域。
    *   输入 `/exfil set extract <名称>` 保存。
    *   *提示*: 撤离点对所有以此地图为模板的游戏实例生效。
3.  **设置出生区域 (Spawn Region)**:
    *   站在您希望作为出生中心的位置。
    *   输入 `/exfil set spawn <半径>` (例如 200)。
    *   *机制*: 游戏开始时，系统会在该半径内随机寻找一个**安全、露天、且远离撤离点和其他玩家**的位置部署玩家。

### 查看与删除

*   **查看列表**:
    *   `/exfil list maps`: 查看所有已注册的地图。
    *   `/exfil list extracts`: 查看所有撤离点。
*   **删除数据**:
    *   `/exfil delete map <ID>`: 删除地图配置及对应的 Slime 世界文件（保留 import_maps 中的源文件）。
    *   `/exfil delete extract <名称>`: 删除指定的撤离点。

---

## 3. 游戏机制 (Game Mechanics)

*   **部署 (Deploy)**: 玩家通过 `/exfil` 菜单选择地图匹配。系统会创建独立的临时游戏实例。
*   **出生点 (Spawning)**: 智能算法确保玩家出生在固体方块上，且头顶无遮挡（室外），并自动避开危险区域（岩浆、水等）。
*   **撤离 (Extraction)**: 玩家到达撤离点后，会看到个人专属的倒计时全息图。倒计时结束即可携带战利品撤离。
*   **死亡与清理 (Death & Cleanup)**:
    *   玩家死亡或掉线会立即被移除出游戏实例。
    *   当实例内无玩家时，系统会在短暂延迟后自动卸载并删除该临时世界，释放服务器资源。

---

## 4. 命令速查 (Commands)

### 玩家命令
| 命令 | 描述 |
| :--- | :--- |
| `/exfil` | 打开游戏主菜单 (部署/组队/仓库) |
| `/exfil stash` | 打开个人仓库 (Stash) |

### 管理员命令 (权限: `exfil.admin`)
| 命令 | 描述 |
| :--- | :--- |
| `/exfil importmap <文件> <名称>` | 导入地图 (支持 zip/文件夹) |
| `/exfil set extract <名称>` | 将 WE 选区设为撤离点 |
| `/exfil set spawn <半径>` | 设置随机出生范围 |
| `/exfil list maps` | 列出所有地图 |
| `/exfil list extracts` | 列出所有撤离点 |
| `/exfil delete map <ID>` | 删除地图 |
| `/exfil delete extract <名称>` | 删除撤离点 |
