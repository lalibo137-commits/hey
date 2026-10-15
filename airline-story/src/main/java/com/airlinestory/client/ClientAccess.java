package com.airlinestory.client;

import net.minecraft.client.Minecraft;

/** Client-only actions reachable from common code through DistExecutor. */
public final class ClientAccess {
    private ClientAccess() {}

    public static void openJournal() {
        Minecraft.getInstance().setScreen(new JournalScreen());
    }
}
