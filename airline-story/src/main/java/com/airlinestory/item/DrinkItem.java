package com.airlinestory.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** A food that is drunk and leaves a container behind (coffee cup, flasks). */
public class DrinkItem extends LoreItem {
    @Nullable
    private final Supplier<? extends net.minecraft.world.item.Item> container;

    public DrinkItem(Properties props, int lore, @Nullable Supplier<? extends net.minecraft.world.item.Item> container) {
        super(props, lore, false);
        this.container = container;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (container == null) return result;
        ItemStack empty = new ItemStack(container.get());
        if (result.isEmpty()) return empty;
        if (entity instanceof Player p && !p.getAbilities().instabuild && !p.getInventory().add(empty)) {
            p.drop(empty, false);
        }
        return result;
    }
}
