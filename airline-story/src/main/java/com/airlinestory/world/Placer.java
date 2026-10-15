package com.airlinestory.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Places blocks for the story's structures quickly (no neighbour updates, no drops), remembering
 * connectable blocks (panes, fences, walls, bars, stairs) so {@link #finish()} can join them up.
 */
public final class Placer {
    public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public final ServerLevel level;
    private final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
    private final List<BlockPos> connect = new ArrayList<>();
    public int placed;
    /** Clue blocks placed by this placer, by position (copied into the story state by the director). */
    public final java.util.Map<BlockPos, String> clues = new java.util.LinkedHashMap<>();

    public void clue(int x, int y, int z, Block type, String id) {
        set(x, y, z, type.defaultBlockState());
        clues.put(new BlockPos(x, y, z), id);
    }

    public Placer(ServerLevel level) {
        this.level = level;
    }

    private static boolean connects(Block b) {
        return b instanceof IronBarsBlock || b instanceof FenceBlock || b instanceof WallBlock || b instanceof StairBlock
                || b instanceof FenceGateBlock || b instanceof ChainBlock || b instanceof VineBlock;
    }

    public void set(int x, int y, int z, BlockState s) {
        m.set(x, y, z);
        level.setBlock(m, s, FLAGS);
        placed++;
        if (connects(s.getBlock())) connect.add(m.immutable());
    }

    public void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }
    public void set(BlockPos p, BlockState s) { set(p.getX(), p.getY(), p.getZ(), s); }
    public void set(BlockPos p, Block b) { set(p.getX(), p.getY(), p.getZ(), b.defaultBlockState()); }

    public BlockState get(int x, int y, int z) { return level.getBlockState(m.set(x, y, z)); }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, s);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) { fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState()); }
    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) { fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState()); }

    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        fill(x1, y1, z1, x2, y2, z1, s);
        fill(x1, y1, z2, x2, y2, z2, s);
        fill(x1, y1, z1, x1, y2, z2, s);
        fill(x2, y1, z1, x2, y2, z2, s);
    }

    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, Block b) { walls(x1, y1, z1, x2, y2, z2, b.defaultBlockState()); }

    /** Joins up every pane, fence, wall, bar and stair placed so far. */
    public void finish() {
        for (BlockPos p : connect) {
            BlockState s = level.getBlockState(p);
            BlockState u = Block.updateFromNeighbourShapes(s, level, p);
            if (u != s) level.setBlock(p, u, FLAGS);
        }
        connect.clear();
    }

    // ------------------------------------------------------------------ state helpers
    public static BlockState facing(Block b, Direction d) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(HorizontalDirectionalBlock.FACING)) return s.setValue(HorizontalDirectionalBlock.FACING, d);
        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return s.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
        if (s.hasProperty(BlockStateProperties.FACING)) return s.setValue(BlockStateProperties.FACING, d);
        return s;
    }

    public static BlockState leaves(Block b) {
        BlockState s = b.defaultBlockState();
        return s.hasProperty(LeavesBlock.PERSISTENT) ? s.setValue(LeavesBlock.PERSISTENT, true) : s;
    }

    public static BlockState stair(Block b, Direction d, boolean top) {
        return b.defaultBlockState().setValue(StairBlock.FACING, d).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    public static BlockState slab(Block b, boolean top) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    public static BlockState axis(Block b, Direction.Axis a) {
        BlockState s = b.defaultBlockState();
        return s.hasProperty(BlockStateProperties.AXIS) ? s.setValue(BlockStateProperties.AXIS, a) : s;
    }

    public static BlockState trapdoor(Block b, Direction d, boolean top, boolean open) {
        return b.defaultBlockState().setValue(TrapDoorBlock.FACING, d).setValue(TrapDoorBlock.HALF, top ? Half.TOP : Half.BOTTOM)
                .setValue(TrapDoorBlock.OPEN, open);
    }

    public static BlockState lit(Block b) {
        BlockState s = b.defaultBlockState();
        return s.hasProperty(BlockStateProperties.LIT) ? s.setValue(BlockStateProperties.LIT, true) : s;
    }

    public void door(int x, int y, int z, Block door, Direction facing, boolean open) {
        BlockState d = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, open);
        set(x, y, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    public void hangingLight(int x, int yCeil, int z, int len, Block light) {
        for (int i = 1; i <= len; i++) set(x, yCeil - i, z, Blocks.CHAIN);
        set(x, yCeil - len - 1, z, light);
    }

    // ------------------------------------------------------------------ text
    private void writeSign(BlockState s, int x, int y, int z, DyeColor color, boolean glow, String... lines) {
        m.set(x, y, z);
        level.setBlock(m, s, Block.UPDATE_CLIENTS);
        BlockEntity be = level.getBlockEntity(m);
        if (be instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
            text = text.setColor(color).setHasGlowingText(glow);
            sign.setText(text, true);
            sign.setText(text, false);
            sign.setWaxed(true);
            sign.setChanged();
            level.sendBlockUpdated(m.immutable(), s, s, Block.UPDATE_CLIENTS);
        }
    }

    /** A wall sign. {@code facing} is the direction the text faces (away from the wall). */
    public void sign(int x, int y, int z, Direction facing, DyeColor color, String... lines) {
        writeSign(Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), x, y, z, color, true, lines);
    }

    public void sign(Block wallSign, int x, int y, int z, Direction facing, DyeColor color, String... lines) {
        writeSign(wallSign.defaultBlockState().setValue(WallSignBlock.FACING, facing), x, y, z, color, true, lines);
    }

    /** A hanging sign (shop names, gate numbers) hanging from the block above. */
    public void hangingSign(Block hanging, int x, int y, int z, Direction facing, DyeColor color, String... lines) {
        BlockState s = hanging.defaultBlockState().setValue(CeilingHangingSignBlock.ROTATION, rotationOf(facing))
                .setValue(CeilingHangingSignBlock.ATTACHED, false);
        writeSign(s, x, y, z, color, true, lines);
    }

    private static int rotationOf(Direction d) {
        return switch (d) { case SOUTH -> 0; case WEST -> 4; case NORTH -> 8; default -> 12; };
    }

    // ------------------------------------------------------------------ entities
    /** An item frame holding {@code item}, on the face {@code facing} of the block it hangs on. */
    public void frame(int x, int y, int z, Direction facing, ItemStack item, boolean invisible) {
        BlockPos p = new BlockPos(x, y, z);
        if (!level.getEntitiesOfClass(ItemFrame.class, new AABB(p)).isEmpty()) return;
        ItemFrame f = new ItemFrame(level, p, facing);
        f.setItem(item, false);
        f.setInvisible(invisible);
        f.getPersistentData().putBoolean("airlinestory_prop", true);
        level.addFreshEntity(f);
    }

    /** A mannequin (armour stand) in Meridian uniform, or any leather colour. */
    public ArmorStand mannequin(double x, double y, double z, float yaw, int leatherColor, boolean head) {
        ArmorStand a = EntityType.ARMOR_STAND.create(level);
        if (a == null) return null;
        a.moveTo(x, y, z, yaw, 0);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = new ItemStack(slot == EquipmentSlot.CHEST ? Items.LEATHER_CHESTPLATE : slot == EquipmentSlot.LEGS ? Items.LEATHER_LEGGINGS : Items.LEATHER_BOOTS);
            ((DyeableLeatherItem) s.getItem()).setColor(s, leatherColor);
            a.setItemSlot(slot, s);
        }
        if (head) a.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
        a.setInvulnerable(true);
        a.setNoGravity(true);
        level.addFreshEntity(a);
        return a;
    }
}
