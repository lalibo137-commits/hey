package com.airlinestory.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** An item with flavour text in its tooltip ("item.airlinestory.<name>.lore0", lore1, ...). */
public class LoreItem extends Item {
    private final int loreLines;
    private final boolean glint;

    public LoreItem(Properties props, int loreLines, boolean glint) {
        super(props);
        this.loreLines = loreLines;
        this.glint = glint;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 0; i < loreLines; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".lore" + i).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        if (stack.hasTag() && stack.getTag().contains("Owner")) {
            tooltip.add(Component.literal("Passenger: " + stack.getTag().getString("Owner")).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return glint || super.isFoil(stack);
    }
}
