package ch.krishd.chunkpermits.neoforge;

import ch.krishd.chunkpermits.ChunkPermitsServices;
import ch.krishd.chunkpermits.claim.ClaimAttemptContext;
import ch.krishd.chunkpermits.claim.ClaimKey;
import ch.krishd.chunkpermits.neoforge.particles.ChunkBorderDisplayManager;
import ch.krishd.chunkpermits.protection.AccessResult;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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
                        .then(Commands.literal("raid")
                                .then(Commands.literal("info")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            long now = System.currentTimeMillis();

                                            var raidsByAttacker = ChunkPermitsServices.RAID_SERVICE.getActiveRaidsByAttacker(
                                                    player.getUUID(),
                                                    now
                                            );

                                            var raidsByVictim = ChunkPermitsServices.RAID_SERVICE.getActiveRaidsByVictim(
                                                    player.getUUID(),
                                                    now
                                            );

                                            player.sendSystemMessage(Component.literal("§6Raid info"));

                                            player.sendSystemMessage(Component.literal("§eYou can raid:"));
                                            if (raidsByAttacker.isEmpty()) {
                                                player.sendSystemMessage(Component.literal("§7- nobody"));
                                            } else {
                                                for (var raid : raidsByAttacker) {
                                                    long remaining = raid.expiresAtEpochMillis() - now;
                                                    String remainingText = ChunkPermitsNeoForgeRaidInfoHelper.formatRemainingTime(remaining);

                                                    player.sendSystemMessage(Component.literal(
                                                            "§a- " + raid.victimName() + " §7(" + remainingText + ")"
                                                    ));
                                                }
                                            }

                                            player.sendSystemMessage(Component.literal("§eCan raid you:"));
                                            if (raidsByVictim.isEmpty()) {
                                                player.sendSystemMessage(Component.literal("§7- nobody"));
                                            } else {
                                                for (var raid : raidsByVictim) {
                                                    long remaining = raid.expiresAtEpochMillis() - now;
                                                    String remainingText = ChunkPermitsNeoForgeRaidInfoHelper.formatRemainingTime(remaining);

                                                    player.sendSystemMessage(Component.literal(
                                                            "§c- " + raid.attackerName() + " §7(" + remainingText + ")"
                                                    ));
                                                }
                                            }

                                            return 1;
                                        }))
                        )
                        .then(Commands.literal("trust")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    player.sendSystemMessage(Component.literal("§e/permit trust add <player>"));
                                    player.sendSystemMessage(Component.literal("§e/permit trust remove <player>"));
                                    player.sendSystemMessage(Component.literal("§e/permit trust list"));
                                    return 1;
                                })
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> {
                                                    ServerPlayer owner = context.getSource().getPlayerOrException();
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                                    AccessResult result = ChunkPermitsServices.TRUST_SERVICE.addTrust(
                                                            owner.getUUID(),
                                                            owner.getGameProfile().getName(),
                                                            target.getUUID(),
                                                            target.getGameProfile().getName()
                                                    );

                                                    if (!result.allowed()) {
                                                        owner.sendSystemMessage(Component.literal(result.reason()));
                                                        return 0;
                                                    }

                                                    owner.sendSystemMessage(Component.literal(
                                                            "§aTrusted " + target.getGameProfile().getName() + " on your claims."
                                                    ));

                                                    target.sendSystemMessage(Component.literal(
                                                            "§aYou were trusted on " + owner.getGameProfile().getName() + "'s claims."
                                                    ));

                                                    return 1;
                                                })))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> {
                                                    ServerPlayer owner = context.getSource().getPlayerOrException();
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                                    AccessResult result = ChunkPermitsServices.TRUST_SERVICE.removeTrust(
                                                            owner.getUUID(),
                                                            target.getUUID()
                                                    );

                                                    if (!result.allowed()) {
                                                        owner.sendSystemMessage(Component.literal(result.reason()));
                                                        return 0;
                                                    }

                                                    owner.sendSystemMessage(Component.literal(
                                                            "§eRemoved trust for " + target.getGameProfile().getName() + "."
                                                    ));

                                                    target.sendSystemMessage(Component.literal(
                                                            "§eYou were removed from " + owner.getGameProfile().getName() + "'s claims."
                                                    ));

                                                    return 1;
                                                })))
                                .then(Commands.literal("list")
                                        .executes(context -> {
                                            ServerPlayer owner = context.getSource().getPlayerOrException();

                                            var trustedPlayers = ChunkPermitsServices.TRUST_SERVICE.getTrustedPlayers(owner.getUUID());

                                            owner.sendSystemMessage(Component.literal("§6Trusted players:"));

                                            if (trustedPlayers.isEmpty()) {
                                                owner.sendSystemMessage(Component.literal("§7- nobody"));
                                                return 1;
                                            }

                                            for (var trust : trustedPlayers) {
                                                owner.sendSystemMessage(Component.literal(
                                                        "§a- " + trust.trustedPlayerName()
                                                ));
                                            }

                                            return 1;
                                        }))
                        )
                        .then(Commands.literal("border")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    ChunkBorderDisplayManager.startDisplay(player);

                                    player.sendSystemMessage(Component.literal(
                                            "§aShowing visible claim borders for " +
                                                    ChunkPermitsServices.CONFIG.borderRules().durationSeconds() +
                                                    " seconds."
                                    ));
                                    return 1;
                                }))

        );
    }
}
