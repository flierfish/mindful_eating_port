package com.minecraftabnormals.mindful_eating.core.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

public class PlayerDataProvider implements ICapabilitySerializable<CompoundTag> {
    private final PlayerData playerData = new PlayerData();
    private final LazyOptional<IPlayerData> optional = LazyOptional.of(() -> this.playerData);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == PlayerDataCapability.PLAYER_DATA) {
            return this.optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.playerData.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.playerData.load(nbt);
    }
}
