package com.drunkard;

import net.minecraft.world.item.Item;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.ItemInteractionResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CashRegisterBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty OPEN = BooleanProperty.create("open");
   private static final VoxelShape X_SHAPE = box(2.0, 0.0, 2.0, 14.0, 9.0, 14.0);
   private static final VoxelShape Z_SHAPE = X_SHAPE;

   public CashRegisterBlock() {
      super(Properties.of().mapColor(MapColor.GOLD).strength(1.5F).sound(SoundType.METAL).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(OPEN, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{FACING, OPEN});
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
      return ((Direction)s.getValue(FACING)).getAxis() == Axis.Z ? Z_SHAPE : X_SHAPE;
   }

   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new CashRegisterEntity(p, s);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
      return l.isClientSide ? null : createTickerHelper(t, (BlockEntityType)Drunkard.CASH_REGISTER_ENTITY.get(), CashRegisterEntity::tick);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof CashRegisterEntity register) {
         RegisterLinking.stop(sp, false);
         sp.openMenu(register, pos);
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      CompoundTag tag = ItemData.get(stack);
      if (tag != null && tag.contains("Profile")) {
         CompoundTag p = tag.getCompound("Profile");
         int rep = p.contains("Reputation") ? p.getInt("Reputation") : 50;
         int stars = Math.max(1, Math.min(5, (rep + 10) / 20));
         tooltip.add(Component.literal("Saved pub · " + "★".repeat(stars) + "☆".repeat(5 - stars)).withStyle(ChatFormatting.GOLD));
         List<String> bought = new ArrayList<>();
         CompoundTag levels = p.getCompound("UpgradeLevels");

         for (BarUpgrades.Upgrade u : BarUpgrades.Upgrade.values()) {
            if (levels.getInt(u.name()) > 0) {
               bought.add(u.title + " " + levels.getInt(u.name()));
            }
         }

         if (!bought.isEmpty()) {
            tooltip.add(Component.literal("Upgrades: " + String.join(", ", bought)).withStyle(ChatFormatting.GRAY));
         }

         int regulars = p.getList("Regulars", 10).size();
         if (regulars > 0) {
            tooltip.add(Component.literal(regulars + (regulars == 1 ? " regular" : " regulars")).withStyle(ChatFormatting.GRAY));
         }

         int style = Math.max(0, Math.min(CashRegisterEntity.STYLES.length - 1, p.getInt("Style")));
         tooltip.add(Component.literal("Customers: " + CashRegisterEntity.STYLES[style]).withStyle(ChatFormatting.GRAY));
         tooltip.add(Component.literal("Place it to reopen this pub; set the door and seats again").withStyle(ChatFormatting.DARK_GRAY));
      } else {
         tooltip.add(Component.literal("Runs a pub. Break it to take the pub's upgrades, regulars and reputation with you").withStyle(ChatFormatting.GRAY));
      }
   }

   public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
      if (!s.is(next.getBlock()) && l.getBlockEntity(p) instanceof CashRegisterEntity register) {
         register.dropContents();
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

   protected MapCodec<CashRegisterBlock> codec() {
      return MapCodec.unit(CashRegisterBlock::new);
   }
}
