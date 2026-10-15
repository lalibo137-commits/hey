package com.airlinestory.story;

import com.airlinestory.AirlineStory;
import com.airlinestory.entity.HollowEntity;
import com.airlinestory.entity.StoryNpc;
import com.airlinestory.registry.ModItems;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge event hooks plus the entry points blocks and entities call into. */
@Mod.EventBusSubscriber(modid = AirlineStory.MODID)
public final class StoryEvents {
    private StoryEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        StoryDirector.serverStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        StoryDirector.serverStopped();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        StoryDirector d = StoryDirector.get();
        if (d != null) d.tick();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        StoryDirector d = StoryDirector.get();
        if (d != null && event.getEntity() instanceof ServerPlayer sp) d.onLogin(sp);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        StoryDirector d = StoryDirector.get();
        if (d == null || !(event.getEntity() instanceof ServerPlayer sp)) return;
        BlockState s = event.getLevel().getBlockState(event.getPos());
        if (s.is(Blocks.IRON_DOOR) && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            BlockPos p = event.getPos();
            Locations.Plane a = Locations.AIR_PLANE;
            Locations.Plane g = Locations.GATE_PLANE;
            boolean cockpit = (p.getX() == a.x() + 4 && Math.abs(p.getY() - a.y() - 1.5) < 1.5 && p.getZ() == a.z())
                    || (p.getX() == g.x() + 4 && Math.abs(p.getY() - g.y() - 1.5) < 1.5 && p.getZ() == g.z());
            if (cockpit) {
                d.onCockpitDoor(sp);
                event.setCanceled(true);
            }
        }
        if (s.is(Blocks.SPRUCE_TRAPDOOR) && event.getPos().equals(com.airlinestory.world.PlaneBuilder.hatch(Locations.AIR_PLANE)) && d.onHatch(sp)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        StoryDirector d = StoryDirector.get();
        if (d != null && event.getSource().getEntity() instanceof ServerPlayer sp) d.onKill(sp, event.getEntity());
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        StoryDirector d = StoryDirector.get();
        if (d != null && event.getEntity() instanceof ServerPlayer sp) d.onPlace(sp, event.getPlacedBlock());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        BlockState s = event.getState();
        if (!(event.getLevel() instanceof ServerLevel level) || event.getPlayer().isCreative()) return;
        if (s.is(Blocks.GRASS) || s.is(Blocks.FERN) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.VINE) || s.is(Blocks.DEAD_BUSH)) {
            if (level.random.nextFloat() < 0.55f) {
                BlockPos p = event.getPos();
                ItemEntity item = new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, new ItemStack(ModItems.PLANT_FIBER.get()));
                item.setDefaultPickUpDelay();
                level.addFreshEntity(item);
            }
        }
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        StoryDirector d = StoryDirector.get();
        if (d == null || !(event.getEntity() instanceof ServerPlayer sp)) return;
        String q = d.state().quest;
        if (q.equals("first_night") || q.equals("night_watch") || q.equals("last_night")) {
            event.setResult(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            d.hint(sp, "You lie down and close your eyes. Something out there takes a step closer. You open them again.");
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("story").requires(src -> src.hasPermission(2))
                .then(Commands.literal("skip").executes(c -> {
                    StoryDirector d = StoryDirector.get();
                    if (d == null) { c.getSource().sendFailure(Component.literal("Not a story world.")); return 0; }
                    d.skip(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> d.status(), false);
                    return 1;
                }))
                .then(Commands.literal("status").executes(c -> {
                    StoryDirector d = StoryDirector.get();
                    if (d == null) { c.getSource().sendFailure(Component.literal("Not a story world.")); return 0; }
                    c.getSource().sendSuccess(() -> d.status(), false);
                    return 1;
                }))
                .then(Commands.literal("goto").then(Commands.argument("quest", StringArgumentType.word()).executes(c -> {
                    StoryDirector d = StoryDirector.get();
                    if (d == null) return 0;
                    d.jump(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "quest"));
                    c.getSource().sendSuccess(() -> d.status(), false);
                    return 1;
                })))
                .then(Commands.literal("flag").then(Commands.argument("flag", StringArgumentType.greedyString()).executes(c -> {
                    StoryDirector d = StoryDirector.get();
                    if (d == null) return 0;
                    d.state().flag(StringArgumentType.getString(c, "flag"));
                    d.markSync();
                    return 1;
                }))));
    }

    // ----------------------------------------------------------------- entry points
    public static void interact(ServerPlayer player, String key, BlockPos pos, BlockState state, Direction face) {
        StoryDirector d = StoryDirector.get();
        if (d != null) d.onInteract(player, key, pos, state, face);
    }

    public static void talk(ServerPlayer player, StoryNpc npc) {
        StoryDirector d = StoryDirector.get();
        if (d != null) d.onTalk(player, npc);
    }

    public static void onSit(ServerPlayer player, BlockPos pos) {
        StoryDirector d = StoryDirector.get();
        if (d != null) d.onSit(player, pos);
    }

    public static void npcHit(ServerPlayer player, StoryNpc npc) {
        StoryDirector d = StoryDirector.get();
        if (d != null) d.npcHit(player, npc);
    }

    public static void validateNpc(StoryNpc npc) {
        StoryDirector d = StoryDirector.get();
        if (d != null) d.validate(npc);
    }

    public static void onHollowTouch(HollowEntity h, Player player) {
        StoryDirector d = StoryDirector.get();
        if (d != null) d.onHollowTouch(h, player);
    }

    @SuppressWarnings("unused")
    private static boolean isDoor(BlockState s) {
        return s.getBlock() instanceof DoorBlock;
    }
}
