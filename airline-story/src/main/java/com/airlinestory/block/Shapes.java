package com.airlinestory.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Builds per-facing shapes from boxes written for a north-facing model (pixel units). */
public final class Shapes {
    private Shapes() {}

    public static Map<Direction, VoxelShape> horizontal(double[]... boxes) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
            for (double[] b : boxes) {
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, rotate(b, d));
            }
            map.put(d, shape.optimize());
        }
        return map;
    }

    public static VoxelShape rotate(double[] b, Direction d) {
        double x1 = b[0], y1 = b[1], z1 = b[2], x2 = b[3], y2 = b[4], z2 = b[5];
        return switch (d) {
            case SOUTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case EAST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
            case WEST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
            default -> Block.box(x1, y1, z1, x2, y2, z2);
        };
    }

    public static double[] box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new double[]{x1, y1, z1, x2, y2, z2};
    }

    public static VoxelShape full() {
        return net.minecraft.world.phys.shapes.Shapes.block();
    }

    @SuppressWarnings("unused")
    private static VoxelShape unused() {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
}
