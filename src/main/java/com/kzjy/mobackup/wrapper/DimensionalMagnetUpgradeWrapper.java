package com.kzjy.mobackup.wrapper;

import com.kzjy.mobackup.core.RSBridge;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 次元磁吸升级的逻辑实现（升级到 Refined Storage 2 / refinedstorage2）
 * 将吸附到的物品优先推送到 RS 网络，失败时回退到背包
 */
public class DimensionalMagnetUpgradeWrapper extends MagnetUpgradeWrapper {

    private static final String PREVENT_REMOTE_MOVEMENT = "PreventRemoteMovement";
    private static final String ALLOW_MACHINE_MOVEMENT = "AllowMachineRemoteMovement";
    private static final int COOLDOWN_TICKS = 10;
    private static final int FULL_COOLDOWN_TICKS = 40;

    public DimensionalMagnetUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
                                           Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    private Network cachedNetwork;
    private long lastNetworkCheckTime = -1;
    private static final int NETWORK_CHECK_INTERVAL = 20;

    private Network getCachedNetwork(Level level) {
        long gameTime = level.getGameTime();
        if (cachedNetwork == null || lastNetworkCheckTime < 0
                || gameTime - lastNetworkCheckTime >= NETWORK_CHECK_INTERVAL) {
            lastNetworkCheckTime = gameTime;
            // RSBridge.getNetwork 已在你的项目中升级为 refinedstorage2 的 RSBridge.getNetwork(...)
            cachedNetwork = RSBridge.getNetwork(level, upgrade);
        }
        return cachedNetwork;
    }

    @Override
    public void tick(@Nullable Entity entity, Level world, BlockPos pos) {
        if (isInCooldown(world)) {
            return;
        }

        int cooldown = shouldPickupItems() ? pickupItemsCustom(entity, world, pos) : FULL_COOLDOWN_TICKS;

        if (shouldPickupXp() && canFillStorageWithXpCustom()) {
            cooldown = Math.min(cooldown, pickupXpOrbsCustom(entity, world, pos));
        }

        setCooldown(world, cooldown);
    }

    private boolean canFillStorageWithXpCustom() {
        return storageWrapper.getFluidHandler().map(fluidHandler -> fluidHandler.fill(ModFluids.EXPERIENCE_TAG, 1,
                ModFluids.XP_STILL.get(), IFluidHandler.FluidAction.SIMULATE) > 0).orElse(false);
    }

    private int pickupXpOrbsCustom(@Nullable Entity entity, Level world, BlockPos pos) {
        List<ExperienceOrb> xpEntities = world.getEntitiesOfClass(ExperienceOrb.class,
                new AABB(pos).inflate(upgradeItem.getRadius()), e -> true);
        if (xpEntities.isEmpty()) {
            return COOLDOWN_TICKS;
        }

        int cooldown = COOLDOWN_TICKS;
        for (ExperienceOrb xpOrb : xpEntities) {
            if (xpOrb.isAlive() && !canNotPickupCustom(xpOrb, entity) && !tryToFillTankCustom(xpOrb, entity, world)) {
                cooldown = FULL_COOLDOWN_TICKS;
                break;
            }
        }
        return cooldown;
    }

    private boolean tryToFillTankCustom(ExperienceOrb xpOrb, @Nullable Entity entity, Level world) {
        int amountToTransfer = XpHelper.experienceToLiquid(xpOrb.getValue());

        return storageWrapper.getFluidHandler().map(fluidHandler -> {
            int amountAdded = fluidHandler.fill(ModFluids.EXPERIENCE_TAG, amountToTransfer, ModFluids.XP_STILL.get(),
                    IFluidHandler.FluidAction.EXECUTE);

            if (amountAdded > 0) {
                Vec3 pos = xpOrb.position();
                xpOrb.value = 0;
                xpOrb.discard();

                if (entity instanceof Player player) {
                    playXpPickupSound(world, player);
                }

                if (amountToTransfer > amountAdded) {
                    world.addFreshEntity(new ExperienceOrb(world, pos.x(), pos.y(), pos.z(),
                            (int) XpHelper.liquidToExperience(amountToTransfer - amountAdded)));
                }
                return true;
            }
            return false;
        }).orElse(false);
    }

    private int pickupItemsCustom(@Nullable Entity entity, Level world, BlockPos pos) {
        List<ItemEntity> itemEntities = world.getEntitiesOfClass(ItemEntity.class,
                new AABB(pos).inflate(upgradeItem.getRadius()), e -> true);
        if (itemEntities.isEmpty()) {
            return COOLDOWN_TICKS;
        }

        Player player = entity instanceof Player ? (Player) entity : null;

        int cooldown = FULL_COOLDOWN_TICKS;
        for (ItemEntity itemEntity : itemEntities) {
            if (!itemEntity.isAlive() || !getFilterLogic().matchesFilter(itemEntity.getItem())
                    || canNotPickupCustom(itemEntity, entity)) {
                continue;
            }
            if (tryToInsertItemCustom(itemEntity, world, player)) {
                if (player != null) {
                    playItemPickupSound(world, player);
                }
                cooldown = COOLDOWN_TICKS;
            }
        }
        return cooldown;
    }

    private boolean canNotPickupCustom(Entity pickedUpEntity, @Nullable Entity entity) {
        CompoundTag data = pickedUpEntity.getPersistentData();
        return entity instanceof Player ? data.contains(PREVENT_REMOTE_MOVEMENT)
                : data.contains(PREVENT_REMOTE_MOVEMENT) && !data.contains(ALLOW_MACHINE_MOVEMENT);
    }

    /**
     * 核心：先尝试把物品插入到 refinedstorage2 网络（StorageNetworkComponent.insert），
     * 插入失败或网络不可用时回退到背包插入逻辑。
     */
    private boolean tryToInsertItemCustom(ItemEntity itemEntity, Level world, @Nullable Player player) {
        ItemStack stack = itemEntity.getItem();

        // 写入 RS 网络（refinedstorage2）
        Network network = getCachedNetwork(world);
        if (network != null) {
            // 把 ItemStack 转为 ResourceAmount（通过公开的 factory）
            Optional<ResourceAmount> maybeResourceAmount = RefinedStorageApi.INSTANCE.getItemResourceFactory().create(stack);
            if (maybeResourceAmount.isPresent()) {
                ResourceAmount resourceAmount = maybeResourceAmount.get();
                ResourceKey resource = resourceAmount.resource();
                long amountToInsert = Math.min(resourceAmount.amount(), stack.getCount());

                var storage = network.getComponent(StorageNetworkComponent.class);
                Action action = Action.EXECUTE; // 在模拟分支 below 会替换为 Action.SIMULATE
                Actor actor = player != null ? new PlayerActor(player) : Actor.EMPTY;

                // 先 try simulate? Here we want to actually insert: use EXECUTE
                long inserted = 0;
                try {
                    inserted = storage.insert(resource, amountToInsert, Action.EXECUTE, actor);
                } catch (Throwable t) {
                    // 插入过程异常，回退到背包
                }

                if (player != null && inserted > 0) {
                    // 标记由玩家插入（RSBridge 中已升级为 refinedstorage2 的友好接口 / no-op fallback）
                    RSBridge.markItemInsertedByPlayer(network, player, stack.copy());
                }

                if (inserted >= amountToInsert) {
                    // 全部插入，移除实体
                    itemEntity.setItem(ItemStack.EMPTY);
                    itemEntity.discard();
                    return true;
                } else if (inserted > 0) {
                    // 部分插入，继续处理剩余部分作为 stack
                    ItemStack remaining = stack.copy();
                    int remainingCount = stack.getCount() - (int) inserted;
                    remaining.setCount(remainingCount);
                    stack = remaining;
                }
                // 否则 inserted == 0 -> 没有插入，回退到背包
            }
        }
        // RS 写入结束或不可用 -> 回退到背包
        IItemHandlerSimpleInserter inventory = storageWrapper.getInventoryForUpgradeProcessing();
        ItemStack remaining = inventory.insertItem(stack, true);
        boolean insertedSomething = false;
        if (remaining.getCount() != stack.getCount()) {
            insertedSomething = true;
            remaining = inventory.insertItem(stack, false);
            itemEntity.setItem(remaining);
            if (remaining.isEmpty()) {
                itemEntity.discard();
            }
        }
        return insertedSomething;
    }

    private static void playItemPickupSound(Level world, @Nonnull Player player) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                0.2F, (world.random.nextFloat() - world.random.nextFloat()) * 1.4F + 2.0F);
    }

    private static void playXpPickupSound(Level world, @Nonnull Player player) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.1F, (world.random.nextFloat() - world.random.nextFloat()) * 0.35F + 0.9F);
    }

    // 覆写 pickup：兼容 IPickupResponseUpgrade
    @Override
    public @NotNull ItemStack pickup(@NotNull Level world, @NotNull ItemStack stack, boolean simulate) {
        if (!shouldPickupItems() || !getFilterLogic().matchesFilter(stack)) {
            return stack;
        }

        Network network = getCachedNetwork(world);
        if (network != null) {
            Optional<ResourceAmount> maybeResourceAmount = RefinedStorageApi.INSTANCE.getItemResourceFactory().create(stack);
            if (maybeResourceAmount.isPresent()) {
                ResourceAmount resourceAmount = maybeResourceAmount.get();
                ResourceKey resource = resourceAmount.resource();
                long amountToInsert = Math.min(resourceAmount.amount(), stack.getCount());

                StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);

                Player ctx = com.kzjy.mobackup.core.PickupContext.current();
                Actor actor = simulate ? Actor.EMPTY : (ctx != null ? new PlayerActor(ctx) : Actor.EMPTY);
                long inserted = 0;
                try {
                    // If we don't have a Player in this context, pass Actor.EMPTY (insertion done by upgrade)
                    if (simulate) {
                        inserted = storage.insert(resource, amountToInsert, Action.SIMULATE, actor);
                    } else {
                        // There is no Player reference in this method; treat as non-player actor
                        inserted = storage.insert(resource, amountToInsert, Action.EXECUTE, actor);
                    }
                } catch (Throwable t) {
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

        return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
    }
}