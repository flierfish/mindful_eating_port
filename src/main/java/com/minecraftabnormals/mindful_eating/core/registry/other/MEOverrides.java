package com.minecraftabnormals.mindful_eating.core.registry.other;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class MEOverrides {

    public static void changeHunger(Item item, int hunger) {
        FoodProperties foodProps = item.getFoodProperties(item.getDefaultInstance(), null);
        if (foodProps != null) {
            ObfuscationReflectionHelper.setPrivateValue(FoodProperties.class, foodProps, hunger, "f_38723_");
        }
    }

    public static void changeSaturation(Item item, float saturation) {
        FoodProperties foodProps = item.getFoodProperties(item.getDefaultInstance(), null);
        if (foodProps != null) {
            ObfuscationReflectionHelper.setPrivateValue(FoodProperties.class, foodProps, saturation, "f_38724_");
        }
    }

    public static void changeFastEating(Item item, boolean fast) {
        FoodProperties foodProps = item.getFoodProperties(item.getDefaultInstance(), null);
        if (foodProps != null) {
            ObfuscationReflectionHelper.setPrivateValue(FoodProperties.class, foodProps, fast, "f_38727_");
        }
    }

    public static void changeCanEatWhenFull(Item item, boolean gorgable) {
        FoodProperties foodProps = item.getFoodProperties(item.getDefaultInstance(), null);
        if (foodProps != null) {
            ObfuscationReflectionHelper.setPrivateValue(FoodProperties.class, foodProps, gorgable, "f_38726_");
        }
    }

    public static void changeStackability(Item item, int size) {
        ObfuscationReflectionHelper.setPrivateValue(Item.class, item, size, "f_41370_");
    }

}
