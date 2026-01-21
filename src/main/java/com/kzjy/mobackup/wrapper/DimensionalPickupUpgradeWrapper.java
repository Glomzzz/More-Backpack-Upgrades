package com.kzjy.mobackup.wrapper;

import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.item.DimensionalMagnetUpgradeItem;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.api.storage.Actor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 次元拾取升级的逻辑实现（升级到 Refined Storage 2 / refinedstorage2）
 * 拾取到的物品优先推送到 RS 网络，必要时回退到背包
 */
public class DimensionalPickupUpgradeWrapper extends PickupUpgradeWrapper {

    private static final int NETWORK_CHECK_INTERVAL = 20;

    public DimensionalPickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
                                           Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    @Override
    public ItemStack pickup(Level world, ItemStack stack, boolean simulate) {
        if (!getFilterLogic().matchesFilter(stack)) {
            return stack;
        }

        // 如果同一存储上装有磁吸升级，则避免重复路由到 RS（由磁吸升级处理）
        if (storageWrapper.getUpgradeHandler().hasUpgrade(DimensionalMagnetUpgradeItem.TYPE)) {
            return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
        }

        Network network = getCachedNetwork(world);
        if (network != null) {
            // 把 ItemStack 转为 ResourceAmount（公开 factory）
            Optional<ResourceAmount> maybe = RefinedStorageApi.INSTANCE.getItemResourceFactory().create(stack);
            if (maybe.isPresent()) {
                ResourceAmount resourceAmount = maybe.get();
                ResourceKey resource = resourceAmount.resource();
                long amountToInsert = Math.min(resourceAmount.amount(), stack.getCount());

                var storage = network.getComponent(StorageNetworkComponent.class);
                Action action = simulate ? Action.SIMULATE : Action.EXECUTE;

                // 尝试从上下文拿到当前触发拾取的玩家（你的项目里有 PickupContext.current()）
                Player ctx = com.kzjy.mobackup.core.PickupContext.current();
                Actor actor = simulate ? Actor.EMPTY : (ctx != null ? new PlayerActor(ctx) : Actor.EMPTY);

                long inserted = 0;
                try {
                    inserted = storage.insert(resource, amountToInsert, action, actor);
                } catch (Throwable ignored) {
                    inserted = 0;
                }

                if (!simulate && inserted > 0 && ctx != null) {
                    // 标记由玩家插入（RSBridge 已在项目中升级；可能是 no-op 或做额外处理）
                    RSBridge.markItemInsertedByPlayer(network, ctx, stack.copy());
                }

                if (inserted > 0) {
                    int remaining = stack.getCount() - (int) inserted;
                    if (remaining <= 0) {
                        return ItemStack.EMPTY;
                    }
                    ItemStack rem = stack.copy();
                    rem.setCount(remaining);
                    return rem;
                }
            }
        }

        // 网络不可用或未插入 -> 回退到背包插入逻辑
        return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
    }

    private Network cachedNetwork;
    private long lastNetworkCheckTime = -1;

    private Network getCachedNetwork(Level level) {
        long gameTime = level.getGameTime();
        if (cachedNetwork == null || lastNetworkCheckTime < 0
                || gameTime - lastNetworkCheckTime >= NETWORK_CHECK_INTERVAL) {
            lastNetworkCheckTime = gameTime;
            cachedNetwork = RSBridge.getNetwork(level, upgrade);
        }
        return cachedNetwork;
    }
}