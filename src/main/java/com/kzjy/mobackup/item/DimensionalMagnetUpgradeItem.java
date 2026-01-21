package com.kzjy.mobackup.item;

import com.kzjy.mobackup.wrapper.DimensionalMagnetUpgradeWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.IntSupplier;

/**
 * 次元磁吸升级物品
 * 继承自核心模组的磁吸升级，增加了与 RS 网络绑定的能力
 */
public class DimensionalMagnetUpgradeItem extends MagnetUpgradeItem implements IRSLinkedItem {
    // 注册自定义 Wrapper，这里使用了泛型技巧：
    // 虽然泛型声明为 MagnetUpgradeWrapper，但实际上构造的是 DimensionalMagnetUpgradeWrapper 子类
    public static final UpgradeType<MagnetUpgradeWrapper> TYPE = new UpgradeType<>(
            DimensionalMagnetUpgradeWrapper::new);

    public DimensionalMagnetUpgradeItem(IntSupplier radius, IntSupplier filterSlotCount,
            IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
        super(radius, filterSlotCount, upgradeTypeLimitConfig);
    }

    @Override
    public @NotNull UpgradeType<MagnetUpgradeWrapper> getType() {
        return TYPE;
    }

    /**
     * 处理右键方块交互
     * 用于绑定 RS 控制器
     */
    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext ctx) {
        InteractionResult result = handleUseOn(ctx);
        if (result != InteractionResult.PASS) {
            return result;
        }
        return super.useOn(ctx);
    }

    /**
     * 添加物品提示信息
     * 显示绑定的 RS 网络坐标
     */

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
                super.appendHoverText(stack, context, tooltip, flagIn);
        IRSLinkedItem.super.appendHoverText(stack, tooltip);
    }

    @Override
    public int getUpgradesPerStorage(@NotNull String storageType) {
        return 1;
    }

    @Override
    public int getUpgradesInGroupPerStorage(@NotNull String storageType) {
        return 1;
    }

//    @Override
//    public Rarity getRarity(ItemStack stack) {
//        return Rarity.EPIC;
//    }

}
