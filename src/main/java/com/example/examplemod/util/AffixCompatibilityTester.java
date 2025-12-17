package com.example.examplemod.util;

import com.whisent.powerful_dummy.Powerful_dummy;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.warphan.iss_magicfromtheeast.ISS_MagicFromTheEast;

import java.util.ArrayList;
import java.util.List;

import static com.whisent.powerful_dummy.entity.DummyEntityRegistry.TEST_DUMMY;
import static io.redspace.ironsspellbooks.IronsSpellbooks.id;

@EventBusSubscriber
public class AffixCompatibilityTester {

    private static boolean isRunning = false;
    private static List<AbstractSpell> spellQueue = new ArrayList<>();
    private static int currentIndex = 0;
    private static ServerPlayer testerPlayer;
    private static LivingEntity dummyTarget;
    private static int tickDelay = 0;

    public static void start(ServerPlayer player) {
        if (isRunning) {
            player.sendSystemMessage(Component.literal("测试已经在进行中了！"));
            return;
        }

        testerPlayer = player;
        spellQueue = new ArrayList<>(SpellRegistry.getEnabledSpells());
        currentIndex = 0;
        isRunning = true;
        tickDelay = 0;

        // 生成测试假人
        createDummy();

        // 初始化报告文件
        CompatibilityReport.start();
        player.sendSystemMessage(Component.literal("开始队列测试，共有 " + spellQueue.size() + " 个法术待测..."));
    }

    private static void createDummy() {
        if (dummyTarget == null || !dummyTarget.isAlive()) {
            var cow = TEST_DUMMY.get().create(testerPlayer.level());
            if (cow != null) {
                cow.setPos(testerPlayer.getX() + 3, testerPlayer.getY(), testerPlayer.getZ());
                cow.setNoAi(true);
                cow.setInvulnerable(true); // 无敌
                testerPlayer.level().addFreshEntity(cow);
                dummyTarget = cow;
            }
        }
    }

    private static void stop() {
        isRunning = false;
        spellQueue.clear();
        if (dummyTarget != null) {
            dummyTarget.discard(); // 清理假人
            dummyTarget = null;
        }
        CompatibilityReport.finish();
        if (testerPlayer != null) {
//            testerPlayer.sendSystemMessage(Component.literal("测试结束！请查看游戏根目录下的 spell_affix_compatibility_report.csv"));
        }
        testerPlayer = null;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!isRunning || testerPlayer == null) return;

        // 稍微加点延迟，每 2 ticks 测一个，给服务器喘息时间
        if (tickDelay > 0) {
            tickDelay--;
            return;
        }
        tickDelay = 4;

        // 检查进度
        if (currentIndex >= spellQueue.size()) {
            stop();
            return;
        }

        // 确保假人还在（防止被某个法术意外删除了）
        if (dummyTarget == null || !dummyTarget.isAlive()) {
            createDummy();
        }
        // 确保假人满血（防止某些法术绕过无敌）
        dummyTarget.setHealth(dummyTarget.getMaxHealth());
        testerPlayer.setHealth(testerPlayer.getMaxHealth());

        AbstractSpell spell = spellQueue.get(currentIndex);

        // --- 执行测试 ---
        runSafeTest(spell, testerPlayer, "SELF");
        runSafeTest(spell, dummyTarget, "TARGET");
        // ----------------

        currentIndex++;

        // 每 20 个法术提示一次进度
        if (currentIndex % 20 == 0) {
            testerPlayer.sendSystemMessage(Component.literal("进度: " + currentIndex + " / " + spellQueue.size()));
        }
    }

    private static void runSafeTest(AbstractSpell spell, LivingEntity target, String typeName) {
        try {
            var c = spell.getSpellResource();
            if
            (
                    c.equals(id("blood_step")) ||
                            c.equals(id("teleport")) ||
                            c.equals(id("frost_step")) ||
                            c.equals(id("pocket_dimension")) ||
                            c.equals(ISS_MagicFromTheEast.id("throw_up")) ||
                            c.equals(ISS_MagicFromTheEast.id("sword_dance")) ||
                            c.equals(ResourceLocation.fromNamespaceAndPath("darkdoppelganger", "doppel_portal")) ||
                            c.equals(ISS_MagicFromTheEast.id("qigong_controlling")) ||
                            c.equals(ISS_MagicFromTheEast.id("bagua_array_circle")) ||
                            c.equals(ISS_MagicFromTheEast.id("dragon_glide"))
            ) return;
            boolean success = SpellCastUtil.castSpell(testerPlayer, spell, 1, target);

            if (success) {
                // 成功了就不刷屏记录了，或者只记录到 log
                CompatibilityReport.log(spell.getSpellResource(), typeName, true, "OK", "");
            }
        } catch (Throwable t) {
            // 这里是第二道防线，防止 castSpell 内部的 try-catch 没兜住（虽然理论上不应该）
            CompatibilityReport.log(spell.getSpellResource(), typeName, false, "CRITICAL ENGINE CRASH: " + t.getMessage(), t.getClass().getSimpleName());
        }
    }
}