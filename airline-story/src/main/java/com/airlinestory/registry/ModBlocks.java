package com.airlinestory.registry;

import com.airlinestory.AirlineStory;
import com.airlinestory.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.airlinestory.block.Shapes.box;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AirlineStory.MODID);
    public static final List<RegistryObject<Block>> ALL = new ArrayList<>();

    private static RegistryObject<Block> reg(String name, Supplier<Block> sup) {
        RegistryObject<Block> ro = BLOCKS.register(name, sup);
        ALL.add(ro);
        return ro;
    }

    private static Properties stone() { return Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6f).sound(SoundType.STONE); }
    private static Properties metal() { return Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL); }
    private static Properties cloth() { return Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.8f).sound(SoundType.WOOL); }
    private static Properties wood() { return Properties.of().mapColor(MapColor.WOOD).strength(2f, 3f).sound(SoundType.WOOD); }
    private static Properties glass() { return Properties.of().mapColor(MapColor.NONE).strength(0.6f).sound(SoundType.GLASS).noOcclusion()
            .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false).isRedstoneConductor((s, l, p) -> false); }
    private static Properties plant() { return Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.GRASS)
            .offsetType(net.minecraft.world.level.block.state.BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY); }

    // ---------- Airport ----------
    public static final RegistryObject<Block> TERMINAL_TILE = reg("terminal_tile", () -> new Block(stone().mapColor(MapColor.QUARTZ)));
    public static final RegistryObject<Block> TERMINAL_CARPET = reg("terminal_carpet", () -> new Block(cloth()));
    public static final RegistryObject<Block> TERMINAL_WALL = reg("terminal_wall", () -> new Block(stone().mapColor(MapColor.SNOW)));
    public static final RegistryObject<Block> CEILING_PANEL = reg("ceiling_panel", () -> new Block(stone().mapColor(MapColor.SNOW)));
    public static final RegistryObject<Block> CEILING_LIGHT = reg("ceiling_light", () -> new Block(stone().mapColor(MapColor.SNOW).lightLevel(s -> 15)));
    public static final RegistryObject<Block> MERIDIAN_LOGO = reg("meridian_logo", () -> new DecorBlock(stone().mapColor(MapColor.COLOR_BLUE).lightLevel(s -> 6)));
    public static final RegistryObject<Block> AIRPORT_SEAT = reg("airport_seat", () -> new SeatBlock(false, metal().noOcclusion(),
            box(1, 0, 1, 15, 2, 15), box(2, 2, 2, 14, 7, 14), box(1, 7, 1, 15, 9, 13), box(1, 7, 13, 15, 18, 15)));
    public static final RegistryObject<Block> CHECKIN_COUNTER = reg("checkin_counter", () -> new DecorBlock(stone().mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final RegistryObject<Block> CONVEYOR_BELT = reg("conveyor_belt", () -> new InteractBlock("conveyor", metal().noOcclusion(), box(0, 0, 0, 16, 9, 16)));
    public static final RegistryObject<Block> XRAY_MACHINE = reg("xray_machine", () -> new InteractBlock("xray", metal()));
    public static final RegistryObject<Block> SCANNER_PILLAR = reg("scanner_pillar", () -> new DecorBlock(metal().noOcclusion().lightLevel(s -> 4), box(4, 0, 4, 12, 16, 12)));
    public static final RegistryObject<Block> SCANNER_TOP = reg("scanner_top", () -> new DecorBlock(metal().lightLevel(s -> 7)));
    public static final RegistryObject<Block> STANCHION = reg("stanchion", () -> new DecorBlock(metal().noOcclusion(),
            box(6, 0, 6, 10, 15, 10), box(0, 11, 7.5, 16, 13, 8.5)).withCollision(box(5, 0, 5, 11, 24, 11), box(0, 0, 6, 16, 24, 10)));
    public static final RegistryObject<Block> STORY_BARRIER = reg("story_barrier", () -> new Block(glass().strength(-1f, 3600000f)));
    public static final RegistryObject<Block> DEPARTURE_BOARD = reg("departure_board", () -> new StateInteractBlock("departure_board", metal().lightLevel(s -> 10)));
    public static final RegistryObject<Block> BOARDING_SCANNER = reg("boarding_scanner", () -> new InteractBlock("boarding_scanner", metal().noOcclusion().lightLevel(s -> 5),
            box(1, 0, 3, 15, 15, 13), box(0, 15, 2, 16, 16, 14)));
    public static final RegistryObject<Block> SMILESCORE_KIOSK = reg("smilescore_kiosk", () -> new InteractBlock("kiosk", metal().lightLevel(s -> 9)));
    public static final RegistryObject<Block> VENDING_MACHINE = reg("vending_machine", () -> new InteractBlock("vending", metal().lightLevel(s -> 8)));
    public static final RegistryObject<Block> SUITCASE = reg("suitcase", () -> new StateInteractBlock("suitcase", cloth().mapColor(MapColor.COLOR_BROWN).noOcclusion(),
            box(1, 0, 4, 15, 12, 12)));

    // ---------- Story objects ----------
    public static final RegistryObject<Block> CLUE_NOTE = reg("clue_note", () -> new InteractBlock("clue", Properties.of().mapColor(MapColor.SNOW)
            .strength(-1f, 3600000f).noCollission().noOcclusion().sound(SoundType.WOOL), box(3, 0, 3, 13, 1, 13)));
    public static final RegistryObject<Block> CLUE_TAPE = reg("clue_tape", () -> new InteractBlock("clue", Properties.of().mapColor(MapColor.COLOR_BLACK)
            .strength(-1f, 3600000f).noCollission().noOcclusion().sound(SoundType.METAL), box(3, 0, 5, 13, 2, 11)));
    public static final RegistryObject<Block> CLUE_PHOTO = reg("clue_photo", () -> new InteractBlock("clue", Properties.of().mapColor(MapColor.SNOW)
            .strength(-1f, 3600000f).noCollission().noOcclusion().sound(SoundType.WOOL), box(4, 0, 4, 12, 1, 12)));
    public static final RegistryObject<Block> WET_FLOOR_SIGN = reg("wet_floor_sign_block", () -> new InteractBlock("pickup", Properties.of().mapColor(MapColor.COLOR_YELLOW)
            .strength(-1f, 3600000f).noOcclusion().sound(SoundType.WOOD), box(4, 0, 4, 12, 14, 12)));
    public static final RegistryObject<Block> LOST_PLUSH = reg("lost_plush", () -> new InteractBlock("pickup", Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
            .strength(-1f, 3600000f).noOcclusion().sound(SoundType.WOOL), box(4, 0, 4, 12, 8, 12)));
    public static final RegistryObject<Block> ENGINE_PART = reg("engine_part", () -> new InteractBlock("pickup", metal().strength(-1f, 3600000f).noOcclusion()
            .lightLevel(s -> 4), box(3, 0, 3, 13, 10, 13)));
    public static final RegistryObject<Block> LOUNGE_TV = reg("lounge_tv", () -> new StateInteractBlock("tv", metal().strength(-1f, 3600000f).noOcclusion()
            .lightLevel(s -> 9), box(0, 0, 14, 16, 16, 16)));
    public static final RegistryObject<Block> MISSING_POSTER = reg("missing_poster", () -> new InteractBlock("poster", Properties.of().mapColor(MapColor.SNOW)
            .strength(-1f, 3600000f).noCollission().noOcclusion().sound(SoundType.WOOL), box(0, 0, 15, 16, 16, 16)));

    // ---------- Airplane ----------
    public static final RegistryObject<Block> FUSELAGE = reg("fuselage", () -> new Block(metal().mapColor(MapColor.SNOW)));
    public static final RegistryObject<Block> FUSELAGE_STRIPE = reg("fuselage_stripe", () -> new Block(metal().mapColor(MapColor.COLOR_BLUE)));
    public static final RegistryObject<Block> AIRPLANE_WINDOW = reg("airplane_window", () -> new Block(metal().mapColor(MapColor.SNOW).noOcclusion()
            .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
    public static final RegistryObject<Block> AIRPLANE_SEAT = reg("airplane_seat", () -> new SeatBlock(true, cloth().noOcclusion(),
            box(1, 0, 2, 15, 3, 14), box(1, 3, 1, 15, 7, 13), box(1, 7, 12, 15, 20, 16)));
    public static final RegistryObject<Block> CABIN_FLOOR = reg("cabin_floor", () -> new Block(cloth()));
    public static final RegistryObject<Block> CABIN_PANEL = reg("cabin_panel", () -> new Block(metal().mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> OVERHEAD_BIN = reg("overhead_bin", () -> new InteractBlock("overhead_bin", metal().mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> CABIN_LIGHT = reg("cabin_light", () -> new LightBlock(metal().mapColor(MapColor.SNOW)));
    public static final RegistryObject<Block> SEATBELT_SIGN = reg("seatbelt_sign", () -> new StateInteractBlock("seatbelt_sign", metal().noOcclusion()
            .lightLevel(s -> s.getValue(StateInteractBlock.LIT) ? 7 : 0), box(3, 10, 5, 13, 16, 11)));
    public static final RegistryObject<Block> GALLEY_COUNTER = reg("galley_counter", () -> new InteractBlock("galley", metal()));
    public static final RegistryObject<Block> LAVATORY_MIRROR = reg("lavatory_mirror", () -> new InteractBlock("mirror", glass().strength(1f), box(1, 1, 14, 15, 15, 16)));
    public static final RegistryObject<Block> CALL_BUTTON = reg("call_button", () -> new InteractBlock("call_button", metal().noOcclusion().lightLevel(s -> 3), box(4, 4, 14, 12, 12, 16)));
    public static final RegistryObject<Block> OXYGEN_MASK = reg("oxygen_mask", () -> new DecorBlock(cloth().mapColor(MapColor.COLOR_YELLOW).noCollission().noOcclusion().instabreak(),
            box(5, 2, 5, 11, 16, 11)));

    // ---------- Island ----------
    public static final RegistryObject<Block> PALM_LOG = reg("palm_log", () -> new RotatedPillarBlock(wood().mapColor(MapColor.TERRACOTTA_BROWN)));
    public static final RegistryObject<Block> PALM_PLANKS = reg("palm_planks", () -> new Block(wood().mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> PALM_LEAVES = reg("palm_leaves", () -> new LeavesBlock(Properties.copy(Blocks.JUNGLE_LEAVES)));
    public static final RegistryObject<Block> COCONUT = reg("coconut", () -> new DecorBlock(wood().strength(0.4f).noOcclusion(), box(4, 3, 4, 12, 11, 12)));
    public static final RegistryObject<Block> THATCH = reg("thatch", () -> new Block(Properties.copy(Blocks.HAY_BLOCK)));
    public static final RegistryObject<Block> TOTEM = reg("totem", () -> new DecorBlock(wood().mapColor(MapColor.COLOR_BROWN)));
    public static final RegistryObject<Block> BITTERLEAF = reg("bitterleaf", () -> new IslandPlantBlock(plant()));
    public static final RegistryObject<Block> TARO_PLANT = reg("taro_plant", () -> new IslandPlantBlock(plant()));
    public static final RegistryObject<Block> GHOST_ORCHID = reg("ghost_orchid", () -> new IslandPlantBlock(plant().lightLevel(s -> 8)));
    public static final RegistryObject<Block> WRECKAGE = reg("wreckage", () -> new Block(metal().mapColor(MapColor.COLOR_GRAY).strength(1.5f)));
    public static final RegistryObject<Block> BOAT_FRAME = reg("boat_frame", () -> new InteractBlock("boat_frame", wood().strength(-1f, 3600000f)));
    public static final RegistryObject<Block> BOAT_HELM = reg("boat_helm", () -> new InteractBlock("boat_helm", wood().strength(-1f, 3600000f).noOcclusion(),
            box(6, 0, 6, 10, 10, 10), box(1, 6, 6, 15, 20, 9)));
    public static final RegistryObject<Block> POLAROID_WALL = reg("polaroid_wall", () -> new InteractBlock("polaroid_wall", wood().mapColor(MapColor.COLOR_BLACK)));
    public static final RegistryObject<Block> HOLLOW_EFFIGY = reg("hollow_effigy", () -> new DecorBlock(wood().mapColor(MapColor.COLOR_BLACK).noOcclusion(),
            box(4, 0, 4, 12, 16, 12)));

    // ---------- The Eye (Meridian station) ----------
    public static final RegistryObject<Block> BUNKER_PANEL = reg("bunker_panel", () -> new Block(metal().mapColor(MapColor.COLOR_GRAY).strength(-1f, 3600000f)));
    public static final RegistryObject<Block> BUNKER_LIGHT = reg("bunker_light", () -> new LightBlock(metal().strength(-1f, 3600000f)));
    public static final RegistryObject<Block> MONITOR = reg("monitor", () -> new StateInteractBlock("monitor", metal().lightLevel(s -> 7)));
    public static final RegistryObject<Block> SERVER_RACK = reg("server_rack", () -> new DecorBlock(metal().lightLevel(s -> 3)));
    public static final RegistryObject<Block> HALO_TERMINAL = reg("halo_terminal", () -> new InteractBlock("halo_terminal", metal().strength(-1f, 3600000f).lightLevel(s -> 10)));
    public static final RegistryObject<Block> KEYPAD = reg("keypad", () -> new InteractBlock("keypad", metal().strength(-1f, 3600000f).noOcclusion().lightLevel(s -> 4),
            box(4, 3, 14, 12, 13, 16)));
    public static final RegistryObject<Block> TRANSMITTER = reg("transmitter", () -> new StateInteractBlock("transmitter", metal().strength(-1f, 3600000f)
            .lightLevel(s -> s.getValue(StateInteractBlock.LIT) ? 12 : 0)));
}
