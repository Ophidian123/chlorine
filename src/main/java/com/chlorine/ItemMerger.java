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
 * === BUG FIX (post-0.1.7) ===
 * An earlier version of this class mutated the ItemStack returned by
 * `entity.getItem()` in place (`.grow()`/`.shrink()`) and relied on that
 * mutation persisting back to the entity automatically. It doesn't
 * necessarily: `getItem()` isn't guaranteed to hand back the entity's
 * live, synced field rather than a copy, and there's no way to tell
 * which from outside without a working build to test against. If it was
 * a copy, the growing entity's stack silently never actually grew, while
 * the shrinking entity still got discarded once its *local* copy read
 * empty — net effect: items deleted, not merged. This is now fixed by
 * always explicitly committing both sides via `entity.setItem(stack)`,
 * which is the standard, unambiguous way to change what an item entity
 * is holding, regardless of what getItem() happens to return.
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
        EntitySpatialIndex<ItemEntity> spatialIndex = new EntitySpatialIndex<>(items, Math.sqrt(mergeDistSq));
        for (int i = 0; i < items.size(); i++) {
            ItemEntity a = items.get(i);
            if (!a.isAlive()) {
                spatialIndex.remove(a);
                continue;
            }
            ItemStack stackA = a.getItem();
            if (stackA.isEmpty() || stackA.getCount() >= stackA.getMaxStackSize()) {
                continue;
            }

            for (ItemEntity b : spatialIndex.nearbyAfter(i)) {
                if (!b.isAlive()) {
                    spatialIndex.remove(b);
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
                if (moved <= 0) {
                    continue;
                }

                // Work on explicit copies and commit both sides via
                // setItem() — never assume in-place mutation of whatever
                // getItem() returns will persist. See BUG FIX note above.
                ItemStack newStackA = stackA.copy();
                newStackA.grow(moved);
                ItemStack newStackB = stackB.copy();
                newStackB.shrink(moved);

                a.setItem(newStackA);
                b.setItem(newStackB);
                stackA = newStackA; // keep local reference in sync for the rest of this loop

                if (newStackB.isEmpty()) {
                    b.discard();
                    spatialIndex.remove(b);
                } else if (newStackB.getCount() == stackB.getCount()) {
                    // Sanity check: shrink() should have changed the count.
                    // If it somehow didn't, don't discard anything and
                    // log it — better to leave a duplicate-looking item
                    // on the ground than silently lose it.
                    Chlorine.LOGGER.warn("Chlorine: item merge shrink had no effect, skipping discard to avoid item loss");
                }
            }
        }
    }
}
