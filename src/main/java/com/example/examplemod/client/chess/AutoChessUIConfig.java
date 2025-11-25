package com.example.examplemod.client.chess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public  class AutoChessUIConfig {

        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDirectory, "local_chess_ui.json");

        private static AutoChessUIConfig INSTANCE;

        // 存储所有 UI 元素的位置信息
        public Map<String, UIElement> elements = new HashMap<>();

        public static class UIElement {
            public String name;
            public float x, y, width, height;
            public boolean visible = true;

            public UIElement(String name, float x, float y, float w, float h) {
                this.name = name; this.x = x; this.y = y; this.width = w; this.height = h;
            }
        }

        public static AutoChessUIConfig get() {
            if (INSTANCE == null) {
                INSTANCE = new AutoChessUIConfig();
                INSTANCE.load();
            }
            return INSTANCE;
        }

        public void load() {
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    AutoChessUIConfig loaded = GSON.fromJson(reader, AutoChessUIConfig.class);
                    if (loaded != null) {
                        this.elements = loaded.elements;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void save() {
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 获取或创建默认配置
        public UIElement getElement(String key, float defaultX, float defaultY, float w, float h) {
            return elements.computeIfAbsent(key, k -> new UIElement(key, defaultX, defaultY, w, h));
        }
    }