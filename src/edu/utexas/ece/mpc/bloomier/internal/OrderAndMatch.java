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
