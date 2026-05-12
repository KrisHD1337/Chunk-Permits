//? if neoforge {
package ch.krishd.chunkpermits.neoforge;

import ch.krishd.chunkpermits.ChunkPermitsServices;
import ch.krishd.chunkpermits.claim.ClaimKey;
import ch.krishd.chunkpermits.neoforge.particles.ChunkBorderDisplayManager;
import ch.krishd.chunkpermits.protection.AccessResult;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.nio.file.Path;

public final class ChunkPermitsNeoForgeEvents {
    private ChunkPermitsNeoForgeEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        Path configDir = FMLPaths.GAMEDIR.get().resolve("config").resolve("chunkpermits");
        Path worldDir = event.getServer().getWorldPath(LevelResource.ROOT);
        ChunkPermitsServices.init(configDir, worldDir.resolve("chunkpermits.db"));
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        Level level = (Level) event.getLevel();
        ChunkPos chunkPos = new ChunkPos(event.getPos());

        ClaimKey key = new ClaimKey(
                level.dimension().location().toString(),
                chunkPos.x,
                chunkPos.z
        );

        AccessResult result = ChunkPermitsServices.ACCESS_SERVICE.canBreak(
                player.getUUID(),
                key,
                ownerUuid -> player.server.getPlayerList().getPlayer(ownerUuid) != null,
                System.currentTimeMillis()
        );

        if (!result.allowed()) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.literal(result.reason()), true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ChunkPos chunkPos = new ChunkPos(pos);

        ClaimKey key = new ClaimKey(
                level.dimension().location().toString(),
                chunkPos.x,
                chunkPos.z
        );

        AccessResult result;

        if (isContainer(level, pos)) {
            result = ChunkPermitsServices.ACCESS_SERVICE.canInteract(
                    player.getUUID(),
                    key,
                    ownerUuid -> player.server.getPlayerList().getPlayer(ownerUuid) != null,
                    System.currentTimeMillis()
            );
        } else {
            result = ChunkPermitsServices.ACCESS_SERVICE.canInteract(
                    player.getUUID(),
                    key,
                    ownerUuid -> player.server.getPlayerList().getPlayer(ownerUuid) != null,
                    System.currentTimeMillis()
            );
        }

        if (!result.allowed()) {
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(result.reason()), true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof BucketItem)) {
            return;
        }

        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        ChunkPos chunkPos = new ChunkPos(pos);
        Level level = event.getLevel();

        ClaimKey key = new ClaimKey(
                level.dimension().location().toString(),
                chunkPos.x,
                chunkPos.z
        );

        AccessResult result = ChunkPermitsServices.ACCESS_SERVICE.canPlace(
                player.getUUID(),
                key,
                ownerUuid -> player.server.getPlayerList().getPlayer(ownerUuid) != null,
                System.currentTimeMillis()
        );

        if (!result.allowed()) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(result.reason()), true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();

        event.getAffectedBlocks().removeIf(pos -> {
            ChunkPos chunkPos = new ChunkPos(pos);

            ClaimKey key = new ClaimKey(
                    level.dimension().location().toString(),
                    chunkPos.x,
                    chunkPos.z
            );

            return ChunkPermitsServices.CLAIM_REPOSITORY.isClaimed(key);
        });
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
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
                attacker.getGameProfile().getName(),
                victim.getUUID(),
                victim.getGameProfile().getName(),
                System.currentTimeMillis()
        );

        attacker.sendSystemMessage(Component.literal(
                "§aYou can now raid " + victim.getGameProfile().getName() + "'s claims temporarily"
        ));

        victim.sendSystemMessage(Component.literal(
                "§c" + attacker.getGameProfile().getName() + "can now raid your claims temporarily"
        ));
    }

    private static boolean isContainer(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity;
    }

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (event.getServer() == null) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ChunkBorderDisplayManager.tick(player);
        }

        ChunkBorderDisplayManager.cleanupOffline(
                playerUuid -> event.getServer().getPlayerList().getPlayer(playerUuid) != null
        );
    }
}
//?}
