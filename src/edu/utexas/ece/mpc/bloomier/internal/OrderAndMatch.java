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

import java.util.List;

public class OrderAndMatch<K> {

    private long hashSeed;
    private List<K> pi;
    private List<Integer> tau;

    public OrderAndMatch(long hashSeed, List<K> pi, List<Integer> tau) {
        this.hashSeed = hashSeed;
        this.pi = pi;
        this.tau = tau;
    }

    public List<K> getPi() {
        return pi;
    }

    public List<Integer> getTau() {
        return tau;
    }

    public long getHashSeed() {
        return hashSeed;
    }
}
