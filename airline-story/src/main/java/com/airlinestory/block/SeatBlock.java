package com.airlinestory.block;

import com.airlinestory.entity.SeatEntity;
import com.airlinestory.story.StoryEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A seat you can sit on by right-clicking (sneak to stand up). Airplane seats also carry a seat-back
 * entertainment screen: clicking the back of a seat looks at its screen instead of sitting.
 */
public class SeatBlock extends DecorBlock {
    public static final IntegerProperty SCREEN = IntegerProperty.create("screen", 0, 2);
    private final boolean hasScreen;

    public SeatBlock(boolean hasScreen, Properties props, double[]... boxes) {
        super(props, boxes);
        this.hasScreen = hasScreen;
        this.registerDefaultState(this.defaultBlockState().setValue(SCREEN, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SCREEN);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer sp = (ServerPlayer) player;
        if (hasScreen && hit.getDirection() == state.getValue(FACING).getOpposite()) {
            StoryEvents.interact(sp, "seat_screen", pos, state, hit.getDirection());
            return InteractionResult.CONSUME;
        }
        if (player.isPassenger()) return InteractionResult.PASS;
        if (SeatEntity.sit(sp, pos, state.getValue(FACING))) {
            StoryEvents.onSit(sp, pos);
        }
        return InteractionResult.CONSUME;
    }
}
