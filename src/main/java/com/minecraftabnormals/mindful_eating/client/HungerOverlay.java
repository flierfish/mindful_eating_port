package com.minecraftabnormals.mindful_eating.client;

import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.minecraftabnormals.mindful_eating.compat.ModCompat;
import com.minecraftabnormals.mindful_eating.core.MindfulEatingConstants;
import com.minecraftabnormals.mindful_eating.core.capability.IPlayerData;
import com.minecraftabnormals.mindful_eating.core.capability.PlayerDataCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.GuiOverlayManager;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class HungerOverlay {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Random random = new Random();
    
    private static final Map<ResourceLocation, Set<IDietGroup>> DIET_CACHE = new HashMap<>();
    private static ResourceLocation lastCachedFood = null;
    private static Set<IDietGroup> lastCachedGroups = null;
    
    public static int hungerBarRightHeight = -1;

    public static void hungerIconOverride(RenderGuiOverlayEvent event) {
        ResourceLocation foodLevel = MindfulEatingConstants.FOOD_LEVEL_ELEMENT;
        if (foodLevel != null) {
            Object overlay = GuiOverlayManager.findOverlay(foodLevel);
            if (overlay != null && Objects.equals(event.getOverlay(), overlay)) {
                ModCompat.resetNourishedHungerOverlay();
            }
        }
    }

    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        ResourceLocation foodLevel = MindfulEatingConstants.FOOD_LEVEL_ELEMENT;
        if (foodLevel != null) {
            event.registerAbove(foodLevel, "mindful_eating_hunger", (gui, graphics, partialTicks, width, height) -> {
                Player player = mc.player;
                if (player == null) return;

                boolean isMounted = player.getVehicle() instanceof LivingEntity;
                if (!isMounted && !mc.options.hideGui && gui.shouldDrawSurvivalElements()) {
                    if (hungerBarRightHeight != -1) {
                        renderHungerIcons(gui, graphics, player);
                    }
                }
            });
        }
    }

    private static void renderHungerIcons(ForgeGui gui, GuiGraphics graphics, Player player) {
        IPlayerData playerData = PlayerDataCapability.getPlayerData(player);
        ResourceLocation lastAte = playerData.getLastFood();

        if (lastAte == null) return;

        Set<IDietGroup> groups;
        if (Objects.equals(lastAte, lastCachedFood)) {
            groups = lastCachedGroups;
        } else {
            groups = DIET_CACHE.computeIfAbsent(lastAte, rl -> {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item == null) return null;
                ItemStack stack = new ItemStack(item);
                return DietApi.getInstance().getGroups(player, stack);
            });
            lastCachedFood = lastAte;
            lastCachedGroups = groups;
        }

        if (groups == null || groups.isEmpty()) return;

        FoodData foodData = player.getFoodData();
        int top = mc.getWindow().getGuiScaledHeight() - hungerBarRightHeight;
        int left = mc.getWindow().getGuiScaledWidth() / 2 + 91;

        drawHungerIcons(player, foodData, top, left, graphics, playerData, groups.toArray(new IDietGroup[0]));
    }

    @SuppressWarnings("null")
    private static void drawHungerIcons(Player player, FoodData stats, int top, int left, GuiGraphics graphics, IPlayerData playerData, IDietGroup[] groups) {
        int level = stats.getFoodLevel();
        int ticks = mc.gui.getGuiTicks();
        float modifiedSaturation = Math.min(stats.getSaturationLevel(), 20.0F);
        
        boolean hasHunger = player.hasEffect(MobEffects.HUNGER);
        boolean isFarmersDelightLoaded = ModCompat.isFarmersDelightLoaded();
        MobEffect nourishmentEffect = ModCompat.getNourishmentEffect();
        boolean hasNourishment = false;
        if (isFarmersDelightLoaded && nourishmentEffect != null) {
            MobEffect effect = nourishmentEffect;
            if (effect != null) {
                hasNourishment = player.hasEffect(effect);
            }
        }
        boolean hasSheen = playerData.getSheenCooldown() > 0;

        for (int i = 0; i < 10; i++) {
            int idx = i * 2 + 1;
            int x = left - i * 8 - 9;
            int y = top;
            int iconOffset = 0;

            FoodGroups foodGroup = FoodGroups.byDietGroup(groups[i % groups.length]);
            int textureV = foodGroup != null ? foodGroup.getTextureOffset() : 0;
            int backgroundU = 0;

            ResourceLocation texture = MindfulEatingConstants.GUI_HUNGER_ICONS;
            if (texture == null) continue;

            if (hasHunger) {
                iconOffset += 36;
                backgroundU = 13;
            }

            if (hasNourishment && ModCompat.isNourishedHungerOverlayEnabled()) {
                ModCompat.setNourishedHungerOverlay(false);
                ResourceLocation nourishmentTexture = MindfulEatingConstants.GUI_NOURISHMENT_ICONS;
                if (nourishmentTexture != null) {
                    texture = nourishmentTexture;
                    iconOffset -= hasHunger ? 45 : 27;
                    backgroundU = 0;
                }
            }

            if (stats.getSaturationLevel() <= 0.0F && ticks % (level * 3 + 1) == 0) {
                y = top + (random.nextInt(3) - 1);
            }

            graphics.blit(texture, x, y, backgroundU * 9, textureV, 9, 9, 126, 45);

            if (idx < level) {
                graphics.blit(texture, x, y, iconOffset + 36, textureV, 9, 9, 126, 45);
            } else if (idx == level) {
                graphics.blit(texture, x, y, iconOffset + 45, textureV, 9, 9, 126, 45);
            }

            // 饱和度显示（由 Mindful Eating 自行渲染）
            {
                float effectiveSaturationOfBar = (modifiedSaturation / 2.0F) - i;
                if (effectiveSaturationOfBar > 0) {
                    int u = 0;
                    if (effectiveSaturationOfBar >= 1) u = 4 * 9;
                    else if (effectiveSaturationOfBar > .75) u = 3 * 9;
                    else if (effectiveSaturationOfBar > .5) u = 2 * 9;
                    else if (effectiveSaturationOfBar > .25) u = 9;

                    ResourceLocation saturationTexture = MindfulEatingConstants.GUI_SATURATION_ICONS;
                    if (saturationTexture != null) {
                        graphics.blit(saturationTexture, x, y, u, textureV, 9, 9, 126, 45);
                    }
                }
            }

            if (idx <= level && hasSheen) {
                int tick = ticks % 20;
                if ((tick < idx + level / 4 && tick > idx - level / 4) || (tick == 49 && i == 0)) {
                    int uOffset = idx == level ? 18 : 9;
                    ResourceLocation sheenTexture = MindfulEatingConstants.GUI_NOURISHMENT_ICONS;
                    if (sheenTexture != null) {
                        graphics.blit(sheenTexture, x, y, uOffset, textureV, 9, 9, 126, 45);
                    }
                }
            }
        }
    }
}
