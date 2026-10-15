package com.airlinestory.story;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

/** Smooths over vanilla sounds that are registered as holders. */
public final class Snd {
    private Snd() {}

    public static SoundEvent of(SoundEvent e) { return e; }

    public static SoundEvent of(Holder<SoundEvent> h) { return h.value(); }
}
