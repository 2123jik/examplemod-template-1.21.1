package com.example.examplemod.client.chess;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

public class ChessAIHelper {

    public static void applyChessAI(Mob mob) {
        // 1. 清空原有的仇恨目标选择器 (Target Selector)
        // 这样它就不会去打玩家、村民或者乱七八糟的东西
        // 注意：这里需要 Accessor 或者反射来清空 goalSelector，简单起见我们直接添加高优先级的目标
        // 如果你能使用 Accessor 清空 wrappedGoals 会更完美
        
        // 2. 添加绝对优先的攻击目标：敌对队伍
        // 优先级 0 (最高)
        mob.targetSelector.addGoal(0, new ChessTargetGoal(mob));
        
        // 3. 让他不要乱跑 (可选，如果不想让它出圈)
        // mob.goalSelector.addGoal(...) 
    }

    // 自定义目标 AI：只攻击非队友的棋子
    static class ChessTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
        public ChessTargetGoal(Mob mob) {
            super(mob, LivingEntity.class, 0, true, false, null);
        }

        @Override
        public boolean canUse() {
            // 必须要有队伍才能工作
            if (this.mob.getTeam() == null) return false;
            return super.canUse();
        }

        @Override
        protected void findTarget() {
            // 1. 获取搜索半径
            double range = this.getFollowDistance();

            // 2. 构建 AABB 包围盒 (基于自身位置向四周扩大 range 的距离)
            // 需要导入: net.minecraft.world.phys.AABB
            net.minecraft.world.phys.AABB searchBox = this.mob.getBoundingBox().inflate(range);

            // 3. 调用 getNearestEntity
            this.target = this.mob.level().getNearestEntity(
                    this.targetType,
                    this.targetConditions.selector(this::isValidChessTarget),
                    this.mob,
                    this.mob.getX(),
                    this.mob.getEyeY(),
                    this.mob.getZ(),
                    searchBox // <--- 这里传入构建好的 AABB
            );
        }

        private boolean isValidChessTarget(LivingEntity target) {
            // 1. 忽略玩家
            if (target instanceof Player) return false;
            // 2. 忽略自己
            if (target == this.mob) return false;
            // 3. 必须是棋子 (有 tag)
            if (!target.getTags().contains("chess_piece")) return false;
            
            // 4. 必须是敌对队伍 (Team check)
            Team myTeam = this.mob.getTeam();
            Team targetTeam = target.getTeam();
            if (myTeam == null || targetTeam == null) return false;
            
            // 如果队伍不同，就是敌人
            return !myTeam.isAlliedTo(targetTeam);
        }
    }
}