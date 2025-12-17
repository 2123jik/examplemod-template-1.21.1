package com.example.examplemod.util;

import net.minecraft.resources.ResourceLocation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CompatibilityReport {
    private static final File REPORT_FILE = new File("spell_affix_compatibility_report.csv");
    private static BufferedWriter writer;

    public static void start() {
        try {
            writer = new BufferedWriter(new FileWriter(REPORT_FILE));
            // 写入表头
            writer.write("SpellID,TargetType,Status,ErrorMessage,ExceptionType\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(ResourceLocation spell, String targetType, boolean success, String errorMsg, String exceptionType) {
        if (writer == null) return;
        try {
            String line = String.format("%s,%s,%s,%s,%s\n",
                    spell.toString(),
                    targetType,
                    success ? "OK" : "CRASH",
                    success ? "" : errorMsg.replace(",", " "), // 防止逗号破坏CSV格式
                    success ? "" : exceptionType
            );
            writer.write(line);
            writer.flush(); // 实时写入，防止崩溃导致数据丢失
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void finish() {
        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}