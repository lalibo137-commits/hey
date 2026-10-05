package com.drunkard;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GlassShelfBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private static final VoxelShape NORTH = box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);
   private static final VoxelShape SOUTH = box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);
   private static final VoxelShape EAST = box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);
   private static final VoxelShape WEST = box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);

   public GlassShelfBlock() {
      super(Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).strength(1.5F).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext c) {
      return (BlockState)this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
   }

   public BlockState rotate(BlockState s, Rotation r) {
      return (BlockState)s.setValue(FACING, r.rotate((Direction)s.getValue(FACING)));
   }

   public BlockState mirror(BlockState s, Mirror m) {
      return s.rotate(m.getRotation((Direction)s.getValue(FACING)));
   }

   public RenderShape getRenderShape(BlockState s) {
      return RenderShape.MODEL;
   }

   public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return switch ((Direction)s.getValue(FACING)) {
         case SOUTH -> SOUTH;
         case EAST -> EAST;
         case WEST -> WEST;
         default -> NORTH;
      };
   }

   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new GlassShelfEntity(p, s);
   }

   public static Direction right(BlockState state) {
      return ((Direction)state.getValue(FACING)).getCounterClockWise();
   }

   public static int slotAt(BlockState state, BlockPos pos, Vec3 hit) {
      Direction right = right(state);
      double across = (hit.x - (double)pos.getX() - 0.5) * (double)right.getStepX()
         + (hit.z - (double)pos.getZ() - 0.5) * (double)right.getStepZ();
      int column = Math.max(0, Math.min(2, (int)Math.floor((across + 0.5) * 3.0)));
      double up = hit.y - (double)pos.getY();
      int tier = up < 0.3125 ? 0 : (up < 0.625 ? 1 : 2);
      return tier * 3 + column;
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.getBlockEntity(pos) instanceof GlassShelfEntity shelf) {
         if (level.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            ItemStack held = player.getItemInHand(hand);
            int slot = slotAt(state, pos, hit.getLocation());
            if (!player.isShiftKeyDown()) {
               if (GlassShelfEntity.glassware(held)) {
                  if (shelf.insert(held, slot)) {
                     level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.5F, 1.4F);
                  }

                  return InteractionResult.CONSUME;
               }

               if (hand == InteractionHand.MAIN_HAND && !((ItemStack)shelf.glasses.get(slot)).isEmpty()) {
                  ItemStack one = shelf.takeFrom(slot);
                  if (!player.getInventory().add(one)) {
                     player.drop(one, false);
                  }

                  level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.5F, 1.6F);
                  return InteractionResult.CONSUME;
               }
            }

            if (hand == InteractionHand.MAIN_HAND) {
               player.openMenu(shelf);
            }

            return InteractionResult.CONSUME;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
      if (!s.is(next.getBlock()) && l.getBlockEntity(p) instanceof GlassShelfEntity shelf) {
         Containers.dropContents(l, p, shelf.glasses);
      }

      super.onRemove(s, l, p, next, moving);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
   ) {
      InteractionResult result = this.use(state, level, pos, player, hand, hit);
      return result == InteractionResult.PASS ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.valueOf(result.name());
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      return this.use(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
   }

   protected MapCodec<GlassShelfBlock> codec() {
      return MapCodec.unit(GlassShelfBlock::new);
   }
}
