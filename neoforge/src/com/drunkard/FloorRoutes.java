package com.drunkard;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.jetbrains.annotations.Nullable;

public final class FloorRoutes {
   private FloorRoutes() {
   }

   @Nullable
   static BlockPos step(BlockGetter level, BlockPos from, Direction d, Predicate<BlockPos> ok) {
      double h = BarWorker.floorAt(level, from);
      if (Double.isNaN(h)) {
         return null;
      } else {
         BlockPos n = from.relative(d);
         BlockPos best = null;
         double bestGap = 9.0;

         for (int dy = 1; dy >= -1; dy--) {
            BlockPos q = n.above(dy);
            if (ok.test(q)) {
               double hq = BarWorker.floorAt(level, q);
               if (!Double.isNaN(hq) && (!(hq > h + 0.1) || level.getBlockEntity(q.below()) == null)) {
                  double gap = Math.abs(hq - h);
                  BlockPos higher = hq > h ? q : from;
                  BlockState below = level.getBlockState(higher.below());
                  boolean stair = below.getBlock() instanceof StairBlock && below.getValue(StairBlock.HALF) == Half.BOTTOM;
                  if (gap <= (stair ? 1.01 : 0.6) && gap < bestGap) {
                     best = q;
                     bestGap = gap;
                  }
               }
            }
         }

         return best;
      }
   }

   @Nullable
   public static List<BlockPos> route(BlockGetter level, BlockPos start, BlockPos goal, Predicate<BlockPos> allowed, int limit) {
      List<BlockPos> r = search(level, start, goal, allowed, true, limit);
      return r != null ? r : search(level, start, goal, allowed, false, limit);
   }

   @Nullable
   private static List<BlockPos> search(BlockGetter level, BlockPos start, BlockPos goal, Predicate<BlockPos> allowed, boolean dodgeWebs, int limit) {
      Predicate<BlockPos> ok = q -> allowed.test(q)
            && (!dodgeWebs || !level.getBlockState(q).is(Blocks.COBWEB))
            && !level.getBlockState(q).is(BlockTags.DOORS);
      Map<BlockPos, BlockPos> from = new HashMap<>();
      ArrayDeque<BlockPos> open = new ArrayDeque<>();
      open.add(start);
      from.put(start, start);
      BlockPos nearest = start;
      double nearestDistance = Double.MAX_VALUE;
      boolean found = false;

      while (!open.isEmpty() && from.size() < limit) {
         BlockPos cell = open.poll();
         double dx = (double)(cell.getX() - goal.getX());
         double dz = (double)(cell.getZ() - goal.getZ());
         double dy = (double)(cell.getY() - goal.getY()) * 2.0;
         double d = dx * dx + dz * dz + dy * dy;
         if (d < nearestDistance) {
            nearest = cell;
            nearestDistance = d;
         }

         if (cell.equals(goal)) {
            found = true;
            break;
         }

         for (Direction dir : Plane.HORIZONTAL) {
            BlockPos n = step(level, cell, dir, ok);
            if (n != null && !from.containsKey(n)) {
               from.put(n, cell);
               open.add(n);
            }
         }
      }

      if (!found && nearestDistance > 4.0) {
         return null;
      } else {
         LinkedList<BlockPos> route = new LinkedList<>();

         for (BlockPos c = found ? goal : nearest; !c.equals(start); c = from.get(c)) {
            route.addFirst(c);
         }

         return route;
      }
   }
}
