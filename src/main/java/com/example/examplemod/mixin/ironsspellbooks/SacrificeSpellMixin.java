package com.example.examplemod.mixin.ironsspellbooks;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.spells.blood.SacrificeSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SacrificeSpell.class)
public abstract class SacrificeSpellMixin {

    @Inject(method = "getUniqueInfo", at = @At("HEAD"), cancellable = true)
    public void getUniqueInfoMixin(int spellLevel, LivingEntity caster, CallbackInfoReturnable<List<MutableComponent>> cir) {

        // 法术等级加成
        float spellPower = ((SacrificeSpell)(Object)this).getSpellPower(spellLevel, caster);

        // 施法者召唤物伤害属性
        float summonDamageAttr = caster != null ? (float)caster.getAttributeValue(AttributeRegistry.SUMMON_DAMAGE) : 1f;

        // 流派法术加成属性
        float schoolPowerAttr = caster != null ? (float) ((SacrificeSpell) (Object) this).getSchoolType().getPowerFor(caster) : 1f;

        // 基础伤害计算（10 + 法术等级加成 × 属性）
        float baseDamage = (10 + spellPower) * summonDamageAttr * schoolPowerAttr;

        // 构建公式文本
        String formulaText = String.format(
            "实际伤害 = (10 + (%d + 1) × %.2f × %.2f) × %.2f + 召唤物当前生命值 × 50%% ) × 爆炸衰减 × %.2f",
            spellLevel, spellPower, schoolPowerAttr, summonDamageAttr, summonDamageAttr
        );

        MutableComponent damageInfo = Component.literal(formulaText)
                .withStyle(ChatFormatting.RED);

        MutableComponent radiusInfo = Component.translatable("ui.irons_spellbooks.radius", 3)
                .withStyle(ChatFormatting.GREEN);

        cir.setReturnValue(List.of(damageInfo, radiusInfo));
    }
}
