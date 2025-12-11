package com.example.examplemod.client.tooltip;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// T: 目标对象类型 (MobEffect 或 AbstractSpell)
public abstract class BaseNameParser<T> implements ResourceManagerReloadListener {

    private final Map<String, T> nameToObjectMap = new HashMap<>();
    private List<String> sortedKeys = new ArrayList<>();
    
    // 关键修复：使用脏标记，避免在资源加载线程中调用 I18n 导致卡死
    private boolean isDirty = true;

    // === 需要子类实现的方法 ===
    /** 提供所有需要匹配的对象流 */
    protected abstract Stream<T> getRegistryStream();
    /** 提供对象的本地化名称 key (例如 effect.minecraft.poison) */
    protected abstract String getTranslationKey(T object);
    /** (可选) 允许子类过滤掉禁用的对象 */
    protected boolean shouldRegister(T object) { return true; }

    // === 逻辑实现 ===

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        // 只标记需要更新，不执行实际逻辑
        this.isDirty = true;
    }

    private void ensureLoaded() {
        if (!isDirty) return;

        nameToObjectMap.clear();
        getRegistryStream().forEach(obj -> {
            if (shouldRegister(obj)) {
                String key = getTranslationKey(obj);
                // 这里才真正调用 I18n，此时肯定是在渲染线程，绝对安全
                String localName = I18n.get(key);
                if (!localName.isEmpty()) {
                    nameToObjectMap.put(localName, obj);
                }
            }
        });

        // 长度倒序排序
        sortedKeys = nameToObjectMap.keySet().stream()
                .sorted((s1, s2) -> Integer.compare(s2.length(), s1.length()))
                .collect(Collectors.toList());

        isDirty = false;
    }

    public record ParseResult<T>(T object, String originalText, int index, String matchedName) {}

    public Optional<ParseResult<T>> findInText(String text) {
        if (text == null || text.isEmpty()) return Optional.empty();
        
        // 第一次渲染时会触发加载
        ensureLoaded();

        String rawText = text.trim();
        for (String name : sortedKeys) {
            int index = rawText.indexOf(name);
            if (index != -1) {
                T obj = nameToObjectMap.get(name);
                if (obj != null) {
                    return Optional.of(new ParseResult<>(obj, rawText, index, name));
                }
            }
        }
        return Optional.empty();
    }
}