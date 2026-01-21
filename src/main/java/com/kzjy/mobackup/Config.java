package com.kzjy.mobackup;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置类
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue dimensionalMagnetRange = BUILDER
            .comment("次元磁吸升级的吸附范围 | The range of Dimensional Magnet Upgrade")
            .defineInRange("dimensionalMagnetRange", 5, 1, 64);


    public static final ModConfigSpec SPEC = BUILDER.build();
}