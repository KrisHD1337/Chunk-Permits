//? if fabric {
package ch.krishd.chunkpermits.fabric;

import ch.krishd.chunkpermits.ChunkPermitsServices;
import ch.krishd.chunkpermits.claim.ClaimAttemptContext;
import ch.krishd.chunkpermits.claim.ClaimKey;
import ch.krishd.chunkpermits.protection.AccessResult;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;

public final class ChunkPermitsFabricCommands {
    private ChunkPermitsFabricCommands() {}

    private static Item resolveConfiguredCostItem() {
        String itemId = ChunkPermitsServices.CONFIG.claimRules().claimCost().itemId();
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(itemId))
                .orElseThrow(() -> new IllegalStateException("Configured claim cost item does not exist: " + itemId));
    }

    public static void register(CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("permit")
                .then(Commands.literal("claim").then(Commands.literal("add").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ChunkPos chunkPos = player.chunkPosition();
                    ClaimKey key = new ClaimKey(player.level().dimension().location().toString(), chunkPos.x, chunkPos.z);
                    Item costItem = resolveConfiguredCostItem();
                    int available = ClaimCostHelperFabric.countItem(player, costItem);
                    AccessResult result = ChunkPermitsServices.CLAIM_SERVICE.claim(
                            player.getUUID(), player.getGameProfile().getName(), key, new ClaimAttemptContext(available));
                    if (!result.allowed()) {
                        player.sendSystemMessage(Component.literal(result.reason()));
                        return 0;
                    }
                    ClaimCostHelperFabric.removeItems(player, costItem, ChunkPermitsServices.CONFIG.claimRules().claimCost().amount());
                    player.sendSystemMessage(Component.literal("Claimed chunk " + chunkPos.x + ", " + chunkPos.z));
                    return 1;
                })).then(Commands.literal("remove").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ChunkPos chunkPos = player.chunkPosition();
                    ClaimKey key = new ClaimKey(player.level().dimension().location().toString(), chunkPos.x, chunkPos.z);
                    AccessResult result = ChunkPermitsServices.CLAIM_SERVICE.unclaim(player.getUUID(), key);
                    if (!result.allowed()) {
                        player.sendSystemMessage(Component.literal(result.reason()));
                        return 0;
                    }
                    player.sendSystemMessage(Component.literal("Unclaimed chunk " + chunkPos.x + ", " + chunkPos.z));
                    return 1;
                })))
                .then(Commands.literal("trust")
                        .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer owner = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            AccessResult result = ChunkPermitsServices.TRUST_SERVICE.addTrust(owner.getUUID(), owner.getGameProfile().getName(), target.getUUID(), target.getGameProfile().getName());
                            owner.sendSystemMessage(Component.literal(result.reason()));
                            return result.allowed() ? 1 : 0;
                        })))
                        .then(Commands.literal("remove").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer owner = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            AccessResult result = ChunkPermitsServices.TRUST_SERVICE.removeTrust(owner.getUUID(), target.getUUID());
                            owner.sendSystemMessage(Component.literal(result.reason()));
                            return result.allowed() ? 1 : 0;
                        }))))
                .then(Commands.literal("info").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int claims = ChunkPermitsServices.CLAIM_SERVICE.countClaims(player.getUUID());
                    player.sendSystemMessage(Component.literal("Your claims: " + claims));
                    return 1;
                }))
                .then(Commands.literal("help").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.sendSystemMessage(Component.literal("/permit claim add|remove"));
                    player.sendSystemMessage(Component.literal("/permit trust add|remove <player>"));
                    return 1;
                }))
        );
    }
}
//?}
