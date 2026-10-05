package com.drunkard;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SeatBlock extends HorizontalDirectionalBlock {
   private final double seatHeight;
   private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

   public SeatBlock(Properties properties, double seatHeight, VoxelShape northShape) {
      super(properties.noOcclusion());
      this.seatHeight = seatHeight;

      for (Direction d : Plane.HORIZONTAL) {
         this.shapes.put(d, rotate(northShape, d));
      }

      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   public double seatHeight() {
      return this.seatHeight;
   }

   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return MapCodec.unit(this);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext c) {
      return (BlockState)this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
   }

   protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return this.shapes.get(s.getValue(FACING));
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (player.isShiftKeyDown()) {
         return InteractionResult.PASS;
      } else if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         return SeatEntity.sit(level, pos, this, player) ? InteractionResult.CONSUME : InteractionResult.PASS;
      }
   }

   public static VoxelShape rotate(VoxelShape shape, Direction facing) {
      VoxelShape out = shape;

      for (int i = 0; i < (facing.get2DDataValue() + 2) % 4; i++) {
         VoxelShape[] turned = new VoxelShape[]{Shapes.empty()};
         out.forAllBoxes((x0, y0, z0, x1, y1, z1) -> turned[0] = Shapes.or(turned[0], Shapes.box(1.0 - z1, y0, x0, 1.0 - z0, y1, x1)));
         out = turned[0];
      }

      return out;
   }
}
