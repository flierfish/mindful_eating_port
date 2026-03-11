package com.minecraftabnormals.mindful_eating.compat;

import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.minecraftabnormals.mindful_eating.core.MindfulEatingConstants;
import com.minecraftabnormals.mindful_eating.core.capability.IPlayerData;
import com.minecraftabnormals.mindful_eating.core.capability.PlayerDataCapability;
import com.minecraftabnormals.mindful_eating.core.network.PlayerDataSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import vectorwing.farmersdelight.common.Configuration;
import vectorwing.farmersdelight.common.block.PieBlock;

import java.util.Set;

@SuppressWarnings("null")
public class ModCompat {
    private static final Minecraft mc = Minecraft.getInstance();
    
    private static MobEffect nourishmentEffect = null;

    // FarmersDelight 配置缓存
    private static Boolean farmersDelightLoaded = null;
    private static Boolean stackableSoupEnabled = null;
    private static Boolean nourishedHungerOverlay = null;
    private static Boolean temporaryNourishedOverlay = null;
    
    // 缓存的刀标签
    private static final TagKey<Item> KNIFE_TAG = TagKey.create(Registries.ITEM, 
            ResourceLocation.fromNamespaceAndPath("forge", "tools/knives"));

    public static boolean isLSOLoaded() {
        return ModList.get().isLoaded(MindfulEatingConstants.LSO_MODID);
    }

    public static boolean isFarmersDelightLoaded() {
        if (farmersDelightLoaded == null) {
            farmersDelightLoaded = ModList.get().isLoaded(MindfulEatingConstants.FARMERS_DELIGHT_MODID);
        }
        return farmersDelightLoaded;
    }

    public static MobEffect getNourishmentEffect() {
        if (nourishmentEffect == null && isFarmersDelightLoaded()) {
            nourishmentEffect = ForgeRegistries.MOB_EFFECTS.getValue(MindfulEatingConstants.NOURISHMENT_EFFECT_RL);
        }
        return nourishmentEffect;
    }

    public static boolean isNourishedHungerOverlayEnabled() {
        if (nourishedHungerOverlay == null && isFarmersDelightLoaded()) {
            nourishedHungerOverlay = Configuration.NOURISHED_HUNGER_OVERLAY.get();
            temporaryNourishedOverlay = nourishedHungerOverlay;
        }
        return isFarmersDelightLoaded() && nourishedHungerOverlay;
    }

    public static boolean isStackableSoupEnabled() {
        if (stackableSoupEnabled == null && isFarmersDelightLoaded()) {
            stackableSoupEnabled = Configuration.ENABLE_STACKABLE_SOUP_ITEMS.get();
        }
        return isFarmersDelightLoaded() && stackableSoupEnabled;
    }

    public static void setNourishedHungerOverlay(boolean flag) {
        if (isFarmersDelightLoaded()) {
            temporaryNourishedOverlay = nourishedHungerOverlay;
            Configuration.NOURISHED_HUNGER_OVERLAY.set(flag);
        }
    }

    public static void resetNourishedHungerOverlay() {
        if (isFarmersDelightLoaded() && temporaryNourishedOverlay != null) {
            nourishedHungerOverlay = temporaryNourishedOverlay;
        }
    }

    public static boolean isPlayerCold() {
        if (!isLSOLoaded()) return false;
        Player player = mc.player;
        if (player == null) return false;
        
        return player.getActiveEffects().stream().anyMatch(effect -> {
            MobEffect type = effect.getEffect();
            ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(type);
            if (key == null) return false;
            return key.getNamespace().equals(MindfulEatingConstants.LSO_MODID) && 
                   (key.getPath().equals("cold_hunger") || key.getPath().equals("frostbite"));
        });
    }

    public static void pieEatenCheck(Block block, Player player, ItemStack heldItem) {
        if (!isFarmersDelightLoaded()) return;
        
        if (block instanceof PieBlock) {
            Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(block));
            String descId = block.asItem().getDescriptionId();
            if (player.getFoodData().needsFood() && !groups.isEmpty() && descId != null) {
                if (!heldItem.is(KNIFE_TAG)) {
                    String rawId = descId.replaceFirst("^item\\.", "").replaceFirst("^block\\.", "").replace(".", ":");
                    ResourceLocation currentFood = rawId != null ? ResourceLocation.tryParse(rawId) : null;
                    if (currentFood != null) {
                        IPlayerData playerData = PlayerDataCapability.getPlayerData(player);
                        playerData.setLastFood(currentFood);
                        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            PlayerDataSyncPacket.syncToClient(serverPlayer);
                        }
                    }
                }
            }
        }
    }
}
