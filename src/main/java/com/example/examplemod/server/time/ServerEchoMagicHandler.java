package com.example.examplemod.server.time;

import com.example.examplemod.init.ModEffects;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber
public class ServerEchoMagicHandler {

    // 标记位：当前是否正在执行残影施法
    public static final ThreadLocal<Boolean> IS_ECHOING = ThreadLocal.withInitial(() -> false);

    private static class EchoTask {
        public final ServerPlayer player;
        public final String spellId;
        public final int spellLevel;
        public final CastSource castSource;
        public final Vec3 originPos;
        public final Vec2 originRot;
        public int ticksRemaining;

        public EchoTask(ServerPlayer player, String spellId, int spellLevel, CastSource source, int delayTicks) {
            this.player = player;
            this.spellId = spellId;
            this.spellLevel = spellLevel;
            this.castSource = source;
            this.originPos = player.position();
            this.originRot = new Vec2(player.getXRot(), player.getYRot());
            this.ticksRemaining = delayTicks;
        }
    }

    private static final List<EchoTask> PENDING_TASKS = new ArrayList<>();

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (IS_ECHOING.get()) return; // 防止残影施法再次被记录

        if (event.getEntity() instanceof ServerPlayer player) {
             if (!player.hasEffect(ModEffects.MAKEN_POWER)) return;

            // 0.8秒 = 16 ticks
            PENDING_TASKS.add(new EchoTask(
                player, event.getSpellId(), event.getSpellLevel(), event.getCastSource(), 16
            ));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide || PENDING_TASKS.isEmpty()) return;

        Iterator<EchoTask> it = PENDING_TASKS.iterator();
        while (it.hasNext()) {
            EchoTask task = it.next();
            if (task.player.level() != event.getLevel() || task.player.isRemoved()) {
                if (task.player.isRemoved()) it.remove();
                continue;
            }

            task.ticksRemaining--;
            if (task.ticksRemaining <= 0) {
                executeEcho(task);
                it.remove();
            }
        }
    }

    private static void executeEcho(EchoTask task) {
        AbstractSpell spell = SpellRegistry.getSpell(task.spellId);
        if (spell == null) return;

        // 拦截位移技能，防止玩家本体乱飞
        if (isMovementSpell(spell)) return;

        ServerPlayer player = task.player;
        Vec3 realPos = player.position();
        float realXRot = player.getXRot();
        float realYRot = player.getYRot();
        float realYHead = player.getYHeadRot();

        try {
            IS_ECHOING.set(true);

            // 1. 瞬移回过去
            player.setPos(task.originPos.x, task.originPos.y, task.originPos.z);
            player.setXRot(task.originRot.x);
            player.setYRot(task.originRot.y);
            player.setYHeadRot(task.originRot.y);

            // 2. 强制施法
            MagicData magicData = MagicData.getPlayerMagicData(player);
            spell.onCast(player.level(), task.spellLevel, player, task.castSource, magicData);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 3. 还原位置
            player.setPos(realPos.x, realPos.y, realPos.z);
            player.setXRot(realXRot);
            player.setYRot(realYRot);
            player.setYHeadRot(realYHead);
            
            IS_ECHOING.set(false);
        }
    }

    private static boolean isMovementSpell(AbstractSpell spell) {
        String id = spell.getSpellId().toLowerCase();
        return id.contains("teleport") || id.contains("dash") || id.contains("charge") || id.contains("blink") || id.contains("step");
    }
}