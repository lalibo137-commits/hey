package com.airlinestory.entity;

import com.airlinestory.story.StoryEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Unrated: faceless figures in passenger clothes. They only move while nobody is looking at them.
 * Watchers never approach; they stand at a distance and vanish when you come close.
 */
public class HollowEntity extends Monster {
    private boolean watcher;
    private boolean harmless;
    private int lifeTicks = 20 * 60 * 4;
    private int attackCooldown;
    private int seenTicks;
    private int variant;

    public HollowEntity(EntityType<? extends HollowEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public HollowEntity configure(boolean watcher, boolean harmless, int lifeTicks) {
        this.watcher = watcher;
        this.harmless = harmless;
        this.lifeTicks = lifeTicks;
        this.variant = random.nextInt(4);
        return this;
    }

    public int variant() {
        return getId() % 4;
    }

    @Override
    protected void registerGoals() {}

    /** Seen = inside the player's view cone with a clear line of sight. */
    public boolean isSeenBy(Player p) {
        Vec3 look = p.getViewVector(1f).normalize();
        Vec3 to = new Vec3(getX() - p.getX(), getEyeY() - p.getEyeY(), getZ() - p.getZ());
        double len = to.length();
        if (len < 0.001) return true;
        double dot = look.dot(to.scale(1 / len));
        return dot > 0.45 && p.hasLineOfSight(this);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (--lifeTicks <= 0 || (level().isDay() && !watcher && level().canSeeSky(blockPosition()))) {
            vanish();
            return;
        }
        Player p = level().getNearestPlayer(this, 64);
        if (p == null || p.isCreative() || p.isSpectator()) {
            getNavigation().stop();
            return;
        }
        boolean seen = isSeenBy(p);
        double dist = distanceTo(p);
        if (watcher) {
            getNavigation().stop();
            if (!seen) getLookControl().setLookAt(p, 180f, 180f);
            if (seen) seenTicks++;
            if (dist < 14 || seenTicks > 60) vanish();
            return;
        }
        if (seen) {
            // Frozen mid-step. Not even its head turns.
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            return;
        }
        getLookControl().setLookAt(p, 180f, 180f);
        if (attackCooldown > 0) attackCooldown--;
        if (dist < 1.8) {
            if (attackCooldown <= 0) {
                if (!harmless) {
                    doHurtTarget(p);
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
                }
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                attackCooldown = 30;
                StoryEvents.onHollowTouch(this, p);
                if (harmless) vanish();
            }
        } else {
            getNavigation().moveTo(p, dist > 12 ? 1.35 : 1.15);
        }
    }

    public void scatter() {
        vanish();
    }

    public void vanish() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.01);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 1f, 0.5f);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && !level().isClientSide && random.nextFloat() < 0.35f) vanish();
        return r;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {}

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SOUL_ESCAPE;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SOUL_ESCAPE;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return dist > 96 * 96;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Watcher", watcher);
        tag.putBoolean("Harmless", harmless);
        tag.putInt("Life", lifeTicks);
        tag.putInt("Variant", variant);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        watcher = tag.getBoolean("Watcher");
        harmless = tag.getBoolean("Harmless");
        lifeTicks = tag.getInt("Life");
        variant = tag.getInt("Variant");
    }
}
