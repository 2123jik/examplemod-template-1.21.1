package com.example.examplemod.client.tooltip;

import artifacts.registry.ModAttributes;
import com.example.examplemod.register.ModItems;
import com.gametechbc.gtbcs_geomancy_plus.api.init.GGAttributes;
import com.gametechbc.gtbcs_geomancy_plus.init.GGItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.xkmc.l2archery.init.L2Archery;
import dev.xkmc.l2archery.init.registrate.ArcheryItems;
import dev.xkmc.l2complements.init.registrate.LCItems;
import dev.xkmc.l2hostility.init.L2Hostility;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.warphan.iss_magicfromtheeast.registries.MFTEAttributeRegistries;
import net.warphan.iss_magicfromtheeast.registries.MFTEItemRegistries;
import twilightforest.TwilightForestMod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.shadowsoffire.apothic_enchanting.Ench.Items.ENDER_LEAD;
import static dev.xkmc.l2hostility.init.registrate.LHItems.BOTTLE_CURSE;

public class TooltipResolver {

    public static final TooltipResolver INSTANCE = new TooltipResolver();
    private boolean initialized = false;
    private static final Pattern DELIMITER_PATTERN = Pattern.compile("(?<segment>.*?)(?<delimiter>和|,|$)");
    @FunctionalInterface
    public interface IconDrawer {
        void draw(GuiGraphics guiGraphics, int x, int y, int size);
    }
    private final Map<String, IconDrawer> NAME_TO_DRAWER = new HashMap<>();
    private List<String> SORTED_KEYS = new ArrayList<>();
    private static final Map<SchoolType, List<AbstractSpell>> SCHOOL_SPELLS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Holder<Attribute>, ItemStack> ATTR_ITEM_MAP = new HashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> ATTR_EFFECT_ID_MAP = new HashMap<>();

    private synchronized void setupMappings() {
        registerItem(AttributeRegistry.FIRE_SPELL_POWER, ItemRegistry.FIRE_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.ICE_SPELL_POWER, ItemRegistry.ICE_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.LIGHTNING_SPELL_POWER,ItemRegistry.LIGHTNING_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.HOLY_SPELL_POWER, ItemRegistry.HOLY_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.ENDER_SPELL_POWER, ItemRegistry.ENDER_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.BLOOD_SPELL_POWER, ItemRegistry.BLOOD_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.EVOCATION_SPELL_POWER, ItemRegistry.EVOCATION_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.NATURE_SPELL_POWER, ItemRegistry.NATURE_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.MAX_MANA, ItemRegistry.MANA_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.COOLDOWN_REDUCTION, ItemRegistry.COOLDOWN_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.SPELL_RESIST, ItemRegistry.PROTECTION_UPGRADE_ORB.get());
        registerItem(AttributeRegistry.SPELL_POWER, ItemRegistry.UPGRADE_ORB.get());
        registerItem(AttributeRegistry.SUMMON_DAMAGE, Items.SPAWNER);
        registerItem(GGAttributes.GEO_SPELL_POWER, GGItems.GEO_UPGRADE_ORB.get());
        registerItem(MFTEAttributeRegistries.SPIRIT_SPELL_POWER, MFTEItemRegistries.SPIRIT_UPGRADE_ORB.get());
        registerItem(MFTEAttributeRegistries.SYMMETRY_SPELL_POWER, MFTEItemRegistries.SYMMETRY_UPGRADE_ORB.get());
        registerItem(Attributes.STEP_HEIGHT,Items.OAK_STAIRS);
        registerItem(ALObjects.Attributes.ARROW_DAMAGE, ArcheryItems.DESTROYER_ARROW.get());
        registerItem(ALObjects.Attributes.ARROW_VELOCITY, LCItems.CAPTURED_WIND.get());
        registerItem(ALObjects.Attributes.CRIT_DAMAGE, ModItems.WARLORD.get());
        registerItem(ALObjects.Attributes.CURRENT_HP_DAMAGE, com.cozary.nameless_trinkets.init.ModItems.GODS_CROWN.get());
        registerItem(Attributes.MINING_EFFICIENCY,Items.GOLDEN_PICKAXE);
        registerItem(Attributes.SNEAKING_SPEED,Items.SCULK_SHRIEKER);
        registerItem(Attributes.FLYING_SPEED,Items.FIREWORK_ROCKET);
        registerItem(ModAttributes.MOVEMENT_SPEED_ON_SNOW, Items.LEATHER_BOOTS);
        registerItem(LHMiscs.ADD_LEVEL, BOTTLE_CURSE.get());
        registerItem(Attributes.FOLLOW_RANGE, ENDER_LEAD.value());
        registerMapping("gtbcs_geomancy_plus:geo_magic_resist", "");
        registerMapping("irons_spellbooks:blood_magic_resist", "");
        registerMapping("irons_spellbooks:eldritch_magic_resist", "");
        registerMapping("irons_spellbooks:ender_magic_resist", "");
        registerMapping("irons_spellbooks:evocation_magic_resist", "");
        registerMapping("irons_spellbooks:fire_magic_resist", "");
        registerMapping("irons_spellbooks:holy_magic_resist", "");
        registerMapping("irons_spellbooks:ice_magic_resist", "");
        registerMapping("irons_spellbooks:lightning_magic_resist", "");
        registerMapping("irons_spellbooks:nature_magic_resist", "");
        registerMapping("irons_spellbooks:spell_resist", "");
        registerMapping("iss_magicfromtheeast:dune_magic_resist", "");
        registerMapping("iss_magicfromtheeast:spirit_magic_resist", "");
        registerMapping("iss_magicfromtheeast:symmetry_magic_resist", "");

        registerMapping("apothic_attributes:armor_pierce", "apothic_attributes:sundering");
        registerMapping("apothic_attributes:armor_shred", "l2complements:armor_corrosion");
        registerMapping("apothic_attributes:cold_damage", "twilightdelight:frozen_range");
        registerMapping("apothic_attributes:crit_chance", "dungeonsdelight:decisive");//todo
        registerMapping("apothic_attributes:dodge_chance", "irons_spellbooks:evasion");
        registerMapping("apothic_attributes:draw_speed", "l2archery:fast_pulling");
        registerMapping("apothic_attributes:elytra_flight", "irons_spellbooks:angel_wings");
        registerMapping("apothic_attributes:experience_gained", "apothic_attributes:knowledge");
        registerMapping("apothic_attributes:fire_damage", "twilightdelight:fire_range");
        registerMapping("apothic_attributes:healing_received", "apothic_attributes:vitality");
        registerMapping("apothic_attributes:life_steal", "examplemod:life_steal");
        registerMapping("apothic_attributes:mining_speed", "examplemod:mining_speed");
        registerMapping("apothic_attributes:overheal", "irons_spellbooks:fortify");
        registerMapping("apothic_attributes:projectile_damage", "examplemod:projectile_damage");
        registerMapping("apothic_attributes:prot_pierce", "gtbcs_geomancy_plus:erode");
        registerMapping("apothic_attributes:prot_shred", "gtbcs_geomancy_plus:erode");
        registerMapping("artifacts:generic.attack_burning_duration", "examplemod:attack_burning_duration");
        registerMapping("artifacts:generic.drinking_speed", "examplemod:drinking_speed");
        registerMapping("artifacts:generic.eating_speed", "examplemod:eating_speed");
        registerMapping("artifacts:generic.flatulence", "examplemod:flatulence");
        registerMapping("artifacts:generic.invincibility_ticks", "l2complements:cleanse");
        registerMapping("artifacts:generic.mount_speed", "examplemod:mount_speed");
        registerMapping("artifacts:player.entity_experience", "apothic_attributes:knowledge");
        registerMapping("artifacts:player.villager_reputation", "minecraft:hero_of_the_village");
        registerMapping("l2damagetracker:damage_absorption", "gtbcs_geomancy_plus:aegis");
        registerMapping("l2damagetracker:damage_reduction", "minecraft:resistance");
        registerMapping("modulargolems:golem_jump", "examplemod:golem_jump");
        registerMapping("aces_spell_utils:goliath_slayer", "examplemod:goliath_slayer");
        registerMapping("modulargolems:golem_regen", "minecraft:regeneration");

        registerMapping("artifacts:generic.slip_resistance", "");
        registerMapping("artifacts:generic.sprinting_speed", "");
        registerMapping("artifacts:generic.sprinting_step_height", "");
        registerMapping("apothic_attributes:ghost_health", "");
        registerMapping("irons_spellbooks:cast_time_reduction", "");
        registerMapping("irons_spellbooks:casting_movespeed", "");
        registerMapping("aces_spell_utils:evasive", "");
        registerMapping("aces_spell_utils:hunger_steal", "");
        registerMapping("aces_spell_utils:mana_rend", "");
        registerMapping("aces_spell_utils:mana_steal", "");
        registerMapping("aces_spell_utils:spell_res_penetration", "");
        registerMapping("irons_spellbooks:mana_regen", "");
        registerMapping("irons_spellbooks:summon_damage", "");
        registerMapping("l2damagetracker:bow_strength", "");
        registerMapping("l2damagetracker:crit_damage", "");
        registerMapping("l2damagetracker:crit_rate", "");
        registerMapping("l2damagetracker:explosion_damage", "");
        registerMapping("l2damagetracker:fire_damage", "");
        registerMapping("l2damagetracker:freezing_damage", "");
        registerMapping("l2damagetracker:lightning_damage", "");
        registerMapping("l2damagetracker:magic_damage", "");
        registerMapping("l2damagetracker:regen", "");
        registerMapping("l2weaponry:reflect_time", "");
        registerMapping("l2weaponry:shield_defense", "");
        registerMapping("modulargolems:golem_size", "");
        registerMapping("modulargolems:golem_sweep", "");
        registerMapping("neoforge:creative_flight", "");
        registerMapping("minecraft:generic.explosion_knockback_resistance", "");
        registerMapping("minecraft:generic.fall_damage_multiplier", "");
        registerMapping("minecraft:generic.gravity", "");
        registerMapping("minecraft:player.block_interaction_range", "");

        registerMapping("neoforge:swim_speed", "minecraft:dolphins_grace");
        registerMapping("minecraft:generic.armor", "examplemod:armor");
        registerMapping("minecraft:generic.armor_toughness", "examplemod:armor_toughness");
        registerMapping("minecraft:generic.attack_damage", "minecraft:strength");
        registerMapping("minecraft:generic.attack_knockback", "examplemod:attack_knockback");
        registerMapping("minecraft:generic.attack_speed", "examplemod:attack_speed");

        registerMapping("minecraft:generic.jump_strength", "minecraft:jump_boost");
        registerMapping("minecraft:generic.knockback_resistance", "examplemod:knockback_resistance");
        registerMapping("minecraft:generic.luck", "minecraft:luck");
        registerMapping("minecraft:generic.max_absorption", "minecraft:absorption");
        registerMapping("minecraft:generic.max_health", "minecraft:health_boost");

        registerMapping("minecraft:generic.movement_speed", "minecraft:speed");
        registerMapping("minecraft:generic.oxygen_bonus", "minecraft:water_breathing");
        registerMapping("minecraft:generic.scale", "examplemod:scale");

        registerMapping("minecraft:player.entity_interaction_range", "examplemod:block_interaction_range");
        registerMapping("minecraft:player.block_break_speed", "examplemod:mining_speed");
    }

    private static void registerItem(Holder<Attribute> attribute, Item item) {
        ATTR_ITEM_MAP.put(attribute, new ItemStack(item));
    }

    private static void registerMapping(String attrId, String effectId) {
        if (attrId != null && !attrId.isEmpty() && effectId != null && !effectId.isEmpty()) {
            ATTR_EFFECT_ID_MAP.put(ResourceLocation.tryParse(attrId), ResourceLocation.tryParse(effectId));
        }
    }

// TooltipResolver.java (新增方法)

    private static IconDrawer createDynamicShieldDrawer() {
        // 护盾模型的 ResourceLocation
        final ModelResourceLocation SHIELD_LOC = ModelResourceLocation.standalone(TwilightForestMod.prefix("item/shield"));
        final Direction[] DIRS = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};

        return (guiGraphics, x, y, size) -> {
            Minecraft mc = Minecraft.getInstance();
            BakedModel model = mc.getModelManager().getModel(SHIELD_LOC);

            if (model == mc.getModelManager().getMissingModel()) {
                return;
            }

            PoseStack stack = guiGraphics.pose();
            stack.pushPose();

            // --- 1. 计算动态旋转参数 (模拟 ShieldLayer) ---

            // 使用客户端时间模拟实体年龄
            float partialTicks = mc.getTimer().getGameTimeDeltaPartialTick(false);
            float age = (float)mc.player.tickCount + partialTicks;

            // 提取 ShieldLayer 的旋转逻辑
            float rotateAngleY = age / -5.0F;
            float rotateAngleX = Mth.sin(age / 5.0F) / 4.0F;
            float rotateAngleZ = Mth.cos(age / 5.0F) / 4.0F;

            // 护盾数量固定为 1 (只渲染一个图标)
            int count = 1;
            int c = 0; // 索引固定为 0

            // --- 2. 转换到 Tooltip 空间 ---

            // 物品渲染通常是 16x16，我们假设模型也是基于此。
            float scale = (float) size / 16.0f;

            // 移动到目标位置 (x, y)
            stack.translate(x, y, 100.0);

            // 居中模型 (9x9 居中)
            stack.translate(size / 2.0f, size / 2.0f, 0);

            // 缩放
            stack.scale(scale, scale, scale);

            // --- 3. 应用 ShieldLayer 的旋转和偏移 ---

            // 旋转 (注意：ShieldLayer 使用弧度，但 rotationDegrees 接受度数，所以需要转换)
            // 180F / (float)Math.PI 是弧度转度数的因子

            // Z 轴旋转
            stack.mulPose(Axis.ZP.rotationDegrees(180.0F + rotateAngleZ * (180F / (float)Math.PI)));

            // Y 轴旋转 (这是主要的旋转动画)
            stack.mulPose(Axis.YP.rotationDegrees(rotateAngleY * (180F / (float)Math.PI) + (float)c * (360.0F / (float)count)));

            // X 轴旋转
            stack.mulPose(Axis.XP.rotationDegrees(rotateAngleX * (180F / (float)Math.PI)));

            // 偏移 (ShieldLayer 的内部偏移，用于将护盾放在实体周围)
            // 我们需要调整这个偏移，使其在 Tooltip 空间内居中且可见

            // 原始偏移：
            // stack.translate(-0.5F, -0.65F, -0.5F);
            // stack.translate(0.0F, 0.0F, -0.7F);

            // 调整后的偏移：
            // 目标是让模型中心 (0, 0, 0) 位于屏幕中心。
            // 护盾模型本身可能在 (-0.5, -0.5, -0.5) 处，所以我们先移回原点。
            stack.translate(-0.5F, -0.5F, -0.5F);

            // 额外的 Z 轴拉远，确保模型不会被裁剪
            stack.translate(0, 0, -0.7F);

            // --- 4. 渲染模型 ---

            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

            for(Direction dir : DIRS) {
                mc.getItemRenderer().renderQuadList(
                        stack,
                        bufferSource.getBuffer(Sheets.translucentCullBlockSheet()),
                        model.getQuads(null, dir, mc.level.getRandom(), ModelData.EMPTY, Sheets.translucentCullBlockSheet()),
                        ItemStack.EMPTY,
                        15728880, // Max Light
                        OverlayTexture.NO_OVERLAY
                );
            }

            // 刷新缓冲区
            bufferSource.endBatch();

            stack.popPose();
        };
    }

    private synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        setupMappings();
        NAME_TO_DRAWER.put("护盾", createDynamicShieldDrawer());
        NAME_TO_DRAWER.clear();
        SCHOOL_SPELLS_CACHE.clear();

        // 1. 处理属性 (Attributes)
        BuiltInRegistries.ATTRIBUTE.holders().forEach(attrHolder -> {
            String name = I18n.get(attrHolder.value().getDescriptionId());
            if (name.isEmpty()) return;

            // 优先级 A: 手动配置的物品图标
            if (ATTR_ITEM_MAP.containsKey(attrHolder)) {
                NAME_TO_DRAWER.put(name, createItemDrawer(ATTR_ITEM_MAP.get(attrHolder)));
                return;
            }

            // 优先级 B: 映射到药水效果
            ResourceLocation attrId = attrHolder.unwrapKey().map(k -> k.location()).orElse(null);
            if (attrId != null && ATTR_EFFECT_ID_MAP.containsKey(attrId)) {
                ResourceLocation effectId = ATTR_EFFECT_ID_MAP.get(attrId);
                BuiltInRegistries.MOB_EFFECT.getHolder(effectId).ifPresent(effectHolder ->
                        NAME_TO_DRAWER.put(name, createEffectDrawer(effectHolder))
                );
            }
        });

        // 2. 处理药水效果 (Mob Effects)
        BuiltInRegistries.MOB_EFFECT.holders().forEach(effectHolder -> {
            String name = I18n.get(effectHolder.value().getDescriptionId());
            if (!name.isEmpty() && !NAME_TO_DRAWER.containsKey(name)) {
                NAME_TO_DRAWER.put(name, createEffectDrawer(effectHolder));
            }
        });

        // 3. 处理法术 (Spells)
        // 此时配置应该已经加载，可以直接调用 isEnabled()
        SpellRegistry.REGISTRY.stream()
                .filter(AbstractSpell::isEnabled)
                .forEach(spell -> {
                    String name = I18n.get(spell.getComponentId());
                    if (!name.isEmpty()) {
                        NAME_TO_DRAWER.put(name, createSpellDrawer(spell));
                    }
                });

        // 4. 处理法术学派 (Schools)
        SpellRegistry.REGISTRY.stream()
                .filter(AbstractSpell::isEnabled) // 确保只处理启用的法术
                .map(AbstractSpell::getSchoolType)
                .distinct()
                .forEach(school -> {
                    String name = school.getDisplayName().getString();
                    if (!name.isEmpty()) {
                        IconDrawer drawer = createSchoolDrawer(school);
                        if (drawer != null) {
                            NAME_TO_DRAWER.put(name, drawer);
                        }
                    }
                });

        // 生成排序后的 Key 列表 (长度倒序，实现最长匹配原则)
        SORTED_KEYS = NAME_TO_DRAWER.keySet().stream()
                .sorted((s1, s2) -> Integer.compare(s2.length(), s1.length()))
                .collect(Collectors.toList());

        initialized = true;
    }

    public IconDrawer getDrawer(String unlocalizedKey) {
        // 确保数据已加载 (触发懒加载)
        ensureInitialized();

        // 属性名称需要先进行本地化，因为 NAME_TO_DRAWER 存储的是本地化后的名称
        String localizedName = I18n.get(unlocalizedKey);

        return NAME_TO_DRAWER.get(localizedName);
    }


    public List<Object> parseText(String text) {
        ensureInitialized();
        if (text == null || text.isEmpty()) return Collections.emptyList();

        List<Object> finalSegments = new ArrayList<>();

        // 使用 Matcher 迭代文本，同时捕获文本段和分隔符
        Matcher matcher = DELIMITER_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            String segment = matcher.group(1); // 文本段
            String delimiter = matcher.group(2); // 分隔符 ("和", ",", 或空字符串 $ 结束)

            // 确保我们处理的是新的文本段
            if (matcher.start() >= lastEnd) {

                // 1. 处理文本段：进行迭代匹配替换
                if (segment != null && !segment.trim().isEmpty()) {
                    // 注意：parseSegment 内部应该保留输入文本的原始空格，
                    // 以免丢失文本间的间距。
                    finalSegments.addAll(parseSegment(segment));
                }

                // 2. 添加分隔符 (如果存在)
                if (delimiter != null && !delimiter.isEmpty()) {
                    finalSegments.add(delimiter);
                }

                lastEnd = matcher.end();
            }

            // 如果匹配到文本末尾的空分隔符，则退出
            if (delimiter == null || delimiter.isEmpty()) {
                break;
            }
        }

        // 清理末尾可能多余的空格（如果 DELIMITER_PATTERN 允许捕获多余空格的话）
        // 建议：如果不需要全局去除字符串两端的空格，移除此步骤，或仅对末尾元素操作。
        return finalSegments.stream()
                .map(obj -> obj instanceof String s ? s.trim() : obj)
                .collect(Collectors.toList());
    }


    private List<Object> parseSegment(String segment) {
        List<Object> result = new ArrayList<>();
        String remainingText = segment;

        // 迭代查找并替换，直到找不到任何关键字
        while (!remainingText.isEmpty()) {
            String bestKey = null;
            IconDrawer bestDrawer = null;
            // 初始化为最大值，我们寻找最小的起始索引（即最早出现的匹配）
            int bestIndex = Integer.MAX_VALUE;
            int bestLength = 0;

            // 1. 查找当前剩余文本中的最佳匹配
            // 必须遍历所有的键，以找到最早出现的那个。
            // 如果 SORTED_KEYS 确实包含了所有键（不论长短），则使用它。
            for (String key : SORTED_KEYS) {
                int index = remainingText.indexOf(key);

                if (index != -1) {
                    if (index < bestIndex) {
                        // 发现了一个更早的匹配，直接更新为最佳
                        bestIndex = index;
                        bestKey = key;
                        bestLength = key.length();
                        bestDrawer = NAME_TO_DRAWER.get(key);
                    } else if (index == bestIndex) {
                        // 索引相同 (重叠匹配)，根据长度倒序的特性，
                        // 我们只需要检查当前 key 是否比已记录的 bestKey 更长。
                        // (注：由于我们按长度倒序遍历，如果 index == bestIndex，
                        // 那么当前的 key 长度必然 >= 已记录的 bestKey 长度，
                        // 除非 bestKey 是在之前的循环中因更小的 index 被设置的。)

                        // 明确的长度检查更健壮，确保在重叠时，最长的关键字胜出。
                        if (key.length() > bestLength) {
                            bestKey = key;
                            bestLength = key.length();
                            bestDrawer = NAME_TO_DRAWER.get(key);
                        }
                    }
                }
            }

            // 2. 应用替换
            if (bestKey != null && bestIndex != Integer.MAX_VALUE) {
                // 添加关键字前面的文本
                if (bestIndex > 0) {
                    result.add(remainingText.substring(0, bestIndex));
                }

                // 添加图标和关键字
                result.add(bestDrawer);
                result.add(bestKey);

                // 更新剩余文本，从关键字之后开始
                remainingText = remainingText.substring(bestIndex + bestKey.length());
            } else {
                // 找不到更多匹配项，添加剩余文本并退出循环
                result.add(remainingText);
                remainingText = "";
            }
        }

        return result;
    }

    // --- 创建 Drawer 的辅助方法 ---

    private static IconDrawer createItemDrawer(ItemStack stack) {
        return (guiGraphics, x, y, size) -> {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            // 物品默认16，如果size是9或10，需要缩放
            float scale = (float) size / 16.0f;
            pose.scale(scale, scale, 1.0f);
            guiGraphics.renderFakeItem(stack, 0, 0);
            pose.popPose();
        };
    }

    private static IconDrawer createEffectDrawer(Holder<MobEffect> effect) {
        return (guiGraphics, x, y, size) -> {
            TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            RenderSystem.enableBlend();
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            float scale = (float) size / 18.0f; // 药水原图18
            pose.scale(scale, scale, 1.0f);
            guiGraphics.blit(0, 0, 0, 18, 18, sprite);
            pose.popPose();
        };
    }

    private static IconDrawer createSpellDrawer(AbstractSpell spell) {
        return (guiGraphics, x, y, size) -> {
            ResourceLocation iconRes = spell.getSpellIconResource();
            RenderSystem.setShaderTexture(0, iconRes);
            RenderSystem.enableBlend();
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            float scale = (float) size / 16.0f; // 法术原图16
            pose.scale(scale, scale, 1.0f);
            guiGraphics.blit(iconRes, 0, 0, 0, 0, 16, 16, 16, 16);
            pose.popPose();
        };
    }

    private static IconDrawer createSchoolDrawer(SchoolType school) {
        // 缓存构建逻辑也简化，直接使用 isEnabled()
        List<AbstractSpell> spells = SCHOOL_SPELLS_CACHE.computeIfAbsent(school, s ->
                SpellRegistry.REGISTRY.stream()
                        .filter(spell -> spell.getSchoolType() == s && spell.isEnabled())
                        .collect(Collectors.toList())
        );

        if (spells.isEmpty()) return null;

        return (guiGraphics, x, y, size) -> {
            // 每秒切换一次图标
            long index = (System.currentTimeMillis() / 1000) % spells.size();
            AbstractSpell spell = spells.get((int) index);
            createSpellDrawer(spell).draw(guiGraphics, x, y, size);
        };
    }
}