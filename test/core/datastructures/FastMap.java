package core.datastructures;

import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfStringData;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class FastMap
{
	com.nary.util.FastMap<String, cfData> fastMap;
	com.nary.util.FastMap<String, cfData> fastMapClone;

	Map.Entry<String, cfData>[] testEntries;

	private com.nary.util.FastMap<String, cfData> cloneFastMapHelperMethod(com.nary.util.FastMap original)
	{
		return (com.nary.util.FastMap<String, cfData>) original.clone();
	}

	@Before
	public void setUpClone() throws Exception
	{
		fastMap = new com.nary.util.FastMap<>();
		fastMapClone = cloneFastMapHelperMethod(fastMap);

		testEntries = new Map.Entry[] {
				new AbstractMap.SimpleEntry<String, cfData>("One",      new cfStringData("One")),
				new AbstractMap.SimpleEntry<String, cfData>("Two",      new cfStringData("Two")),
				new AbstractMap.SimpleEntry<String, cfData>("Three",    new cfStringData("Three")),
				new AbstractMap.SimpleEntry<String, cfData>("Four",     new cfStringData("Four")),
				new AbstractMap.SimpleEntry<String, cfData>("Five",     new cfStringData("Five")),
				new AbstractMap.SimpleEntry<String, cfData>("Six",      new cfStringData("Six"))
		};
	}

	@Test
	public void testFastMap_clone_returnsNewReference() throws Exception
	{
		assertTrue(System.identityHashCode(fastMap) != System.identityHashCode(fastMapClone));
	}

	@Test
	public void testFastMap_clone_cloneDoesNotChangeOriginal()
	{
		String key = "John";
		cfData data = new cfStringData("Doe");
		fastMapClone.put(key, data);

		assertNotEquals(fastMap.size(), fastMapClone.size());;
		assertFalse(fastMap.containsKey(key));
		assertTrue(fastMapClone.containsKey(key));
	}

	@Test
	public void testFastMap_clone_originalDoesNotChangeClone()
	{
		String key = "John";
		cfData data = new cfStringData("Doe");
		fastMap.put(key, data);

		assertNotEquals(fastMap.size(), fastMapClone.size());
		assertFalse(fastMapClone.containsKey(key));
	}

	@Test
	public void testFastMap_clone_valuesAreCopiedByRef() throws Exception
	{
		for (Map.Entry<String, cfData> testEntry : testEntries)
		{
			fastMap.put(testEntry.getKey(), testEntry.getValue());
		}

		fastMapClone = cloneFastMapHelperMethod(fastMap);

		for (Map.Entry<String, cfData> testEntry : testEntries)
		{
			assertTrue(fastMapClone.containsKey(testEntry.getKey()));
			assertTrue(System.identityHashCode(fastMapClone.get(testEntry.getKey())) == System.identityHashCode(testEntry.getValue()));
		}
	}

	@Test
	public void testFastMap_clone_entriesAreCopiedByRef() throws Exception
	{
		Set<Map.Entry<String, cfData>> entriesOriginal = fastMap.entrySet();
		Set<Map.Entry<String, cfData>> entriesCloned = fastMapClone.entrySet();

		Map.Entry<String, cfData>[] entriesOriginalAsArray = entriesOriginal.toArray(new Map.Entry[entriesOriginal.size()]);
		Map.Entry<String, cfData>[] entriesClonedAsArray = entriesOriginal.toArray(new Map.Entry[entriesCloned.size()]);

		for (int i = 0; i < entriesOriginalAsArray.length; i++)
		{
			assertTrue(System.identityHashCode(entriesOriginalAsArray[i]) == System.identityHashCode(entriesClonedAsArray[i]));
		}
	}
}
