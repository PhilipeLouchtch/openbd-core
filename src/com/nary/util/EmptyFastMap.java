/* 
 *  Copyright (C) 2000 - 2010 TagServlet Ltd
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
 *  
 */

/*
 * This is used for times when we want a completely empty HashMap that will
 * never contain any values
 */

package com.nary.util;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EmptyFastMap<K extends String, V> implements CaseSensitiveMap<K, V>, Serializable {
	private static final long serialVersionUID = 1L;
	
	@Override
	public boolean isCaseSensitive() {
		return false;
	}

	@Override
	public void clear() {
	}

	@Override
	public boolean containsKey(Object arg0) {
		return false;
	}

	@Override
	public boolean containsValue(Object arg0) {
		return false;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return Collections.<Entry<K, V>>emptySet();
	}

	@Override
	public V get(Object arg0) {
		return null;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public Set<K> keySet() {
		return Collections.<K>emptySet();
	}

	@Override
	public V put(K arg0, V arg1) {
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> arg0) {
	}

	@Override
	public V remove(Object arg0) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Collection<V> values() {
		return Collections.<V>emptyList();
	}

	@Override
	public V putIfAbsent(K key, V value)
	{
		return null;
	}

	@Override
	public boolean remove(Object key, Object value)
	{
		return false;
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue)
	{
		return false;
	}

	@Override
	public V replace(K key, V value)
	{
		return null;
	}
}
