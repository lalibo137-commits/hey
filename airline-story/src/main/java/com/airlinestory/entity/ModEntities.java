package com.airlinestory.entity;

import com.airlinestory.AirlineStory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AirlineStory.MODID);

    public static final RegistryObject<EntityType<StoryNpc>> NPC = ENTITIES.register("story_npc",
            () -> EntityType.Builder.<StoryNpc>of(StoryNpc::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build("story_npc"));
    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(10).noSummon().build("seat"));
    public static final RegistryObject<EntityType<HollowEntity>> HOLLOW = ENTITIES.register("hollow",
            () -> EntityType.Builder.<HollowEntity>of(HollowEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10).build("hollow"));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(NPC.get(), StoryNpc.createAttributes().build());
        event.put(HOLLOW.get(), HollowEntity.createAttributes().build());
    }
}
