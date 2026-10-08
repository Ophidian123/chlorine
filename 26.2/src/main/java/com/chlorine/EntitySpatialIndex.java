package com.chlorine;

import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Indexes entities by position so merge passes only compare candidates in
 * nearby grid cells. Candidate order matches the original entity list.
 */
final class EntitySpatialIndex<T extends Entity> {
    private final List<T> entities;
    private final double cellSize;
    private final Map<Cell, Set<Integer>> buckets = new HashMap<>();
    private final IdentityHashMap<T, Integer> indexes = new IdentityHashMap<>();

    EntitySpatialIndex(List<T> entities, double searchRadius) {
        this.entities = entities;
        this.cellSize = Math.max(1.0, searchRadius);

        for (int i = 0; i < entities.size(); i++) {
            T entity = entities.get(i);
            indexes.put(entity, i);
            buckets.computeIfAbsent(cellFor(entity), ignored -> new HashSet<>()).add(i);
        }
    }

    List<T> nearbyAfter(int index) {
        T entity = entities.get(index);
        Cell cell = cellFor(entity);
        List<Integer> candidates = new ArrayList<>();

        for (long x = cell.x - 1; x <= cell.x + 1; x++) {
            for (long y = cell.y - 1; y <= cell.y + 1; y++) {
                for (long z = cell.z - 1; z <= cell.z + 1; z++) {
                    Set<Integer> bucket = buckets.get(new Cell(x, y, z));
                    if (bucket == null) {
                        continue;
                    }
                    for (int candidate : bucket) {
                        if (candidate > index) {
                            candidates.add(candidate);
                        }
                    }
                }
            }
        }

        Collections.sort(candidates);
        List<T> nearby = new ArrayList<>(candidates.size());
        for (int candidate : candidates) {
            nearby.add(entities.get(candidate));
        }
        return nearby;
    }

    void remove(T entity) {
        Integer index = indexes.remove(entity);
        if (index == null) {
            return;
        }

        Cell cell = cellFor(entity);
        Set<Integer> bucket = buckets.get(cell);
        if (bucket != null) {
            bucket.remove(index);
            if (bucket.isEmpty()) {
                buckets.remove(cell);
            }
        }
    }

    private Cell cellFor(Entity entity) {
        return new Cell(
            (long) Math.floor(entity.getX() / cellSize),
            (long) Math.floor(entity.getY() / cellSize),
            (long) Math.floor(entity.getZ() / cellSize)
        );
    }

    private record Cell(long x, long y, long z) {
    }
}
