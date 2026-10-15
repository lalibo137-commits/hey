package com.airlinestory;

import com.airlinestory.entity.ModEntities;
import com.airlinestory.net.Net;
import com.airlinestory.registry.ModBlocks;
import com.airlinestory.registry.ModItems;
import com.airlinestory.registry.ModTabs;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AirlineStory.MODID)
public class AirlineStory {
    public static final String MODID = "airlinestory";
    public static final Logger LOG = LoggerFactory.getLogger("AirlineStory");

    public AirlineStory() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModTabs.TABS.register(bus);
        bus.addListener(ModEntities::onAttributes);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Net::register);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
