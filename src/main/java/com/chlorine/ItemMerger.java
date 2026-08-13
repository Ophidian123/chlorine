package com.chlorine;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

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
        double scanRadius = Chlorine.CONFIG.itemMergeScanRadius;
        double mergeDistSq = Chlorine.CONFIG.itemMergeRadius * Chlorine.CONFIG.itemMergeRadius;
        int merged = 0;

        for (ServerLevel level : server.getAllLevels()) {
            Set<ItemEntity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            List<ItemEntity> items = new ArrayList<>();
            for (ServerPlayer player : level.players()) {
                AABB box = player.getBoundingBox().inflate(scanRadius);
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                    if (seen.add(item)) {
                        items.add(item);
                    }
                }
            }
            merged += mergeCluster(items, mergeDistSq);
        }

        ticksUntilNextPass = merged >= Math.max(1, Chlorine.CONFIG.itemMergeBurstThreshold)
            ? Math.max(20, Chlorine.CONFIG.itemMergeBurstIntervalTicks)
            : Math.max(20, Chlorine.CONFIG.itemMergeIntervalTicks);
    }

    private int mergeCluster(List<ItemEntity> items, double mergeDistSq) {
        int merged = 0;
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
                    merged++;
                }
            }
        }
        return merged;
    }
}
