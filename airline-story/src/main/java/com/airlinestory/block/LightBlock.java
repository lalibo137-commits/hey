package com.airlinestory.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A full light panel the story can switch off and on (cabin lights flicker). */
public class LightBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public LightBlock(Properties props) {
        super(props.lightLevel(s -> s.getValue(LIT) ? 15 : 0));
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}
