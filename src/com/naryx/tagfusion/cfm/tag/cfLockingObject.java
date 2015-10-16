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
 */

package com.naryx.tagfusion.cfm.tag;

import java.io.Serializable;
import java.util.Iterator;

import com.nary.util.FastMap;
import com.naryx.tagfusion.cfm.engine.cfData;

public class cfLockingObject extends cfData implements Serializable {
	private static final long serialVersionUID = 1L;

	private FastMap<String, Integer> readOnly = new FastMap<>(); // holds readonly counters
	private FastMap<String, Integer> exclusive = new FastMap<>(); // holds exclusive counters

	private int refCount;

	public cfLockingObject() {
		setImplicit(true); // part of fix for bug #2083
	}

	public String getString() {
		return "";
	}

	public synchronized boolean isFree() {
		return ((refCount == 0) && (readOnly.size() == 0) && (exclusive.size() == 0));
	}

	public synchronized void incrRefCount() {
		refCount += 1;
	}

	public synchronized void decrRefCount() {
		refCount -= 1;
	}

	public String toString() {
		return "cfLockingObject; exclusive = " + exclusive + ", read-only = "
				+ readOnly;
	}

	public boolean lock(String type, long msecs) {
		return (type.equals(cfLOCK.TYPE_EXCLUSIVE) ? lockExclusive(msecs)
				: lockReadOnly(msecs));
	}

	public void unlock(String type) {
		if (type.equals(cfLOCK.TYPE_EXCLUSIVE))
			unlockExclusive();
		else
			unlockReadOnly();
	}

	/**
	 * Return true if no thread other than the current thread has an exclusive
	 * lock.
	 */
	private synchronized boolean canGetReadOnlyLock(Thread currentThread) {
		for (String threadName : exclusive.keySet()) {
			if (!threadName.equals(currentThread.getName()))
				return false;
		}
		return true;
	}

	private synchronized boolean getReadOnlyLock() {
		Thread currentThread = Thread.currentThread();

		// make sure no other thread has an exclusive lock
		if (!canGetReadOnlyLock(currentThread))
			return false;

		// increment the read-only counter for this thread
		Integer readOnlyCount = readOnly.get(currentThread.getName());
		if (readOnlyCount == null) {
			readOnlyCount = 0;
		}
		readOnly.put(currentThread.getName(), readOnlyCount + 1);
		return true;
	}

	private boolean lockReadOnly(long msecs) {
		if (getReadOnlyLock())
			return true;

		long startTime = System.currentTimeMillis();
		long timeElapsed = 0;
		long sleepInterval = 1;

		while (timeElapsed < msecs) {
			try {
				Thread.sleep(sleepInterval);
			} catch (InterruptedException e) {
			}
			if (getReadOnlyLock()) {
				return true;
			}
			if (sleepInterval < 128) {
				sleepInterval = sleepInterval * 2;
			}
			timeElapsed = System.currentTimeMillis() - startTime;
		}

		return getReadOnlyLock();
	}

	private synchronized void unlockReadOnly() {
		Thread currentThread = Thread.currentThread();
		if (readOnly.containsKey(currentThread.getName())) {
			// decrement the read-only counter for this thread
			int readOnlyCount = readOnly.get(currentThread.getName()) - 1;
			if (readOnlyCount == 0) {
				readOnly.remove(currentThread.getName());
			}
			else {
				readOnly.put(currentThread.getName(), readOnlyCount);
			}
		}
	}

	/**
	 * Return true if no thread other than the current thread has an exclusive or
	 * read-only lock.
	 */
	private synchronized boolean getExclusiveLock() {
		Thread currentThread = Thread.currentThread();

		// make sure no other thread has an exclusive lock
		if (!canGetReadOnlyLock(currentThread))
			return false;

		// make sure no other thread has a read-only lock
		for (String threadName : readOnly.keySet())
		{
			if (!threadName.equals(currentThread.getName())) {
				return false;
			}
		}

		// increment the exclusive counter for this thread
		Integer exclusiveCount = exclusive.get(currentThread.getName());
		if (exclusiveCount == null) {
			exclusiveCount = 0;
		}
		exclusive.put(currentThread.getName(), exclusiveCount + 1);
		return true;
	}

	private boolean lockExclusive(long msecs) {
		if (getExclusiveLock())
			return true;

		long startTime = System.currentTimeMillis();
		long timeElapsed = 0;
		long sleepInterval = 1;

		while (timeElapsed < msecs) {
			try {
				Thread.sleep(sleepInterval);
			} catch (InterruptedException e) {
			}
			if (getExclusiveLock()) {
				return true;
			}
			if (sleepInterval < 128) {
				sleepInterval = sleepInterval * 2;
			}
			timeElapsed = System.currentTimeMillis() - startTime;
		}

		return getExclusiveLock();
	}

	private synchronized void unlockExclusive() {
		Thread currentThread = Thread.currentThread();
		if (exclusive.containsKey(currentThread.getName())) {
			// decrement the exclusive counter for this thread
			int exclusiveCount = exclusive.get(currentThread.getName()) - 1;
			if (exclusiveCount == 0) {
				exclusive.remove(currentThread.getName());
			}
			else {
				exclusive.put(currentThread.getName(), exclusiveCount);
			}
		}
	}
}
