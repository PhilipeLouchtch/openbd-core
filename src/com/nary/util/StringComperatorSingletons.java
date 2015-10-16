package com.nary.util;

import javolution.util.FastComparator;

public class StringComperatorSingletons
{
	private static class StringComparatorIgnoreCase extends FastComparator<String> {
		private static final long serialVersionUID = 1L;

		// the formal definition of this method says you're supposed to allow nulls;
		// but we're dealing with keys here, which aren't allowed to be null, so
		// don't bother checking for nulls
		public boolean areEqual(String key1, String key2) {
			return key1.equalsIgnoreCase(key2);
		}

		public int compare(String key1, String key2) {
			return key1.compareToIgnoreCase(key2);
		}

		public int hashCodeOf(String key) {
			// toLowerCase() performs much better here than toUpperCase()
			return key.toLowerCase().hashCode();
		}
	}

	public static final FastComparator<String> ignoreCaseStringComperator = new StringComparatorIgnoreCase();
}
