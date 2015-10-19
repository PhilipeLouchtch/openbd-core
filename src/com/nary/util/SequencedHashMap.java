/*
 * The Apache Software License, Version 2
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

/**
 * This is a modified version of org.apache.commons.collections.SequencedHashMap
 * that uses com.nary.util.FastMap for storing entries (instead of java.util.Map)
 * and that supports case-sensitive or case-insensitive keys.
 */
package com.nary.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

/**
 *  A map of objects whose mapping entries are sequenced based on the order in
 *  which they were added.  This data structure has fast <I>O(1)</I> search
 *  time, deletion time, and insertion time.
 *
 *  <P>Although this map is sequenced, it cannot implement {@link
 *  java.util.List} because of incompatible interface definitions.  The remove
 *  methods in List and Map have different return values (see: {@link
 *  java.util.List#remove(Object)} and {@link java.util.Map#remove(Object)}).
 *
 *  <P>This class is not thread safe.  When a thread safe implementation is
 *  required, use {@link Collections#synchronizedMap(Map)} as it is documented,
 *  or use explicit synchronization controls.
 *
 *  @since 2.0
 *  @author <a href="mailto:mas@apache.org">Michael A. Smith</A>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 */
public class SequencedHashMap<K extends String, V> implements CaseSensitiveMap<K, V>, Cloneable, Externalizable
{
  /**
   *  {@link java.util.Map.Entry} that doubles as a node in the linked list
   *  of sequenced mappings.
   **/
  class Entry<K, V> implements Map.Entry<K, V>, Cloneable {

    private final K key;
    private V value;

    public Entry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return this.key;
    }

    @Override
    public V getValue() {
      return this.value;
    }

    @Override
    public V setValue(V value) {
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    @Override
    public int hashCode() {
      // implemented per api docs for Map.Entry.hashCode()
      return ((getKey() == null ? 0 : getKey().hashCode()) ^
              (getValue()==null ? 0 : getValue().hashCode()));
    }

	  /**
       * Creates a shallow copy of the Entry
       * @return Shallow copy of Entry
       * @throws CloneNotSupportedException
       */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
      Entry<K, V> clonedEntry = (Entry<K, V>) super.clone();
      return clonedEntry;
    }

    @Override
    public boolean equals(Object obj) {
      if(obj == null) {
        return false;
      }
      else if(obj == this) {
        return true;
      }
      else if(!(obj instanceof Map.Entry)) {
        return false;
      }
      else {
        Map.Entry other = (Map.Entry) obj;

        // implemented per api docs for Map.Entry.equals(Object)
        return((getKey() == null ?
                  other.getKey() == null :
                  getKey().equals(other.getKey())) &&
                  (getValue() == null ?
                      other.getValue() == null :
                      getValue().equals(other.getValue())));
      }
    }

    public String toString() {
      return "[" + getKey() + "=" + getValue() + "]";
    }
  }

  /**
   * List keeping track of the order of the insertion order of the entries Map
   */
  List<Entry<K, V>> orderedEntries = new LinkedList<>();

  /**
   *  Map of keys to entries
   **/
  private FastMap<K, Entry<K, V>> entries;

  /**
   *  Holds the number of modifications that have occurred to the map, excluding modifications made through a collection view's iterator
   *  (e.g. entrySet().iterator().remove()).  This is used to create a fail-fast behavior with the iterators.
   **/
  private transient long modCount = 0;

  /**
   *  Construct a new sequenced hash map with default initial size and load factor and case-sensitive keys.
   **/
  public SequencedHashMap() {
    entries = new FastMap<K, Entry<K, V>>();
  }

  /**
   *  Construct a new sequenced hash map with default initial size and load factor and specified case-sensitivity.
   **/
  public SequencedHashMap(boolean isCaseSensitive) {
      entries = new FastMap<K, Entry<K, V>>(isCaseSensitive);
  }

  /**
   *  Construct a new sequenced hash map with the specified initial size and default load factor and case-sensitive keys.
   *
   *  @param initialSize the initial size for the hash table
   *
   *  @see FastMap#FastMap(int)
   **/
  public SequencedHashMap(int initialSize) {
    entries = new FastMap<K, Entry<K, V>>(initialSize);
  }

  /**
   *  Construct a new sequenced hash map and add all the elements in the specified map.
   *  The order in which the mappings in the specified map are added is defined by {@link #putAll(Map)}.
   **/
  public SequencedHashMap(Map m) {
    this();
    putAll(m);
  }

  public SequencedHashMap( Map m, boolean caseSensitive ) {
      this( caseSensitive );
      putAll( m );
  }

  @Override
  public boolean isCaseSensitive() {
      return entries.isCaseSensitive();
  }

  /**
   *  Removes the entry from the linked list. Does not remove it from the underlying map.
   **/
  private void removeEntryFromOrderedEntriesList(Entry entry) {
    orderedEntries.remove(entry);
  }

  /**
   *  Appends the new entry to the linked list.
   **/
  private void insertEntryToOrderedEntriesList(Entry<K, V> entry) {
    orderedEntries.add(entry);
  }

  @Override
  public int size() {
    return entries.size();
  }

  @Override
  public boolean isEmpty() {
    return orderedEntries.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return entries.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    // unfortunately, we cannot just pass this call to the underlying map because we are mapping keys to entries, not keys to values.
    // The underlying map doesn't have an efficient implementation anyway, so this isn't a big deal.

    // Optimization: If the given value is null we can simply look for a null Entry.value; faster equality check
    if(value == null) {
      for(Entry<K, V> pos : orderedEntries) {
        if(pos.getValue() == null) {
          return true;
        }
      }
    }
    // Normal path: loop over the orderedEntries linked list and compare each element until we find an Entry.value that is equal to
    // the value we are looking for
    else {
      for(Entry<K, V> pos : orderedEntries) {
        if(value.equals(pos.getValue())) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public V get(Object o) {
    Entry<K, V> entry = entries.get((K) o);

    if(entry == null) {
      // Key not found
      return null;
    }
    else {
      // Key found, extract the value from the Entry
      return entry.getValue();
    }
  }

  /**
   *  Return the entry for the "oldest" mapping.
   *  @return The first entry in the sequence, or <code>null</code> if the map is empty.
   **/
  public Map.Entry<K, V> getFirst() {
    return isEmpty() ? null : orderedEntries.get(0);
  }

  /**
   *  Return the key for the "oldest" mapping.
   *  @return The first key in the sequence, or <code>null</code> if the map is empty.
   **/
  public K getFirstKey() {
    return isEmpty() ? null : getFirst().getKey();
  }

  /**
   *  Return the value for the "oldest" mapping.
   *  @return The first value in the sequence, or <code>null</code> if the map is empty.
   **/
  public V getFirstValue() {
    return isEmpty() ? null : getFirst().getValue();
  }

  /**
   *  Return the entry for the "newest" mapping.
   *  @return The last entry in the sequence, or <code>null</code> if the map is empty.
   **/
  public Map.Entry<K, V> getLast() {
    return isEmpty() ? null : getLast();
  }

  /**
   *  Return the key for the "newest" mapping.   *
   *  @return The last key in the sequence, or <code>null</code> if the map is empty.
   **/
  public K getLastKey() {
    return isEmpty() ? null : getLast().getKey();
  }

  /**
   *  Return the value for the "newest" mapping.   *
   *  @return The last value in the sequence, or <code>null</code> if the map is empty.
   **/
  public V getLastValue() {
    return isEmpty() ? null : getLast().getValue();
  }

  @Override
  public V put(K key, V value) {
    modCount++;

    V oldValue = null;

    // lookup the entry for the specified key
    Entry<K, V> e = entries.get(key);

    // check to see if it already exists
    if(e != null) {
      // remove from list so the entry gets "moved" to the end of list
      removeEntryFromOrderedEntriesList(e);

      // update value in map
      oldValue = e.setValue(value);

      // Note: We do not update the key here because it's unnecessary.  We only
      // do comparisons using equals(Object) and we know the specified key and
      // that in the map are equal in that sense.  This may cause a problem if
      // someone does not implement their hashCode() and/or equals(Object)
      // method properly and then use it as a key in this map.
    } else {
      // add new entry
      e = new Entry<K, V>(key, value);
      entries.put(key, e);
    }

    // add to list
    insertEntryToOrderedEntriesList(e);

    return oldValue;
  }

  /**
   *  Implements {@link Map#remove(Object)}.
   */
  @Override
  public V remove(Object key) {
    Entry<K, V> e = removeImpl(key);
    return (e == null) ? null : e.getValue();
  }

  /**
   *  Fully remove an entry from the map, returning the old entry or null if
   *  there was no such entry with the specified key.
   **/
  private Entry<K, V> removeImpl(Object key) {
    Entry<K, V> e = entries.remove(key);

    if(e == null) {
      return null;
    }
    else {
      modCount++;
      removeEntryFromOrderedEntriesList(e);
      return e;
    }
  }

  /**
   *  Adds all the mappings in the specified map to this map, replacing any mappings that already exist (as per {@link Map#putAll(Map)}).
   *  The order in which the entries are added is determined by the iterator returned from {@link Map#entrySet()} for the specified map.
   *
   *  @param t the mappings that should be added to this map.
   *  @exception NullPointerException if <code>sourceMap</code> is <code>null</code>
   **/
  @Override
  public void putAll(Map<? extends K, ? extends V> sourceMap) {
    for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet())
    {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    modCount++;

    // remove all from the underlying map
    entries.clear();
    orderedEntries.clear();
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) return false;
    if(obj == this) return true;

    if(!(obj instanceof Map)) return false;

    return entrySet().equals(((Map)obj).entrySet());
  }

  @Override
  public int hashCode() {
    return entrySet().hashCode();
  }

  /**
   *  Provides a string representation of the entries within the map. The format of the returned string may change with different releases,
   *  so this method is suitable for debugging purposes only. If a specific format is required, use {@link #entrySet()}.{@link Set#iterator() iterator()}
   *  and iterate over the entries in the map formatting them as appropriate.
   **/
  @Override
  public String toString() {
  	StringBuilder stringBuilder = new StringBuilder().append('[');
    for(Entry<K, V> pos : orderedEntries) {
      stringBuilder.append(pos.getKey()).append('=').append(pos.getValue());
    }
    if (orderedEntries.size() > 0) {
      // remove last comma if present
      stringBuilder.setLength(stringBuilder.length() - 1);
    }
    stringBuilder.append(']');

    return stringBuilder.toString();
  }

  @Override
  public Set<K> keySet() {
    return new AbstractSet<K>() {

      // required impls
      @Override
      public Iterator<K> iterator() { return new OrderedIterator(KEY); }
      @Override
      public boolean remove(Object o) {
        Entry e = SequencedHashMap.this.removeImpl(o);
        return (e != null);
      }

      // more efficient impls than abstract set
      @Override
      public void clear() {
        SequencedHashMap.this.clear();
      }
      @Override
      public int size() {
        return SequencedHashMap.this.size();
      }
      @Override
      public boolean isEmpty() {
        return SequencedHashMap.this.isEmpty();
      }
      @Override
      public boolean contains(Object o) {
        return SequencedHashMap.this.containsKey(o);
      }

    };
  }

  @Override
  public Collection<V> values() {
    return new AbstractCollection<V>() {

      // required impls
      @Override
      public Iterator<V> iterator() { return new OrderedIterator(VALUE); }
      @Override
      public boolean remove(Object value) {
        // do null comparison outside loop so we only need to do it once.  This
        // provides a tighter, more efficient loop at the expense of slight
        // code duplication.
        if(value == null) {
          for(Entry<K, V> pos : orderedEntries) {
            if(pos.getValue() == null) {
              SequencedHashMap.this.removeImpl(pos.getKey());
              return true;
            }
          }
        } else {
          for(Entry<K, V> pos : orderedEntries) {
            if(value.equals(pos.getValue())) {
              SequencedHashMap.this.removeImpl(pos.getKey());
              return true;
            }
          }
        }

        return false;
      }

      // more efficient impls than abstract collection
      @Override
      public void clear() {
        SequencedHashMap.this.clear();
      }
      @Override
      public int size() {
        return SequencedHashMap.this.size();
      }
      @Override
      public boolean isEmpty() {
        return SequencedHashMap.this.isEmpty();
      }
      @Override
      public boolean contains(Object o) {
        return SequencedHashMap.this.containsValue(o);
      }
    };
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new AbstractSet<Map.Entry<K, V>>() {
      // helper
      private Entry<K, V> findEntry(Object o) {
        if(o == null) {
          return null;
        }
        else if(!(o instanceof Map.Entry)) {
          return null;
        }
        else
        {
          Map.Entry<K, V> e = (Map.Entry<K, V>) o;

          Entry<K, V> entry = entries.get(e.getKey());
          if (entry != null && entry.equals(e)) {
            return entry;
          }
          else {
            return null;
          }
        }
      }

      // required impl
      @Override
      public Iterator<Map.Entry<K, V>> iterator() {
        return new OrderedIterator(ENTRY);
      }
      @Override
      public boolean remove(Object o) {
        Entry<K, V> e = findEntry(o);
        if(e == null) return false;

        return SequencedHashMap.this.removeImpl(e.getKey()) != null;
      }

      // more efficient impls than abstract collection
      @Override
      public void clear() {
        SequencedHashMap.this.clear();
      }
      @Override
      public int size() {
        return SequencedHashMap.this.size();
      }
      @Override
      public boolean isEmpty() {
        return SequencedHashMap.this.isEmpty();
      }
      @Override
      public boolean contains(Object o) {
        return findEntry(o) != null;
      }
    };
  }

  // constants to define what the iterator should return on "next"
  private static final int KEY = 0;
  private static final int VALUE = 1;
  private static final int ENTRY = 2;
  private static final int REMOVED_MASK = 0x80000000;

  // TODO: Make type-safe by either subclassing for each return type or an anonymous function given to ctor
  private class OrderedIterator implements Iterator {
    /**
     *  Holds the type that should be returned from the iterator.  The value
     *  should be either {@link #KEY}, {@link #VALUE}, or {@link #ENTRY}.  To
     *  save a tiny bit of memory, this field is also used as a marker for when
     *  remove has been called on the current object to prevent a second remove
     *  on the same element.  Essientially, if this value is negative (i.e. the
     *  bit specified by {@link #REMOVED_MASK} is set), the current position
     *  has been removed.  If positive, remove can still be called.
     **/
    private int returnType;

    /**
     *  Holds the "current" position in the iterator.  When pos.next is the
     *  sentinel, we've reached the end of the list.
     **/
    private int pos = -1;

    /**
     *  Holds the expected modification count.  If the actual modification
     *  count of the map differs from this value, then a concurrent
     *  modification has occurred.
     **/
	private transient long expectedModCount = modCount;

    /**
     *  Construct an iterator over the sequenced elements in the order in which
     *  they were added.  The {@link #next()} method returns the type specified
     *  by <code>returnType</code> which must be either {@link #KEY}, {@link
     *  #VALUE}, or {@link #ENTRY}.
     **/
    public OrderedIterator(int returnType) {
      //// Since this is a private inner class, nothing else should have
      //// access to the constructor.  Since we know the rest of the outer
      //// class uses the iterator correctly, we can leave of the following
      //// check:
      //if(returnType >= 0 && returnType <= 2) {
      //  throw new IllegalArgumentException("Invalid iterator type");
      //}

      // Set the "removed" bit so that the iterator starts in a state where
      // "next" must be called before "remove" will succeed.
      this.returnType = returnType | REMOVED_MASK;
    }

    /**
     *  Returns whether there is any additional elements in the iterator to be
     *  returned.
     *
     *  @return <code>true</code> if there are more elements left to be
     *  returned from the iterator; <code>false</code> otherwise.
     **/
    public boolean hasNext() {
      return (pos + 1) < orderedEntries.size();
    }

    /**
     *  Returns the next element from the iterator.
     *
     *  @return the next element from the iterator.
     *
     *  @exception NoSuchElementException if there are no more elements in the
     *  iterator.
     *
     *  @exception ConcurrentModificationException if a modification occurs in
     *  the underlying map.
     **/
    public Object next() {
      if(modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }

      if(!hasNext()) {
        throw new NoSuchElementException();
      }
      else {
        pos++;
      }


      // clear the "removed" flag
      returnType = returnType & ~REMOVED_MASK;

      Entry<K,V> entry = orderedEntries.get(pos);
      switch(returnType) {
      case KEY:
        return entry.getKey();
      case VALUE:
        return entry.getValue();
      case ENTRY:
        return entry;
      default:
        // should never happen
        throw new Error("bad iterator type: " + returnType);
      }

    }
    
    /**
     *  Removes the last element returned from the {@link #next()} method from
     *  the sequenced map.
     *
     *  @exception IllegalStateException if there isn't a "last element" to be
     *  removed.  That is, if {@link #next()} has never been called, or if
     *  {@link #remove()} was already called on the element.
     *
     *  @exception ConcurrentModificationException if a modification occurs in
     *  the underlying map.
     **/
    public void remove() {
      if((returnType & REMOVED_MASK) != 0) {
        throw new IllegalStateException("remove() must follow next()");
      }
      if(modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }

      SequencedHashMap.this.removeImpl(orderedEntries.get(pos).getKey());

      // update the expected mod count for the remove operation
      expectedModCount++;

      // set the removed flag
      returnType = returnType | REMOVED_MASK;
    }
  }

  // APIs maintained from previous version of SequencedHashMap for backwards
  // compatibility

  /**
   * Creates a shallow copy of this object, preserving the internal structure by copying only references.
   * The keys and values themselves are not <code>clone()</code>'d.
   * The cloned object maintains the same sequence.
   *
   * @return A clone of this instance.     *
   * @exception CloneNotSupportedException if clone is not supported by a subclass.
   */
  public Object clone () throws CloneNotSupportedException {
    // yes, calling super.clone() silly since we're just blowing away all the stuff that super might be doing anyway,
    // but for motivations on this, see: http://www.javaworld.com/javaworld/jw-01-1999/jw-01-object.html
    SequencedHashMap<K, V> map = (SequencedHashMap<K, V>) super.clone();

    // create a new, empty entry map
    // note: this does not preserve the initial capacity and load factor.
    map.entries = new FastMap<>();
      
    // add all the mappings
    map.putAll(this);

    // copy the ordering
    map.orderedEntries.addAll(this.orderedEntries);

    return map;
  }

  /**
   *  Returns the Map.Entry at the specified index
   *
   *  @exception ArrayIndexOutOfBoundsException if the specified index is
   *  <code>&lt; 0</code> or <code>&gt;</code> the size of the map.
   **/
  private Map.Entry<K, V> getEntry(int index) {
    if (index < 0) {
      throw new ArrayIndexOutOfBoundsException(index + " < 0");
    }
    if (index >= orderedEntries.size()) {
      throw new ArrayIndexOutOfBoundsException(index + " >= " + orderedEntries.size());
    }

    return orderedEntries.get(index);
  }

  /**
   * Returns the key at the specified index.
   *
   *  @exception ArrayIndexOutOfBoundsException if the <code>index</code> is
   *  <code>&lt; 0</code> or <code>&gt;</code> the size of the map.
   */
  public K get(int index)
  {
    return getEntry(index).getKey();
  }

  /**
   * Returns the value at the specified index.
   *
   *  @exception ArrayIndexOutOfBoundsException if the <code>index</code> is
   *  <code>&lt; 0</code> or <code>&gt;</code> the size of the map.
   */
  public V getValue(int index)
  {
    return getEntry(index).getValue();
  }

  /**
   * Returns the index of the specified key.
   */
  public int indexOf(K key)
  {
    Entry<K, V> entryFromMap = entries.get(key);
    int pos = 0;
    for (Entry<K, V> entryFromList : orderedEntries) {
      if (entryFromList == entryFromMap) {
        break;
      }

      pos++;
    }

    return pos;
  }

  /**
   * Returns a key iterator.
   */
  public Iterator<K> iterator()
  {
    return keySet().iterator();
  }

  /**
   * Returns the last index of the specified key.
   */
  public int lastIndexOf(K key)
  {
    // keys in a map are guarunteed to be unique
    return indexOf(key);
  }

  /**
   * Returns a List view of the keys rather than a set view.  The returned
   * list is unmodifiable.  This is required because changes to the values of
   * the list (using {@link java.util.ListIterator#set(Object)}) will
   * effectively remove the value from the list and reinsert that value at
   * the end of the list, which is an unexpected side effect of changing the
   * value of a list.  This occurs because changing the key, changes when the
   * mapping is added to the map and thus where it appears in the list.
   *
   * <P>An alternative to this method is to use {@link #keySet()}
   *
   * @see #keySet()
   * @return The ordered list of keys.  
   */
  public List<K> sequence()
  {
    List<K> l = new ArrayList<>(keySet());

    return Collections.unmodifiableList(l);
  }

  /**
   * Removes the element at the specified index.
   *
   * @param index The index of the object to remove.
   * @return      The previous value coressponding the <code>key</code>, or <code>null</code> if none existed.
   *
   * @exception ArrayIndexOutOfBoundsException if the <code>index</code> is
   * <code>&lt; 0</code> or <code>&gt;</code> the size of the map.
   */
  public Object remove(int index)
  {
    return remove(get(index));
  }

  // per Externalizable.readExternal(ObjectInput)

  /**
   *  Deserializes this map from the given stream.
   *
   *  @param in the stream to deserialize from
   *  @throws IOException if the stream raises it
   *  @throws ClassNotFoundException if the stream raises it
   */
  public void readExternal( ObjectInput in ) 
    throws IOException, ClassNotFoundException 
  {
    int size = in.readInt();    
    for(int i = 0; i < size; i++)  {
      Object key = (K) in.readObject();
      Object value = (V) in.readObject();

      put((K) key, (V) value);
    }
  }
  
  /**
   *  Serializes this map to the given stream.
   *
   *  @param out  the stream to serialize to
   *  @throws IOException  if the stream raises it
   */
  public void writeExternal( ObjectOutput out ) throws IOException {
    out.writeInt(size());
    for(Entry<K, V> pos : orderedEntries) {
      out.writeObject(pos.getKey());
      out.writeObject(pos.getValue());
    }
  }

  // Should this function also move the Entry to the "newest" end of orderedEntries list if not absent?
  @Override
  public V putIfAbsent(K key, V value)
  {
    Entry<K, V> entryToPut = new Entry<>(key, value);
    Entry<K, V> entryPrevious = this.entries.putIfAbsent(key, entryToPut);

    if (entryPrevious == null) {
      // entry was not present, add it to the orderedEntries list
      this.orderedEntries.add(entryToPut);
      return null;
    }
    else {
      return entryPrevious.value;
    }
  }

  @Override
  public boolean remove(Object key, Object value)
  {
    boolean valueIsRemoved = false;

    V valueAtKey = get(key);
    if (value.equals(valueAtKey)) {
      removeImpl(key);
      valueIsRemoved = true;
    }

    return valueIsRemoved;
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue)
  {
    boolean valueIsReplaced = false;

    V valueAtKey = get(key);
    if (valueAtKey.equals(oldValue)) {
      put(key, newValue);
      valueIsReplaced = true;
    }

    return valueIsReplaced;
  }

  @Override
  public V replace(K key, V value)
  {
    V oldValue = null;

    if (containsKey(key)) {
      oldValue = put(key, value);
    }

    return oldValue;
  }

  // add a serial version uid, so that if we change things in the future
  // without changing the format, we can still deserialize properly.
  private static final long serialVersionUID = 3380552487888102931L;
}
