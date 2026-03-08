package com.minecraftabnormals.mindful_eating.core;

import net.minecraft.resources.ResourceLocation;

public class MindfulEatingConstants {
    public static final String MODID = MindfulEating.MODID;

    // Mod IDs
    public static final String LSO_MODID = "legendarysurvivaloverhaul";
    public static final String FARMERS_DELIGHT_MODID = "farmersdelight";
    public static final String APPLESKIN_MODID = "appleskin";

    // Textures
    public static final ResourceLocation GUI_HUNGER_ICONS = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/hunger_icons.png");
    public static final ResourceLocation GUI_NOURISHMENT_ICONS = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/nourished_icons.png");
    public static final ResourceLocation GUI_SATURATION_ICONS = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/saturation_icons.png");

    // Elements
    public static final ResourceLocation FOOD_LEVEL_ELEMENT = ResourceLocation.fromNamespaceAndPath("minecraft", "food_level");

    // Effects
    public static final ResourceLocation NOURISHMENT_EFFECT_RL = ResourceLocation.fromNamespaceAndPath(FARMERS_DELIGHT_MODID, "nourishment");
}
