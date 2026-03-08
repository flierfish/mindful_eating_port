package com.minecraftabnormals.mindful_eating.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minecraftabnormals.mindful_eating.core.registry.other.MEOverrides;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = MindfulEating.MODID)
public class MEConfig {

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {

        event.addListener((stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {

                // reload original values for food, so if changes are removed they leave no trace

                for (Map.Entry<String, Integer> entry : MindfulEating.ORIGINAL_ITEMS.entrySet()) {
                    String key = entry.getKey();
                    if (key == null) continue;
                    ResourceLocation itemRl = ResourceLocation.tryParse(key);
                    if (itemRl == null) continue;
                    Item item = ForgeRegistries.ITEMS.getValue(itemRl);
                    if (item != null) {
                        MEOverrides.changeStackability(item, entry.getValue());
                    }
                }

                for (Map.Entry<String, FoodProperties> entry : MindfulEating.ORIGINAL_FOODS.entrySet()) {
                    String key = entry.getKey();
                    if (key == null) continue;
                    ResourceLocation itemRl = ResourceLocation.tryParse(key);
                    if (itemRl == null) continue;
                    Item item = ForgeRegistries.ITEMS.getValue(itemRl);
                    if (item != null) {
                        MEOverrides.changeHunger(item, entry.getValue().getNutrition());
                        MEOverrides.changeSaturation(item, entry.getValue().getSaturationModifier());
                        MEOverrides.changeFastEating(item, entry.getValue().isFastFood());
                        MEOverrides.changeCanEatWhenFull(item, entry.getValue().canAlwaysEat());
                    }
                }

                ResourceLocation path = ResourceLocation.fromNamespaceAndPath(MindfulEating.MODID, "food_changes.json");
                Optional<Resource> resource = Optional.empty();
                if (path != null) {
                    resource = resourceManager.getResource(path);
                }
                if(resource.isPresent()){
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().open()))) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                        String[] names = {"hunger", "saturation", "speedy", "stackability", "gorgable"};

                        for (int i = 0; i < names.length; i++) {

                            JsonObject object = json.get(names[i]).getAsJsonObject();

                            for (Map.Entry<String, JsonElement> map : object.entrySet()) {
                                String name = map.getKey();
                                if (name == null) continue;
                                ResourceLocation itemRl = ResourceLocation.tryParse(name);
                                if (itemRl == null) continue;
                                Item item = ForgeRegistries.ITEMS.getValue(itemRl);
                                if (item == null) continue;

                                // record original values before food is changed for the first time
                                if (!MindfulEating.ORIGINAL_ITEMS.containsKey(name)) {
                                    @SuppressWarnings("deprecation")
                                    int maxStackSize = item.getMaxStackSize();
                                    MindfulEating.ORIGINAL_ITEMS.put(name, maxStackSize);
                                }

                                if (item.isEdible() && !MindfulEating.ORIGINAL_FOODS.containsKey(name)) {
                                    FoodProperties food = item.getFoodProperties(item.getDefaultInstance(), null);
                                    if (food != null) {
                                        FoodProperties.Builder builder = (new FoodProperties.Builder()).nutrition(food.getNutrition())
                                                .saturationMod(food.getSaturationModifier());
                                        if (food.isFastFood()) builder.fast();
                                        if (food.canAlwaysEat()) builder.alwaysEat();
                                        MindfulEating.ORIGINAL_FOODS.put(name, builder.build());
                                    }
                                }

                                // reflects values
                                switch (i) {
                                    case 0:
                                        MEOverrides.changeHunger(item, map.getValue().getAsInt());
                                        break;
                                    case 1:
                                        MEOverrides.changeSaturation(item, map.getValue().getAsFloat());
                                        break;
                                    case 2:
                                        MEOverrides.changeFastEating(item, map.getValue().getAsBoolean());
                                        break;
                                    case 3:
                                        MEOverrides.changeStackability(item, map.getValue().getAsInt());
                                        break;
                                    case 4:
                                        MEOverrides.changeCanEatWhenFull(item, map.getValue().getAsBoolean());
                                }
                            }
                        }
                    } catch (IOException e) {

                        MindfulEating.LOGGER.error("Failed at reader {}", path);
                    }

                }else{
                    MindfulEating.LOGGER.error("Failed to load config at {}", path);
                }

            }, backgroundExecutor);

            return future.thenCompose(stage::wait);
        });
    }

    public static class Common {
        public final ConfigValue<Boolean> proportionalDiet;
        public final ConfigValue<Boolean> nativeDietBuffs;
        public final ConfigValue<Double> exhaustionReduction;
        public final ConfigValue<String>[] foodGroupExhaustion;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("MindfulEating common configuration").push("common");
            builder.push("mode");

            proportionalDiet = builder.comment("Whether the saturation bonus is dependent on Diet's mechanics. If false, it will instead be based on the last food eaten. Default: false").define("Proportional Diet", false);
            nativeDietBuffs = builder.comment("Whether the buffs added by the Diet mod are enabled. Default: false").define("Native Diet Buffs", false);
            exhaustionReduction = builder.comment("The amount exhaustion is reduced by (if the above config is false). Default: 0.75").define("Exhaustion Reduction", 0.75);

            builder.pop();
            builder.comment("For multiple food groups, separate groups with a /, for example: fruits/vegetables.").push("exhaustion sources");

            @SuppressWarnings("unchecked")
            ConfigValue<String>[] tempFoodGroupExhaustion = new ConfigValue[8];
            foodGroupExhaustion = tempFoodGroupExhaustion;
            String[] defaultFoodGroup = {"fruits", "vegetables", "vegetables", "grains", "proteins", "proteins", "sugars", "sugars"};

            for (int i = 0; i < foodGroupExhaustion.length; i++) {
                String exhaustionSourceName = ExhaustionSource.values()[i].toString().toLowerCase();
                foodGroupExhaustion[i] = builder.comment("The food group/s corresponding to the " + exhaustionSourceName + " exhaustion source. Default: "
                        + defaultFoodGroup[i]).define("Food Group - " + exhaustionSourceName, defaultFoodGroup[i]);
            }

            builder.pop();
            builder.pop();
        }
    }

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        final Pair<Common, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = commonSpecPair.getRight();
        COMMON = commonSpecPair.getLeft();
    }
}
