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

package edu.utexas.ece.mpc.bloomier;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;
import com.esotericsoftware.kryo.SerializationException;

import edu.utexas.ece.mpc.bloomier.internal.BloomierHasher;
import edu.utexas.ece.mpc.bloomier.internal.OrderAndMatch;
import edu.utexas.ece.mpc.bloomier.internal.OrderAndMatchFinder;

public class ImmutableBloomierFilter<K, V> {
    protected final Kryo kryo;
    protected final ObjectBuffer kryoSerializer;

    protected final Class<V> valueClass;

    protected final int m;
    protected final int k;
    protected final int q;

    protected long hashSeed;
    protected BloomierHasher<K> hasher;

    protected byte[][] table;
    protected int tableEntrySize;

    private ImmutableBloomierFilter(int m, int k, int q, Class<V> valueClass) {
        kryo = new Kryo();
        kryo.setRegistrationOptional(true);
        kryoSerializer = new ObjectBuffer(kryo, DEFAULT_OBJECT_BUFFER_INITIAL_SIZE,
                                          Integer.MAX_VALUE);

        this.m = m;
        this.k = k;
        this.q = q;

        this.valueClass = valueClass;

        // Create table with correctly sized byte arrays for encoded entries
        tableEntrySize = q / 8;
        table = new byte[m][tableEntrySize];

        // The rest of the initialization will be handled by create() in public constructors
    }

    public ImmutableBloomierFilter(Map<K, V> map, int m, int k, int q, Class<V> valueClass,
                                   int timeoutMs) throws TimeoutException {
        this(m, k, q, valueClass);

        OrderAndMatchFinder<K> oamf = new OrderAndMatchFinder<K>(map.keySet(), m, k, q);
        OrderAndMatch<K> oam = oamf.find(timeoutMs);
        create(map, oam);
    }

    public ImmutableBloomierFilter(Map<K, V> map, int m, int k, int q, Class<V> valueClass,
                                   long hashSeedHint) {
        this(m, k, q, valueClass);

        OrderAndMatchFinder<K> oamf = new OrderAndMatchFinder<K>(map.keySet(), m, k, q,
                                                                 hashSeedHint);
        OrderAndMatch<K> oam;
        try {
            oam = oamf.find(Integer.MAX_VALUE);
        } catch (TimeoutException e) {
            throw new AssertionError("Should never be possible");
        }
        create(map, oam);
    }

    public ImmutableBloomierFilter(Map<K, V> map, int m, int k, int q, Class<V> valueClass,
                                   int timeoutMs, long hashSeedHint) throws TimeoutException {
        this(m, k, q, valueClass);

        OrderAndMatchFinder<K> oamf = new OrderAndMatchFinder<K>(map.keySet(), m, k, q,
                                                                 hashSeedHint);
        OrderAndMatch<K> oam = oamf.find(timeoutMs);
        create(map, oam);
    }

    // This package private constructor can be used by entities that want to supply their own OrderAndMatch
    ImmutableBloomierFilter(Map<K, V> map, int m, int k, int q, Class<V> valueClass,
                            OrderAndMatch<K> oam) {
        this(m, k, q, valueClass);

        create(map, oam);
    }

    public ImmutableBloomierFilter(int m, int k, int q, Class<V> valueClass, long hashSeed,
                                      byte[][] table) {
        this(m, k, q, valueClass);

        this.hashSeed = hashSeed;
        this.table = table;

        hasher = new BloomierHasher<K>(hashSeed, m, k, q);
    }

    public ImmutableBloomierFilter(ImmutableBloomierFilter<K, V> orig) {
        this(orig.m, orig.k, orig.q, orig.valueClass, orig.hashSeed, orig.table); // TODO: it should be okay to share
                                                                                  // the underlying table since it's
                                                                                  // immutable, but beware this might
                                                                                  // not be true
    }

    private void create(Map<K, V> map, OrderAndMatch<K> oam) {
        hashSeed = oam.getHashSeed();
        hasher = new BloomierHasher<K>(hashSeed, m, k, q);

        List<K> pi = oam.getPi();
        List<Integer> tau = oam.getTau();

        for (int i = 0; i < pi.size(); i++) {
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

    public int getM() {
        return m;
    }

    public int getK() {
        return k;
    }

    public int getQ() {
        return q;
    }

    public long getHashSeed() {
        return hashSeed;
    }

    public byte[][] getTable() {
        return table;
    }

    private void byteArrayXor(byte[] resultArray, byte[] xorArray) {
        // TODO: may want to rewrite this to more intuitively handle hetero-sized arrays
        int length = Math.min(resultArray.length, xorArray.length);

        for (int i = 0; i < length; i++) {
            resultArray[i] ^= xorArray[i];
        }
    }

    private byte[] encode(V value) {
        byte[] serializedValue = kryoSerializer.writeObjectData(value);
        if (serializedValue.length > tableEntrySize) {
            throw new IllegalArgumentException("Encoded values are too big to fit in table (q=" + q
                                               + "; must be >= " + serializedValue.length
                                               * Byte.SIZE + ")");
        }

        // Pad with zeros up to required tableEntrySize
        return Arrays.copyOf(serializedValue, tableEntrySize);
    }

    private V decode(byte[] value) {
        ByteBuffer buffer = ByteBuffer.wrap(value);
        V result;
        try {
            result = (V) kryo.readObjectData(buffer, valueClass);
        } catch (SerializationException e) {
            return null; // Serialization exception likely caused by trying to decode non existent entry
        }

        // Check leftovers (all must be zero of this is a detected false positive)
        while (buffer.hasRemaining()) {
            if (buffer.get() != 0) {
                return null;
            }
        }

        return result;
    }

    private static final int DEFAULT_OBJECT_BUFFER_INITIAL_SIZE = 2 * 1024;
}
