package com.kzjy.mobackup.rs;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.api.storage.Actor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Optional;

/**
 * 仅插入的物品处理器（升级到 refinedstorage2 API）
 * 将物品写入 RS 网络，并标记为玩家操作
 */
public class RSInsertOnlyHandler implements IItemHandler {
    private final Network network;
    private final Player player;

    public RSInsertOnlyHandler(Network network, Player player) {
        this.network = network;
        this.player = player;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty() || network == null) {
            return stack;
        }

        // 使用公开的 item resource factory 将 ItemStack 转为 ResourceAmount
        Optional<ResourceAmount> maybeResourceAmount = RefinedStorageApi.INSTANCE.getItemResourceFactory().create(stack);

        if (maybeResourceAmount.isEmpty()) {
            // 不能识别该物品为资源，返回原栈
            return stack;
        }

        ResourceAmount resourceAmount = maybeResourceAmount.get();
        ResourceKey resource = resourceAmount.resource();
        long amountToInsert = Math.min(resourceAmount.amount(), stack.getCount());

        // 获取网络的存储组件并插入
        var storage = network.getComponent(StorageNetworkComponent.class);
        Action action = simulate ? Action.SIMULATE : Action.EXECUTE;
        Actor actor = simulate ? Actor.EMPTY : new PlayerActor(player);

        long inserted = 0;
        try {
            inserted = storage.insert(resource, amountToInsert, action, actor);
        } catch (Exception e) {
            // 如果插入过程中出错，保持原栈不变
            return stack;
        }

        if (!simulate && inserted > 0) {
            // 标记（保留你现有的标记逻辑；根据需要可以改为传入具体 resource/数量）
            com.kzjy.mobackup.core.RSBridge.markItemInsertedByPlayer(network, player, stack);
        }

        int remaining = stack.getCount() - (int) inserted;
        if (remaining <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack rem = stack.copy();
        rem.setCount(remaining);
        return rem;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}