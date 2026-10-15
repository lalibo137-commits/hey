package com.airlinestory.item;

import com.airlinestory.entity.HollowEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** An emergency flare from the plane's survival kit. Its red light drives the Unrated away. */
public class FlareItem extends LoreItem {
    public FlareItem(Properties props) {
        super(props, 2, false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 0.6f);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 1.5, player.getZ(), 30, 0.6, 0.6, 0.6, 0.05);
            sl.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.5, player.getZ(), 60, 1.5, 1.0, 1.5, 0.02);
            int n = 0;
            for (HollowEntity h : sl.getEntitiesOfClass(HollowEntity.class, player.getBoundingBox().inflate(24))) {
                h.scatter();
                n++;
            }
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            player.displayClientMessage(Component.translatable(n > 0 ? "message.airlinestory.flare_scatter" : "message.airlinestory.flare"), true);
            player.getCooldowns().addCooldown(this, 100);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
