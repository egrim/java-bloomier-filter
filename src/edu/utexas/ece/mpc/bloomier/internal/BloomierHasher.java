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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Queue;

public class BloomierHasher<K> {
    // TODO: memoize/cache hashes
    private long hashSeed;

    private int m;
    private int k;
    private int q;

    public BloomierHasher(long hashSeed, int m, int k, int q) {
        this.hashSeed = hashSeed;
        this.m = m;
        this.k = k;
        this.q = q;
    }

    public int[] getNeighborhood(K key) {
        DataInputStream stream = new DataInputStream(new HashInputStream(key));

        int[] hashes = new int[k];
        for (int i = 0; i < hashes.length; i++) {
            try {
                hashes[i] = Math.abs(stream.readInt()) % m; // Massage value to be in [0,m)
            } catch (IOException e) {
                // Shouldn't be possible
                throw new IllegalStateException("Hash generation failed", e);
            }
        }

        return hashes;
    }

    public byte[] getM(K key) {
        DataInputStream stream = new DataInputStream(new HashInputStream(key));

        byte[] hashes = new byte[q / Byte.SIZE + 1];
        for (int i = 0; i < hashes.length; i++) {
            try {
                hashes[i] = stream.readByte();
            } catch (IOException e) {
                // Shouldn't be possible
                throw new IllegalStateException("Hash generation failed", e);
            }
        }

        return hashes;
    }

    private class HashInputStream extends InputStream {
        private byte[] data;
        private long salt;
        private MessageDigest md;

        private Queue<Byte> buffer;

        public HashInputStream(K key) {
            this.data = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).putInt(key.hashCode())
                                  .array(); // TODO: use .toString instead of hashCode to get a better hash range
            this.salt = hashSeed;

            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Missing required hashing algorithm", e);
            }

            buffer = new ArrayDeque<Byte>(md.getDigestLength());
            topOff();
        }

        @Override
        public int read() {
            if (buffer.isEmpty()) {
                topOff();
            }
            return buffer.remove() - Byte.MIN_VALUE;
        }

        private void topOff() {
            md.update(ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(salt).array());
            md.update(data);

            for (byte b: md.digest()) {
                buffer.add(b);
            }

            salt++;
        }
    }
}