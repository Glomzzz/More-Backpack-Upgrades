package com.kzjy.mobackup.item;

import com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import java.util.function.IntSupplier;

/**
 * 次元卸货升级物品
 * 继承自 Sophisticated Backpacks 的卸货升级，实现与 RS 网络的联动
 */
public class DimensionalDepositUpgradeItem extends DepositUpgradeItem {
    // 注册自定义的 Wrapper 类型
    private static final UpgradeType<DepositUpgradeWrapper> TYPE = new UpgradeType<>(
            DimensionalDepositUpgradeWrapper::new);

    public DimensionalDepositUpgradeItem(IntSupplier filterSlotCount) {
        super(filterSlotCount);
    }

    @Override
    public UpgradeType<DepositUpgradeWrapper> getType() {
        return TYPE;
    }

    // 限制每个背包只能放 1 个此类升级
    @Override
    public int getUpgradesPerStorage(String storageType) {
        return 1;
    }

    @Override
    public int getUpgradesInGroupPerStorage(String storageType) {
        return 1;
    }

    // 设置物品稀有度为史诗（紫色）
//    @Override
//    public Rarity getRarity(ItemStack stack) {
//        return Rarity.EPIC;
//    }
}
