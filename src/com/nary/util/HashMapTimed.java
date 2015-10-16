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

import java.util.Iterator;

import org.aw20.util.SystemClockEvent;

import com.naryx.tagfusion.cfm.engine.cfEngine;

/**
 * This implements a timed hashtable. If a certain piece of data isn't accessed
 * after a given period of time then it is removed from the table.
 * 
 */

// can't extend FastMap, which doesn't allow the clear() method to be overridden
//public class HashMapTimed<K extends String, V> extends FastMap<K, HashMapTimed.IdleableObject<V>> implements SystemClockEvent {
public class HashMapTimed<K extends String, V> extends FastMap<K, V> implements SystemClockEvent {

	private static final long serialVersionUID = 1L;
	private HashMapTimedCallback callback = null;
	
	public HashMapTimed() {
		this(600);
	}

	public HashMapTimed(int timeOutSeconds) {
		cfEngine.thisPlatform.timerSetListenerMinute( this, (int)timeOutSeconds/60 );
	}
	
	public void setCallback( HashMapTimedCallback callback ){
		this.callback	= callback;
	}
	
	public void destroy(){
		cfEngine.thisPlatform.timerCancel( this );
	}

	protected void finalize() throws Throwable {
		// make sure there is references held by the Alarm manager
		super.finalize();
		destroy();
	}

	public synchronized void clockEvent(int type) {
		Iterator<K> iter = super.keySet().iterator();
//		while (iter.hasNext()) {
//			String key = iter.next();
//			IdleableObject<V> idleableObject = super.get(key);
//			if (idleableObject.isIdle()) {
//				iter.remove();
//
//				if ( callback != null )
//					callback.onRemoveFromMap(key, idleableObject.data);
//
//			} else {
//				idleableObject.setIdle(true);
//			}
//		}
	}

//	@Override
//	public IdleableObject<V> put(K key, IdleableObject<V> value)
//	{
//		return super.put(key, value);
//	}

	public synchronized V put(K key, V value) {
		return super.put(key, value);
		//return super.put(key, new IdleableObject<V>(value)).data;
	}

	public synchronized V get(K key) {
		return super.get(key);

//		if (containsKey(Key)) {
//			IdleableObject<V> idleableObject = super.get(key);
//			idleableObject.setIdle(false);
//			return idleableObject.data;
//		} else {
//			return null;
//		}
	}

	// ------------------------------------------------
	// ------------------------------------------------

	static class IdleableObject<T> {

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
