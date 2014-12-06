package com.senz.network;

import java.util.Collection;
import com.senz.core.Senz;
import com.senz.core.Beacon;
import com.senz.core.BeaconWithSenz;
import com.senz.network.Cacher;

/***********************************************************************************************************************
 * @ClassName:   Cache
 * @Author:      zhzhzoo
 * @CommentBy:   Woodie
 * @Description: This class provide some static function for operating Cacher.
 *               - checkCacher: check the cache is empty or not.
 *               - lookupBeacon: search BeaconWithSenz in cacher by beacon.
 *               - addBeaconsWithSenz: add BeaconWithSenz into cacher.
 ***********************************************************************************************************************/

public class Cache {
    // It's a LruCache actually.
    static private Cacher cacher;

    static private void checkCacher() {
        if (cacher == null)
            // Cache 300 most visited Beacons by default
            cacher = new Cacher(300);
    }

    static public BeaconWithSenz lookupBeacon(Beacon beacon) {
        Cache.checkCacher();
        return Cache.cacher.lookupBeacon(beacon);
    }

    static public void addBeaconsWithSenz(Collection<BeaconWithSenz> bwss) {
        Cache.checkCacher();
        // Add a list of BeaconWithSenz into cacher.
        for (BeaconWithSenz bws : bwss)
            Cache.cacher.addBeaconWithSenz(bws);
    }
}
