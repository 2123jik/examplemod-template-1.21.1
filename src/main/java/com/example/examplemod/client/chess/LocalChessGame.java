package com.example.examplemod.client.chess;

import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.mobs.registries.InvaderRegistry;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalChessGame {

    public static final int ROW = 4;
    public static final int COL = 7;
    private static final int[] XP_TABLE = {0, 2, 4, 8, 12, 20, 32, 50, 70, 999};

    // --- 斗兽场配置 ---
    private Vec3 arenaCenter = null;
    private BlockPos fixedArenaBase = null; // 锁定的场地基准点
    private boolean arenaBuilt = false;
    
    // --- 战斗状态 ---
    private int battleTickCounter = 0;
    private static final int BATTLE_GRACE_PERIOD = 60;
    private static final String TEAM_PLAYER = "chess_p";
    private static final String TEAM_ENEMY = "chess_e";

    public record Synergy(String id, String name, int[] thresholds, String[] descriptions) {}
    public record SynergyStatus(Synergy synergy, int count) {
        public boolean isActive() { return count >= synergy.thresholds[0]; }
        public String getNextThreshold() {
            for(int t : synergy.thresholds) if(count < t) return "/" + t;
            return " (MAX)";
        }
    }
    
    public static final List<Synergy> SYNERGIES = List.of(
            new Synergy("亡灵", "亡灵", new int[]{2, 4}, new String[]{"友军攻击力 +25%", "友军攻击力 +60%"}),
            new Synergy("野兽", "野兽", new int[]{2, 4}, new String[]{"全体生命值 +300", "全体生命值 +800"}),
            new Synergy("射手", "射手", new int[]{2, 4}, new String[]{"攻速 +15%", "攻速 +40% & 射程 +1"}),
            new Synergy("法师", "法师", new int[]{2, 4}, new String[]{"技能伤害 +20%", "技能伤害 +50% & 回蓝加快"}),
            new Synergy("海洋", "海洋", new int[]{2, 4}, new String[]{"受到伤害减少 10%", "受到伤害减少 25%"}),
            new Synergy("战士", "战士", new int[]{2, 4}, new String[]{"普攻造成额外 20% 真实伤害", "普攻造成额外 50% 真实伤害"})
    );

    private static final LocalChessGame INSTANCE = new LocalChessGame();
    public static LocalChessGame get() { return INSTANCE; }

    public enum State { PREPARE, BATTLE }
    private State state = State.PREPARE;
    private int round = 1;
    private int gold = 10;
    private int level = 1;
    private int xp = 0;
    private int winStreak = 0;
    private int lossStreak = 0;

    private boolean shopLocked = false;
    private ShopCard[] currentShop = new ShopCard[5];
    private final Map<Integer, ClientUnit> units = new HashMap<>();
    public final List<ItemStack> itemBench = new ArrayList<>();

    private final List<BattleUnit> battleUnits = new ArrayList<>();
    public final List<Projectile> projectiles = new ArrayList<>();
    public final Map<UUID, Float> damageStats = new ConcurrentHashMap<>();
    public final Map<UUID, EntityType<?>> damageSources = new ConcurrentHashMap<>();

    public LocalChessGame() {
        itemBench.add(new ItemStack(Items.IRON_SWORD));
        refreshShop(true);
    }

    // --- 游戏循环 ---
    
    public void tick() {
        if (state != State.BATTLE) return;
        battleTickCounter++;

        // 每 10 tick 检测一次结果
        if (battleTickCounter % 10 == 0) {
            checkBattleResult();
        }

        // 每 20 tick 检查出界
        if (battleTickCounter % 20 == 0) {
            enforceArenaBoundaries();
        }
        
        // 超时强制结束 (90秒)
        if (battleTickCounter > 20 * 90) {
            endBattle(false);
        }
    }

    public void startBattle() {
        if (state == State.BATTLE || units.isEmpty()) return;
        damageStats.clear();
        damageSources.clear();
        
        Player player = Minecraft.getInstance().player;
        if (!(player instanceof LocalPlayer localPlayer)) return;

        if (!arenaBuilt) buildArena(localPlayer);

        // 1. 清理战场
        cleanupArena(localPlayer);

        // 2. 准备数据
        battleUnits.clear();
        spawnEnemyData(round); // 生成敌人数据

        // 将玩家棋子转换为战斗数据
        for (Map.Entry<Integer, ClientUnit> entry : units.entrySet()) {
            if (entry.getKey() >= 0) {
                int r = entry.getKey() / COL;
                int c = entry.getKey() % COL;
                battleUnits.add(new BattleUnit(entry.getValue(), true, c, r));
            }
        }

        // 3. 生成实体
        spawnRealEntities(localPlayer);

        // 4. 状态切换
        state = State.BATTLE;
        battleTickCounter = 0;
        Minecraft.getInstance().setScreen(null); // 关闭 UI
        playSound(SoundEvents.RAID_HORN.value());
        player.displayClientMessage(Component.literal("§6[自走棋] 战斗开始！"), true);
    }

    public void restartGame() {
        this.state = State.PREPARE;
        this.round = 1;
        this.gold = 10;
        this.level = 1;
        this.xp = 0;
        this.units.clear();
        this.itemBench.clear();
        this.battleUnits.clear();
        this.winStreak = 0;
        this.lossStreak = 0;

        Player player = Minecraft.getInstance().player;
        if(player instanceof LocalPlayer lp) {
            lp.connection.sendCommand("gamerule doMobSpawning true");
            cleanupArena(lp);
        }
        refreshShop(true);
        playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
    }

    // --- 斗兽场与实体管理 ---

    private void buildArena(LocalPlayer player) {
        if (this.fixedArenaBase == null) {
            this.fixedArenaBase = player.blockPosition().offset(0, 50, 0);
            this.arenaCenter = new Vec3(fixedArenaBase.getX() + 0.5, fixedArenaBase.getY(), fixedArenaBase.getZ() + 0.5);
        }

        int sizeX = 20; int sizeZ = 25; int height = 10;
        BlockPos center = fixedArenaBase;
        BlockPos corner1 = center.offset(-sizeX, 0, -sizeZ);
        BlockPos corner2 = center.offset(sizeX, height, sizeZ);

        // 1. 黑曜石地板
        player.connection.sendCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:obsidian",
                corner1.getX(), center.getY(), corner1.getZ(), corner2.getX(), center.getY(), corner2.getZ()));
        // 2. 玻璃墙
        player.connection.sendCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:glass outline",
                corner1.getX(), center.getY() + 1, corner1.getZ(), corner2.getX(), corner2.getY(), corner2.getZ()));
        // 3. 清空内部
        player.connection.sendCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air",
                corner1.getX() + 1, center.getY() + 1, corner1.getZ() + 1, corner2.getX() - 1, corner2.getY() - 1, corner2.getZ() - 1));

        // 队伍设置
        setupTeams(player);

        // 传送玩家到观战点
        player.connection.sendCommand(String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f 180 30",
                center.getX() + 0.5, center.getY() + height + 2.0, center.getZ() - sizeZ - 2.0));
        
        this.arenaBuilt = true;
    }

    private void setupTeams(LocalPlayer p) {
        createTeam(p, TEAM_PLAYER, "green", "友军");
        createTeam(p, TEAM_ENEMY, "red", "敌军");
    }

    private void createTeam(LocalPlayer p, String id, String color, String name) {
        p.connection.sendCommand("team add " + id + " \"" + name + "\"");
        p.connection.sendCommand("team modify " + id + " color " + color);
        p.connection.sendCommand("team modify " + id + " friendlyFire false");
        p.connection.sendCommand("team modify " + id + " collisionRule never");
    }

    private void spawnRealEntities(LocalPlayer player) {
        if (fixedArenaBase == null) return;

        double spacingX = 2.5;
        double spacingZ = 2.5;
        double cx = fixedArenaBase.getX() + 0.5;
        double y = fixedArenaBase.getY() + 1.0;
        double cz = fixedArenaBase.getZ() + 0.5;
        double playerZStart = cz + 2.0;
        double enemyZStart = cz - 2.0;

        for (BattleUnit unit : battleUnits) {
            if (unit.sourceInvader == null || unit.sourceRarity == null) continue;

            double xOffset = (unit.gridCol - 3.5) * spacingX;
            double finalX, finalZ;
            float yaw;
            String team;

            if (unit.isPlayer) {
                finalX = cx + xOffset;
                finalZ = playerZStart + (unit.gridRow * spacingZ);
                yaw = 180f;
                team = TEAM_PLAYER;
            } else {
                finalX = cx + xOffset;
                finalZ = enemyZStart - (unit.gridRow * spacingZ);
                yaw = 0f;
                team = TEAM_ENEMY;
            }

            ResourceLocation bossId = InvaderRegistry.INSTANCE.getKey(unit.sourceInvader);
            ResourceLocation rarityId = RarityRegistry.INSTANCE.getKey(unit.sourceRarity);

            if (bossId != null && rarityId != null) {
                // 生成 Boss
                player.connection.sendCommand(String.format(Locale.ROOT, "apoth spawn_boss %.2f %.2f %.2f %s %s",
                        finalX, y, finalZ, bossId, rarityId));

                // 设置队伍和标签
                String selector = String.format(Locale.ROOT,
                        "@e[type=!player,x=%.2f,y=%.2f,z=%.2f,distance=..1.0,limit=1,sort=nearest]",
                        finalX, y, finalZ);
                player.connection.sendCommand("tag " + selector + " add chess_piece");
                player.connection.sendCommand("team join " + team + " " + selector);
                player.connection.sendCommand(String.format(Locale.ROOT, "tp %s %.2f %.2f %.2f %.1f 0", selector, finalX, y, finalZ, yaw));
            }
        }
    }

    private void cleanupArena(Player player) {
        if (fixedArenaBase == null) return;
        // 优先清除 tag
        if (player instanceof LocalPlayer lp) {
            lp.connection.sendCommand("kill @e[tag=chess_piece]");
            // 区域清除保险
            lp.connection.sendCommand(String.format(Locale.ROOT,
                    "kill @e[type=!player,type=!item,x=%d,y=%d,z=%d,distance=..40]",
                    fixedArenaBase.getX(), fixedArenaBase.getY(), fixedArenaBase.getZ()));
        }
    }

    private void enforceArenaBoundaries() {
        if (fixedArenaBase == null) return;
        runOnServer(() -> {
            ServerLevel sl = getServerLevel();
            if (sl == null) return;
            Vec3 center = new Vec3(fixedArenaBase.getX() + 0.5, fixedArenaBase.getY(), fixedArenaBase.getZ() + 0.5);
            List<Entity> entities = sl.getEntities((Entity)null, new AABB(fixedArenaBase).inflate(60), e -> e.getTags().contains("chess_piece"));
            for (Entity e : entities) {
                if (e.distanceToSqr(center) > 900) { // 30格
                    e.teleportTo(center.x, center.y + 2, center.z);
                    if (e instanceof LivingEntity le) le.setGlowingTag(true);
                }
            }
        });
    }

    private void spawnEnemyData(int round) {
        if (Minecraft.getInstance().player == null) return;
        int count = Math.min(round + 2, 8);
        for(int i=0; i<count; i++) {
            int enemyLevel = Math.min(10, (round / 2) + 1);
            UnitDefinition def = ApotheosisChessAdapter.createRandomUnit(Minecraft.getInstance().player, enemyLevel);
            ClientUnit cu = new ClientUnit(def, 1, def.cost);
            battleUnits.add(new BattleUnit(cu, false, i % COL, i / COL));
        }
    }

    // --- 战斗判定与结算 ---

    private void checkBattleResult() {
        if (battleTickCounter < BATTLE_GRACE_PERIOD || fixedArenaBase == null) return;
        runOnServer(() -> {
            ServerLevel level = getServerLevel();
            if (level == null) return;
            AABB arenaBox = new AABB(fixedArenaBase).inflate(40, 20, 40);
            List<LivingEntity> pieces = level.getEntitiesOfClass(LivingEntity.class, arenaBox,
                    e -> e.getTags().contains("chess_piece") && e.isAlive());

            int playerCount = 0, enemyCount = 0;
            for (LivingEntity e : pieces) {
                if (e.getTeam() == null) continue;
                if (TEAM_PLAYER.equals(e.getTeam().getName())) playerCount++;
                else if (TEAM_ENEMY.equals(e.getTeam().getName())) enemyCount++;
            }

            if (enemyCount == 0 && playerCount > 0) endBattle(true);
            else if (playerCount == 0 && enemyCount > 0) endBattle(false);
            else if (playerCount == 0 && enemyCount == 0) endBattle(false);
        });
    }

    private void endBattle(boolean playerWin) {
        state = State.PREPARE;
        round++;
        int baseGold = 5;
        int interest = Math.min(gold / 10, 5);
        int winGold = playerWin ? 1 : 0;
        if (playerWin) { winStreak++; lossStreak = 0; } else { lossStreak++; winStreak = 0; }
        int streakBonus = (winStreak >= 5 || lossStreak >= 5) ? 3 : ((winStreak >= 4 || lossStreak >= 4) ? 2 : ((winStreak >= 2 || lossStreak >= 2) ? 1 : 0));
        
        gold += baseGold + interest + winGold + streakBonus;
        if (!shopLocked) refreshShop(false);
        playSound(playerWin ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.VILLAGER_NO);
        
        Player player = Minecraft.getInstance().player;
        if (player != null) cleanupArena(player);
    }

    // --- 经济与棋子操作 ---

    private void refreshShop(boolean force) {
        if (Minecraft.getInstance().player == null) return;
        this.currentShop = ApotheosisChessAdapter.refreshShop(Minecraft.getInstance().player, this.level);
    }

    public boolean buyUnit(int index) {
        if (state == State.BATTLE || index < 0 || index >= currentShop.length) return false;
        ShopCard card = currentShop[index];
        if (card == null || gold < card.def.cost) return false;
        int slot = findEmptyBench();
        if (slot != -999) {
            gold -= card.def.cost;
            ClientUnit u = new ClientUnit(card.def, 1, card.def.cost);
            units.put(slot, u);
            currentShop[index] = null;
            checkMerge(u.def, 1);
            playSound(SoundEvents.EXPERIENCE_ORB_PICKUP);
            return true;
        }
        return false;
    }

    public void sellUnit(int index) {
        if (state == State.BATTLE) return;
        ClientUnit u = units.remove(index);
        if (u != null) {
            gold += u.baseCost;
            for (ItemStack stack : u.items) addItemToBench(stack);
            playSound(SoundEvents.SAND_BREAK);
        }
    }

    public void moveUnit(int from, int to) {
        if (state == State.BATTLE || from == to) return;
        if (from < 0 && to >= 0 && !units.containsKey(to)) {
            if (countUnitsOnBoard() >= level) return;
        }
        ClientUnit u = units.remove(from);
        if (u != null) {
            ClientUnit target = units.get(to);
            if (target != null) units.put(from, target);
            units.put(to, u);
            playSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM);
        }
    }

    private void checkMerge(UnitDefinition def, int star) {
        if (star >= 3) return;
        List<Integer> matches = new ArrayList<>();
        for (Map.Entry<Integer, ClientUnit> e : units.entrySet()) {
            if (e.getValue().def.type == def.type && e.getValue().star == star &&
                Objects.equals(e.getValue().def.sourceInvader, def.sourceInvader)) {
                matches.add(e.getKey());
            }
        }
        if (matches.size() >= 3) {
            int keep = matches.get(0);
            for(int i : matches) if (i >= 0) { keep = i; break; }
            ClientUnit base = units.get(keep);
            List<ItemStack> gatheredItems = new ArrayList<>(base.items);
            for(int i=1; i<3; i++) gatheredItems.addAll(units.get(matches.get(i)).items);
            units.remove(matches.get(0)); units.remove(matches.get(1)); units.remove(matches.get(2));
            
            ClientUnit newUnit = new ClientUnit(def, star + 1, base.baseCost * 3);
            for (ItemStack s : gatheredItems) {
                if (newUnit.items.size() < 3) newUnit.items.add(s); else addItemToBench(s);
            }
            units.put(keep, newUnit);
            playSound(SoundEvents.ANVIL_USE);
            checkMerge(def, star + 1);
        }
    }

    public void rollShop() { if (gold >= 2) { gold -= 2; refreshShop(true); playSound(SoundEvents.UI_BUTTON_CLICK.value()); } }
    public void buyXp() {
        if (gold >= 4) {
            gold -= 4; xp += 4;
            while (level < XP_TABLE.length && xp >= XP_TABLE[level]) { xp -= XP_TABLE[level]; level++; playSound(SoundEvents.PLAYER_LEVELUP); }
        }
    }
    public void toggleLock() { this.shopLocked = !this.shopLocked; playSound(SoundEvents.UI_BUTTON_CLICK.value()); }
    
    public void equipUnit(int unitIndex, ItemStack stack) {
        if (state == State.BATTLE) return;
        ClientUnit u = units.get(unitIndex);
        // 自走棋通常限制3个装备槽
        if (u != null && u.items.size() < 3) {
            u.items.add(stack);
            playSound(SoundEvents.ARMOR_EQUIP_IRON.value());
        } else {
            addItemToBench(stack);
        }
    }
    
    public void addItemToBench(ItemStack stack) {
        if (itemBench.size() < 10) itemBench.add(stack);
        else gold += 1;
    }

    private int findEmptyBench() { for (int i = -1; i >= -9; i--) if (!units.containsKey(i)) return i; return -999; }
    public int countUnitsOnBoard() { return (int)units.keySet().stream().filter(k -> k >= 0).count(); }
    
    public List<SynergyStatus> getSynergyReport() {
        List<SynergyStatus> list = new ArrayList<>();
        Map<String, Integer> traitCounts = new HashMap<>();
        units.forEach((pos, unit) -> {
            if (pos >= 0) {
                for(String t : unit.def.trait.split("/")) traitCounts.put(t, traitCounts.getOrDefault(t, 0) + 1);
            }
        });
        for (Synergy s : SYNERGIES) {
            int count = traitCounts.getOrDefault(s.id, 0);
            if (count > 0) list.add(new SynergyStatus(s, count));
        }
        list.sort((a, b) -> Boolean.compare(b.isActive(), a.isActive()));
        return list;
    }

    // --- 工具方法 ---
    
    private void runOnServer(Runnable action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) server.execute(action);
    }
    private ServerLevel getServerLevel() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null && Minecraft.getInstance().level != null) {
            return server.getLevel(Minecraft.getInstance().level.dimension());
        }
        return null;
    }
    private static void playSound(SoundEvent e) {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(e, (float) 1.0));
    }
    public void recordDamage(UUID uuid, EntityType<?> type, float amount) {
        if (state != State.BATTLE) return;
        damageStats.merge(uuid, amount, Float::sum);
        damageSources.computeIfAbsent(uuid, k -> type);
    }

    // --- Getters & Records ---
    
    public State getState() { return state; }
    public int getGold() { return gold; }
    public int getRound() { return round; }
    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public boolean isShopLocked() { return shopLocked; }
    public ShopCard[] getShop() { return currentShop; }
    public ClientUnit getUnit(int idx) { return units.get(idx); }
    public List<BattleUnit> getBattleUnits() { return battleUnits; }
    public int getWinStreak() { return winStreak; }
    public int getLossStreak() { return lossStreak; }
    public int getRarityColor(int cost) { return ApotheosisChessAdapter.getRarityColor(cost); }

    public record UnitDefinition(EntityType<?> type, int cost, String name, String trait, int baseHp, int baseDmg, float range, Invader sourceInvader, LootRarity sourceRarity) {}
    public static class ClientUnit {
        public UnitDefinition def;
        public int star;
        public int baseCost;
        public List<ItemStack> items = new ArrayList<>();
        public ClientUnit(UnitDefinition def, int star, int baseCost) { this.def = def; this.star = star; this.baseCost = baseCost; }
        public int star() { return star; }
        public int baseCost() { return baseCost; }
    }
    public record ShopCard(UnitDefinition def, UUID id) {}
    public static class Projectile {
        public float sx, sz, ex, ez; public int age;
        public Projectile(float sx, float sz, float ex, float ez) { this.sx=sx;this.sz=sz;this.ex=ex;this.ez=ez; }
    }
    public static class BattleUnit {
        public EntityType<?> type;
        public boolean isPlayer;
        public int gridCol, gridRow;
        public Invader sourceInvader;
        public LootRarity sourceRarity;
        public int star;
        public List<ItemStack> items = new ArrayList<>();
        public float x, z;
        public BattleUnit(ClientUnit u, boolean player, int c, int r)  {
            this.type = u.def.type;
            this.sourceInvader = u.def.sourceInvader;
            this.sourceRarity = u.def.sourceRarity;
            this.isPlayer = player;
            this.star = u.star;
            this.items.addAll(u.items);
            this.gridCol = c; this.gridRow = r;
            this.x = c; this.z = r;
        }
    }
}