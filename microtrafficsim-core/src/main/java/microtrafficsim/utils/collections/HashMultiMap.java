package microtrafficsim.utils.collections;

import java.util.*;


public class HashMultiMap<K, V> extends AbstractMultiMap<K, V, Set<V>> {

    public HashMultiMap() {
        super(new HashMap<>(), HashSet<V>::new);
    }

    public HashMultiMap(int initialCapacity) {
        super(new HashMap<>(initialCapacity), HashSet<V>::new);
    }

    public HashMultiMap(int initialCapacity, float loadFactor) {
        super(new HashMap<>(initialCapacity, loadFactor), HashSet<V>::new);
    }

    public HashMultiMap(Map<? extends K, ? extends V> map) {
        super(AbstractMultiMap.baseMap(new HashMap<>(), map, HashSet<V>::new), HashSet<V>::new);
    }

    public HashMultiMap(MultiMap<? extends K, ? extends V, ? extends Collection<V>> map) {
        super(AbstractMultiMap.baseMap(new HashMap<K, Set<V>>(), map, HashSet<V>::new), HashSet<V>::new);
    }
}