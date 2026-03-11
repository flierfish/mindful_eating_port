package com.minecraftabnormals.mindful_eating.core.registry.other;

import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.DietEvent;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.common.capability.DietCapability;
import com.minecraftabnormals.mindful_eating.compat.ModCompat;
import com.minecraftabnormals.mindful_eating.core.ExhaustionSource;
import com.minecraftabnormals.mindful_eating.core.MEConfig;
import com.minecraftabnormals.mindful_eating.core.MindfulEating;
import com.minecraftabnormals.mindful_eating.core.capability.IPlayerData;
import com.minecraftabnormals.mindful_eating.core.capability.PlayerDataCapability;
import com.minecraftabnormals.mindful_eating.core.network.PlayerDataSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Mod.EventBusSubscriber(modid = MindfulEating.MODID)
public class MEEvents {

    private static final Map<ResourceLocation, Set<IDietGroup>> DIET_CACHE = new HashMap<>();

    @SubscribeEvent
    public static void disableDietBuffs(DietEvent.ApplyEffect event) {
        event.setCanceled(!MEConfig.COMMON.nativeDietBuffs.get());
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (event.getItem().isEdible() && event.getEntity() instanceof Player player) {
            String descId = event.getItem().getDescriptionId();
            ResourceLocation currentFood = descId != null ? parse(descId) : null;

            IPlayerData playerData = PlayerDataCapability.getPlayerData(player);
            Item item = event.getItem().getItem();
            if (item == null) return;
            Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(item));

            if (currentFood != null && !groups.isEmpty()) {
                playerData.setLastFood(currentFood);
                syncToClient(player);
            }

            if (ModCompat.isStackableSoupEnabled() && !(item instanceof SuspiciousStewItem))
                return;

            if (item instanceof BowlFoodItem || item instanceof SuspiciousStewItem) {
                event.getItem().shrink(1);
                if (event.getItem().isEmpty()) {
                    Item bowl = Items.BOWL;
                    if (bowl != null) {
                        event.setResultStack(new ItemStack(bowl));
                    }
                } else {
                    if (!player.getAbilities().instabuild) {
                        Item bowl = Items.BOWL;
                        if (bowl != null) {
                            ItemStack itemstack = new ItemStack(bowl);
                            if (!player.getInventory().add(itemstack)) {
                                player.drop(itemstack, false);
                            }
                        }
                    }

                    event.setResultStack(event.getItem());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCakeEaten(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        if (pos == null) return;
        Block block = event.getLevel().getBlockState(pos).getBlock();
        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();

        if (block instanceof CakeBlock) {
            Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(block));
            String descId = block.asItem().getDescriptionId();
            if (player.getFoodData().needsFood() && !groups.isEmpty() && descId != null) {
                ResourceLocation currentFood = parse(descId);
                if (currentFood != null) {
                    IPlayerData playerData = PlayerDataCapability.getPlayerData(player);
                    playerData.setLastFood(currentFood);
                    syncToClient(player);
                }
            }
        }

        if (ModCompat.isFarmersDelightLoaded()) {
            ModCompat.pieEatenCheck(block, player, heldItem);
        }
    }

    private static ResourceLocation parse(String input) {
        String[] parts = input.split("\\.");
        if (parts.length < 3) return null;
        String result = parts[1] + ":" + parts[2];
        return ResourceLocation.tryParse(result);
    }

    @SubscribeEvent
    public static void onPlayerMining(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.swinging) {
            exhaustionReductionShortSheen(player, ExhaustionSource.MINE);
        }
    }

    @SubscribeEvent
    public static void onBlockHarvested(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.MINE);
        player.causeFoodExhaustion(0.005F * ratio);
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.ATTACK);
        player.causeFoodExhaustion(0.1F * ratio);
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.HURT);
            player.causeFoodExhaustion(event.getSource().getFoodExhaustion() * ratio);
        }
    }

    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.HEAL);
            player.causeFoodExhaustion(6.0F * event.getAmount() * ratio);
        }
    }

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.JUMP);
            if (player.isSprinting()) {
                player.causeFoodExhaustion(0.2F * ratio);
            } else {
                player.causeFoodExhaustion(0.05F * ratio);
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START)
            return;

        Player player = event.player;
        IPlayerData playerData = PlayerDataCapability.getPlayerData(player);

        if (playerData.getSheenCooldown() > 0) {
            playerData.setSheenCooldown(Math.max(0, playerData.getSheenCooldown() - 1));
        }

        if (player.getActiveEffects().size() != 0) {
            for (MobEffectInstance effect : player.getActiveEffects()) {
                MobEffect effectType = effect.getEffect();
                MobEffect hunger = MobEffects.HUNGER;
                if (hunger != null && effectType == hunger) {
                    MobEffectInstance hungerEffect = player.getEffect(hunger);
                    if (hungerEffect != null) {
                        player.causeFoodExhaustion(0.0025F * (float) (hungerEffect.getAmplifier() + 1) * exhaustionReductionShortSheen(player, ExhaustionSource.EFFECT));
                    }
                    break;
                }
            }
        }

        float reduction = 0;

        double disX = player.getX() - player.xOld;
        double disZ = player.getZ() - player.zOld;

        if (player.getDeltaMovement().length() == 0.0 || disX == 0.0 && disZ == 0.0) {
            return;
        }

        int distance = Math.round(Mth.sqrt((float) disX * (float) disX + (float) disZ * (float) disZ) * 100.0F);

        boolean isEyeInWater = false;
        var waterTag = FluidTags.WATER;
        if (waterTag != null) {
            isEyeInWater = player.isEyeInFluid(waterTag);
        }
        if (player.isSwimming() || isEyeInWater) {
            reduction = 0.0001F * exhaustionReductionShortSheen(player, ExhaustionSource.SWIM) * Math.round(Mth.sqrt((float) disX * (float) disX + (float) disZ * (float) disZ) * 100.0F);
        } else if (player.isInWater()) {
            reduction = 0.0001F * exhaustionReductionShortSheen(player, ExhaustionSource.SWIM) * distance;
        } else if (player.onGround() && player.isSprinting()) {
            reduction = 0.001F * exhaustionReductionShortSheen(player, ExhaustionSource.SPRINT) * distance;
        }

        player.getFoodData().addExhaustion(reduction);
    }


    public static float exhaustionReductionShortSheen(Player player, ExhaustionSource source) {
        return exhaustionReductionLongSheen(player, source, 7);
    }

    public static float exhaustionReductionLongSheen(Player player, ExhaustionSource source) {
        return exhaustionReductionLongSheen(player, source, 15); 
    }

    public static float exhaustionReductionLongSheen(Player player, ExhaustionSource source, int cooldown) {
        IPlayerData playerData = PlayerDataCapability.getPlayerData(player);

        playerData.setHurtOrHeal(source == ExhaustionSource.HURT || source == ExhaustionSource.HEAL);

        if (!MEConfig.COMMON.proportionalDiet.get()) {
            ResourceLocation lastFood = playerData.getLastFood();
            if (lastFood == null) return 0.0F;

            Set<IDietGroup> groups = DIET_CACHE.computeIfAbsent(lastFood, rl -> {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item == null) return Set.of();
                return DietApi.getInstance().getGroups(player, new ItemStack(item));
            });

            for (IDietGroup group : groups) {
                for (String configGroup : MEConfig.COMMON.foodGroupExhaustion[source.ordinal()].get().split("/")) {
                    if (group.getName().equals(configGroup)) {
                        playerData.setSheenCooldown(cooldown);
                        syncToClient(player);
                        return -MEConfig.COMMON.exhaustionReduction.get().floatValue();
                    }
                }
            }

            return 0.0F;

        } else {
            AtomicReference<Float> percentage = new AtomicReference<>(0.0F);
            DietCapability.get(player).ifPresent(tracker -> {
                float maxPercentage = 0.0F;
                for (String configGroup : MEConfig.COMMON.foodGroupExhaustion[source.ordinal()].get().split("/")) {
                    maxPercentage = Math.max(maxPercentage, tracker.getValue(configGroup));
                }
                percentage.set(maxPercentage);
            });
            if (percentage.get() > 0.0F) {
                playerData.setSheenCooldown(cooldown);
                syncToClient(player);
            }
            return -percentage.get();
        }
    }

    private static void syncToClient(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PlayerDataSyncPacket.syncToClient(serverPlayer);
        }
    }
}
