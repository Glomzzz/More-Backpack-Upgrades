package com.kzjy.mobackup.registry;

import com.kzjy.mobackup.Config;
import com.kzjy.mobackup.MoBackup;
import com.kzjy.mobackup.item.DimensionalDepositUpgradeItem;
import com.kzjy.mobackup.item.DimensionalMagnetUpgradeItem;
import com.kzjy.mobackup.item.DimensionalPickupUpgradeItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册表
 * 集中注册模组所有的自定义物品（升级卡）
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(MoBackup.MODID);

    // 注册次元磁吸升级
    public static final DeferredHolder<Item, DimensionalMagnetUpgradeItem> DIMENSIONAL_MAGNET_UPGRADE = ITEMS.register(
            "dimensional_magnet_upgrade",
            () -> new DimensionalMagnetUpgradeItem(Config.dimensionalMagnetRange::get,
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedMagnetUpgrade.filterSlots::get,
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.maxUpgradesPerStorage));

    // 注册次元拾取升级
    public static final DeferredHolder<Item, DimensionalPickupUpgradeItem> DIMENSIONAL_PICKUP_UPGRADE = ITEMS.register(
            "dimensional_pickup_upgrade",
            () -> new DimensionalPickupUpgradeItem(
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedPickupUpgrade.filterSlots::get,
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.maxUpgradesPerStorage));

    // 注册次元卸货升级
    public static final DeferredHolder<Item, DimensionalDepositUpgradeItem> DIMENSIONAL_DEPOSIT_UPGRADE = ITEMS.register(
            "dimensional_deposit_upgrade",
            () -> new DimensionalDepositUpgradeItem(
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedDepositUpgrade.filterSlots::get));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
