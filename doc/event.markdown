这是一份按照 **Minecraft/NeoForge 加载生命周期 (Lifecycle)** 和 **逻辑执行顺序** 整理的排序列表。
要在 NeoForge 中写逻辑：
锁定事件：
判定条件：
执行动作：
我将这些事件分为了几个主要阶段：
1.  **Mod 构造与注册阶段 (Mod Construction & Registration)** - 在游戏启动早期，Mod总线 (Mod Event Bus) 上触发。
2.  **数据与资源加载 (Data & Resources)** - 资源包、数据包加载。
3.  **服务端启动与同步 (Server Startup & Sync)** - 世界加载前。
4.  **游戏循环：Tick (Game Loop: Ticks)** - 游戏运行时的心跳。
5.  **游戏逻辑：世界/实体/玩家 (Gameplay Logic)** - 物理、交互、AI。
6.  **客户端渲染与交互 (Client Rendering & Interaction)** - 每一帧的渲染循环。
7.  **关闭阶段 (Shutdown)**。

---

### 第一阶段：Mod 初始化与注册 (Mod Event Bus)
*这些事件在游戏启动时触发，用于注册物品、方块、客户端设置等。*

**1. 核心注册与定义**
*   `ModConfigEvent` (加载配置文件)
*   `NewRegistryEvent` (创建新的注册表)
*   `RegisterDataMapTypesEvent` (注册 Data Maps 类型)
*   `RegisterEvent` (注册物品、方块、实体等绝大多数内容)
*   `RegisterCapabilitiesEvent` (注册 Capability/Data Attachments)
*   `EntityAttributeCreationEvent` (定义生物属性，如血量、攻击力)
*   `RegisterSpawnPlacementsEvent` (定义生物生成规则)
*   `RegisterTicketControllersEvent` (区块加载票据控制器)
*   `RegisterPayloadHandlersEvent` (注册网络包 Payload)
*   `RegisterConfigurationTasksEvent` (注册网络配置任务)
*   `ModifyRegistriesEvent` (修改现有注册表)
*   `RegisterStructureConversionsEvent` (结构转换)

**2. 游戏内容扩展**
*   `RegisterBrewingRecipesEvent` (注册酿造配方)
*   `RegisterCauldronFluidContentEvent` (炼药锅液体交互)
*   `ExtendPoiTypesEvent` (扩展村民兴趣点)

**3. 客户端初始化 (Client Setup)**
*   `RegisterShadersEvent` (注册着色器)
*   `RegisterKeyMappingsEvent` (注册按键绑定)
*   `RegisterMenuScreensEvent` (绑定容器与屏幕 GUI)
*   `EntityRenderersEvent` (注册实体渲染器)
*   `RegisterParticleProvidersEvent` (注册粒子效果)
*   `RegisterItemDecorationsEvent` (注册物品装饰)
*   `RegisterColorHandlersEvent` (方块/物品染色)
*   `RegisterNamedRenderTypesEvent` (注册命名渲染类型)
*   `RegisterRenderBuffersEvent` (注册渲染缓冲)
*   `RegisterSpriteSourceTypesEvent` (Atlas 精灵来源)
*   `RegisterGuiLayersEvent` (注册 GUI 图层)
*   `RegisterClientTooltipComponentFactoriesEvent` (注册 Tooltip 组件工厂)
*   `RegisterClientReloadListenersEvent` (注册资源重载监听器)
*   `RegisterClientExtensionsEvent` (客户端扩展)
*   `RegisterClientCommandsEvent` (客户端专用指令)
*   `RegisterDimensionSpecialEffectsEvent` (维度渲染特效)
*   `RegisterDimensionTransitionScreenEvent` (维度切换屏幕)
*   `RegisterEntitySpectatorShadersEvent` (旁观者模式着色器)
*   `RegisterMapDecorationRenderersEvent` (地图图标渲染)
*   `RegisterJsonAnimationTypesEvent` (JSON 动画)
*   `RegisterRecipeBookCategoriesEvent` (配方书分类)
*   `RegisterPresetEditorsEvent` (世界预设编辑器)

**4. 资源与模型 (Resources & Models)**
*   `ModelEvent` (模型加载/烘焙，包含 ModifyBakingResult 等)
*   `RegisterMaterialAtlasesEvent` (材质图集)
*   `TextureAtlasStitchedEvent` (贴图缝合完成)
*   `RegisterStageEvent` (在 RenderLevelStageEvent 中使用的阶段注册)

**5. 生命周期收尾与数据生成**
*   `ModLifecycleEvent` (FMLCommonSetup, ClientSetup 等)
*   `GatherDataEvent` (DataGenerator 运行，仅在生成数据时触发)

---

### 第二阶段：数据加载与重载 (Data & Resources)
*当数据包 (Datapack) 或资源包 (Resourcepack) 加载/重载时触发。*

*   `AddPackFindersEvent` (添加资源包/数据包寻找器)
*   `AddReloadListenerEvent` (添加服务端重载监听器)
*   `LootTableLoadEvent` (加载战利品表)
*   `TagsUpdatedEvent` (标签加载完成)
*   `RecipesUpdatedEvent` (配方加载完成)
*   `DataMapsUpdatedEvent` (Data Maps 更新)

---

### 第三阶段：服务端启动与世界加载 (Server & World Load)
*服务器启动、指令注册、维度加载。*

*   `ServerLifecycleEvent` (ServerStarting -> ServerStarted)
*   `RegisterCommandsEvent` (注册服务端指令)
*   `CommandEvent` (指令执行时)
*   `OnDatapackSyncEvent` (玩家加入时同步数据包)
*   `PermissionGatherEvent` (权限收集)
*   `RegisterGameTestsEvent` (注册游戏内测试)
*   `LevelEvent` (Load/Save/Unload)
*   `ChunkTicketLevelUpdatedEvent` (区块加载等级变化)

---

### 第四阶段：游戏循环 - Tick (The Heartbeat)
*每秒运行 20 次的核心循环。*

1.  **ServerTickEvent** (服务端逻辑心跳)
2.  **LevelTickEvent** (维度世界心跳)
3.  **ClientTickEvent** (客户端逻辑心跳)

---

### 第五阶段：运行时逻辑 (Runtime Gameplay Logic)
*发生在 Tick 内部或由特定动作触发。*

**1. 实体与属性 (Entity & Attributes)**
*   `EntityEvent` (基类)
*   `EntityAttributeModificationEvent` (属性修改)
*   `ModifyDefaultComponentsEvent` (修改默认组件)
*   `BabyEntitySpawnEvent` (幼体生成)
*   `MobSplitEvent` (史莱姆等分裂)
*   `XpOrbTargetingEvent` (经验球寻找目标)
*   `SpawnPlacementCheck` (生物生成检查)
*   `ModifyCustomSpawnersEvent` (自定义刷怪笼)

**2. 玩家交互 (Player Interaction)**
*   `ItemEntityPickupEvent` (拾取物品)
*   `ItemStackedOnOtherEvent` (物品在背包中堆叠交互)
*   `PlayerNegotiationEvent` (玩家连接协商)
*   `BonemealEvent` (骨粉催熟)
*   `UseItemOnBlockEvent` (右键方块)
*   `GetEnchantmentLevelEvent` (获取附魔等级)
*   `EnchantmentLevelSetEvent` (设定附魔等级)
*   `BuildCreativeModeTabContentsEvent` (构建创造模式物品栏 - 只有打开时才触发或初始化时)

**3. 方块与世界 (Block & World)**
*   `BlockEvent` (Break, Place, NeighborNotify 等)
*   `BlockEntityTypeAddBlocksEvent` (方块实体关联方块)
*   `ExplosionEvent` (爆炸)
*   `AlterGroundEvent` (修改地面，如放置花朵)
*   `FarmlandTrampleEvent` (属于 BlockEvent，踩踏耕地)
*   `ChunkWatchEvent` (玩家通过网络观测区块)
*   `VanillaGameEvent` (原版游戏事件，如震动)
*   `PlayLevelSoundEvent` (播放世界音效)

**4. 经济与合成 (Economy & Crafting)**
*   `VillagerTradesEvent` (村民交易)
*   `WandererTradesEvent` (流浪商人交易)
*   `VillageSiegeEvent` (僵尸围城)
*   `AnvilUpdateEvent` (铁砧逻辑)
*   `GrindstoneEvent` (砂轮逻辑)
*   `FurnaceFuelBurnTimeEvent` (燃料燃烧时间)
*   `PotionBrewEvent` (酿造台逻辑)
*   `ItemAttributeModifierEvent` (物品属性修饰)

**5. 聊天与网络**
*   `ServerChatEvent` (服务端接收聊天)
*   `ClientChatEvent` (客户端发送聊天)
*   `ClientChatReceivedEvent` (客户端收到聊天)
*   `ClientPlayerNetworkEvent` (登录/登出/重生)

---

### 第六阶段：客户端渲染与帧循环 (Client Rendering)
*按每帧渲染流程排序。*

**1. 摄像机与环境 (Camera & Environment)**
*   `ViewportEvent` (视口计算，Fog 颜色等)
*   `ComputeFovModifierEvent` (视野 FOV 修改)
*   `CalculateDetachedCameraDistanceEvent` (第三人称相机距离)
*   `CalculatePlayerTurnEvent` (玩家转向)
*   `SelectMusicEvent` (背景音乐选择)
*   `SoundEvent` (声音系统)

**2. 世界渲染 (World Rendering)**
*   `RenderLevelStageEvent` (3D 世界渲染的主要阶段)
*   `RenderLivingEvent` (实体渲染)
*   `RenderHighlightEvent` (方块高亮/选框)
*   `RenderBlockScreenEffectEvent` (方块覆盖效果，如在水中/火中)

**3. 界面与 HUD (GUI & HUD)**
*   `RenderGuiEvent` (InGame HUD 渲染)
*   `RenderGuiLayerEvent` (具体 HUD 图层)
*   `RenderFrameEvent` (Item Frame 渲染)
*   `RenderItemInFrameEvent` (展示框内物品)
*   `CustomizeGuiOverlayEvent` (自定义 HUD 覆盖层)
*   `ToastAddEvent` (添加右上角弹窗)

**4. 屏幕与菜单 (Screens)**
*   `ScreenEvent` (GUI 屏幕基类，Init, Render 等)
*   `ContainerScreenEvent` (容器屏幕渲染)
*   `InputEvent` (鼠标/键盘输入)
*   `ClientPauseChangeEvent` (暂停状态改变)

**5. 手部与物品 (Hand & Items)**
*   `RenderHandEvent` (第一人称手部)
*   `RenderArmEvent` (第一人称手臂 - 没拿东西时)

**6. 工具提示 (Tooltips)**
*   `RenderTooltipEvent` (渲染 Tooltip)
    *   `GatherComponents` (在 Tooltip 渲染前收集组件)
*   `AddAttributeTooltipsEvent` (添加属性 Tooltip)
*   `GatherEffectScreenTooltipsEvent` (药水效果 Tooltip)
*   `GatherSkippedAttributeTooltipsEvent` (被跳过的属性)

**7. 其他**
*   `ScreenshotEvent` (截图时)
*   `ClientPlayerChangeGameTypeEvent` (游戏模式切换)

---

### 第七阶段：关闭与异常 (Shutdown & Misc)
*   `ModMismatchEvent` (Mod 版本不匹配)
*   `ServerLifecycleEvent` (ServerStopping -> ServerStopped)
*   `GameShuttingDownEvent` (游戏完全关闭)