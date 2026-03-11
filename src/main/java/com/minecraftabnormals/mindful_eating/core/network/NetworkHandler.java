package com.minecraftabnormals.mindful_eating.core.network;

import com.minecraftabnormals.mindful_eating.core.MindfulEating;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(MindfulEating.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.messageBuilder(PlayerDataSyncPacket.class, packetId++)
            .encoder(PlayerDataSyncPacket::encode)
            .decoder(PlayerDataSyncPacket::decode)
            .consumerMainThread(PlayerDataSyncPacket::handle)
            .add();
    }
}
