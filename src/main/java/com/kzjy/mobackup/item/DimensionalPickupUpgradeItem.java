package com.kzjy.mobackup.item;

import com.kzjy.mobackup.wrapper.DimensionalPickupUpgradeWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntSupplier;

/**
 * 次元拾取升级物品
 * 继承自核心模组的拾取升级，拾取物品时直接尝试存入绑定的 RS 网络
 */
public class DimensionalPickupUpgradeItem extends PickupUpgradeItem implements IRSLinkedItem {
    public static final UpgradeType<PickupUpgradeWrapper> TYPE = new UpgradeType<>(
            DimensionalPickupUpgradeWrapper::new);

    public DimensionalPickupUpgradeItem(IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
        super(filterSlotCount, upgradeTypeLimitConfig);
    }

    @Override
    public UpgradeType<PickupUpgradeWrapper> getType() {
        return TYPE;
    }

    /**
     * 处理右键交互，支持绑定 RS 控制器
     */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        InteractionResult result = handleUseOn(ctx);
        if (result != InteractionResult.PASS) {
            return result;
        }
        return super.useOn(ctx);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        IRSLinkedItem.super.appendHoverText(stack, tooltip);
    }

    @Override
    public int getUpgradesPerStorage(String storageType) {
        return 1;
    }

    @Override
    public int getUpgradesInGroupPerStorage(String storageType) {
        return 1;
    }

//    @Override
//    public Rarity getRarity(ItemStack stack) {
//        return Rarity.EPIC;
//    }

}
