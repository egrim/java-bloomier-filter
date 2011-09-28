/*
 * Copyright (c) 2011, The University of Texas at Austin
 * Produced in the Mobile and Pervasive Computing Lab
 * Originally written by Evan Grim
 * 
 * All rights reserved.
 * 
 * See included LICENSE.txt for licensing details
 * 
 */

package edu.utexas.ece.mpc.bloomier.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SingletonFindingTweaker<K> {
    private BloomierHasher<K> hasher;
    private Set<Integer> nonSingletons;

    public SingletonFindingTweaker(Collection<K> keys, BloomierHasher<K> hasher) {
        this.hasher = hasher;

        Set<Integer> hashesSeen = new HashSet<Integer>();
        nonSingletons = new HashSet<Integer>();

        for (K key: keys) {
            int[] neighborhood = hasher.getNeighborhood(key);

            // First pass - see if any currently qualify as singletons
            for (int hash: neighborhood) {
                if (hashesSeen.contains(hash)) {
                    nonSingletons.add(hash);
                }
            }

            // Second pass - add to seen hashes
            for (int hash: neighborhood) {
                hashesSeen.add(hash);
            }
        }
    }

    public Integer tweak(K key) {
        int[] neighborhood = hasher.getNeighborhood(key);
        for (int i = 0; i < neighborhood.length; i++) {
            if (nonSingletons.contains(neighborhood[i]) == false) {
                return i;
            }
        }
        return null;
    }
}