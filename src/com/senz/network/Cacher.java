package com.senz.network;

import java.util.Collection;
import android.util.LruCache;
import com.senz.core.Beacon;
import com.senz.core.BeaconWithSenz;

/***********************************************************************************************************************
 * @ClassName:   Cacher
 * @Author:      zhzhzoo
 * @CommentBy:   Woodie
 * @CommentAt:   Tue, Nov 27, 2014
 * @Reference:   - LruCahche :
 *               A cache that holds strong references to a limited number of values. Each time a value is accessed, it is
 *               moved to the head of queue. When a value is added to a full cache, the value at the end of that queue
 *               is evicted and may become eligible for garbage collection.
 * @Description: This class is similar to a LruCache's subclass. It packages the LruCache's put() and get() for BeaconWithSenz.
 ***********************************************************************************************************************/

public class Cacher {
    private LruCache<Beacon, BeaconWithSenz> mBeaconCache;

    public Cacher(int cacheSize) {
        // Init a LruCache, and its' maxsize is cachesize which defined by user.
        mBeaconCache = new LruCache<Beacon, BeaconWithSenz>(cacheSize);
    }

    public BeaconWithSenz lookupBeacon(Beacon beacon) {
        // Returns the value for key if it exists in the cache or can be created by #create.
        return mBeaconCache.get(beacon);
    }

    public void addBeaconWithSenz(BeaconWithSenz bws) {
        // Caches bws(value) for bws(key).
        mBeaconCache.put(bws, bws);
    }
}
