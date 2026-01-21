package com.kzjy.mobackup.core;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemTargetBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Refined Storage 2 (refinedstorage2) 桥接工具类（升级到 1.21.1 / NeoForge）
 *
 * 说明：
 * - 获取 Network 的方式已改为读取目标方块实体（BlockEntity），如果该方块实体实现了
 *   NetworkItemTargetBlockEntity，则使用其 getNetworkForItem() 方法获取 Network。
 * - 以前通过反射去标记“插入者”的实现已在新版本不再必要：在 refinedstorage2 中，
 *   插入时应传入 PlayerActor（见 RSInsertOnlyHandler 的实现），网络组件会依据 Actor 做相应处理。
 *   因此下面的 markItemInsertedByPlayer() 方法实现为 best-effort no-op（保留兼容点），
 *   并记录 debug 日志；如确实需要调用内部追踪器，需针对目标 RS 版本做更细致的反射适配。
 */
public class RSBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(RSBridge.class);

    // NBT keys
    private static final String NBT_RECEIVER_X = "ReceiverX";
    private static final String NBT_RECEIVER_Y = "ReceiverY";
    private static final String NBT_RECEIVER_Z = "ReceiverZ";
    private static final String NBT_DIMENSION = "Dimension";
    /**
     * 从物品组件读取绑定的方块坐标
     *
     * @return 如果数据不完整则返回 null
     */
    @Nullable
    public static BlockPos getCoordinate(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.getUnsafe();
            if (tag.contains(NBT_RECEIVER_X) && tag.contains(NBT_RECEIVER_Y) && tag.contains(NBT_RECEIVER_Z)) {
                return new BlockPos(
                        tag.getInt(NBT_RECEIVER_X),
                        tag.getInt(NBT_RECEIVER_Y),
                        tag.getInt(NBT_RECEIVER_Z));
            }
        }
        return null;
    }

    /**
     * 从物品组件读取绑定的维度
     */
    @Nullable
    public static ResourceKey<Level> getDimension(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.getUnsafe();
            if (tag.contains(NBT_DIMENSION)) {
                ResourceLocation name = ResourceLocation.tryParse(tag.getString(NBT_DIMENSION));
                if (name == null) {
                    return null;
                }
                return ResourceKey.create(Registries.DIMENSION, name);
            }
        }
        return null;
    }

    /**
     * 将目标方块坐标和维度保存到物品组件中
     */
    public static void saveCoordinate(ItemStack stack, BlockPos pos, ResourceKey<Level> dimension) {
        // 获取或创建 CustomData
        CustomData currentData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag;

        if (currentData != null) {
            // 如果已有数据，复制它（因为我们需要修改）
            tag = currentData.copyTag();
        } else {
            // 创建新的 NBT
            tag = new CompoundTag();
        }

        // 保存坐标和维度
        tag.putInt(NBT_RECEIVER_X, pos.getX());
        tag.putInt(NBT_RECEIVER_Y, pos.getY());
        tag.putInt(NBT_RECEIVER_Z, pos.getZ());
        tag.putString(NBT_DIMENSION, dimension.location().toString());

        // 设置回物品
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 获取物品绑定的 RS 网络实例（refinedstorage2）
     * 检查维度、区块加载状态与 BlockEntity 实现（NetworkItemTargetBlockEntity）
     */
    @Nullable
    public static Network getNetwork(Level level, ItemStack stack) {
        if (level.isClientSide) {
            return null;
        }

        BlockPos pos = getCoordinate(stack);
        ResourceKey<Level> dim = getDimension(stack);

        if (pos == null || dim == null) {
            return null;
        }

        if (level.getServer() == null) {
            return null;
        }
        ServerLevel serverLevel = level.getServer().getLevel(dim);
        if (serverLevel == null) {
            return null;
        }

        // 确保目标区块已加载，否则不要触发网络初始化或创建
        if (!serverLevel.isLoaded(pos)) {
            return null;
        }

        var be = serverLevel.getBlockEntity(pos);
        if (be instanceof NetworkItemTargetBlockEntity target) {
            // getNetworkForItem() 可能返回 null（未连接网络 / 尚未初始化）
            return target.getNetworkForItem();
        }

        return null;
    }

    /**
     * 标记物品插入由玩家执行（refinedstorage2）
     *
     * 说明：
     * - refinedstorage2 推荐在执行 insert 时传入 PlayerActor（参见示例 RSInsertOnlyHandler）以便网络记录来源。
     * - 旧版通过反射访问内部 tracker 的做法在 2.0 中可能不可用或不稳定，因此此方法只做 best-effort（不保证有效）。
     * - 如果你确实需要在运行时调用内部追踪器，请告知目标 refinedstorage2 的具体版本与你想调用的内部 API，我们可以为该版本写专门的反射适配。
     */
    public static void markItemInsertedByPlayer(Network network, net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (network == null || player == null || stack == null || stack.isEmpty()) {
            return;
        }

        // 推荐做法：在插入时传入 PlayerActor，网络会追踪 actor 来源；因此这里通常不需要额外操作。
        // 下面仅记录调试信息以便排查。如果你希望尝试反射访问内部追踪器，可以在此处扩展（版本敏感）。
        LOGGER.debug("markItemInsertedByPlayer called but no-op in refinedstorage2 bridge. " +
                        "Prefer passing PlayerActor to storage.insert(...). Network: {}, Player: {}, Item: {}",
                network, player.getGameProfile().getName(), stack.getItem().builtInRegistryHolder().getRegisteredName());

        // 保留空实现以保证旧代码兼容；如果需要尝试特定反射实现，请在这里实现并记录异常。
    }
}