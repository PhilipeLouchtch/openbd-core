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
 * that uses com.nary.util.HashMap for storing entries (instead of java.util.Map)
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
  private static class Entry<K, V> implements Map.Entry<K, V> {
    // Note: This class cannot easily be made clonable.  While the actual
    // implementation of a clone would be simple, defining the semantics is
    // difficult.  If a shallow clone is implemented, then entry.next.prev !=
    // entry, which is unintuitive and probably breaks all sorts of assumptions
    // in code that uses this implementation.  If a deep clone is
    // implementated, then what happens when the linked list is cyclical (as is
    // the case with SequencedHashMap)?  It's impossible to know in the clone
    // when to stop cloning, and thus you end up in a recursive loop,
    // continuously cloning the "next" in the list.

    private final K key;
    private V value;

    public Entry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    // per Map.Entry.getKey()
    public K getKey() {
      return this.key;
    }

    // per Map.Entry.getValue()
    public V getValue() {
      return this.value;
    }

    // per Map.Entry.setValue()
    public V setValue(V value) {
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    public int hashCode() {
      // implemented per api docs for Map.Entry.hashCode()
      return ((getKey() == null ? 0 : getKey().hashCode()) ^
              (getValue()==null ? 0 : getValue().hashCode()));
    }

    public boolean equals(Object obj) {
      if(obj == null) return false;
      if(obj == this) return true;
      if(!(obj instanceof Map.Entry)) return false;

      Map.Entry other = (Map.Entry)obj;

      // implemented per api docs for Map.Entry.equals(Object)
      return((getKey() == null ?
              other.getKey() == null :
              getKey().equals(other.getKey()))  &&
             (getValue() == null ?
              other.getValue() == null :
              getValue().equals(other.getValue())));
    }
    public String toString() {
      return "[" + getKey() + "=" + getValue() + "]";
    }
  }

  /**
   *  Construct an empty sentinel used to hold the head (sentinel.next) and the
   *  tail (sentinel.prev) of the list.  The sentinel has a <code>null</code>
   *  key and value.
   **/
//  private static<KType, VType> Entry<KType, VType> createSentinel() {
//    Entry s = new Entry<KType, VType>(null, null);
//    s.prev = s;
//    return s;
//  }
//
//  /**
//   *  Sentinel used to hold the head and tail of the list of entries.
//   **/
//  private Entry<K, V> sentinel;

  List<Entry<K, V>> orderedEntries = new LinkedList<>();

  /**
   *  Map of keys to entries
   **/
  private FastMap<K, Entry<K, V>> entries;

  /**
   *  Holds the number of modifications that have occurred to the map,
   *  excluding modifications made through a collection view's iterator
   *  (e.g. entrySet().iterator().remove()).  This is used to create a
   *  fail-fast behavior with the iterators.
   **/
  private transient long modCount = 0;

  /**
   *  Construct a new sequenced hash map with default initial size and load
   *  factor and case-sensitive keys.
   **/
  public SequencedHashMap() {
    entries = new FastMap<K, Entry<K, V>>();
  }

	/**
	 *  Construct a new sequenced hash map with default initial size and load
	 *  factor and specified case-sensitivity.
	 **/
	public SequencedHashMap( boolean caseSensitive ) {
		entries = new FastMap<K, Entry<K, V>>( caseSensitive );
	}

  /**
   *  Construct a new sequenced hash map with the specified initial size and
   *  default load factor and case-sensitive keys.
   *
   *  @param initialSize the initial size for the hash table 
   *
   *  @see FastMap#FastMap(int)
   **/
  public SequencedHashMap(int initialSize) {
    entries = new FastMap<K, Entry<K, V>>(initialSize);
  }

  /**
   *  Construct a new sequenced hash map and add all the elements in the
   *  specified map.  The order in which the mappings in the specified map are
   *  added is defined by {@link #putAll(Map)}.  
   **/
  public SequencedHashMap(Map m) {
    this();
    putAll(m);
  }
  
	public SequencedHashMap( Map m, boolean caseSensitive ) {
		this( caseSensitive );
		putAll( m );
	}
	
	public boolean isCaseSensitive() {
		return entries.isCaseSensitive();
	}

  /**
   *  Removes an internal entry from the linked list.  This does not remove
   *  it from the underlying map.
   **/
  private void removeEntry(Entry entry) {
    orderedEntries.remove(entry);
  }

  /**
   *  Inserts a new internal entry to the tail of the linked list.  This does
   *  not add the entry to the underlying map.
   **/
  private void insertEntry(Entry<K, V> entry) {
    orderedEntries.add(entry);
  }

  // per Map.size()

  /**
   *  Implements {@link Map#size()}.
   */
  public int size() {
    // use the underlying Map's size since size is not maintained here.
    return entries.size();
  }

  /**
   *  Implements {@link Map#isEmpty()}.
   */
  public boolean isEmpty() {
    // for quick check whether the map is empty, we can check the linked list
    // and see if there's anything in it.
    return orderedEntries.isEmpty();
  }

  /**
   *  Implements {@link Map#containsKey(Object)}.
   */
  public boolean containsKey(Object key) {
    // pass on to underlying map implementation
    return entries.containsKey(key);
  }

  /**
   *  Implements {@link Map#containsValue(Object)}.
   */
  public boolean containsValue(Object value) {
    // unfortunately, we cannot just pass this call to the underlying map
    // because we are mapping keys to entries, not keys to values.  The
    // underlying map doesn't have an efficient implementation anyway, so this
    // isn't a big deal.

    // do null comparison outside loop so we only need to do it once.  This
    // provides a tighter, more efficient loop at the expense of slight
    // code duplication.
    if(value == null) {
      for(Entry<K, V> pos : orderedEntries) {
        if(pos.getValue() == null) return true;
      } 
    } else {
      for(Entry<K, V> pos : orderedEntries) {
        if(value.equals(pos.getValue())) return true;
      }
    }
    return false;      
  }

  /**
   *  Implements {@link Map#get(Object)}.
   */
  public V get(Object o) {
    // find entry for the specified key object
    Entry<K, V> entry = entries.get((K) o);
    if(entry == null) return null;
      
    return entry.getValue();
  }

  /**
   *  Return the entry for the "oldest" mapping.  That is, return the Map.Entry
   *  for the key-value pair that was first put into the map when compared to
   *  all the other pairings in the map.  This behavior is equivalent to using
   *  <code>entrySet().iterator().next()</code>, but this method provides an
   *  optimized implementation.
   *
   *  @return The first entry in the sequence, or <code>null</code> if the
   *  map is empty.
   **/
  public Map.Entry<K, V> getFirst() {
    // sentinel.next points to the "first" element of the sequence -- the head
    // of the list, which is exactly the entry we need to return.  We must test
    // for an empty list though because we don't want to return the sentinel!
    return isEmpty() ? null : orderedEntries.get(0);
  }

  /**
   *  Return the key for the "oldest" mapping.  That is, return the key for the
   *  mapping that was first put into the map when compared to all the other
   *  objects in the map.
   *
   *  @return The first key in the sequence, or <code>null</code> if the
   *  map is empty.
   **/
  public K getFirstKey() {
    return isEmpty() ? null : getFirst().getKey();
  }

  /**
   *  Return the value for the "oldest" mapping.  That is, return the value for
   *  the mapping that was first put into the map when compared to all the
   *  other objects in the map.
   *
   *  @return The first value in the sequence, or <code>null</code> if the
   *  map is empty.
   **/
  public V getFirstValue() {
    return isEmpty() ? null : getFirst().getValue();
  }

  /**
   *  Return the entry for the "newest" mapping.  That is, return the Map.Entry
   *  for the key-value pair that was first put into the map when compared to
   *  all the other pairings in the map.
   *
   *  @return The last entry in the sequence, or <code>null</code> if the map
   *  is empty.
   **/
  public Map.Entry<K, V> getLast() {
    // sentinel.prev points to the "last" element of the sequence -- the tail
    // of the list, which is exactly the entry we need to return.  We must test
    // for an empty list though because we don't want to return the sentinel!
    return isEmpty() ? null : getLast();
  }

  /**
   *  Return the key for the "newest" mapping.  That is, return the key for the
   *  mapping that was last put into the map when compared to all the other
   *  objects in the map.
   *
   *  @return The last key in the sequence, or <code>null</code> if the map is
   *  empty.
   **/
  public K getLastKey() {
    return isEmpty() ? null : getLast().getKey();
  }

  /**
   *  Return the value for the "newest" mapping.  That is, return the value for
   *  the mapping that was last put into the map when compared to all the other
   *  objects in the map.
   *
   *  @return The last value in the sequence, or <code>null</code> if the map
   *  is empty.
   **/
  public V getLastValue() {
    return isEmpty() ? null : getLast().getValue();
  }

  /**
   *  Implements {@link Map#put(Object, Object)}.
   */
  @Override
  public V put(K key, V value) {
    modCount++;

    V oldValue = null;

    // lookup the entry for the specified key
    Entry<K, V> e = entries.get(key);

    // check to see if it already exists
    if(e != null) {
      // remove from list so the entry gets "moved" to the end of list
      removeEntry(e);

      // update value in map
      oldValue = e.setValue(value);

      // Note: We do not update the key here because its unnecessary.  We only
      // do comparisons using equals(Object) and we know the specified key and
      // that in the map are equal in that sense.  This may cause a problem if
      // someone does not implement their hashCode() and/or equals(Object)
      // method properly and then use it as a key in this map.  
    } else {
      // add new entry
      e = new Entry<K, V>(key, value);
      entries.put(key, e);
    }
    // assert(entry in map, but not list)

    // add to list
    insertEntry(e);

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
    if(e == null) return null;
    modCount++;
    removeEntry(e);
    return e;
  }

  /**
   *  Adds all the mappings in the specified map to this map, replacing any
   *  mappings that already exist (as per {@link Map#putAll(Map)}).  The order
   *  in which the entries are added is determined by the iterator returned
   *  from {@link Map#entrySet()} for the specified map.
   *
   *  @param t the mappings that should be added to this map.
   *
   *  @exception NullPointerException if <code>t</code> is <code>null</code>
   **/
  public void putAll(Map<? extends K, ? extends V> t) {
    for (Map.Entry<? extends K, ? extends V> entry : t.entrySet())
    {
      put(entry.getKey(), entry.getValue());
    }
  }

  /**
   *  Implements {@link Map#clear()}.
   *  NOTE: Not thread-safe
   */
  public void clear() {
    modCount++;

    // remove all from the underlying map
    entries.clear();
    orderedEntries.clear();
  }

  /**
   *  Implements {@link Map#equals(Object)}.
   */
  public boolean equals(Object obj) {
    if(obj == null) return false;
    if(obj == this) return true;

    if(!(obj instanceof Map)) return false;

    return entrySet().equals(((Map)obj).entrySet());
  }

  /**
   *  Implements {@link Map#hashCode()}.
   */
  public int hashCode() {
    return entrySet().hashCode();
  }

  /**
   *  Provides a string representation of the entries within the map.  The
   *  format of the returned string may change with different releases, so this
   *  method is suitable for debugging purposes only.  If a specific format is
   *  required, use {@link #entrySet()}.{@link Set#iterator() iterator()} and
   *  iterate over the entries in the map formatting them as appropriate.
   **/
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

  /**
   *  Implements {@link Map#keySet()}.
   */
  // TODO: Type Safe iterator
  public Set<K> keySet() {
    return new AbstractSet<K>() {

      // required impls
      public Iterator iterator() { return new OrderedIterator(KEY); }
      public boolean remove(Object o) {
        Entry e = SequencedHashMap.this.removeImpl(o);
        return (e != null);
      }

      // more efficient impls than abstract set
      public void clear() { 
        SequencedHashMap.this.clear(); 
      }
      public int size() { 
        return SequencedHashMap.this.size(); 
      }
      public boolean isEmpty() { 
        return SequencedHashMap.this.isEmpty(); 
      }
      public boolean contains(Object o) { 
        return SequencedHashMap.this.containsKey(o);
      }

    };
  }

  /**
   *  Implements {@link Map#values()}.
   */
  // TODO: Type Safe iterator
  public Collection values() {
    return new AbstractCollection() {
      // required impl
      public Iterator iterator() { return new OrderedIterator(VALUE); }
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
      public void clear() { 
        SequencedHashMap.this.clear(); 
      }
      public int size() { 
        return SequencedHashMap.this.size(); 
      }
      public boolean isEmpty() { 
        return SequencedHashMap.this.isEmpty(); 
      }
      public boolean contains(Object o) {
        return SequencedHashMap.this.containsValue(o);
      }
    };
  }

  /**
   *  Implements {@link Map#entrySet()}.
   */
  // TODO: Type safe
  public Set entrySet() {
    return new AbstractSet() {
      // helper
      private Entry findEntry(Object o) {
        if(o == null) return null;
        if(!(o instanceof Map.Entry)) return null;
        
        Map.Entry e = (Map.Entry)o;
        Entry<K, V> entry = entries.get(e.getKey());
        if(entry != null && entry.equals(e)) return entry;
        else return null;
      }

      // required impl
      public Iterator iterator() { 
        return new OrderedIterator(ENTRY); 
      }
      public boolean remove(Object o) {
        Entry e = findEntry(o);
        if(e == null) return false;

        return SequencedHashMap.this.removeImpl(e.getKey()) != null;
      }        

      // more efficient impls than abstract collection
      public void clear() { 
        SequencedHashMap.this.clear(); 
      }
      public int size() { 
        return SequencedHashMap.this.size(); 
      }
      public boolean isEmpty() { 
        return SequencedHashMap.this.isEmpty(); 
      }
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
   * Creates a shallow copy of this object, preserving the internal structure
   * by copying only references.  The keys and values themselves are not
   * <code>clone()</code>'d.  The cloned object maintains the same sequence.
   *
   * @return A clone of this instance.  
   *
   * @exception CloneNotSupportedException if clone is not supported by a
   * subclass.
   */
  public Object clone () throws CloneNotSupportedException {
    // yes, calling super.clone() silly since we're just blowing away all
    // the stuff that super might be doing anyway, but for motivations on
    // this, see:
    // http://www.javaworld.com/javaworld/jw-01-1999/jw-01-object.html
    SequencedHashMap<K, V> map = (SequencedHashMap<K, V>) super.clone();

    // create a new, empty entry map
    // note: this does not preserve the initial capacity and load factor.
    map.entries = new FastMap<>();
      
    // add all the mappings
    map.putAll(this);

    // copy the ordering
    map.orderedEntries.addAll(this.orderedEntries);

    // Note: We cannot just clone the hashmap and sentinel because we must
    // duplicate our internal structures.  Cloning those two will not clone all
    // the other entries they reference, and so the cloned hash map will not be
    // able to maintain internal consistency because there are two objects with
    // the same entries.  See discussion in the Entry implementation on why we
    // cannot implement a clone of the Entry (and thus why we need to recreate
    // everything).

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
  public V getValue (int index)
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
  public Iterator iterator()
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
  public List sequence()
  {
    List<K> l = new ArrayList<>(keySet());

    return Collections.unmodifiableList(l);
  }

  /**
   * Removes the element at the specified index.
   *
   * @param index The index of the object to remove.
   * @return      The previous value coressponding the <code>key</code>, or
   *              <code>null</code> if none existed.
   *
   * @exception ArrayIndexOutOfBoundsException if the <code>index</code> is
   * <code>&lt; 0</code> or <code>&gt;</code> the size of the map.
   */
  public Object remove (int index)
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

  // add a serial version uid, so that if we change things in the future
  // without changing the format, we can still deserialize properly.
  private static final long serialVersionUID = 3380552487888102930L;

}
