package com.kzjy.mobackup;

import com.kzjy.mobackup.registry.ModCreativeModeTabs;
import com.kzjy.mobackup.registry.ModItems;
import com.mojang.logging.LogUtils;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemTargetBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeTab;
import org.slf4j.Logger;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@Mod(MoBackup.MODID)
public class MoBackup {
    public static final String MODID = "mobackup";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MoBackup(IEventBus modEventBus, ModContainer modContainer) {

        // 注册配置
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC,"MoreBackpackUpgrades-common.toml");

        // 注册物品和创造模式标签
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        // 注册生命周期事件监听
        modEventBus.addListener(this::commonSetup);

        // 注册 Forge 事件总线
        NeoForge.EVENT_BUS.register(this);
    }

    /**
     * 通用初始化阶段
     * 用于注册升级容器类型
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_MAGNET_UPGRADE.getId()),
                    DIMENSIONAL_MAGNET_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_PICKUP_UPGRADE.getId()),
                    DIMENSIONAL_PICKUP_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_DEPOSIT_UPGRADE.getId()),
                    DIMENSIONAL_DEPOSIT_TYPE);
        });
    }


    public static ResourceLocation rl(String path) {
        return ResourceLocation.tryBuild(MODID, path);
    }

    // 定义升级容器类型
    public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> DIMENSIONAL_PICKUP_TYPE = new UpgradeContainerType<>(
            ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper, MagnetUpgradeContainer> DIMENSIONAL_MAGNET_TYPE = new UpgradeContainerType<>(
            MagnetUpgradeContainer::new);
    public static final UpgradeContainerType<DepositUpgradeWrapper, DepositUpgradeContainer> DIMENSIONAL_DEPOSIT_TYPE = new UpgradeContainerType<>(
            DepositUpgradeContainer::new);


    /**
     * 玩家拾取物品前触发
     * 将玩家实例压入上下文，供升级逻辑使用
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPickupPre(ItemEntityPickupEvent.Pre event) {
        com.kzjy.mobackup.core.PickupContext.push(event.getPlayer());
    }

    /**
     * 玩家拾取物品后触发
     * 清理上下文，防止内存泄漏或逻辑污染
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPickupPost(ItemEntityPickupEvent.Post event) {
        com.kzjy.mobackup.core.PickupContext.pop();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        if (!player.isShiftKeyDown()) {
            return;
        }
        var stack = player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem)) {
            return;
        }
        var world = event.getLevel();
        var pos = event.getPos();
        var backpackWrapper = BackpackWrapper.fromStack(stack);
        var upgrades = backpackWrapper.getUpgradeHandler().getWrappersThatImplement(
                net.p3pp3rf1y.sophisticatedbackpacks.api.IItemHandlerInteractionUpgrade.class);
        boolean hasDimensionalDeposit = upgrades.stream()
                .anyMatch(u -> u instanceof com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper);
        if (!hasDimensionalDeposit) {
            return;
        }
        var state = world.getBlockState(pos);
        var key = state.getBlock().builtInRegistryHolder().key();
        boolean isRSBlock = "refinedstorage".equals(key.location().getNamespace());

        if (isRSBlock && world instanceof ServerLevel serverLevel) {
            Network rsNetwork = findNearbyNetwork(serverLevel, pos);
            if (rsNetwork != null /* && isNetworkUsable(rsNetwork) 可选更严格检查 */) {
                final Network targetNet = rsNetwork;
                upgrades.stream()
                        .filter(u -> u instanceof com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper)
                        .forEach(u -> u.onHandlerInteract(
                                new com.kzjy.mobackup.rs.RSInsertOnlyHandler(targetNet, player),
                                player
                        ));
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
        }
            player.displayClientMessage(
                    net.minecraft.network.chat.Component
                            .translatable("misc.refinedstorage.network_card.not_found"),
                    true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
        }

private static Network findNearbyNetwork(ServerLevel serverLevel, BlockPos pos) {
    // 0 步
    Network network = getNetworkFromBlockEntity(serverLevel.getBlockEntity(pos));
    if (isValid(network)) {
        return network;
    }

    // 1 步
    for (Direction dir : Direction.values()) {
        network = getNetworkFromBlockEntity(serverLevel.getBlockEntity(pos.relative(dir)));
        if (isValid(network)) {
            return network;
        }
    }

    // 2 步
    outer:
    for (Direction dir1 : Direction.values()) {
        for (Direction dir2 : Direction.values()) {
            network = getNetworkFromBlockEntity(serverLevel.getBlockEntity(pos.relative(dir1).relative(dir2)));
            if (isValid(network)) {
                return network;
            }
        }
    }

    return null;
}

private static Network getNetworkFromBlockEntity(BlockEntity be) {
    if (be instanceof NetworkItemTargetBlockEntity target) {
        // getNetworkForItem() 可能返回 null
        return target.getNetworkForItem();
    }
    return null;
}

// 判断 network 是否“可用”
// 最简单：非 null 即使用；如需更严格可按注释那样检查组件或容器
private static boolean isValid(Network network) {
    if (network == null) {
        return false;
    }

    // 可选更严格判断示例 —— 确保网络中至少有一个容器（近似“已激活/存在”）
    try {
        return !network.getComponent(GraphNetworkComponent.class).getContainers().isEmpty();
    } catch (Exception e) {
        // 如果不想依赖组件检查，直接认为 network != null 即有效
        return true;
    }

    // 或者仅使用：
    // return network != null;
}

    @EventBusSubscriber(modid = MoBackup.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static class ClientModEvents {
        @net.neoforged.bus.api.SubscribeEvent
        public static void clientSetup(final net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager.registerTab(
                        MoBackup.DIMENSIONAL_PICKUP_TYPE,
                        (net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer<net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper> uc,
                                net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position p,
                                net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase<?> s) ->
                                new net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeTab.Advanced(
                                        uc, p, s,
                                        net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedPickupUpgrade.slotsInRow.get(),
                                        net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));

                net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager.registerTab(
                        MoBackup.DIMENSIONAL_MAGNET_TYPE,
                        (net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer uc,
                                net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position p,
                                net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase<?> s) ->
                                new net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeTab.Advanced(
                                        uc, p, s,
                                        net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedMagnetUpgrade.slotsInRow.get(),
                                        net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));

                net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager.registerTab(
                        MoBackup.DIMENSIONAL_DEPOSIT_TYPE,
                        DepositUpgradeTab.Advanced::new);
            });
        }
    }
    }
