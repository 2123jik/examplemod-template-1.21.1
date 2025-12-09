package com.example.examplemod.server.loot;

import com.mojang.logging.LogUtils; // 引入日志
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry; // 引入 Registry
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import java.util.Optional;

public class ReplaceItemWithTagModifier extends LootModifier {

    private static final Logger LOGGER = LogUtils.getLogger(); // 日志记录器

    public static final MapCodec<ReplaceItemWithTagModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(
                    inst.group(
                            BuiltInRegistries.ITEM.byNameCodec().fieldOf("target").forGetter(m -> m.targetItem),
                            TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(m -> m.replacementTag)
                    )
            ).apply(inst, ReplaceItemWithTagModifier::new)
    );

    private final Item targetItem;
    private final TagKey<Item> replacementTag;

    public ReplaceItemWithTagModifier(LootItemCondition[] conditionsIn, Item targetItem, TagKey<Item> replacementTag) {
        super(conditionsIn);
        this.targetItem = targetItem;
        this.replacementTag = replacementTag;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // --- 调试日志 ---
        // 如果控制台连这句话都没印，说明 JSON 配置里的 conditions 不满足，或者没有在 global_loot_modifiers.json 注册
        // LOGGER.info("Modifier Running for: {}, Trying to replace: {}", context.getQueriedLootTableId(), targetItem);

        // 1. 关键修复：从 Context 获取 RegistryAccess，而不是用静态的 BuiltInRegistries
        Registry<Item> itemRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.ITEM);

        // 2. 获取 Tag 内容
        Optional<HolderSet.Named<Item>> tagContent = itemRegistry.getTag(this.replacementTag);

        // 检查 Tag 是否存在且有内容
        if (tagContent.isEmpty() || tagContent.get().size() == 0) {
            // LOGGER.warn("Tag is empty or missing: {}", replacementTag.location());
            return generatedLoot;
        }

        ObjectArrayList<ItemStack> newLoot = new ObjectArrayList<>();
        boolean changed = false;

        for (ItemStack stack : generatedLoot) {
            // 检查是否是目标物品
            if (stack.is(this.targetItem)) {
                // 3. 随机取样
                Optional<Holder<Item>> randomItem = tagContent.get().getRandomElement(context.getRandom());

                if (randomItem.isPresent()) {
                    newLoot.add(new ItemStack(randomItem.get(), stack.getCount()));
                    changed = true;
                } else {
                    newLoot.add(stack);
                }
            } else {
                newLoot.add(stack);
            }
        }
        return newLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}