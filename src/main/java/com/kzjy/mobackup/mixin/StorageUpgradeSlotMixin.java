package com.kzjy.mobackup.mixin;

import com.kzjy.mobackup.item.DimensionalDepositUpgradeItem;
import com.kzjy.mobackup.item.DimensionalMagnetUpgradeItem;
import com.kzjy.mobackup.item.DimensionalPickupUpgradeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase.StorageUpgradeSlot.class)
/**
 * 限制可放入背包升级槽的物品类型
 * 仅允许次元磁吸、拾取
 */
public class StorageUpgradeSlotMixin {
    @Inject(method = "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void mobackup$mayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Item item = stack.getItem();
        if (item instanceof DimensionalMagnetUpgradeItem || item instanceof DimensionalPickupUpgradeItem
                || item instanceof DimensionalDepositUpgradeItem) {
            cir.setReturnValue(!stack.isEmpty());
        }
    }
}
