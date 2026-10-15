package com.airlinestory.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Interactive facing block with an extra "lit" flag and a 0..2 display "mode" (screens, boards, luggage). */
public class StateInteractBlock extends InteractBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 2);

    public StateInteractBlock(String key, Properties props, double[]... boxes) {
        super(key, props, boxes);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, true).setValue(MODE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT, MODE);
    }
}
