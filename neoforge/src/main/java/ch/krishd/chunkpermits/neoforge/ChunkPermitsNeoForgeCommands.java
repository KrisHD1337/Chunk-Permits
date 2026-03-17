package ch.krishd.chunkpermits.neoforge;

import ch.krishd.chunkpermits.ChunkPermitsServices;
import ch.krishd.chunkpermits.claim.ClaimAttemptContext;
import ch.krishd.chunkpermits.claim.ClaimKey;
import ch.krishd.chunkpermits.protection.AccessResult;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;

public final class ChunkPermitsNeoForgeCommands {
    private ChunkPermitsNeoForgeCommands() {
    }

    private static Item resolveConfiguredCostItem() {
        String itemId = ChunkPermitsServices.CONFIG.claimRules().claimCost().itemId();

        var itemOptional = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                net.minecraft.resources.ResourceLocation.tryParse(itemId)
        );

        return itemOptional.orElseThrow(() ->
                new IllegalStateException("Configured claim cost item does not exist: " + itemId)
        );
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("permit")
                        .requires(source -> source.hasPermission(0))

                        .then(Commands.literal("claim")
                                .then(Commands.literal("add")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ChunkPos chunkPos = player.chunkPosition();

                                            ClaimKey key = new ClaimKey(
                                                    player.level().dimension().location().toString(),
                                                    chunkPos.x,
                                                    chunkPos.z
                                            );

                                            Item costItem = resolveConfiguredCostItem();
                                            int availableItems = ClaimCostHelperNeoForge.countItem(player, costItem);

                                            AccessResult result = ChunkPermitsServices.CLAIM_SERVICE.claim(
                                                    player.getUUID(),
                                                    player.getGameProfile().getName(),
                                                    key,
                                                    new ClaimAttemptContext(availableItems)
                                            );

                                            if (!result.allowed()) {
                                                player.sendSystemMessage(Component.literal(result.reason()));
                                                return 0;
                                            }

                                            int costAmount = ChunkPermitsServices.CONFIG.claimRules().claimCost().amount();
                                            ClaimCostHelperNeoForge.removeItems(player, costItem, costAmount);

                                            player.sendSystemMessage(Component.literal(
                                                    "Claimed chunk " + chunkPos.x + ", " + chunkPos.z
                                            ));
                                            return 1;
                                        }))
                                .then(Commands.literal("remove")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ChunkPos chunkPos = player.chunkPosition();

                                            ClaimKey key = new ClaimKey(
                                                    player.level().dimension().location().toString(),
                                                    chunkPos.x,
                                                    chunkPos.z
                                            );

                                            AccessResult result = ChunkPermitsServices.CLAIM_SERVICE.unclaim(
                                                    player.getUUID(),
                                                    key
                                            );

                                            if (!result.allowed()) {
                                                player.sendSystemMessage(Component.literal(result.reason()));
                                                return 0;
                                            }

                                            player.sendSystemMessage(Component.literal(
                                                    "Unclaimed chunk " + chunkPos.x + ", " + chunkPos.z
                                            ));
                                            return 1;
                                        }))
                        )

                        .then(Commands.literal("info")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ChunkPos chunkPos = player.chunkPosition();

                                    ClaimKey key = new ClaimKey(
                                            player.level().dimension().location().toString(),
                                            chunkPos.x,
                                            chunkPos.z
                                    );

                                    var claimOptional = ChunkPermitsServices.CLAIM_SERVICE.getClaim(key);
                                    var rules = ChunkPermitsServices.CLAIM_SERVICE.getClaimRules();
                                    int currentClaims = ChunkPermitsServices.CLAIM_SERVICE.countClaims(player.getUUID());

                                    player.sendSystemMessage(Component.literal("Your claims: " + currentClaims + "/" + rules.maxClaimsPerPlayer()));
                                    player.sendSystemMessage(Component.literal(
                                            "Claim cost: " + rules.claimCost().amount() + "x " + rules.claimCost().itemId()
                                    ));
                                    player.sendSystemMessage(Component.literal("Chunk info:"));
                                    player.sendSystemMessage(Component.literal("World: " + key.levelKey()));
                                    player.sendSystemMessage(Component.literal("Chunk: " + key.chunkX() + ", " + key.chunkZ()));

                                    if (claimOptional.isPresent()) {
                                        var claim = claimOptional.get();
                                        player.sendSystemMessage(Component.literal("Claimed: yes"));
                                        player.sendSystemMessage(Component.literal("Owner: " + claim.ownerName()));
                                    } else {
                                        player.sendSystemMessage(Component.literal("Claimed: no"));
                                    }

                                    return 1;
                                }))

                        .then(Commands.literal("help")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    player.sendSystemMessage(Component.literal("/permit claim add"));
                                    player.sendSystemMessage(Component.literal("/permit claim remove"));
                                    player.sendSystemMessage(Component.literal("/permit info"));
                                    return 1;
                                }))
        );
    }
}