上一份文档是为了方便阅读而整理的**核心精简版**。

如果你需要一份**完全包含你提供列表中所有类**的详尽开发文档，请使用下方这份**全量版**。我已经将列表中的每一个条目都进行了归类整理，并标注了包含的子事件（如 `Pre`/`Post` 或 `Start`/`Tick`）。

---

# Minecraft NeoForge 全量事件开发参考手册

**文档说明**：本文档包含输入列表中所有的事件类，按照 **API 来源** 和 **功能模块** 进行了严格分类。

## 目录
1.  [核心架构 (Core & Lifecycle)](#1-核心架构-core--lifecycle)
2.  [注册与数据 (Registry & Data)](#2-注册与数据-registry--data)
3.  [实体与玩家 (Entity & Player)](#3-实体与玩家-entity--player)
4.  [方块与世界 (Block & Level)](#4-方块与世界-block--level)
5.  [客户端与渲染 (Client & Render)](#5-客户端与渲染-client--render)
6.  [GeckoLib 动画库](#6-geckolib-动画库)
7.  [Curios 饰品栏](#7-curios-饰品栏)
8.  [Iron's Spells 'n Books 魔法](#8-irons-spells-n-books-魔法)
9.  [Apotheosis & Placebo](#9-apotheosis--placebo)
10. [L2 Series & Modular Golems](#10-l2-series--modular-golems)
11. [其他模组集成 (Patchouli, Lionfish, etc.)](#11-其他模组集成)

---

## 1. 核心架构 (Core & Lifecycle)
> **总线**: `ModEventBus` (大部分) / `GameBus` (部分生命周期)

| 事件名 | 包路径 | 说明 |
| :--- | :--- | :--- |
| **Object** | `java.lang` | Java 基类 |
| **Event** | `bus.api` | 事件基类 |
| **ModLifecycleEvent** | `fml.event.lifecycle` | 模组生命周期基类 |
| **FMLConstructModEvent** | `fml.event.lifecycle` | 构造阶段 |
| **FMLCommonSetupEvent** | `fml.event.lifecycle` | 通用初始化 |
| **FMLClientSetupEvent** | `fml.event.lifecycle` | 客户端初始化 |
| **FMLDedicatedServerSetupEvent** | `fml.event.lifecycle` | 服务端初始化 |
| **FMLLoadCompleteEvent** | `fml.event.lifecycle` | 加载完成 |
| **InterModEnqueueEvent** | `fml.event.lifecycle` | IMC 入队 |
| **InterModProcessEvent** | `fml.event.lifecycle` | IMC 处理 |
| **ParallelDispatchEvent** | `fml.event.lifecycle` | 并行分发 |
| **GameShuttingDownEvent** | `neoforge.event` | 游戏关闭中 |
| **ServerLifecycleEvent** | `neoforge.event.server` | 服务端生命周期基类 |
| **ServerAboutToStartEvent** | `neoforge.event.server` | 即将启动 |
| **ServerStartingEvent** | `neoforge.event.server` | 启动中 |
| **ServerStartedEvent** | `neoforge.event.server` | 已启动 |
| **ServerStoppingEvent** | `neoforge.event.server` | 停止中 |
| **ServerStoppedEvent** | `neoforge.event.server` | 已停止 |
| **ModConfigEvent** | `fml.event.config` | 配置更改 (`Loading`, `Reloading`, `Unloading`) |

---

## 2. 注册与数据 (Registry & Data)
> **总线**: `ModEventBus`

| 事件名 | 包路径 | 说明 |
| :--- | :--- | :--- |
| **RegisterEvent** | `neoforge.registries` | 核心注册事件 |
| **NewRegistryEvent** | `neoforge.registries` | 创建新注册表 |
| **DataPackRegistryEvent** | `neoforge.registries` | 数据包注册 (`NewRegistry`) |
| **DataMapsUpdatedEvent** | `neoforge.registries.datamaps` | 数据映射更新 |
| **RegisterDataMapTypesEvent** | `neoforge.registries.datamaps` | 注册数据映射类型 |
| **GatherDataEvent** | `neoforge.data.event` | 数据生成 (DataGen) |
| **RegisterCapabilitiesEvent** | `neoforge.capabilities` | 注册 Capability |
| **RegisterPayloadHandlersEvent** | `neoforge.network.event` | 网络包处理器注册 |
| **RegisterConfigurationTasksEvent** | `neoforge.network.event` | 配置任务注册 |
| **ModifyRegistriesEvent** | `neoforge.registries` | 修改注册表 |
| **IdMappingEvent** | `neoforge.registries` | ID 映射重映射 |
| **ModifyDefaultComponentsEvent** | `neoforge.event` | 修改默认组件 |
| **OnDatapackSyncEvent** | `neoforge.event` | 数据包同步 |
| **TagsUpdatedEvent** | `neoforge.event` | 标签更新 |
| **LootTableLoadEvent** | `neoforge.event` | 战利品表加载 |
| **AddPackFindersEvent** | `neoforge.event` | 添加资源包查找器 |
| **AddReloadListenerEvent** | `neoforge.event` | 添加资源重载监听器 |

---

## 3. 实体与玩家 (Entity & Player)
> **总线**: `GameBus`

### 3.1 实体通用与属性
| 事件名 | 包路径 | 说明 |
| :--- | :--- | :--- |
| **EntityEvent** | `neoforge.event.entity` | 实体基类 (`EnteringSection`, `EntityConstructing`, `Size`) |
| **EntityJoinLevelEvent** | `neoforge.event.entity` | 加入世界 |
| **EntityLeaveLevelEvent** | `neoforge.event.entity` | 离开世界 |
| **EntityAttributeCreationEvent** | `neoforge.event.entity` | **ModBus** 属性创建 |
| **EntityAttributeModificationEvent**| `neoforge.event.entity` | **ModBus** 属性修改 |
| **EntityTickEvent** | `neoforge.event.tick` | 实体 Tick (`Pre`, `Post`) |
| **EntityTeleportEvent** | `neoforge.event.entity` | 传送 (`ChorusFruit`, `EnderEntity`, `EnderPearl`, `TeleportCommand`, `SpreadPlayersCommand`) |
| **EntityTravelToDimensionEvent** | `neoforge.event.entity` | 跨维度 |
| **EntityMountEvent** | `neoforge.event.entity` | 骑乘 |
| **EntityStruckByLightningEvent** | `neoforge.event.entity` | 被雷劈 |
| **ProjectileImpactEvent** | `neoforge.event.entity` | 投射物撞击 |
| **RegisterSpawnPlacementsEvent** | `neoforge.event.entity` | **ModBus** 刷怪位置注册 |
| **XpOrbTargetingEvent** | `neoforge.entity` | 经验球寻路 |

### 3.2 生物 (Living)
| 事件名 | 包路径 | 说明 |
| :--- | :--- | :--- |
| **LivingEvent** | `neoforge.event.entity.living` | 生物基类 (`LivingJumpEvent`, `LivingVisibilityEvent`) |
| **LivingDamageEvent** | `neoforge.event.entity.living` | 受伤前 |
| **LivingIncomingDamageEvent** | `neoforge.event.entity.living` | 受伤计算中 |
| **LivingIncomingDamageEventMixin**| `dev.xkmc.l2damagetracker.mixin`| L2 伤害追踪 Mixin |
| **LivingDeathEvent** | `neoforge.event.entity.living` | 死亡 |
| **LivingDropsEvent** | `neoforge.event.entity.living` | 掉落物 |
| **LivingHealEvent** | `neoforge.event.entity.living` | 治疗 |
| **LivingFallEvent** | `neoforge.event.entity.living` | 摔落 |
| **LivingKnockBackEvent** | `neoforge.event.entity.living` | 击退 |
| **LivingBreatheEvent** | `neoforge.event.entity.living` | 呼吸 |
| **LivingDrownEvent** | `neoforge.event.entity.living` | 溺水 |
| **LivingChangeTargetEvent** | `neoforge.event.entity.living` | 更改攻击目标 |
| **LivingConversionEvent** | `neoforge.event.entity.living` | 生物转化 (如僵尸村民) |
| **LivingEquipmentChangeEvent** | `neoforge.event.entity.living` | 装备变更 |
| **LivingExperienceDropEvent** | `neoforge.event.entity.living` | 经验掉落 |
| **LivingGetProjectileEvent** | `neoforge.event.entity.living` | 获取弹射物 |
| **LivingShieldBlockEvent** | `neoforge.event.entity.living` | 盾牌格挡 |
| **LivingSwapItemsEvent** | `neoforge.event.entity.living` | 交换主副手 |
| **LivingUseTotemEvent** | `neoforge.event.entity.living` | 使用图腾 |
| **LivingDestroyBlockEvent** | `neoforge.event.entity.living` | 生物破坏方块(如末影人) |
| **AnimalTameEvent** | `neoforge.event.entity.living` | 动物驯服 |
| **ArmorHurtEvent** | `neoforge.event.entity.living` | 护甲耐久损耗 |
| **EffectParticleModificationEvent** | `neoforge.event.entity.living` | 药水粒子修改 |
| **MobEffectEvent** | `neoforge.event.entity.living` | 药水效果相关 |
| **EnderManAngerEvent** | `neoforge.event.entity.living` | 末影人愤怒 |
| **BabyEntitySpawnEvent** | `neoforge.event.entity.living` | 幼体生成 |
| **MobSplitEvent** | `neoforge.event.entity.living` | 史莱姆分裂 |
| **MobSpawnEvent** | `neoforge.event.entity.living` | 刷怪 (`FinalizeSpawn`, `PositionCheck`, `SpawnPlacementCheck`) |
| **MobDespawnEvent** | `neoforge.event.entity.living` | 怪物消失 |
| **SpawnClusterSizeEvent** | `neoforge.event.entity.living` | 刷怪簇大小 |
| **ComputeEnchantedLootBonusEvent**| `fuzs.puzzleslib...` | 计算抢夺附魔加成 |

### 3.3 玩家 (Player)
| 事件名 | 包路径 | 说明 |
| :--- | :--- | :--- |
| **PlayerEvent** | `neoforge.event.entity.player` | 玩家基类 |
| **ItemEntityPickupEvent** | `neoforge.event.entity.player` | 拾起物品 (`Pre`, `Post`) |
| **UseItemOnBlockEvent** | `neoforge.event.entity.player` | 右键方块 |
| **BonemealEvent** | `neoforge.event.entity.player` | 骨粉 |
| **CanContinueSleepingEvent** | `neoforge.event.entity.player` | 持续睡眠检查 |
| **PlayerNegotiationEvent** | `neoforge.event.entity.player` | 连接协商 |
| **AdvancementEvent** | `neoforge.event.entity.player` | (隐含在PlayerEvent中) |
| **AnvilUpdateEvent** | `neoforge.event` | 铁砧逻辑 |
| **GrindstoneEvent** | `neoforge.event` | 砂轮 (`OnPlaceItem`, `OnTakeItem`) |
| **ItemTossEvent** | `neoforge.event.entity.item` | 丢弃物品 |
| **LivingEntityUseItemEvent** | `neoforge.event.entity.living` | 使用物品中/结束 |
| **PermissionGatherEvent** | `neoforge.server.permission.events` | 权限收集 (`Handler`, `Nodes`) |

---

## 4. 方块与世界 (Block & Level)
> **总线**: `GameBus`

| 事件名 | 包路径 | 说明 |
| :--- | :--- | :--- |
| **LevelEvent** | `neoforge.event.level` | 世界事件 (`Load`, `Unload`, `Save`, `CreateSpawnPosition`, `PotentialSpawns`) |
| **LevelTickEvent** | `neoforge.event.tick` | 世界 Tick (`Pre`, `Post`) |
| **BlockEvent** | `neoforge.event.level` | 方块基类 |
| **BreakEvent** | `neoforge.event.level` | 破坏方块 |
| **EntityPlaceEvent** | `neoforge.event.level` | 实体放置方块 |
| **EntityMultiPlaceEvent** | `neoforge.event.level` | 放置多方块 (如床) |
| **NeighborNotifyEvent** | `neoforge.event.level` | 邻居更新 |
| **FarmlandTrampleEvent** | `neoforge.event.level` | 踩坏耕地 |
| **FluidPlaceBlockEvent** | `neoforge.event.level` | 流体生成方块 |
| **PortalSpawnEvent** | `neoforge.event.level` | 传送门生成 |
| **BlockToolModificationEvent** | `neoforge.event.level` | 工具修改方块 (削皮等) |
| **NoteBlockEvent** | `neoforge.event.level` | 音符盒 (`Play`, `Change`) |
| **PistonEvent** | `neoforge.event.level` | 活塞 (`Pre`, `Post`) |
| **CropGrowEvent** | `neoforge.event.level.block` | 作物生长 (`Pre`, `Post`) |
| **CreateFluidSourceEvent** | `neoforge.event.level.block` | 无限水形成 |
| **BlockDropsEvent** | `neoforge.event.level` | 方块掉落 |
| **ExplosionEvent** | `neoforge.event.level` | 爆炸 (`Start`, `Detonate`) |
| **ExplosionKnockbackEvent** | `neoforge.event.level` | 爆炸击退 |
| **ChunkEvent** | `neoforge.event.level` | 区块 (`Load`, `Unload`) |
| **ChunkDataEvent** | `neoforge.event.level` | 区块数据 (`Load`, `Save`) |
| **ChunkWatchEvent** | `neoforge.event.level` | 区块监视 (`Watch`, `UnWatch`, `Sent`) |
| **ChunkTicketLevelUpdatedEvent**| `neoforge.event.level` | 票据等级更新 |
| **ModifyCustomSpawnersEvent** | `neoforge.event.level` | 修改刷怪笼 |
| **RegisterTicketControllersEvent**| `neoforge.common.world.chunk`| 注册区块加载票据 |
| **AlterGroundEvent** | `neoforge.event.level` | 改变地面 (如树木生长) |
| **BlockGrowFeatureEvent** | `neoforge.event.level` | 特性生长 |
| **SleepFinishedTimeEvent** | `neoforge.event.level` | 睡眠结束时间 |
| **ExtendPoiTypesEvent** | `neoforge.common.world.poi` | 扩展兴趣点 (POI) |
| **RegisterCauldronFluidContent**| `neoforge.fluids` | 炼药锅流体注册 |
| **RegisterBrewingRecipesEvent** | `neoforge.event.brewing` | 酿造配方注册 |
| **PotionBrewEvent** | `neoforge.event.brewing` | 酿造过程 (`Pre`, `Post`) |
| **VillagerTradesEvent** | `neoforge.event.village` | 村民交易 |
| **WandererTradesEvent** | `neoforge.event.village` | 流浪商人交易 |
| **VillageSiegeEvent** | `neoforge.event.village` | 僵尸围城 |

---

## 5. 客户端与渲染 (Client & Render)
> **总线**: `ModEventBus` (注册类) / `neoforge.client.event` (运行时)

### 5.1 注册与初始化 (Mod Bus)
| 事件名 | 包路径 | 说明 |
| :--- | :--- | :--- |
| **EntityRenderersEvent** | `neoforge.client.event` | `RegisterRenderers`, `RegisterLayerDefinitions`, `CreateSkullModels`, `AddLayers` |
| **RegisterColorHandlersEvent** | `neoforge.client.event` | `Item`, `Block`, `ColorResolvers` |
| **RegisterParticleProvidersEvent**| `neoforge.client.event` | 注册粒子工厂 |
| **RegisterKeyMappingsEvent** | `neoforge.client.event` | 注册按键 |
| **RegisterGuiLayersEvent** | `neoforge.client.event` | 注册 HUD 图层 |
| **RegisterMenuScreensEvent** | `neoforge.client.event` | 绑定菜单与屏幕 |
| **RegisterNamedRenderTypesEvent** | `neoforge.client.event` | 注册命名渲染类型 |
| **RegisterShadersEvent** | `neoforge.client.event` | 注册着色器 |
| **RegisterItemDecorationsEvent** | `neoforge.client.event` | 注册物品装饰 (Durability bar等) |
| **RegisterClientTooltipComponent**| `neoforge.client.event` | 注册 Tooltip 组件工厂 |
| **RegisterClientReloadListeners** | `neoforge.client.event` | 客户端资源重载监听 |
| **RegisterDimensionSpecialEffects**| `neoforge.client.event` | 维度特效 |
| **RegisterPresetEditorsEvent** | `neoforge.client.event` | 预设编辑器 |
| **RegisterRecipeBookCategories** | `neoforge.client.event` | 配方书分类 |
| **RegisterSpriteSourceTypesEvent**| `neoforge.client.event` | 精灵图源类型 |
| **RegisterMapDecorationRenderers**| `neoforge.client.gui.map` | 地图图标渲染器 |
| **RegisterEntitySpectatorShaders**| `neoforge.client.event` | 旁观模式着色器 |
| **RegisterRenderBuffersEvent** | `neoforge.client.event` | 渲染缓冲 |
| **RegisterClientExtensionsEvent** | `neoforge.client.extensions.common`| 客户端扩展 |
| **ModelEvent** | `neoforge.client.event` | `RegisterGeometryLoaders`, `RegisterAdditional`, `ModifyBakingResult`, `BakingCompleted` |
| **TextureAtlasStitchedEvent** | `neoforge.client.event` | 纹理拼接完成 |

### 5.2 游戏内渲染与交互 (Game Bus)
| 事件名 | 包路径 | 说明 |
| :--- | :--- | :--- |
| **RenderLevelStageEvent** | `neoforge.client.event` | 关卡渲染阶段 (`RegisterStageEvent`) |
| **RenderLivingEvent** | `neoforge.client.event` | 生物渲染 (`Pre`, `Post`) |
| **RenderNameTagEvent** | `neoforge.client.event` | 名字标签渲染 |
| **RenderHandEvent** | `neoforge.client.event` | 手部渲染 |
| **RenderArmEvent** | `neoforge.client.event` | 手臂渲染 |
| **RenderItemInFrameEvent** | `neoforge.client.event` | 展示框物品渲染 |
| **RenderHighlightEvent** | `neoforge.client.event` | 高亮渲染 (`Block`, `Entity`) |
| **RenderBlockScreenEffectEvent** | `neoforge.client.event` | 方块屏幕特效 (火/水) |
| **RenderGuiEvent** | `neoforge.client.event` | GUI 总渲染 (`Pre`, `Post`) |
| **RenderGuiLayerEvent** | `neoforge.client.event` | GUI 图层渲染 (`Pre`, `Post`) |
| **RenderFrameEvent** | `neoforge.client.event` | 帧渲染 (`Pre`, `Post`) |
| **CustomizeGuiOverlayEvent** | `neoforge.client.event` | 自定义 HUD (`BossEventProgress`, `Chat`, `DebugText`) |
| **ContainerScreenEvent** | `neoforge.client.event` | 容器屏幕 (`Render` -> `Background`/`Foreground`) |
| **ScreenEvent** | `neoforge.client.event` | 屏幕基类 (`Init`, `Render`, `Mouse...`, `Key...`, `Closing`, `Opening`) |
| **InputEvent** | `neoforge.client.event` | 输入 (`Key`, `MouseButton`, `MouseScrolling`) |
| **ClientTickEvent** | `neoforge.client.event` | 客户端 Tick (`Pre`, `Post`) |
| **ComputeFovModifierEvent** | `neoforge.client.event` | FOV 修改 |
| **CalculatePlayerTurnEvent** | `neoforge.client.event` | 玩家转向计算 |
| **CalculateDetachedCameraDistance**| `neoforge.client.event` | 第三人称相机距离 |
| **ViewportEvent** | `neoforge.client.event` | 视口 (`ComputeCameraAngles`, `ComputeFogColor`, `ComputeFov`, `RenderFog`) |
| **RenderTooltipEvent** | `neoforge.client.event` | 提示栏 (`Pre`, `Color`, `GatherComponents`) |
| **GatherEffectScreenTooltipsEvent**| `neoforge.client.event` | 效果提示栏 |
| **AddAttributeTooltipsEvent** | `neoforge.client.event` | 添加属性提示 |
| **AddSectionGeometryEvent** | `neoforge.client.event` | 添加区块几何体 |
| **ClientChatEvent** | `neoforge.client.event` | 客户端发送聊天 |
| **ClientChatReceivedEvent** | `neoforge.client.event` | 客户端接收聊天 (`Player`, `System`) |
| **ClientPlayerNetworkEvent** | `neoforge.client.event` | 客户端网络 (`LoggingIn`, `LoggingOut`, `Clone`) |
| **ClientPlayerChangeGameTypeEvent**| `neoforge.client.event` | 游戏模式变更 |
| **ClientPauseChangeEvent** | `neoforge.client.event` | 暂停状态变更 (`Pre`, `Post`) |
| **ScreenshotEvent** | `neoforge.client.event` | 截图 |
| **SelectMusicEvent** | `neoforge.client.event` | 选择背景音乐 |
| **ToastAddEvent** | `neoforge.client.event` | 添加 Toast 提示 |
| **SoundEvent** | `neoforge.client.event.sound` | 声音基类 (`PlaySoundEvent`, `SoundEngineLoadEvent`, `SoundSourceEvent`) |

---

## 6. GeckoLib 动画库
> **包路径**: `software.bernie.geckolib.event`

| 事件名 | 子事件 | 说明 |
| :--- | :--- | :--- |
| **GeoRenderEvent** | **主事件** | GeckoLib 渲染基类 |
| (Inner Class) | `Armor` | 护甲渲染 (`Pre`, `Post`, `CompileRenderLayers`) |
| (Inner Class) | `Block` | 方块渲染 (`Pre`, `Post`, `CompileRenderLayers`) |
| (Inner Class) | `Entity` | 实体渲染 (`Pre`, `Post`, `CompileRenderLayers`) |
| (Inner Class) | `Item` | 物品渲染 (`Pre`, `Post`, `CompileRenderLayers`) |
| (Inner Class) | `Object` | 对象渲染 (`Pre`, `Post`, `CompileRenderLayers`) |
| (Inner Class) | `ReplacedEntity` | 替换实体渲染 (`Pre`, `Post`, `CompileRenderLayers`) |

---

## 7. Curios 饰品栏
> **包路径**: `top.theillusivec4.curios.api.event`

| 事件名 | 说明 |
| :--- | :--- |
| **CurioAttributeModifierEvent** | 计算饰品属性修饰符 |
| **CurioCanEquipEvent** | 检查是否可装备 |
| **CurioCanUnequipEvent** | 检查是否可卸下 |
| **CurioChangeEvent** | 饰品栏变更 |
| **CurioDropsEvent** | 饰品死亡掉落 |
| **DropRulesEvent** | 掉落规则 |
| **SlotModifiersUpdatedEvent** | 插槽修饰符更新 |
| **RegisterCuriosExtensionsEvent** | (Extensions) 注册扩展 |

---

## 8. Iron's Spells 'n Books 魔法
> **包路径**: `io.redspace.ironsspellbooks`

| 事件名 | 路径/子类 | 说明 |
| :--- | :--- | :--- |
| **SpellDamageEvent** | `api.events` | 魔法伤害 |
| **SpellHealEvent** | `api.events` | 魔法治疗 |
| **SpellSummonEvent** | `api.events` | 魔法召唤 |
| **SpellTeleportEvent** | `api.events` | 魔法传送 |
| **CounterSpellEvent** | `api.events` | 魔法反制 |
| **ModifySpellLevelEvent** | `api.events` | 修改法术等级 |
| **SpellCooldownAddedEvent** | `api.events` | 添加冷却 (`Pre`, `Post`) |
| **AlchemistCauldronBuild...** | `block.alchemist_cauldron` | 炼金锅构建交互 |
| **SetupJewelcraftingResultEvent**| `io.redspace.ironsjewelry.event` | 珠宝加工结果 |

---

## 9. Apotheosis & Placebo
> **包路径**: `dev.shadowsoffire...`

| 事件名 | 子模块/路径 | 说明 |
| :--- | :--- | :--- |
| **ApotheosisCommandEvent** | `apothic_attributes.event` | 属性命令 |
| **StackAttributeModifiersEvent**| `apothic_attributes.modifiers` | 堆叠属性修饰符 |
| **CanSocketGemEvent** | `apotheosis.event` | 是否可镶嵌 |
| **GetItemSocketsEvent** | `apotheosis.event` | 获取插槽数 |
| **ItemSocketingEvent** | `apotheosis.event` | 镶嵌过程 |
| **AnvilLandEvent** | `placebo.events` | 铁砧落地 |
| **ResourceReloadEvent** | `placebo.events` | 资源重载 |
| **GateEvent** | `gateways.event` | 传送门 (`Opened`, `Completed`, `Failed`, `WaveStarted`, `WaveEnd`, `WaveEntitySpawned`) |

---

## 10. L2 Series & Modular Golems
> **包路径**: `dev.xkmc...`

| 事件名 | 模组/路径 | 说明 |
| :--- | :--- | :--- |
| **ArrowFindEvent** | `l2backpack.events` | (ArrowBag) 箭矢查找 |
| **EnderTickEvent** | `l2backpack...` | 远程/末影 Tick |
| **CreateSourceEvent** | `l2damagetracker...` | 创建伤害源 |
| **EnderPickupEvent** | `l2complements...` | 末影拾取 |
| **HostilityInitEvent** | `l2hostility.events` | 敌对性初始化 |
| **CustomRecipeEvent** | `modulargolems...jei` | 自定义 JEI 配方 |
| **GolemEvent** | `modulargolems...event` | 傀儡事件 |
| **GolemToOwnerEvent** | `modulargolems...event` | 傀儡归属 |
| **GolemRenderItemInHandEvent** | `modulargolems...event` | 傀儡手持渲染 |
| **HumanoidSkinEvent** | `modulargolems...event` | 人形皮肤 |

---

## 11. 其他模组集成

| 事件名 | 来源/包 | 说明 |
| :--- | :--- | :--- |
| **BookContentsReloadEvent** | `vazkii.patchouli.api` | Patchouli 手册重载 |
| **BookDrawScreenEvent** | `vazkii.patchouli.api` | Patchouli 屏幕绘制 |
| **AnimationEvent** | `lionfishapi` / `llibrary` | 动画事件 (`Start`, `Tick`) |
| **StandOnFluidEvent** | `lionfishapi` | 站立于流体上 |
| **EventGetFluidRenderType** | `lionfishapi.client` | 获取流体渲染类型 |
| **EventPosePlayerHand** | `cataclysm.client` | 玩家手部姿态 (灾变模组) |
| **TileBCoreInitEvent** | `brandonscore` | Brandon's Core Tile 初始化 |
| **NeoForgeMobEffectWidgetEvent**| `fuzs.stylisheffects` | 药水效果组件 (`EffectTooltip`, `MouseClicked`) |

---

## 12. 其他杂项与工具 (Utils)

| 事件名 | 包路径 | 说明 |
| :--- | :--- | :--- |
| **CommandEvent** | `neoforge.event` | 命令执行 |
| **RegisterCommandsEvent** | `neoforge.event` | 注册命令 |
| **RegisterGameTestsEvent** | `neoforge.event` | 注册游戏测试 |
| **RegisterStructureConversions**| `neoforge.event` | 结构转换 |
| **DifficultyChangeEvent** | `neoforge.event` | 难度变更 |
| **ServerChatEvent** | `neoforge.event` | 服务端聊天 |
| **PlayLevelSoundEvent** | `neoforge.event` | 播放世界音效 (`AtEntity`, `AtPosition`) |
| **VanillaGameEvent** | `neoforge.event` | 原版游戏事件 (Sculk感测等) |
| **ItemAttributeModifierEvent** | `neoforge.event` | 物品属性修饰 |
| **ItemStackedOnOtherEvent** | `neoforge.event` | 物品堆叠交互 (Bundle逻辑) |
| **BlockEntityTypeAddBlocksEvent**| `neoforge.event` | 方块实体类型添加方块 |
| **EnchantmentLevelSetEvent** | `neoforge.event.enchanting` | 附魔等级设定 |
| **GetEnchantmentLevelEvent** | `neoforge.event.enchanting` | 获取附魔等级 |
| **FurnaceFuelBurnTimeEvent** | `neoforge.event.furnace` | 熔炉燃料燃烧时间 |
| **ItemEvent** | `neoforge.event.entity.item` | 物品实体事件基类 |
| **ItemExpireEvent** | `neoforge.event.entity.item` | 物品消失 |
| **GatherSkippedAttributeTooltips**| `neoforge.client.event` | 收集跳过的属性提示 |