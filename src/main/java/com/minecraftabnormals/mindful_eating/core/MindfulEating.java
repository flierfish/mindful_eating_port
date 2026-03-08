package com.minecraftabnormals.mindful_eating.core;

import com.minecraftabnormals.mindful_eating.client.HungerOverlay;
import com.minecraftabnormals.mindful_eating.compat.AppleskinCompat;
import com.minecraftabnormals.mindful_eating.compat.ModCompat;
import com.teamabnormals.blueprint.common.world.storage.tracking.DataProcessors;
import com.teamabnormals.blueprint.common.world.storage.tracking.TrackedData;
import com.teamabnormals.blueprint.common.world.storage.tracking.TrackedDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

@Mod(MindfulEating.MODID)
public class MindfulEating {
    public static final String MODID = "mindful_eating";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final HashMap<String, Integer> ORIGINAL_ITEMS = new HashMap<>();
    public static final HashMap<String, FoodProperties> ORIGINAL_FOODS = new HashMap<>();

    public static final TrackedData<ResourceLocation> LAST_FOOD = TrackedData.Builder.create(DataProcessors.RESOURCE_LOCATION, () -> ResourceLocation.tryParse("minecraft:cooked_beef")).enableSaving().build();
    public static final TrackedData<Integer> SHEEN_COOLDOWN = TrackedData.Builder.create(DataProcessors.INT, () -> 0).enableSaving().build();
    public static final TrackedData<Boolean> HURT_OR_HEAL = TrackedData.Builder.create(DataProcessors.BOOLEAN, () -> false).enableSaving().build();

    @SuppressWarnings("removal")
    public MindfulEating() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(this::clientSetup);
        
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            bus.addListener(HungerOverlay::registerOverlay);
            if (ModCompat.isAppleskinLoaded()) {
                MinecraftForge.EVENT_BUS.register(AppleskinCompat.class);
            }
        });

        registerTrackedData();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MEConfig.COMMON_SPEC);
    }

    private void registerTrackedData() {
        TrackedDataManager.INSTANCE.registerData(ResourceLocation.fromNamespaceAndPath(MODID, "last_food"), LAST_FOOD);
        TrackedDataManager.INSTANCE.registerData(ResourceLocation.fromNamespaceAndPath(MODID, "correct_food"), SHEEN_COOLDOWN);
        TrackedDataManager.INSTANCE.registerData(ResourceLocation.fromNamespaceAndPath(MODID, "hurt_or_heal"), HURT_OR_HEAL);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MinecraftForge.EVENT_BUS.addListener(HungerOverlay::hungerIconOverride);
        });
    }
}
