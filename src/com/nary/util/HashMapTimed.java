/* 
 *  Copyright (C) 2000 - 2011 TagServlet Ltd
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
 *  $Id: $
 */

package com.nary.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.lang.NotImplementedException;
import org.aw20.util.SystemClockEvent;

import com.naryx.tagfusion.cfm.engine.cfEngine;

/**
 * This implements a timed hashtable. If a certain piece of data isn't accessed
 * after a given period of time then it is removed from the table.
 * 
 */

// can't extend FastMap, which doesn't allow the clear() method to be overridden
//public class HashMapTimed<K extends String, V> extends FastMap<K, HashMapTimed.IdleableObject<V>> implements SystemClockEvent {
public class HashMapTimed<K extends String, V> implements CaseSensitiveMap<K, V>, SystemClockEvent {

	private static final long serialVersionUID = 2L;
	private HashMapTimedCallback callback = null;

	private final FastMap<K, IdleableObject<V>> underlyingMap = new FastMap<>();
	
	public HashMapTimed() {
		this(600);
	}

	public HashMapTimed(int timeOutSeconds) {
		cfEngine.thisPlatform.timerSetListenerMinute( this, (int)timeOutSeconds/60 );
	}
	
	public void setCallback( HashMapTimedCallback callback ){
		this.callback = callback;
	}
	
	public void destroy(){
		cfEngine.thisPlatform.timerCancel(this);
	}

	protected void finalize() throws Throwable {
		// make sure there is references held by the Alarm manager
		super.finalize();
		destroy();
	}

	public synchronized void clockEvent(int type) {
		Iterator<K> iter = keySet().iterator();
		while (iter.hasNext())
		{
			K key = iter.next();
			IdleableObject<V> idleableObject = this.underlyingMap.get(key);

			if (idleableObject.isIdle())
			{
				iter.remove();

				if (callback != null)
				{
					callback.onRemoveFromMap(key, idleableObject.data);
				}
			}
			else
			{
				idleableObject.setIdle(true);
			}
		}
	}

	@Override
	public V put(K key, V value) {
		IdleableObject<V> idleableObject = this.underlyingMap.put(key, new IdleableObject<V>(value));
		if (idleableObject == null) {
			return null;
		}
		else {
			return idleableObject.data;
		}
	}

	public V get(K key) {
		if (containsKey(key))
		{
			IdleableObject<V> idleableObject = this.underlyingMap.get(key);
			idleableObject.setIdle(false);
			return idleableObject.data;
		}
		else
		{
			return null;
		}
	}

	@Override
	public boolean isCaseSensitive()
	{
		return this.underlyingMap.isCaseSensitive();
	}

	@Override
	public int size()
	{
		return this.underlyingMap.size();
	}

	@Override
	public boolean isEmpty()
	{
		return this.underlyingMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key)
	{
		return this.underlyingMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value)
	{
		return this.underlyingMap.containsValue(value);
	}

	@Override
	public V get(Object key)
	{
		IdleableObject<V> idleableObject = this.underlyingMap.get(key);
		return idleableObject.data;
	}

	@Override
	public V remove(Object key)
	{
		IdleableObject<V> previousIdleableObject = this.underlyingMap.remove(key);
		return previousIdleableObject.data;
	}


	@Override
	public V replace(K key, V value)
	{
		IdleableObject<V> previousIdleableObject = this.underlyingMap.replace(key, new IdleableObject<V>(value));
		return previousIdleableObject.data;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> sourceMap)
	{
		// Can't use underlyingMap.putAll as our underlyingMap holds V objects wrapped in IdleableObject, so have to wrap each one here
		for (Entry<? extends K, ? extends V> sourceEntry : sourceMap.entrySet())
		{
			this.underlyingMap.put(sourceEntry.getKey(), new IdleableObject<V>(sourceEntry.getValue()));
		}
	}

	@Override
	public void clear()
	{
		this.underlyingMap.clear();
	}

	@Override
	public Set<K> keySet()
	{
		return this.underlyingMap.keySet();
	}

	@Override
	public Collection<V> values()
	{
		// Can't simply use the underlyingMap.value method as the values are wrapped in IdleableObject, have to unwrap them manually here
		ArrayList<V> values = new ArrayList<>(this.underlyingMap.size());
		for (IdleableObject<V> idleableObject: this.underlyingMap.values())
		{
			values.add(idleableObject.data);
		}

		return values;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet()
	{
		// Can't simply use the underlyingMap.value method as the values are wrapped in IdleableObject, have to unwrap them manually here and re-create the entities
		Set<Entry<K, V>> entries = new HashSet<>();
		for (Entry<K, IdleableObject<V>> wrappedEntry : this.underlyingMap.entrySet())
		{
			entries.add(new AbstractMap.SimpleEntry<K, V>(wrappedEntry.getKey(), wrappedEntry.getValue().data));
		}
		
		return entries;
	}

	@Override
	public V putIfAbsent(K key, V value)
	{
		IdleableObject<V> previousIdleableObject = this.underlyingMap.putIfAbsent(key, new IdleableObject<V>(value));
		return previousIdleableObject.data;
	}

	@Override
	public boolean remove(Object key, Object value)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue)
	{
		throw new UnsupportedOperationException();
	}

	// ------------------------------------------------
	// ------------------------------------------------
	private class IdleableObject<T> {
		public T data;
		private boolean idle;

		public IdleableObject(T _data) {
			data = _data;
			idle = false;
		}

		public boolean isIdle() {
			return idle;
		}

		public void setIdle(boolean _idle) {
			idle = _idle;
		}
	}
}
