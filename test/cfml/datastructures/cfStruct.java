package cfml.datastructures;

import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfStringData;
import com.naryx.tagfusion.cfm.engine.cfStructData;
import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class cfStruct
{
	cfStructData struct;
	cfStructData structClone;

	Map.Entry<String, cfData>[] testEntries;

	@Before
	public void setUp() throws Exception
	{
		struct = new cfStructData();
		structClone = (cfStructData) struct.duplicate();

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
	public void cloningReturnsNewReference()
	{
		assertFalse(System.identityHashCode(struct) == System.identityHashCode(structClone));
	}

	@Test
	public void cloneIsShallowCopy() throws Exception
	{
		for (Map.Entry<String, cfData> testEntry : testEntries)
		{
			struct.put(testEntry.getKey(), testEntry.getValue());
		}

		structClone = (cfStructData) struct.clone();

		for (Map.Entry<String, cfData> testEntry : testEntries)
		{
			assertTrue(structClone.containsKey(testEntry.getValue()));

			Object valueInClone = structClone.getData(testEntry.getKey());
			Object valueOriginal = testEntry.getValue();
			assertTrue(System.identityHashCode(valueOriginal) == System.identityHashCode(valueInClone));

			assertTrue(System.identityHashCode(struct.getData(testEntry.getKey())) == System.identityHashCode(structClone.getData(testEntry.getKey())));
		}
	}

	@Test
	public void duplicateIsDeepishCopy() throws Exception
	{
		for (Map.Entry<String, cfData> testEntry : testEntries)
		{
			struct.put(testEntry.getKey(), testEntry.getValue());
		}

		cfStructData structDuplicate = (cfStructData) struct.duplicate();

		for (Map.Entry<String, cfData> testEntry : testEntries)
		{
			assertTrue(structDuplicate.containsKey(testEntry.getValue()));

			Object valueOriginal = testEntry.getValue();
			Object valueInClone = structDuplicate.getData(testEntry.getKey());
			assertTrue(System.identityHashCode(valueOriginal) != System.identityHashCode(valueInClone));

			assertTrue(System.identityHashCode(struct.getData(testEntry.getKey())) != System.identityHashCode(structDuplicate.getData(testEntry.getKey()))
			);
		}
	}
}
