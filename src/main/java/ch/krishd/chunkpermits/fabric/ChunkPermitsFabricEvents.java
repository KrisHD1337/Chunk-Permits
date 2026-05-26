//? if fabric {
package ch.krishd.chunkpermits.fabric;

import ch.krishd.chunkpermits.ChunkPermitsServices;
import ch.krishd.chunkpermits.claim.ClaimKey;
import ch.krishd.chunkpermits.fabric.particles.ChunkBorderDisplayManager;
import ch.krishd.chunkpermits.protection.AccessResult;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.nio.file.Path;
import java.lang.reflect.Proxy;

public final class ChunkPermitsFabricEvents {
    private ChunkPermitsFabricEvents() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("chunkpermits");
            Path worldDir = server.getWorldPath(LevelResource.ROOT);
            ChunkPermitsServices.init(configDir, worldDir.resolve("chunkpermits.db"));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ChunkPermitsFabricCommands.register(dispatcher)
        );

        PlayerBlockBreakEvents.BEFORE.register((level, playerEntity, pos, state, blockEntity) -> {
            if (level.isClientSide()) {
                return true;
            }

            if (!(playerEntity instanceof ServerPlayer player)) {
                return true;
            }

            ChunkPos chunkPos = new ChunkPos(pos);
            ClaimKey key = new ClaimKey(
                    FabricLevelKeys.levelKey(level),
                    chunkPos.x,
                    chunkPos.z
            );

            AccessResult result = ChunkPermitsServices.ACCESS_SERVICE.canBreak(
                    player.getUUID(),
                    key,
                    ownerUuid -> level.getServer().getPlayerList().getPlayer(ownerUuid) != null,
                    System.currentTimeMillis()
            );

            if (!result.allowed()) {
                player.displayClientMessage(Component.literal(result.reason()), true);
                return false;
            }

            return true;
        });

        UseBlockCallback.EVENT.register((playerEntity, level, hand, blockHit) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            if (!(playerEntity instanceof ServerPlayer player)) {
                return InteractionResult.PASS;
            }

            BlockPos pos = blockHit.getBlockPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            ClaimKey key = new ClaimKey(
                    FabricLevelKeys.levelKey(level),
                    chunkPos.x,
                    chunkPos.z
            );

            AccessResult result = ChunkPermitsServices.ACCESS_SERVICE.canInteract(
                    player.getUUID(),
                    key,
                    ownerUuid -> level.getServer().getPlayerList().getPlayer(ownerUuid) != null,
                    System.currentTimeMillis()
            );

            if (!result.allowed()) {
                player.displayClientMessage(Component.literal(result.reason()), true);
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        registerUseItemCallback();

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity.level().isClientSide()) {
                return;
            }
            if (!(entity instanceof ServerPlayer victim)) {
                return;
            }

            Entity attackerEntity = damageSource.getEntity();
            if (!(attackerEntity instanceof ServerPlayer attacker)) {
                return;
            }
            if (victim.getUUID().equals(attacker.getUUID())) {
                return;
            }

            if (ChunkPermitsServices.TRUST_SERVICE.isTrusted(victim.getUUID(), attacker.getUUID())) {
                return;
            }

            ChunkPermitsServices.RAID_SERVICE.startRaid(
                    attacker.getUUID(),
                    attacker.getName().getString(),
                    victim.getUUID(),
                    victim.getName().getString(),
                    System.currentTimeMillis()
            );

            attacker.sendSystemMessage(Component.literal(
                    "§aYou can now raid " + victim.getName().getString() + "'s claims temporarily"
            ));

            victim.sendSystemMessage(Component.literal(
                    "§c" + attacker.getName().getString() + " can now raid your claims temporarily"
            ));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ChunkBorderDisplayManager.tick(player);
            }

            ChunkBorderDisplayManager.cleanupOffline(
                    playerUuid -> server.getPlayerList().getPlayer(playerUuid) != null
            );
        });
    }

    private static boolean isContainer(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerUseItemCallback() {
        try {
            Class<?> callbackClass = Class.forName("net.fabricmc.fabric.api.event.player.UseItemCallback");
            Object event = callbackClass.getField("EVENT").get(null);
            Object callback = Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[]{callbackClass},
                    (proxy, method, args) -> {
                        if (!"interact".equals(method.getName())) {
                            return null;
                        }

                        InteractionResult result = handleUseItem(args[0], args[1], args[2]);
                        return adaptUseItemResult(result, method.getReturnType(), args[0], (InteractionHand) args[2]);
                    }
            );

            ((Event) event).register(callback);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register Fabric use item callback", exception);
        }
    }

    private static InteractionResult handleUseItem(Object playerEntity, Object levelObject, Object handObject) {
        Level level = (Level) levelObject;
        InteractionHand hand = (InteractionHand) handObject;

        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND || !(playerEntity instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BucketItem)) {
            return InteractionResult.PASS;
        }

        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = blockHit.getBlockPos();
        ChunkPos chunkPos = new ChunkPos(pos);
        ClaimKey key = new ClaimKey(
                FabricLevelKeys.levelKey(level),
                chunkPos.x,
                chunkPos.z
        );

        AccessResult result = ChunkPermitsServices.ACCESS_SERVICE.canPlace(
                player.getUUID(),
                key,
                ownerUuid -> level.getServer().getPlayerList().getPlayer(ownerUuid) != null,
                System.currentTimeMillis()
        );

        if (!result.allowed()) {
            player.displayClientMessage(Component.literal(result.reason()), true);
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    private static Object adaptUseItemResult(InteractionResult result, Class<?> returnType, Object playerEntity, InteractionHand hand) {
        if (returnType == InteractionResult.class) {
            return result;
        }

        try {
            Class<?> holderClass = Class.forName("net.minecraft.world.InteractionResultHolder");
            String methodName = result == InteractionResult.FAIL ? "fail" : "pass";
            ItemStack stack = playerEntity instanceof ServerPlayer player ? player.getItemInHand(hand) : ItemStack.EMPTY;
            return holderClass.getMethod(methodName, ItemStack.class).invoke(null, stack);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to adapt Fabric use item result", exception);
        }
    }
}
//?}
