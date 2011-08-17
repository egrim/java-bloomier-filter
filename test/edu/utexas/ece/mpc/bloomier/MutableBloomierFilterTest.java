package edu.utexas.ece.mpc.bloomier;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MutableBloomierFilterTest {

	private MutableBloomierFilter<Integer, Integer> uut;

	@Before
	public void setUp() throws Exception {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		
		for (int i=0; i < 1000; i++) {
			map.put(i, i);
		}
		
		uut = new MutableBloomierFilter<Integer, Integer>(map, map.keySet().size()*10, 10, 700, 10000);
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
	public void modify() {
		uut.set(500, 10);
		Assert.assertEquals(Integer.valueOf(10), uut.get(500));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void illegalModify() {
		uut.set(2000, 10);
	}

}
