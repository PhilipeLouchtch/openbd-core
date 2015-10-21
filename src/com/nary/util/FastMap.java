/* 
 *  Copyright (C) 2000 - 2008 TagServlet Ltd
 *
 *  This file is part of Open BlueDragon (OpenBD) CFML Server Engine.
 *  
 *  OpenBD is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  Free Software Foundation,version 3.
 *  
 *  OpenBD is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with OpenBD.  If not, see http://www.gnu.org/licenses/
 *  
 *  Additional permission under GNU GPL version 3 section 7
 *  
 *  If you modify this Program, or any covered work, by linking or combining 
 *  it with any of the JARS listed in the README.txt (or a modified version of 
 *  (that library), containing parts covered by the terms of that JAR, the 
 *  licensors of this Program grant you additional permission to convey the 
 *  resulting work. 
 *  README.txt @ http://www.openbluedragon.org/license/README.txt
 *  
 *  http://www.openbluedragon.org/
 */

package com.nary.util;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A subclass of javolution.util.FastMap that supports case-insensitive keys.
 * Keys are always strings.
 */
public class FastMap<K extends String, V> extends ConcurrentHashMap<K, V> implements CaseSensitiveMap<K, V>, Serializable, Cloneable {
	static final long serialVersionUID = 1;

	public static final boolean CASE_SENSITIVE = true;
	public static final boolean CASE_INSENSITIVE = false;

	private boolean isCaseSensitive;

	public FastMap(boolean isCaseSensitive) {
		this.isCaseSensitive = isCaseSensitive;
	}

	public FastMap() {
		// FastMaps are case-sensitive by default
		this(CASE_SENSITIVE);
	}

	public FastMap(FastMap<K, V> map) {
		isCaseSensitive = map.isCaseSensitive();
		putAll(map);
	}

	public FastMap(java.util.Map<K, V> map) {
		// java.util.Map is case sensitive
		this(CASE_SENSITIVE);
		putAll(map);
	}

	public FastMap(int initialCapacity) {
		super(initialCapacity);
	}

	public FastMap<K, V> clone() throws CloneNotSupportedException {
		FastMap<K, V> clonedFastMap = (FastMap<K, V>) super.clone();

		return clonedFastMap;
	}

	// this method is not part of the standard java.util.Map interface
	public boolean isCaseSensitive() {
		return isCaseSensitive;
	}

	protected K convertCasingIfNeeded(K key) {
		return !isCaseSensitive() ? (K) key.toLowerCase() : key;
	}

	@Override
	public V put(K key, V value) {
		return super.put((K) convertCasingIfNeeded(key).intern(), value);
	}

	@Override
	public V get(Object k)
	{
		return super.get(convertCasingIfNeeded((K) k));
	}

	@Override
	public boolean containsKey(Object k)
	{
		return super.containsKey(convertCasingIfNeeded((K) k));
	}

	@Override
	public V remove(Object k)
	{
		return super.remove(convertCasingIfNeeded((K) k));
	}
}
