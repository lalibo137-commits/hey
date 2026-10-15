package com.airlinestory.client;

import com.airlinestory.dialogue.Dialogue;
import com.airlinestory.dialogue.Dialogues;
import com.airlinestory.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Handles packets from the server on the client thread. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void handle(Object msg) {
        Minecraft mc = Minecraft.getInstance();
        if (msg instanceof Net.Sync s) {
            String old = ClientStory.INSTANCE.quest;
            ClientStory.INSTANCE.apply(s);
            if (!old.equals(s.quest())) ClientFx.onQuestChanged();
        } else if (msg instanceof Net.OpenDialogue o) {
            Dialogue d = Dialogues.get(o.tree());
            if (d == null || mc.player == null) return;
            Entity npc = o.npc() >= 0 && mc.level != null ? mc.level.getEntity(o.npc()) : null;
            if (CutscenePlayer.isPlaying()) return;
            mc.setScreen(new DialogueScreen(d, o.node(), o.npc(), npc));
        } else if (msg instanceof Net.PlayCutscene c) {
            CutscenePlayer.play(c.id());
        } else if (msg instanceof Net.Fx f) {
            ClientFx.handle(f);
        } else if (msg instanceof Net.Ending e) {
            mc.setScreen(new EndingScreen(e.kind(), e.pip(), e.rating()));
        }
    }
}
