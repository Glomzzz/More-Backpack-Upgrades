package com.kzjy.mobackup.registry;

import com.kzjy.mobackup.MoBackup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式标签页注册表
 * 将模组物品归类到独立的创造模式标签页中
 */
public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MoBackup.MODID);

    // 注册主标签页
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MO_BACKUP_TAB = CREATIVE_MODE_TABS.register("mobackup_tab",
            () -> CreativeModeTab.builder()
                    // 使用磁吸升级作为标签图标
                    .icon(() -> new ItemStack(ModItems.DIMENSIONAL_MAGNET_UPGRADE.get()))
                    .title(Component.translatable("creativetab.mobackup_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        // 添加所有升级物品到标签页
                        pOutput.accept(ModItems.DIMENSIONAL_MAGNET_UPGRADE.get());
                        pOutput.accept(ModItems.DIMENSIONAL_PICKUP_UPGRADE.get());
                        pOutput.accept(ModItems.DIMENSIONAL_DEPOSIT_UPGRADE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
