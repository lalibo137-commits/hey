package com.drunkard;

import net.minecraft.world.item.Item;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.ItemInteractionResult;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SafeBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

   public SafeBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops());
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

   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new SafeEntity(p, s);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof SafeEntity safe && safe.owner == null) {
         safe.owner = player.getUUID();
         safe.sync();
      }
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof SafeEntity safe) {
         if (safe.owner == null) {
            safe.owner = player.getUUID();
            safe.sync();
         }

         if (!safe.mayUse(player)) {
            player.displayClientMessage(Component.literal("This safe belongs to someone else"), true);
            return InteractionResult.CONSUME;
         }

         sp.openMenu(safe, pos);
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof SafeEntity safe && !safe.mayUse(player)) {
         return 0.0F;
      }

      return super.getDestroyProgress(state, player, level, pos);
   }

   public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      CompoundTag tag = ItemData.get(stack);
      if (tag != null && tag.getInt("Balance") > 0) {
         tooltip.add(Component.literal("Holds $" + tag.getInt("Balance")).withStyle(ChatFormatting.GOLD));
      } else {
         tooltip.add(Component.literal("Store your bar's money; spend it on upgrades and supplies").withStyle(ChatFormatting.GRAY));
      }
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

   protected MapCodec<SafeBlock> codec() {
      return MapCodec.unit(SafeBlock::new);
   }
}
