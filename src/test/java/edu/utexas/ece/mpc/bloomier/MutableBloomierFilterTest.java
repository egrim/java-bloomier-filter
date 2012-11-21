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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class MutableBloomierFilterTest {

    private MutableBloomierFilter<Integer, Integer> uut;
    private final Map<Integer, Integer> originalMap = new HashMap<Integer, Integer>();
    @Rule
	public final ErrorCollector errorCollector = new ErrorCollector();

    @Before
    public void setUp() throws Exception {

        for (int i = 0; i < 1000; i++) {
            originalMap.put(i, i);
        }

        uut = new MutableBloomierFilter<Integer, Integer>(originalMap, originalMap.keySet().size() * 10, 10, 32,
                                                          10000);
    }

    @Test
    public void member() {
        Assert.assertEquals(Integer.valueOf(1), uut.get(1));
    }

    @Test
    public void notMember() {
        Integer result = uut.get(2000);
        Assert.assertNull(result);
    }

    @Test
    public void testHighBoundary() {
        Integer result = uut.get(1000);
        Assert.assertNull(result);
    }
    
    @Test
    public void testLowBoundary() {
        Integer result = uut.get(-1);
        Assert.assertNull(result);
    }


    @Test
    public void testNegativeNumber() {
    	
        Integer result = uut.get(-1);
        Assert.assertNull(result);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNegativeNumbers() {
		uut.set(-1, 10);
    }

    @Test
    public void modify() {
        uut.set(500, 10);
        Assert.assertEquals(Integer.valueOf(10), uut.get(500));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void modifyWithNegativeNumbers() {
        uut.set(-500, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalModify() {
        uut.set(2000, 10);
    }
    

    @Test
	public void testAllMembers() throws Exception {
    	final Set<Entry<Integer, Integer>> entrySet = this.originalMap.entrySet();
    	for(Entry<Integer, Integer> entry : entrySet) {
    		this.errorCollector.checkThat(this.uut.get(entry.getKey()), is(equalTo(entry.getValue())));
    	}
	}

}
