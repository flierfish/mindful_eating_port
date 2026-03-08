package com.minecraftabnormals.mindful_eating.compat;

import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.minecraftabnormals.mindful_eating.core.MindfulEating;
import com.teamabnormals.blueprint.common.world.storage.tracking.IDataManager;
import com.teamabnormals.blueprint.core.util.TagUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import vectorwing.farmersdelight.common.Configuration;
import vectorwing.farmersdelight.common.block.PieBlock;

import java.util.Set;

public class FarmersDelightCompat {

    public static boolean ENABLE_STACKABLE_SOUP_ITEMS = Configuration.ENABLE_STACKABLE_SOUP_ITEMS.get();
    public static boolean NOURISHED_HUNGER_OVERLAY = Configuration.NOURISHED_HUNGER_OVERLAY.get();

    public static boolean TEMPORARY_NOURISHED_HUNGER_OVERLAY = NOURISHED_HUNGER_OVERLAY;
    public static void setNourishedHungerOverlay(boolean flag) {
        TEMPORARY_NOURISHED_HUNGER_OVERLAY = NOURISHED_HUNGER_OVERLAY;
        Configuration.NOURISHED_HUNGER_OVERLAY.set(flag);
    }

    public static void resetNourishedHungerOverlay() {
        NOURISHED_HUNGER_OVERLAY = TEMPORARY_NOURISHED_HUNGER_OVERLAY;
    }

    public static void pieEatenCheck(Block block, Player player, ItemStack heldItem) {
        if (block instanceof PieBlock) {
            Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(block));
            String descId = block.asItem().getDescriptionId();
            if (player.getFoodData().needsFood() && !groups.isEmpty() && descId != null) {
                var tag = TagUtil.itemTag("forge", "tools/knives");
                if (tag != null && !heldItem.is(tag)) {
                    String rawId = descId.replaceFirst("^item\\.", "").replaceFirst("^block\\.", "").replace(".", ":");
                    ResourceLocation currentFood = rawId != null ? ResourceLocation.tryParse(rawId) : null;
                    if (currentFood != null) {
                        IDataManager playerManager = ((IDataManager) player);
                        playerManager.setValue(MindfulEating.LAST_FOOD, currentFood);
                    }
                }
            }
        }
    }
}
