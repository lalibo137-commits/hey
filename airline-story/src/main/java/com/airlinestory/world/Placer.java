package com.airlinestory.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Places blocks for the story's structures quickly (no neighbour updates, no drops). */
public final class Placer {
    public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public final ServerLevel level;
    private final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
    public int placed;

    public Placer(ServerLevel level) {
        this.level = level;
    }

    public void set(int x, int y, int z, BlockState s) {
        m.set(x, y, z);
        level.setBlock(m, s, FLAGS);
        placed++;
    }

    public void set(int x, int y, int z, Block b) {
        set(x, y, z, b.defaultBlockState());
    }

    public void set(BlockPos p, BlockState s) {
        set(p.getX(), p.getY(), p.getZ(), s);
    }

    public BlockState get(int x, int y, int z) {
        return level.getBlockState(m.set(x, y, z));
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, s);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState());
    }

    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState());
    }

    /** Hollow box walls only (no floor/ceiling). */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        fill(x1, y1, z1, x2, y2, z1, s);
        fill(x1, y1, z2, x2, y2, z2, s);
        fill(x1, y1, z1, x1, y2, z2, s);
        fill(x2, y1, z1, x2, y2, z2, s);
    }

    public static BlockState facing(Block b, Direction d) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(HorizontalDirectionalBlock.FACING)) return s.setValue(HorizontalDirectionalBlock.FACING, d);
        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return s.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
        return s;
    }

    public static BlockState leaves(Block b) {
        BlockState s = b.defaultBlockState();
        return s.hasProperty(LeavesBlock.PERSISTENT) ? s.setValue(LeavesBlock.PERSISTENT, true) : s;
    }

    /** A glowing wall sign. {@code facing} is the direction the text faces (away from the wall). */
    public void sign(int x, int y, int z, Direction facing, DyeColor color, String... lines) {
        BlockState s = Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing);
        m.set(x, y, z);
        level.setBlock(m, s, Block.UPDATE_CLIENTS);
        BlockEntity be = level.getBlockEntity(m);
        if (be instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
            text = text.setColor(color).setHasGlowingText(true);
            sign.setText(text, true);
            sign.setWaxed(true);
            sign.setChanged();
            level.sendBlockUpdated(m.immutable(), s, s, Block.UPDATE_CLIENTS);
        }
    }
}
