package com.kzjy.mobackup.item;

import com.kzjy.mobackup.core.RSBridge;
import com.refinedmods.refinedstorage.common.controller.ControllerBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 接口：标记该物品可以绑定到 RS 网络
 * 提供通用的右键绑定逻辑和工具提示显示逻辑
 */
public interface IRSLinkedItem {
    /**
     * 处理右键方块事件
     * 如果点击的是 RS 控制器，则保存坐标和维度到物品 NBT
     */
    default InteractionResult handleUseOn(UseOnContext ctx) {
        if (ctx.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        Block block = ctx.getLevel().getBlockState(ctx.getClickedPos()).getBlock();
        // 检查是否为 RS 控制器
        if (block instanceof ControllerBlock) {
            ItemStack stack = ctx.getPlayer().getItemInHand(ctx.getHand());
            // 保存绑定信息
            RSBridge.saveCoordinate(stack, ctx.getClickedPos(), ctx.getLevel().dimension());
            ctx.getPlayer().displayClientMessage(Component.translatable("misc.refinedstorage.network_card.linked"), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * 添加工具提示
     * 显示已绑定的坐标，或未找到绑定的提示
     */
    default void appendHoverText(ItemStack stack, List<Component> tooltip) {
        BlockPos pos = RSBridge.getCoordinate(stack);
        ResourceKey<Level> dim = RSBridge.getDimension(stack);

        if (pos != null && dim != null) {
            tooltip.add(Component.translatable(
                    "misc.refinedstorage.network_card.tooltip",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    dim.location().toString()
            ).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("misc.refinedstorage.network_card.not_found").withStyle(ChatFormatting.RED));
        }
    }
}