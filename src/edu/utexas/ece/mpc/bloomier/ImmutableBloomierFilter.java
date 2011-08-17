package edu.utexas.ece.mpc.bloomier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import edu.utexas.ece.mpc.bloomier.internal.BloomierHasher;
import edu.utexas.ece.mpc.bloomier.internal.OrderAndMatch;
import edu.utexas.ece.mpc.bloomier.internal.OrderAndMatchFinder;

public class ImmutableBloomierFilter<K, V extends Serializable> {
	final protected int m;
	final protected int k;
	final protected int q;
	
	protected long hashSeed;
	protected BloomierHasher<K> hasher;
	
	private byte[][] table;
	private int tableEntrySize;

	public ImmutableBloomierFilter(Map<K, V> map, int m, int k, int q, long timeoutMs) throws TimeoutException {
		this(map, m, k, q, new OrderAndMatchFinder<K>(map.keySet(), m, k, q).find(timeoutMs)); // TODO: find a more elegant solution that doesn't cram this all into one statement
	}
	
	// This package private constructor can be used by entities that want to supply their own OrderAndMatch
	ImmutableBloomierFilter(Map<K, V> map, int m, int k, int q, OrderAndMatch<K> oam) {
		this.m = m;
		this.k = k;
		this.q = q;
		
		// Create table with correctly sized byte arrays for encoded entries
		tableEntrySize = q/8;
		table = new byte[m][tableEntrySize];		

		hashSeed = oam.getHashSeed();
		hasher = new BloomierHasher<K>(hashSeed, m, k, q);

		populateTable(map, oam);
		
		// TODO: if hasher caches hashes, clear cache here (to reclaim memory)
	}
	
	public V get(K key) {
		int[] neighborhood = hasher.getNeighborhood(key);
		byte[] mask = hasher.getM(key);
		
		byte[] resultArray = new byte[tableEntrySize];
		
		byteArrayXor(resultArray, mask);
		for (int hash: neighborhood) {
			byteArrayXor(resultArray, table[hash]);
		}
		
		return decode(resultArray);
	}

	protected void populateTable(Map<K, V> map, OrderAndMatch<K> oam) {
		List<K> pi = oam.getPi();
		List<Integer> tau = oam.getTau();
		
		for (int i=0; i < pi.size(); i++) {
			K key = pi.get(i);
			V value = map.get(key);
			byte[] encodedValue = encode(value);
			
			int[] neighborhood = hasher.getNeighborhood(key);
			byte[] mask = hasher.getM(key);
			
			int indexOfStorage = neighborhood[tau.get(i)];
			byte[] valueToStore = new byte[tableEntrySize]; 
				
			byteArrayXor(valueToStore, encodedValue);
			byteArrayXor(valueToStore, mask);
			
			for (int hash: neighborhood) {
				byteArrayXor(valueToStore, table[hash]);
			}
			
			table[indexOfStorage] = valueToStore;
		}
	}

	private void byteArrayXor(byte[] resultArray, byte[] xorArray) {
		int length = Math.min(resultArray.length, xorArray.length);
		
		for (int i=0; i<length; i++) {
			resultArray[i] ^= xorArray[i];
		}
	}

	private byte[] encode(V value) {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try {
			ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
			outStream.writeObject(value);
		} catch (IOException e) {
			// Shouldn't ever happen
			throw new IllegalStateException("Value could not be encoded", e);
		}
		
		byte[] encodedArray = byteStream.toByteArray();
		if (encodedArray.length > tableEntrySize){
			throw new IllegalArgumentException("Encoded values are too big to fit in table (q=" + q + "; must be >= " + encodedArray.length * Byte.SIZE + ")");
		}
		return Arrays.copyOf(byteStream.toByteArray(), tableEntrySize);
	}

	@SuppressWarnings("unchecked")
	private V decode(byte[] value) {
		V result = null;
		ByteArrayInputStream byteStream = new ByteArrayInputStream(value);
		try {
			ObjectInputStream inStream = new ObjectInputStream(byteStream);
			result = (V) inStream.readObject();
			
			// Check leftovers (all must be zero or this is a detected false positive)
			byte[] leftovers = new byte[value.length];
			int leftoverCount = byteStream.read(leftovers);
			for (int i=0; i < leftoverCount; i++) {
				if (leftovers[i] != 0x00) {
					return null;
				}
			}
		} catch (ObjectStreamException e) {
			return null; // Okay; probably means lookup of key not in structure
		} catch (IOException e) {
			// Should not happen
			throw new IllegalStateException("Could not decode value", e);
		} catch (ClassNotFoundException e) {
			// Convert to runtime exception
			throw new RuntimeException("Could not decode due to missing class", e);
		}
		
		return result;
	}
}
