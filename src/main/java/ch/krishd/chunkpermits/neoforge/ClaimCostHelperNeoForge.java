//? if neoforge {
package ch.krishd.chunkpermits.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ClaimCostHelperNeoForge {
    private ClaimCostHelperNeoForge() {
    }

    public static int countItem(ServerPlayer player, Item item) {
        int total = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static void removeItems(ServerPlayer player, Item item, int amount) {
        int remaining = amount;

        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(item)) {
                continue;
            }

            int taken = Math.min(stack.getCount(), remaining);
            stack.shrink(taken);
            remaining -= taken;

            if (remaining <= 0) {
                break;
            }
        }

        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }
}
//?}
