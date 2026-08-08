package com.chlorine;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Periodically merges nearby stackable item entities near each player.
 *
 * Vanilla already merges items that are touching, but dense drops (mob
 * farms, crop farms, big fights) routinely leave dozens of separate item
 * entities sitting a block or two apart, each still ticking and rendering
 * on its own. This scans a modest radius around each online player every
 * few seconds and merges what it finds — pure server-side bookkeeping,
 * no mixins, no rendering/AI internals. Scoped to near players (not the
 * whole world) to keep the cost bounded and predictable.
 *
 * === RISK NOTE ===
 * `ItemStack.isSameItemSameComponents(...)` is the post-component-rework
 * equality check (Minecraft replaced NBT-based item comparison with a
 * components system a while back). If this method name has moved again
 * by 26.2, open net.minecraft.world.item.ItemStack in your IDE and swap
 * in whatever the current "are these stacks the same item/data" check is
 * called.
 */
public final class ItemMerger {
    private int ticksUntilNextPass = 0;

    private ItemMerger() {
    }

    public static void register() {
        ItemMerger merger = new ItemMerger();
        ServerTickEvents.END_SERVER_TICK.register(merger::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        if (!Chlorine.CONFIG.enableItemMerging) {
            return;
        }
        if (--ticksUntilNextPass > 0) {
            return;
        }
        ticksUntilNextPass = Math.max(20, Chlorine.CONFIG.itemMergeIntervalTicks);

        double scanRadius = Chlorine.CONFIG.itemMergeScanRadius;
        double mergeDistSq = Chlorine.CONFIG.itemMergeRadius * Chlorine.CONFIG.itemMergeRadius;

        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                AABB box = player.getBoundingBox().inflate(scanRadius);
                List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box);
                mergeCluster(items, mergeDistSq);
            }
        }
    }

    private void mergeCluster(List<ItemEntity> items, double mergeDistSq) {
        for (int i = 0; i < items.size(); i++) {
            ItemEntity a = items.get(i);
            if (!a.isAlive()) {
                continue;
            }
            ItemStack stackA = a.getItem();
            if (stackA.isEmpty() || stackA.getCount() >= stackA.getMaxStackSize()) {
                continue;
            }

            for (int j = i + 1; j < items.size(); j++) {
                ItemEntity b = items.get(j);
                if (!b.isAlive()) {
                    continue;
                }
                ItemStack stackB = b.getItem();
                if (stackB.isEmpty()) {
                    continue;
                }
                if (a.distanceToSqr(b) > mergeDistSq) {
                    continue;
                }
                if (!ItemStack.isSameItemSameComponents(stackA, stackB)) {
                    continue;
                }

                int room = stackA.getMaxStackSize() - stackA.getCount();
                if (room <= 0) {
                    break;
                }
                int moved = Math.min(room, stackB.getCount());
                stackA.grow(moved);
                stackB.shrink(moved);
                if (stackB.isEmpty()) {
                    b.discard();
                }
            }
        }
    }
}
