package com.minecraftabnormals.mindful_eating.core.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public class PlayerData implements IPlayerData {
    private ResourceLocation lastFood = ResourceLocation.tryParse("minecraft:cooked_beef");
    private int sheenCooldown = 0;
    private boolean hurtOrHeal = false;

    @Override
    public ResourceLocation getLastFood() {
        return this.lastFood;
    }

    @Override
    public void setLastFood(ResourceLocation food) {
        this.lastFood = food;
    }

    @Override
    public int getSheenCooldown() {
        return this.sheenCooldown;
    }

    @Override
    public void setSheenCooldown(int cooldown) {
        this.sheenCooldown = Math.max(0, cooldown);
    }

    @Override
    public boolean getHurtOrHeal() {
        return this.hurtOrHeal;
    }

    @Override
    public void setHurtOrHeal(boolean value) {
        this.hurtOrHeal = value;
    }

    @Override
    public void copyFrom(IPlayerData other) {
        this.lastFood = other.getLastFood();
        this.sheenCooldown = other.getSheenCooldown();
        this.hurtOrHeal = other.getHurtOrHeal();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (this.lastFood != null) {
            String foodStr = this.lastFood.toString();
            if (foodStr != null) {
                tag.putString("LastFood", foodStr);
            }
        }
        tag.putInt("SheenCooldown", this.sheenCooldown);
        tag.putBoolean("HurtOrHeal", this.hurtOrHeal);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("LastFood", Tag.TAG_STRING)) {
            String foodStr = tag.getString("LastFood");
            if (foodStr != null && !foodStr.isEmpty()) {
                this.lastFood = ResourceLocation.tryParse(foodStr);
            }
        }
        this.sheenCooldown = tag.getInt("SheenCooldown");
        this.hurtOrHeal = tag.getBoolean("HurtOrHeal");
    }
}
