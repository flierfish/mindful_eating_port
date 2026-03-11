package com.minecraftabnormals.mindful_eating.core.capability;

import com.minecraftabnormals.mindful_eating.core.MindfulEating;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MindfulEating.MODID)
public class PlayerDataCapability {
    public static final Capability<IPlayerData> PLAYER_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerData.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<?> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MindfulEating.MODID, "player_data"),
                new PlayerDataProvider()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(PLAYER_DATA).ifPresent(oldData -> {
                event.getEntity().getCapability(PLAYER_DATA).ifPresent(newData -> {
                    newData.copyFrom(oldData);
                });
            });
        }
    }

    public static IPlayerData getPlayerData(Player player) {
        return player.getCapability(PLAYER_DATA).orElseThrow(
            () -> new IllegalArgumentException("Player " + player.getName().getString() + " has no PlayerData capability")
        );
    }
}
