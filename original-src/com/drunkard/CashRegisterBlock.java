package com.drunkard;

import net.minecraft.core.BlockPos;
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
import net.minecraftforge.network.NetworkHooks;

public class CashRegisterBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   public static final BooleanProperty OPEN = BooleanProperty.m_61465_("open");
   private static final VoxelShape X_SHAPE = m_49796_(2.0, 0.0, 2.0, 14.0, 9.0, 14.0);
   private static final VoxelShape Z_SHAPE = X_SHAPE;

   public CashRegisterBlock() {
      super(Properties.m_284310_().m_284180_(MapColor.f_283757_).m_60978_(1.5F).m_60918_(SoundType.f_56743_).m_60955_());
      this.m_49959_((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH)).m_61124_(OPEN, false));
   }

   protected void m_7926_(Builder<Block, BlockState> b) {
      b.m_61104_(new Property[]{FACING, OPEN});
   }

   public BlockState m_5573_(BlockPlaceContext c) {
      return (BlockState)this.m_49966_().m_61124_(FACING, c.m_8125_().m_122424_());
   }

   public BlockState m_6843_(BlockState s, Rotation r) {
      return (BlockState)s.m_61124_(FACING, r.m_55954_((Direction)s.m_61143_(FACING)));
   }

   public BlockState m_6943_(BlockState s, Mirror m) {
      return s.m_60717_(m.m_54846_((Direction)s.m_61143_(FACING)));
   }

   public RenderShape m_7514_(BlockState s) {
      return RenderShape.MODEL;
   }

   public VoxelShape m_5940_(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return ((Direction)s.m_61143_(FACING)).m_122434_() == Axis.Z ? Z_SHAPE : X_SHAPE;
   }

   public BlockEntity m_142194_(BlockPos p, BlockState s) {
      return new CashRegisterEntity(p, s);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level l, BlockState s, BlockEntityType<T> t) {
      return l.f_46443_ ? null : m_152132_(t, (BlockEntityType)Drunkard.CASH_REGISTER_ENTITY.get(), CashRegisterEntity::tick);
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.f_46443_ && player instanceof ServerPlayer sp && level.m_7702_(pos) instanceof CashRegisterEntity register) {
         RegisterLinking.stop(sp, false);
         NetworkHooks.openScreen(sp, register, pos);
      }

      return InteractionResult.m_19078_(level.f_46443_);
   }

   public void m_6810_(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
      if (!s.m_60713_(next.m_60734_()) && l.m_7702_(p) instanceof CashRegisterEntity register) {
         register.dropContents();
      }

      super.m_6810_(s, l, p, next, moving);
   }
}
