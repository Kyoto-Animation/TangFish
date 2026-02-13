# 🎏 TangFish - 极致高性能的自定义钓鱼插件

TangFish 是一款专为追求极致性能的服务器主设计的 Minecraft 钓鱼增强插件。它打破了版本间的隔阂，让你在 1.12 到 1.21 的任何服务器上都能轻松配置个性化的钓鱼奖池。

## ✨ 核心特性

- 🌍 **全版本兼容**：采用 XSeries 技术，一套代码完美适配 1.12.2 - 1.21，告别材质名报错。
- ⚡ **极致性能**：纯 Kotlin 编写，拒绝昂贵的反射操作，核心算法采用 ThreadLocalRandom。
- ⚖️ **精准权重**：基于权重的区间抽奖算法，即便有 100 种掉落物也能秒速计算。
- 🎨 **颜色增强**：完美支持旧版 `&` 颜色代码及 1.16+ 的 Hex 十六进制颜色（`&#RRGGBB`）。
- 🛠️ **开发者友好**：提供详细的 Debug 日志，配置重载无需重启服务器。

## 📊 兼容性概览

| 服务端类型 | 支持版本 | 状态 |
|:----------:|:--------:|:----:|
| Bukkit | 1.12.2 - 1.21 | ✅ |
| Spigot | 1.12.2 - 1.21 | ✅ |
| Paper | 1.12.2 - 1.21 | ✅ |
| Purpur | 1.12.2 - 1.21 | ✅ |

## � 项目框架指南

### 源码结构 (`src/main/kotlin`)

```
com.kyotoanimation.tangfish
│
├── TangFish.kt          # 核心入口 - 负责插件生命周期管理（启动、关闭、重载）
│
├── config/              # 配置中心
│   ├── ConfigManager    # 加载掉落表，管理奖池数据
│   └── LanguageManager  # 处理多语言映射，支持国际化
│
├── listeners/           # 事件枢纽
│   └── FishingListener  # 拦截原版钓鱼事件，插件的逻辑起点
│
├── models/              # 数据结构
│   └── FishingDrop      # 掉落物模型，封装跨版本 XMaterial 转换逻辑
│
├── commands/            # 指令系统
│   └── MainCommand      # 处理 /tangfish 及其子命令
│
└── utils/               # 工具包
    ├── RandomUtils      # 高性能权重随机算法
    └── Extensions       # 字符串颜色处理（支持 Hex 颜色）
```

### 资源结构 (`src/main/resources`)

| 文件 | 描述 |
|:-----|:-----|
| `plugin.yml` | 插件的身份证 - 定义名称、版本、主类、命令、权限 |
| `config.yml` | 主配置文件 - 语言设置、调试模式、奖池配置 |
| `languages/` | 多语言存储库 - 默认包含 `zh_CN.yml` 和 `en_US.yml` |

## 🚀 快速开始

### 安装

1. 下载最新的 `TangFish.jar` 文件
2. 将文件放入服务器的 `plugins` 目录
3. 重启服务器，插件会自动生成配置文件

### 构建源码

```bash
git clone https://github.com/yourusername/TangFish.git
cd TangFish
gradle shadowJar
```

构建产物位于 `build/libs/TangFish.jar`

## ⚙️ 配置文件

### 主配置 (`config.yml`)

```yaml
# 语言设置 (zh_CN / en_US)
language: zh_CN

# 调试模式
debug: false

# 掉落物奖池配置
drops:
  common_fish:
    id: COD
    chance: 50.0
    amount: 1
    display-name: "&f普通鱼"
    lore:
      - "&7一条普通的鱼"
  rare_fish:
    id: SALMON
    chance: 30.0
    amount: 1
    display-name: "&b稀有鲑鱼"
    lore:
      - "&7闪闪发光的鲑鱼"
  legendary_fish:
    id: PUFFERFISH
    chance: 15.0
    amount: 1
    display-name: "&#FFD700传说河豚"
    lore:
      - "&#FF5555极其罕见的河豚"
      - "&#55FF55据说能带来好运"
  treasure:
    id: DIAMOND
    chance: 5.0
    amount: 1
    display-name: "&#55FFFF钻石宝藏"
```

## 📝 指令说明

| 指令 | 描述 | 权限 |
|:-----|:-----|:-----|
| `/tangfish` 或 `/tf` | 主指令 | - |
| `/tf reload` | 重载配置文件 | `tangfish.admin` |
| `/tf list` / `/tf pool` | 查看奖池列表 | `tangfish.user` |
| `/tf help` | 显示帮助菜单 | - |

### 权限节点

| 权限 | 描述 | 默认 |
|:-----|:-----|:-----|
| `tangfish.admin` | 管理员权限 | OP |
| `tangfish.user` | 用户权限 | 所有人 |

## 🔧 技术规格

| 项目 | 详情 |
|:-----|:-----|
| 编程语言 | Kotlin 1.9.x |
| 编译目标 | Java 8 |
| 核心依赖 | XSeries 9.4.0 |
| 构建工具 | Gradle + ShadowJar |

## 🎯 性能优化亮点

- **ThreadLocalRandom**：多线程环境下无锁竞争，性能远超传统 `Random`
- **材质缓存**：XMaterial 解析结果缓存，避免重复字符串匹配
- **延迟初始化**：Kotlin `lazy` 委托，按需加载减少启动时间
- **事件过滤**：`ignoreCancelled = true` 跳过已取消事件，减少无效处理

## 🌐 国际化支持

默认提供中英文语言文件：

- `zh_CN.yml` - 简体中文
- `en_US.yml` - English

可在 `plugins/TangFish/languages/` 目录下自定义语言文件。

## 🤝 致开发者

TangFish 采用**非反射式开发**，我们欢迎所有追求性能的开发者提交 PR。

如果你发现了 1.12 与 1.21 之间新的兼容性问题，请务必开启一个 Issue 告诉我们。

### 贡献指南

- 🐛 **Bug 报告**：请提供详细的复现步骤和服务器版本信息
- 💡 **功能建议**：欢迎提出新功能想法，但请先确认是否符合项目定位
- 🔧 **代码贡献**：请确保代码风格与现有代码一致，并添加必要的注释

## 📜 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

## 👤 作者

**KyotoAnimation**

---

*Made with ❤️ for Minecraft Server Owners*
