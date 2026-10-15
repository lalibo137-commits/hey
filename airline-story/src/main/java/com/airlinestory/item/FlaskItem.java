package com.airlinestory.item;

import com.airlinestory.registry.ModItems;
import com.airlinestory.story.Locations;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** An empty canteen: fill it at the island's freshwater spring, or with seawater anywhere else. */
public class FlaskItem extends LoreItem {
    public FlaskItem(Properties props) {
        super(props, 2, false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        BlockPos pos = hit.getBlockPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER)) return InteractionResultHolder.pass(stack);
        boolean fresh = Locations.isFreshWater(pos);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1f, 1f);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(fresh ? "message.airlinestory.flask_fresh" : "message.airlinestory.flask_salt"), true);
        }
        ItemStack filled = new ItemStack(fresh ? ModItems.WATER_FLASK.get() : ModItems.SEA_WATER_FLASK.get());
        return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, player, filled), level.isClientSide());
    }
}
