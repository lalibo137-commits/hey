package com.airlinestory.block;

import com.airlinestory.story.StoryEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A facing block whose right-click is handled by the story ({@link StoryEvents#interact}). */
public class InteractBlock extends DecorBlock {
    public final String key;

    public InteractBlock(String key, Properties props, double[]... boxes) {
        super(props, boxes);
        this.key = key;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            StoryEvents.interact(sp, key, pos, state, hit.getDirection());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
