package com.example.examplemod.accessor;

public interface IExtendedMobEffect {
    // 获取总时间
    int getOriginalDuration();
    // 设置总时间 (用于同步或初始化)
    void setOriginalDuration(int duration);
}