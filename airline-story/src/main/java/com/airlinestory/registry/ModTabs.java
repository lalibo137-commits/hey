package com.airlinestory.registry;

import com.airlinestory.AirlineStory;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    private ModTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AirlineStory.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.airlinestory"))
            .icon(() -> new ItemStack(ModItems.BOARDING_PASS.get()))
            .displayItems((params, out) -> {
                for (var b : ModBlocks.ALL) out.accept(b.get());
                for (var i : ModItems.ALL) out.accept(i.get());
            })
            .build());
}
