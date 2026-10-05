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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(
   modid = "drunkard"
)
public final class RegisterLinking {
   private static final String KEY = "DrunkardRegisterLink";
   private static final int TIMEOUT = 2400;

   public static void start(Player player, BlockPos register, RegisterLinking.Mode mode) {
      CompoundTag tag = new CompoundTag();
      tag.putLong("Register", register.asLong());
      tag.putString("Mode", mode.name());
      tag.putLong("Since", player.level().getGameTime());
      player.getPersistentData().put("DrunkardRegisterLink", tag);
      player.displayClientMessage(
         Component.literal(
            mode == RegisterLinking.Mode.DOOR
               ? "Right-click a wooden door for customers to come in through"
               : "Right-click bar stools, chairs, stairs, slabs or trapdoors to add or remove seats · sneak-click to finish"
         ),
         true
      );
   }

   public static void stop(Player player, boolean announce) {
      if (player.getPersistentData().contains("DrunkardRegisterLink")) {
         player.getPersistentData().remove("DrunkardRegisterLink");
         if (announce) {
            player.displayClientMessage(Component.literal("Done picking"), true);
         }
      }
   }

   static CompoundTag link(Player player) {
      return player.getPersistentData().contains("DrunkardRegisterLink") ? player.getPersistentData().getCompound("DrunkardRegisterLink") : null;
   }

   @SubscribeEvent
   public static void click(RightClickBlock event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide) {
         CompoundTag link = link(player);
         if (link != null) {
            BlockPos registerPos = BlockPos.of(link.getLong("Register"));
            if (player.level().getBlockEntity(registerPos) instanceof CashRegisterEntity register) {
               if (!event.getPos().equals(registerPos)) {
                  BlockState state = player.level().getBlockState(event.getPos());
                  if (player.isShiftKeyDown()) {
                     event.setCanceled(true);
                     if (event.getHand() == InteractionHand.MAIN_HAND) {
                        stop(player, true);
                     }
                  } else if (RegisterLinking.Mode.valueOf(link.getString("Mode")) == RegisterLinking.Mode.DOOR) {
                     if (!(state.getBlock() instanceof DoorBlock)) {
                        // Leave the click alone so the player can still place blocks, e.g. the door they want to link.
                        if (event.getHand() == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()) {
                           player.displayClientMessage(Component.literal("That's not a door · place a wooden door, then right-click it"), true);
                        }

                        return;
                     }

                     event.setCanceled(true);
                     if (event.getHand() == InteractionHand.MAIN_HAND) {
                        if (!state.is(BlockTags.WOODEN_DOORS)) {
                           player.displayClientMessage(Component.literal("Customers can only open wooden doors"), true);
                           return;
                        }

                        BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? event.getPos().below() : event.getPos();
                        register.setDoor(lower);
                        player.displayClientMessage(Component.literal("Door set · customers will come in through here"), true);
                        stop(player, false);
                     }
                  } else {
                     if (!SeatEntity.seat(state)) {
                        // Not a seat: let the click through so blocks can still be placed while picking seats.
                        if (event.getHand() == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()) {
                           player.displayClientMessage(Component.literal("Pick a bar stool, chair, stairs, slab or trapdoor"), true);
                        }

                        return;
                     }

                     event.setCanceled(true);
                     if (event.getHand() == InteractionHand.MAIN_HAND) {
                        if (register.seats.size() >= 32 && !register.seats.contains(event.getPos())) {
                           player.displayClientMessage(Component.literal("That's the most seats one register can manage (32)"), true);
                           return;
                        }

                        boolean added = register.toggleSeat(event.getPos());
                        player.displayClientMessage(
                           Component.literal((added ? "Seat added" : "Seat removed") + " · " + register.seats.size() + " seats · sneak-click to finish"),
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
   public static void tick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0) {
         CompoundTag link = link(player);
         if (link != null) {
            if (player.level().getGameTime() - link.getLong("Since") > 2400L) {
               stop(player, true);
            } else {
               if (!(player.level().getBlockEntity(BlockPos.of(link.getLong("Register"))) instanceof CashRegisterEntity register)) {
                  stop(player, false);
                  return;
               }

               ServerLevel var8 = player.serverLevel();

               for (BlockPos seat : register.seats) {
                  var8.sendParticles(
                     player,
                     ParticleTypes.HAPPY_VILLAGER,
                     true,
                     (double)seat.getX() + 0.5,
                     (double)seat.getY() + 0.9,
                     (double)seat.getZ() + 0.5,
                     2,
                     0.2,
                     0.1,
                     0.2,
                     0.0
                  );
               }

               if (register.door != null) {
                  var8.sendParticles(
                     player,
                     ParticleTypes.WAX_ON,
                     true,
                     (double)register.door.getX() + 0.5,
                     (double)(register.door.getY() + 1),
                     (double)register.door.getZ() + 0.5,
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
