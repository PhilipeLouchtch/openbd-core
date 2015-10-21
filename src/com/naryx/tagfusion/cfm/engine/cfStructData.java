/*
 *  Copyright (C) 2000 - 2012 TagServlet Ltd
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
 *  http://openbd.org/
 *  $Id: $  
 */

package com.naryx.tagfusion.cfm.engine;

import com.nary.util.CaseSensitiveMap;
import com.nary.util.FastMap;
import com.nary.util.SequencedHashMap;
import com.naryx.tagfusion.cfm.parser.script.userDefinedFunction;
import com.naryx.tagfusion.cfm.tag.cfDUMP;
import com.naryx.tagfusion.cfm.tag.tagUtils;

import javax.mail.Session;
import java.util.*;

/**
 * This class implements the CFML data structure
 */

@SuppressWarnings("deprecation")
public class cfStructData extends cfStructDataBase implements Map, java.io.Serializable {

	/*************************************************************************************
	 * IMPORTANT - PLEASE READ
	 * 
	 * Synchronization. The internal representation of a CFML struct is now based
	 * on java.util.Map instead of java.util.Hashtable. A key difference is that
	 * Hashtable is synchronized (thread-safe) but Map is not. Instead we've
	 * synchronized the methods that access the Map.
	 * 
	 * But, that's not all there is to it. Whenever iterating over a Map (whether
	 * in this class or a subclass), you *MUST* manually synchronize on the Map
	 * object. Use this code as a prototype:
	 * 
	 * Map hashdata = getHashData(); // only subclasses need to do this
	 * synchronized ( hashdata ) { Iterator iter = hashdata.keySet().iterator();
	 * while ( iter.hasNext() ) { String key = (String)iter.next(); cfData val =
	 * (cfData)hashdata.get( key ); } }
	 * 
	 * The safe way for a class that is not a subclass of cfStructData to iterate
	 * through the elements is to use the keys() method and loop over the keys:
	 * 
	 * Object[] keys = struct.keys(); for ( int i = 0; i < keys.length; i++ ) {
	 * String key = (String)keys[ i ]; cfData val = struct.getData( key ); }
	 * 
	 *************************************************************************************/

	static final long serialVersionUID = 1;

	protected boolean isBDAdminStruct = false;

	public cfStructData() {
		this(FastMap.CASE_INSENSITIVE);
	}

	public cfStructData(boolean caseSensitive) {
		// what about subclasses?
		this(new FastMap<String, cfData>(caseSensitive));
	}

	public cfStructData(Map<String, cfData> _hashdata) {
		setHashData(_hashdata);
		setInstance(this);
	}

	public byte getDataType() {
		return cfData.CFSTRUCTDATA;
	}

	public String getDataTypeName() {
		return "struct";
	}

	public boolean isStruct() {
		return true;
	}

	/**************************************************************************
	 * SUBCLASSES: the following methods contain references to the private
	 * hashdata data store and MUST be overridden by subclasses that use an
	 * alternate data store.
	 **************************************************************************/

	protected Map<String, cfData> getHashData() {
		return hashdata;
	}

	protected void setHashData(Map<String, cfData> _hashdata) {
		hashdata = _hashdata;
		setInstance(hashdata);
	}

	public boolean isCaseSensitive() {
		if (hashdata instanceof CaseSensitiveMap) {
			return ((CaseSensitiveMap<String, cfData>) hashdata).isCaseSensitive();
		} else {
			throw new UnsupportedOperationException(); // return true?
		}
	}

	public synchronized cfData getData(String _key) {
		return hashdata.get(_key);
	}

	public synchronized void setData(String _key, cfData _data) {
		hashdata.put(_key, _data);
	}

	public synchronized void setData(String _key, String _data) {
		hashdata.put(_key, new cfStringData(_data) );
	}

	public synchronized void setData(String _key, int _data) {
		hashdata.put(_key, new cfNumberData(_data) );
	}

	public synchronized void setData(String _key, long _data) {
		hashdata.put(_key, new cfNumberData(_data) );
	}

	public synchronized void setData(String _key, Date _data) {
		hashdata.put(_key, new cfDateData(_data) );
	}

	public synchronized void deleteData(String _key) throws cfmRunTimeException {
		hashdata.remove(_key);
	}

	public synchronized boolean containsKey(String _key) {
		return hashdata.containsKey(_key);
	}

	public synchronized boolean containsValue(cfData _data) {
		return hashdata.containsValue(_data);
	}

	public synchronized Set<String> keys() {
		return hashdata.keySet();
	}

	// TODO: Improve code that makes use of this method by rewriting it to loop over EntrySets or KeySey + Using generics
	public synchronized String[] keysArray() {
		Set<String> keySet = keys();
		String[] keysArray = new String[keySet.size()];

		return keySet.toArray(keysArray);
	}

	// Map interface method
	public synchronized void clear() {
		hashdata.clear();
	}

	// Map interface method
	public int size() {
		return hashdata.size();
	}

	// Map interface method
	public boolean isEmpty() {
		return hashdata.isEmpty();
	}

	// Map interface method
	public synchronized Set<String> keySet() {
		return hashdata.keySet();
	}

	// Map interface method
	public synchronized boolean equals(Object o) {
		if (o instanceof cfStructData)
			return hashdata.equals(((cfStructData) o).hashdata);

		return false;
	}

	public synchronized boolean equals(cfData o) {
		if (o.getDataType() == cfData.CFSTRUCTDATA)
			return hashdata.equals(((cfStructData) o).hashdata);

		return false;
	}

	// Map interface method
	public synchronized int hashCode() {
		return hashdata.hashCode();
	}

	// create a shallow copy
	protected Map<String, cfData> cloneHashdata() {
		if (hashdata instanceof FastMap) {
			return new FastMap((FastMap) hashdata);
		} else if (hashdata instanceof SequencedHashMap) { // arguments, see bug
																												// #3226
			return new SequencedHashMap(hashdata, ((SequencedHashMap) hashdata).isCaseSensitive());
		} else {
			throw new UnsupportedOperationException();
		}
	}

	/**************************************************************************
	 * The following methods do not reference the private hashdata attribute. They
	 * do not need to be overridden by subclasses that use an alternate data store
	 **************************************************************************/

	public cfData getData(cfData arrayIndex) throws cfmRunTimeException {
		return getData(arrayIndex.getString());
	}

	public void setData(cfData _key, cfData _data) throws cfmRunTimeException {
		setData(_key.getString(), _data);
	}

	public cfData removeData(String _key) throws cfmRunTimeException {
		cfData data = getData(_key);
		if (data != null) {
			deleteData(_key);
		}
		return data;
	}

	public cfArrayData getKeyArray() throws cfmRunTimeException {
		cfArrayData array = cfArrayData.createArray(1);

		Set<String> keySet = keys();
		String[] keyArray = new String[keySet.size()];
		keyArray = keys().toArray(keyArray);

		for (String s : keyArray)
		{
			array.addElement(new cfStringData(s));
		}

		return array;
	}

	public synchronized Object clone() {
		// Creates a shallow copy.
		return new cfStructData(cloneHashdata());
	}

	public synchronized Map<String, cfData> copy() {
		Map<String, cfData> copy = cloneHashdata();

		Set<Entry<String, cfData>> entrySet = hashdata.entrySet();

		for (Entry<String, cfData> entry : entrySet)
		{
			String key = entry.getKey();
			cfData value = entry.getValue();

			if (value instanceof cfArrayData) {
				copy.put(key, value.duplicate());
			}
		}

		return copy;
	}

	public synchronized cfData duplicate() {
		Map<String, cfData> dupData = cloneHashdata();

		Set<Entry<String, cfData>> entrySet = hashdata.entrySet();
		for (Entry<String, cfData> entry : entrySet)
		{
			String key = entry.getKey();
			cfData value = entry.getValue();
			cfData valueCopy = null;

			if (value != null) {
				if (value.isImplicit()) // part of the fix for bug #2083
					continue;

				valueCopy = value.duplicate();
				if (valueCopy == null) { // return null if struct contains
					// non-duplicatable type
					return null;
				}
			}

			dupData.put(key, valueCopy);
		}

		return new cfStructData(dupData);
	}

	public String getKeyList(String delimiter) {
		StringBuilder listBuilder = new StringBuilder(512);

		Set<String> keySet = keys();
		for (String key : keySet)
		{
			listBuilder.append(key).append(delimiter);
		}

		// Removing trailing delimiter
		if (keySet.size() > 0) {
			listBuilder.setLength(listBuilder.length() - delimiter.length());
		}

		return listBuilder.toString();
	}

	public synchronized String toString() {
		if (isBDAdminStruct) {
			return "{STRUCT: [toString() disabled]}";
		}

		StringBuilder stringBuilder = new StringBuilder(512);
		Set<Entry<String, cfData>> entries = hashdata.entrySet();

		stringBuilder.append("{STRUCT:");
		for (Entry<String, cfData> entry : entries)
		{
			stringBuilder.append(entry.getKey());
			stringBuilder.append('=');
			stringBuilder.append(entry.getValue());
			stringBuilder.append(',');
		}

		// Removing trailing delimiter
		if (entries.size() > 0) {
			stringBuilder.setLength(stringBuilder.length() - 1);
		}

		stringBuilder.append('}');

		return stringBuilder.toString();
	}

	public void dump(java.io.PrintWriter out) {
		dump(out, false, "", cfDUMP.TOP_DEFAULT);
	}

	public void dump(java.io.PrintWriter out, String _lbl, int _top) {
		dump(out, false, _lbl, _top);
	}

	public void dumpLong(java.io.PrintWriter out) {
		dump(out, true, "", cfDUMP.TOP_DEFAULT);
	}

	public void dumpLong(java.io.PrintWriter out, String _lbl, int _top) {
		dump(out, true, _lbl, _top);
	}

	protected synchronized void dump(java.io.PrintWriter out, boolean longVersion, String _lbl, int _top) {
		dump("struct",out,longVersion,_lbl,_top);
	}
	
	protected synchronized void dump(String tablename, java.io.PrintWriter out, boolean longVersion, String _lbl, int _top) {
		out.write("<table class='cfdump_table_struct'>");
		out.write("<th class='cfdump_th_struct' colspan='2'>");
		if (_lbl.length() > 0) {
			out.write(_lbl);
			out.write(" - ");
		}

		if (isBDAdminStruct) {
			out.write(tablename);
			out.write(" [dump disabled]</th>");
		}
		else {
			Set<Entry<String, cfData>> entries = hashdata.entrySet();

			if (entries.size() > 0) {
				out.write(tablename);
				out.write("</th>");

				ArrayList<Entry<String, cfData>> entriesList = new ArrayList<>(entries);
				java.util.Collections.sort(
						entriesList,
						(o1, o2) -> o1.getKey().compareTo(o2.getKey())
				);

				for (Entry<String, cfData> entry : entriesList) {
					out.write("<tr><td class='cfdump_td_struct'>");
					out.write(entry.getKey());
					out.write("</td><td class='cfdump_td_value'>");

					if (_top > 1) {
						cfData value = entry.getValue();
						if (value != null) {
							// Self-reference; don't dump the value again in itself (can cause stack-overflows otherwise)
							if (value == this) {
								out.write("<span style='background-color: blue; color: white;'>[SELF-REFERENCE]</span>");
							}
							else {
								int newTop = (value.getDataType() == cfData.CFSTRUCTDATA ? _top - 1 : _top);

								if (longVersion) {
									value.dumpLong(out, "", newTop);
								}
								else {
									value.dump(out, "", newTop);
								}
							}
						}
						else {
							out.write("[null]");
						}
					}
					out.write("</td></tr>");
				}

			}
			else {
				out.write(tablename);
				out.write(" [empty]</th>");
			}
		}

		out.write("</table>");
	}

	public void dumpWDDX(int version, java.io.PrintWriter out) {
		if (version > 10) {
			out.write("<s>");
		}
		else {
			out.write("<struct>");
		}

		Set<Entry<String, cfData>> entries = hashdata.entrySet();
		for (Entry<String, cfData> entry : entries)
		{
			if (version > 10) {
				out.write("<v n='");
			}
			else {
				out.write("<var name='");
			}

			out.write(entry.getKey());
			out.write("'>");
			entry.getValue().dumpWDDX(version, out);

			if (version > 10){
				out.write("</v>");
			}
			else {
				out.write("</var>");
			}
		}

		if (version > 10) {
			out.write("</s>");
		}
		else {
			out.write("</struct>");
		}
	}

	/*******************************************************************************
	 * The following methods implement the java.util.Map interface. They're
	 * implemented to support variable sharing between CFML and servlets/JSP
	 * pages. These methods automatically convert between internal BlueDragon data
	 * types and "natural" Java data types.
	 * 
	 * Subclasses that use an alternate data store (intead of hashdata) and that
	 * store "natural" Java objects will be more efficient if they override the
	 * get, put, and containsValue methods to avoid converting from Java objects
	 * to CFML variables and back again. (However, this isn't necessary for
	 * correct operation).
	 * 
	 * These methods MUST not reference the underlying hashdata directly!
	 *******************************************************************************/

	// Returns true if this map contains a mapping for the specified key.
	public boolean containsKey(Object key) {
		return containsKey(key.toString());
	}

	// Returns true if this map maps one or more keys to the specified value.
	public boolean containsValue(Object value) {
		return containsValue(tagUtils.convertToCfData(value));
	}

	// Returns the value to which this map maps the specified key.
	public Object get(Object key) {
		return tagUtils.getNatural((getData(key.toString())));
	}

	// Associates the specified value with the specified key in this map.
	public Object put(Object key, Object value) {
		Object oldValue = get(key);
		setData(key.toString(), tagUtils.convertToCfData(value));
		return oldValue;
	}

	// Copies all of the mappings from the specified map to this map.
	public void putAll(Map m) {
		for (String key : (Set<String>) m.keySet()) // Todo: use generics
		{
			Object val = m.get(key);
			put(key, val);
		}
	}

	// Removes the mapping for this key from this map if it is present.
	public Object remove(Object key) {
		try {
			Object value = get(key);
			deleteData(key.toString());
			return value;
		} catch (cfmRunTimeException exc) {
			throw new IllegalArgumentException(exc.getMessage());
		}
	}

	// convert values to natural Java objects
	public synchronized Collection<Object> values() {
		return tagUtils.getNaturalMap(this).values();
	}

	// convert values to natural Java objects
	public synchronized Set<Entry<String, Object>> entrySet() {
		return tagUtils.getNaturalMap(this).entrySet();
	}

	/**
	 * Special function for looping around the data with a UserDefinedFunction
	 * 
	 * @param SessionData
	 * @param _data
	 * @throws cfmRunTimeException
	 */
	public void each( cfDataSession SessionData, cfData _data ) throws cfmRunTimeException {
		if ( _data.getDataType() != cfData.CFUDFDATA ) {
			throw new cfmRunTimeException( catchDataFactory.generalException("Invalid Attribute", "Must be a user defined function") );
		}

		userDefinedFunction	udf	= (userDefinedFunction)_data;
		List<cfData> args = new ArrayList<cfData>(1);

		Set<Entry<String, cfData>> entrySet = hashdata.entrySet();
		for (Entry<String, cfData> entry : entrySet)
		{
			args.clear();
			args.add(new cfStringData(entry.getKey()));
			args.add(entry.getValue());
			udf.execute(SessionData.Session, args);
		}
	}
}