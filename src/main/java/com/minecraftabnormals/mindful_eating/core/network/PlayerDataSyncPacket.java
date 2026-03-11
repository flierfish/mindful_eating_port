package com.minecraftabnormals.mindful_eating.core.network;

import com.minecraftabnormals.mindful_eating.core.capability.PlayerDataCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class PlayerDataSyncPacket {
    private static final ResourceLocation DEFAULT_FOOD = ResourceLocation.tryParse("minecraft:cooked_beef");
    
    private final ResourceLocation lastFood;
    private final int sheenCooldown;
    private final boolean hurtOrHeal;

    public PlayerDataSyncPacket(ResourceLocation lastFood, int sheenCooldown, boolean hurtOrHeal) {
        this.lastFood = lastFood != null ? lastFood : DEFAULT_FOOD;
        this.sheenCooldown = sheenCooldown;
        this.hurtOrHeal = hurtOrHeal;
    }

    public static void encode(PlayerDataSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.lastFood);
        buffer.writeInt(packet.sheenCooldown);
        buffer.writeBoolean(packet.hurtOrHeal);
    }

    public static PlayerDataSyncPacket decode(FriendlyByteBuf buffer) {
        return new PlayerDataSyncPacket(
            buffer.readResourceLocation(),
            buffer.readInt(),
            buffer.readBoolean()
        );
    }

    public static void handle(PlayerDataSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClientSide(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClientSide(PlayerDataSyncPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                data.setLastFood(packet.lastFood);
                data.setSheenCooldown(packet.sheenCooldown);
                data.setHurtOrHeal(packet.hurtOrHeal);
            });
        }
    }

    public static void syncToClient(ServerPlayer player) {
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PlayerDataSyncPacket(data.getLastFood(), data.getSheenCooldown(), data.getHurtOrHeal())
            );
        });
    }

    public static void syncToTracking(ServerPlayer player) {
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            NetworkHandler.INSTANCE.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> player),
                new PlayerDataSyncPacket(data.getLastFood(), data.getSheenCooldown(), data.getHurtOrHeal())
            );
        });
    }
}
