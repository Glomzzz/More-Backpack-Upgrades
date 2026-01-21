package com.kzjy.mobackup.wrapper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogic;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterType;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.FilteredItemHandler;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 次元卸货升级逻辑包装器
 * 处理与容器交互时的物品传输逻辑
 */
public class DimensionalDepositUpgradeWrapper extends DepositUpgradeWrapper {

    public DimensionalDepositUpgradeWrapper(IStorageWrapper backpackWrapper, ItemStack upgrade,
            Consumer<ItemStack> upgradeSaveHandler) {
        super(backpackWrapper, upgrade, upgradeSaveHandler);
    }

    /**
     * 当玩家潜行右键容器时触发
     * 执行物品从背包到目标容器（或 RS 网络代理）的传输
     */
    @Override
    public void onHandlerInteract(@NotNull IItemHandler itemHandler, @NotNull Player player) {
        DepositFilterLogic filterLogic = getFilterLogic();

        AtomicInteger stacksAdded = new AtomicInteger(0);

        // 如果目标不是 RS 网络代理且过滤器设置为 INVENTORY 类型，则更新过滤器上下文
        if (filterLogic.getDepositFilterType() == DepositFilterType.INVENTORY
                && !(itemHandler instanceof com.kzjy.mobackup.rs.RSInsertOnlyHandler)) {
            filterLogic.setInventory(itemHandler);
        }

        // 执行传输：从升级处理清单（背包内容）传输到目标容器
        InventoryHelper.transfer(storageWrapper.getInventoryForUpgradeProcessing(),
                new FilteredItemHandler<>(itemHandler, Collections.singletonList(filterLogic),
                        Collections.emptyList()),
                s -> stacksAdded.incrementAndGet());

        // 发送反馈消息给玩家
        int stacksDeposited = stacksAdded.get();
        String translKey = stacksDeposited > 0 ? "gui.sophisticatedbackpacks.status.stacks_deposited"
                : "gui.sophisticatedbackpacks.status.nothing_to_deposit";
        player.displayClientMessage(Component.translatable(translKey, stacksDeposited), true);
    }
}
