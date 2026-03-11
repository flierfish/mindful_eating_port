package com.minecraftabnormals.mindful_eating.core.capability;

import net.minecraft.resources.ResourceLocation;

public interface IPlayerData {
    ResourceLocation getLastFood();

    void setLastFood(ResourceLocation food);

    int getSheenCooldown();

    void setSheenCooldown(int cooldown);

    boolean getHurtOrHeal();

    void setHurtOrHeal(boolean value);

    void copyFrom(IPlayerData other);
}
