/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashSet<Key> extends AbstractSet<Key> {
    private static final Object NULL = new Object();

    // use write concurrency level 1 (last param) to decrease memory consumption by ConcurrentHashMap
    private ConcurrentHashMap<Key, Object> map = new ConcurrentHashMap<Key, Object>(16, 0.75f, 1);

    /** return true if object was added as "first value" for this key */
    @Override
    public boolean add(Key key) {
        return this.map.put(key, NULL) == null; // null means there was no value for given key previously
    }

    @Override
    public boolean contains(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public Iterator<Key> iterator() {
        return this.map.keySet().iterator();
    }

    /** return true if key was indeed removed */
    @Override
    public boolean remove(Object key) {
        return this.map.remove(key) == NULL; // if value not null it was existing in the map
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public void clear() {
        this.map.clear();
    }
}
