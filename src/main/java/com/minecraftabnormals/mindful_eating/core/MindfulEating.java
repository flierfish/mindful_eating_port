package com.minecraftabnormals.mindful_eating.core;

import com.minecraftabnormals.mindful_eating.client.HungerOverlay;
import com.minecraftabnormals.mindful_eating.core.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.world.food.FoodProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

@Mod(MindfulEating.MODID)
public class MindfulEating {
    public static final String MODID = "mindful_eating";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final HashMap<String, Integer> ORIGINAL_ITEMS = new HashMap<>();
    public static final HashMap<String, FoodProperties> ORIGINAL_FOODS = new HashMap<>();

    @SuppressWarnings("removal")
    public MindfulEating() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(this::commonSetup);
        bus.addListener(this::clientSetup);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            bus.addListener(HungerOverlay::registerOverlay);
        });

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MEConfig.COMMON_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
        });
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MinecraftForge.EVENT_BUS.addListener(HungerOverlay::hungerIconOverride);
        });
    }
}
