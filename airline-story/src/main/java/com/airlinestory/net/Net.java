package com.airlinestory.net;

import com.airlinestory.AirlineStory;
import com.airlinestory.client.ClientHooks;
import com.airlinestory.story.StoryDirector;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.*;
import java.util.function.Supplier;

public final class Net {
    private Net() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(AirlineStory.id("main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(Sync.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(Sync::encode).decoder(Sync::decode)
                .consumerMainThread(Net::toClient).add();
        CHANNEL.messageBuilder(OpenDialogue.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(OpenDialogue::encode).decoder(OpenDialogue::decode)
                .consumerMainThread(Net::toClient).add();
        CHANNEL.messageBuilder(PlayCutscene.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(PlayCutscene::encode).decoder(PlayCutscene::decode)
                .consumerMainThread(Net::toClient).add();
        CHANNEL.messageBuilder(Fx.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(Fx::encode).decoder(Fx::decode)
                .consumerMainThread(Net::toClient).add();
        CHANNEL.messageBuilder(Ending.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(Ending::encode).decoder(Ending::decode)
                .consumerMainThread(Net::toClient).add();
        CHANNEL.messageBuilder(Choice.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(Choice::encode).decoder(Choice::decode)
                .consumerMainThread(Net::toServer).add();
        CHANNEL.messageBuilder(CloseDialogue.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(CloseDialogue::encode).decoder(CloseDialogue::decode)
                .consumerMainThread(Net::toServer).add();
        CHANNEL.messageBuilder(CutsceneDone.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(CutsceneDone::encode).decoder(CutsceneDone::decode)
                .consumerMainThread(Net::toServer).add();
        CHANNEL.messageBuilder(Rate.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(Rate::encode).decoder(Rate::decode)
                .consumerMainThread(Net::toServer).add();
        CHANNEL.messageBuilder(EndingDone.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(EndingDone::encode).decoder(EndingDone::decode)
                .consumerMainThread(Net::toServer).add();
    }

    private static void toClient(Object msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.handle(msg));
    }

    private static void toServer(Object msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        StoryDirector d = StoryDirector.get();
        if (player != null && d != null) d.onPacket(player, msg);
    }

    public static void send(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void toServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    // ------------------------------------------------------------------ packets
    public record Sync(String quest, List<String> flags, Map<String, Integer> counters, float rating,
                       List<String> completed, List<String> memories, List<String> ratingLog) {
        void encode(FriendlyByteBuf b) {
            b.writeUtf(quest);
            b.writeCollection(flags, FriendlyByteBuf::writeUtf);
            b.writeMap(counters, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
            b.writeFloat(rating);
            b.writeCollection(completed, FriendlyByteBuf::writeUtf);
            b.writeCollection(memories, FriendlyByteBuf::writeUtf);
            b.writeCollection(ratingLog, FriendlyByteBuf::writeUtf);
        }

        static Sync decode(FriendlyByteBuf b) {
            return new Sync(b.readUtf(), b.readList(FriendlyByteBuf::readUtf), b.readMap(HashMap::new, FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt),
                    b.readFloat(), b.readList(FriendlyByteBuf::readUtf), b.readList(FriendlyByteBuf::readUtf), b.readList(FriendlyByteBuf::readUtf));
        }
    }

    public record OpenDialogue(int npc, String tree, String node, boolean rate) {
        void encode(FriendlyByteBuf b) { b.writeVarInt(npc); b.writeUtf(tree); b.writeUtf(node); b.writeBoolean(rate); }
        static OpenDialogue decode(FriendlyByteBuf b) { return new OpenDialogue(b.readVarInt(), b.readUtf(), b.readUtf(), b.readBoolean()); }
    }

    public record Rate(int npc, int stars) {
        void encode(FriendlyByteBuf b) { b.writeVarInt(npc); b.writeVarInt(stars); }
        static Rate decode(FriendlyByteBuf b) { return new Rate(b.readVarInt(), b.readVarInt()); }
    }

    public record PlayCutscene(String id) {
        void encode(FriendlyByteBuf b) { b.writeUtf(id); }
        static PlayCutscene decode(FriendlyByteBuf b) { return new PlayCutscene(b.readUtf()); }
    }

    /** A client effect: "pa", "halo", "hint", "rating", "remember", "turbulence", "fade_out", "black", "scare", "glitch", "chime", "loading". */
    public record Fx(String type, String text, float value, int ticks) {
        void encode(FriendlyByteBuf b) { b.writeUtf(type); b.writeUtf(text, 32767); b.writeFloat(value); b.writeVarInt(ticks); }
        static Fx decode(FriendlyByteBuf b) { return new Fx(b.readUtf(), b.readUtf(32767), b.readFloat(), b.readVarInt()); }
    }

    public record Ending(String kind, boolean pip, float rating) {
        void encode(FriendlyByteBuf b) { b.writeUtf(kind); b.writeBoolean(pip); b.writeFloat(rating); }
        static Ending decode(FriendlyByteBuf b) { return new Ending(b.readUtf(), b.readBoolean(), b.readFloat()); }
    }

    public record Choice(int npc, String tree, String node, int index) {
        void encode(FriendlyByteBuf b) { b.writeVarInt(npc); b.writeUtf(tree); b.writeUtf(node); b.writeVarInt(index); }
        static Choice decode(FriendlyByteBuf b) { return new Choice(b.readVarInt(), b.readUtf(), b.readUtf(), b.readVarInt()); }
    }

    public record CloseDialogue(int npc) {
        void encode(FriendlyByteBuf b) { b.writeVarInt(npc); }
        static CloseDialogue decode(FriendlyByteBuf b) { return new CloseDialogue(b.readVarInt()); }
    }

    public record CutsceneDone(String id) {
        void encode(FriendlyByteBuf b) { b.writeUtf(id); }
        static CutsceneDone decode(FriendlyByteBuf b) { return new CutsceneDone(b.readUtf()); }
    }

    public record EndingDone() {
        void encode(FriendlyByteBuf b) {}
        static EndingDone decode(FriendlyByteBuf b) { return new EndingDone(); }
    }
}
