# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 在此代码库中工作时提供指导。
必须中文回答

## 项目概述

UtopiaBosses 是一个基于 Fabric 的 Minecraft 1.20.1 模组，添加了Boss实体、自定义工具/盔甲和祭坛机制。该模组使用 GeckoLib 实现3D动画模型。

## 构建命令

```bash
# 构建模组
./build.bat  # Windows构建脚本，同时会复制到mods文件夹
gradle build # 如果全局安装了gradle的替代方案

# 输出JAR位置
build/libs/UtopiaBosses-[version].jar
```

## 架构

### 包结构
- `lt.utopiabosses` - 根包
- `entity/` - Boss实体（向日葵Boss、树Boss）和投射物
- `item/` - 自定义物品、工具、盔甲套装（大量使用GeckoLib）
- `block/` - 自定义方块（自然祭坛、向日葵祭坛、太阳方块）
- `client/` - 客户端渲染（model/、renderer/）
- `registry/` - 所有模组内容的集中注册
- `network/` - 客户端-服务器通信的数据包处理
- `mixin/` - 用于修改原版行为的Mixins

### 关键技术
- **Fabric Loader 0.16.9** 配合 Fabric API 0.92.2
- **GeckoLib 4.7** - 所有实体和许多物品使用GeckoLib实现动画
- **Java 17** 目标兼容性
- `libs/`中的外部依赖：HWGMod、legendarymonsters

### 注册模式
所有内容注册都集中在`registry/`类中：
- `ItemRegistry` - 所有物品和工具
- `BlockRegistry` - 所有方块
- `EntityRegistry` - 所有实体
- `SoundRegistry` - 自定义声音

### 客户端/服务器分离
- 主模组类：`Utopiabosses.java`
- 客户端初始化器：`UtopiabossesClient.java`
- 渲染逻辑正确分离在`client/`包中

### GeckoLib集成
大多数实体和物品继承GeckoLib类：
- 实体继承`GeoEntity`，配有对应的`GeoModel`和`GeoRenderer`
- 物品使用`GeoItem`实现动画工具/盔甲
- 动画/模型文件位于`resources/assets/utopiabosses/geo/`

## 开发注意事项

- 无自动化测试 - 仅手动测试
- 运行配置位于`.idea/runConfigurations/Minecraft_Server.xml`
- 模组添加两种主要Boss类型：向日葵Boss和树Boss
- 祭坛方块用于召唤/合成机制
- 大量使用自定义声音和粒子效果