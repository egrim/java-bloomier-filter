package edu.utexas.ece.mpc.bloomier;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import edu.utexas.ece.mpc.bloomier.internal.BloomierHasher;
import edu.utexas.ece.mpc.bloomier.internal.OrderAndMatch;
import edu.utexas.ece.mpc.bloomier.internal.OrderAndMatchFinder;

public class MutableBloomierFilter<K, V extends Serializable> {
	private ImmutableBloomierFilter<K, Integer> tauTable;
	private V[] valueTable;
	
	private long hashSeed;
	private BloomierHasher<K> hasher;
	
	private List<K> pi;
	private List<Integer> tau;

	@SuppressWarnings("unchecked")
	public MutableBloomierFilter(Map<K, V> map, int m, int k, int q,
			long timeoutMs) throws TimeoutException {
		OrderAndMatchFinder<K> oamf = new OrderAndMatchFinder<K>(map.keySet(), m, k, q);
		OrderAndMatch<K> oam = oamf.find(timeoutMs);
		
		valueTable = (V[]) new Serializable[m];
		
		hashSeed = oam.getHashSeed();
		hasher = new BloomierHasher<K>(hashSeed, m, k, q);
		
		pi = oam.getPi();
		tau = oam.getTau();
		
		Map<K, Integer> tauMap = new HashMap<K, Integer>();
		for (int i=0; i < pi.size(); i++) {
			K key = pi.get(i);
			V value = map.get(key);
			
			int iota = tau.get(i);
			tauMap.put(key, iota);
			
			int hashIndex = hasher.getNeighborhood(key)[iota];
			valueTable[hashIndex] = value;
		}
		
		tauTable = new ImmutableBloomierFilter<K, Integer>(tauMap, m, k, q, oam);
	}
	
	public V get(K key) {
		Integer iota = tauTable.get(key);
		if (iota == null) {
			return null;
		}
		
		Integer hashIndex = hasher.getNeighborhood(key)[iota];
		return valueTable[hashIndex];
	}
	
	public void set(K key, V value) {
		Integer iota = tauTable.get(key);
		
		if (iota == null) {
			throw new IllegalArgumentException("Supplied key (" + key + ") is involid");
		}
		
		Integer hashIndex = hasher.getNeighborhood(key)[iota];
		valueTable[hashIndex] = value;
	}
}