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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import edu.utexas.ece.mpc.bloomier.ImmutableBloomierFilter;

public class ImmutableBloomierFilterTest {
    ImmutableBloomierFilter<Integer, Integer> uut;
    
    @Rule
    public final ErrorCollector errorCollector = new ErrorCollector();

	private final HashMap<Integer, Integer> originalMap = new HashMap<Integer, Integer>();

    @Before
    public void setUp() throws Exception {

        for (int i = 0; i < 1000; i++) {
            originalMap.put(i, i);
        }

        uut = new ImmutableBloomierFilter<Integer, Integer>(originalMap, originalMap.keySet().size() * 10, 10, 32,
                                                            Integer.class, 10000);
    }

    @Test
    public void member() {
        assertEquals(Integer.valueOf(1), uut.get(1));
    }

    @Test
    public void notMember() {
        Integer result = uut.get(2000);
        Assert.assertNull(result);
    }
    
    @Test
	public void testAllMembers() throws Exception {
    	final Set<Entry<Integer, Integer>> entrySet = this.originalMap.entrySet();
    	for(Entry<Integer, Integer> entry : entrySet) {
    		this.errorCollector.checkThat(this.uut.get(entry.getKey()), is(equalTo(entry.getValue())));
    	}
	}

}
