package com.drunkard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "drunkard"
)
public final class RegisterLinking {
   private static final String KEY = "DrunkardRegisterLink";
   private static final int TIMEOUT = 2400;

   public static void start(Player player, BlockPos register, RegisterLinking.Mode mode) {
      CompoundTag tag = new CompoundTag();
      tag.m_128356_("Register", register.m_121878_());
      tag.m_128359_("Mode", mode.name());
      tag.m_128356_("Since", player.m_9236_().m_46467_());
      player.getPersistentData().m_128365_("DrunkardRegisterLink", tag);
      player.m_5661_(
         Component.m_237113_(
            mode == RegisterLinking.Mode.DOOR
               ? "Right-click a wooden door for customers to come in through"
               : "Right-click bar stools, chairs, stairs, slabs or trapdoors to add or remove seats · sneak-click to finish"
         ),
         true
      );
   }

   public static void stop(Player player, boolean announce) {
      if (player.getPersistentData().m_128441_("DrunkardRegisterLink")) {
         player.getPersistentData().m_128473_("DrunkardRegisterLink");
         if (announce) {
            player.m_5661_(Component.m_237113_("Done picking"), true);
         }
      }
   }

   static CompoundTag link(Player player) {
      return player.getPersistentData().m_128441_("DrunkardRegisterLink") ? player.getPersistentData().m_128469_("DrunkardRegisterLink") : null;
   }

   @SubscribeEvent
   public static void click(RightClickBlock event) {
      Player player = event.getEntity();
      if (!player.m_9236_().f_46443_) {
         CompoundTag link = link(player);
         if (link != null) {
            BlockPos registerPos = BlockPos.m_122022_(link.m_128454_("Register"));
            if (player.m_9236_().m_7702_(registerPos) instanceof CashRegisterEntity register) {
               if (!event.getPos().equals(registerPos)) {
                  BlockState state = player.m_9236_().m_8055_(event.getPos());
                  if (player.m_6144_()) {
                     event.setCanceled(true);
                     if (event.getHand() == InteractionHand.MAIN_HAND) {
                        stop(player, true);
                     }
                  } else if (RegisterLinking.Mode.valueOf(link.m_128461_("Mode")) == RegisterLinking.Mode.DOOR) {
                     if (!(state.m_60734_() instanceof DoorBlock)) {
                        // Leave the click alone so the player can still place blocks, e.g. the door they want to link.
                        if (event.getHand() == InteractionHand.MAIN_HAND && player.m_21205_().m_41619_()) {
                           player.m_5661_(Component.m_237113_("That's not a door · place a wooden door, then right-click it"), true);
                        }

                        return;
                     }

                     event.setCanceled(true);
                     if (event.getHand() == InteractionHand.MAIN_HAND) {
                        if (!state.m_204336_(BlockTags.f_13095_)) {
                           player.m_5661_(Component.m_237113_("Customers can only open wooden doors"), true);
                           return;
                        }

                        BlockPos lower = state.m_61143_(DoorBlock.f_52730_) == DoubleBlockHalf.UPPER ? event.getPos().m_7495_() : event.getPos();
                        register.setDoor(lower);
                        player.m_5661_(Component.m_237113_("Door set · customers will come in through here"), true);
                        stop(player, false);
                     }
                  } else {
                     if (!SeatEntity.seat(state)) {
                        // Not a seat: let the click through so blocks can still be placed while picking seats.
                        if (event.getHand() == InteractionHand.MAIN_HAND && player.m_21205_().m_41619_()) {
                           player.m_5661_(Component.m_237113_("Pick a bar stool, chair, stairs, slab or trapdoor"), true);
                        }

                        return;
                     }

                     event.setCanceled(true);
                     if (event.getHand() == InteractionHand.MAIN_HAND) {
                        if (register.seats.size() >= 32 && !register.seats.contains(event.getPos())) {
                           player.m_5661_(Component.m_237113_("That's the most seats one register can manage (32)"), true);
                           return;
                        }

                        boolean added = register.toggleSeat(event.getPos());
                        player.m_5661_(
                           Component.m_237113_((added ? "Seat added" : "Seat removed") + " · " + register.seats.size() + " seats · sneak-click to finish"),
                           true
                        );
                     }
                  }
               }
            } else {
               stop(player, false);
            }
         }
      }
   }

   @SubscribeEvent
   public static void tick(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player && player.f_19797_ % 10 == 0) {
         CompoundTag link = link(player);
         if (link != null) {
            if (player.m_9236_().m_46467_() - link.m_128454_("Since") > 2400L) {
               stop(player, true);
            } else {
               if (!(player.m_9236_().m_7702_(BlockPos.m_122022_(link.m_128454_("Register"))) instanceof CashRegisterEntity register)) {
                  stop(player, false);
                  return;
               }

               ServerLevel var8 = player.m_284548_();

               for (BlockPos seat : register.seats) {
                  var8.m_8624_(
                     player,
                     ParticleTypes.f_123748_,
                     true,
                     (double)seat.m_123341_() + 0.5,
                     (double)seat.m_123342_() + 0.9,
                     (double)seat.m_123343_() + 0.5,
                     2,
                     0.2,
                     0.1,
                     0.2,
                     0.0
                  );
               }

               if (register.door != null) {
                  var8.m_8624_(
                     player,
                     ParticleTypes.f_175828_,
                     true,
                     (double)register.door.m_123341_() + 0.5,
                     (double)(register.door.m_123342_() + 1),
                     (double)register.door.m_123343_() + 0.5,
                     3,
                     0.2,
                     0.5,
                     0.2,
                     0.0
                  );
               }
            }
         }
      }
   }

   private RegisterLinking() {
   }

   public static enum Mode {
      DOOR,
      SEATS;
   }
}
