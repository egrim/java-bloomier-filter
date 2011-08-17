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
			for (int hash: hasher.getNeighborhood(key)) {
				if (hashesSeen.contains(hash)) {
					nonSingletons.add(hash);
				}
				
				hashesSeen.add(hash);
			}
		}
	}
	
	public Integer tweak(K key) {
		int[] neighborhood = hasher.getNeighborhood(key);
		for (int i=0; i < neighborhood.length; i++) {
			if (nonSingletons.contains(neighborhood[i]) == false) {
				return i;
			}
		}
		return null;
	}
}