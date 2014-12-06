package com.senz.network;

import android.location.Location;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;
import java.io.IOException;

import com.senz.core.*;
import com.senz.network.Cache;
import com.senz.network.Network;
import com.senz.service.DeviceInfo;
import com.senz.utils.Asyncfied;
import com.senz.utils.L;


/***********************************************************************************************************************
 * @ClassName:   Query
 * @Author:      zhzhzoo
 * @CommentBy:   Woodie
 * @CommentAt:   Tue, Oct 27, 2014
 * @Description: Query provide a framework that query for senz info from AvosCloud Server.
 *               - First, it provide a callback for user to decide how to handle their query result.
 *               - Second, it call the Network module to query.
 *               - Last but not least, it provide a cacher to cache the senz info, it's aim is to reduce the query times.
 ***********************************************************************************************************************/

public class Query {
    static private <T> void addAllToHashSet(Collection<T> c, HashSet<T> hs) {
        for (T t : c)
            hs.add(t);
    }

    // This function provide location and callback, and it will query on server, then return a result about senz.
    static public void senzesFromLocationAsync(final Location location, final SenzReadyCallback cb, final ErrorHandler eh) {
        // It's a Class that can make your defined callbacks running in an defined order.
        Asyncfied.runAsyncfiable(new Asyncfied.Asyncfiable<ArrayList<Senz>>() {
            // First, it will try runAndReturn first, and get a return value(this value will be passed to onReturn())
            // - senzesFromLocation: send location to AVOSCloud Server to get Senz info back.
            @Override
            public ArrayList<Senz> runAndReturn() throws IOException {
                L.i("query running for locations");
                //return senzesFromLocation(location);
                return Network.queryLocation(location);
            }
            // Finally, it will handle the result which is returned by runAndReturn()
            // - onSenzReady: It is defined by user, here we can use it to get senz result.
            @Override
            public void onReturn(ArrayList<Senz> result) {
                L.i("query returnning");
                //L.i(" - query senz: " + result.toString());
                cb.onSenzReady(result);
            }
            // If it throw an error, it will run this.
            @Override
            public void onError(Exception e) {
                L.e("query location error");
                eh.onError(e);
            }
        });
    }

    // It seems like senzesFromLocationAsync. No more comment.
    // The only difference is runAndReturn(), it will run senzesFromBeacons().
    static public void senzesFromBeaconsAsync(final Collection<Beacon> beacons, final Location location, final SenzReadyCallback cb, final ErrorHandler eh) {
        Asyncfied.runAsyncfiable(new Asyncfied.Asyncfiable<ArrayList<Senz>>() {
            @Override
            public ArrayList<Senz> runAndReturn() throws IOException {
                L.i("query running for beacons");
                //return senzesFromBeacons(beacons, location);
                return Network.queryBeacons(beacons, location);
            }

            @Override
            public void onReturn(ArrayList<Senz> result) {
                L.i("query returnning");
                //L.i(" - query senz: " + result.toString());
                cb.onSenzReady(result);
            }

            @Override
            public void onError(Exception e) {
                L.e("query beacons error");
                eh.onError(e);
            }
        });
    }

    // It seems like senzesFromLocationAsync. No more comment.
    // The only difference is runAndReturn(), it will run senzesFromBeacons().
    static public void staticInfoFromBasicInfoAsync(final Collection<App> apps, final DeviceInfo device, final StaticInfoReadyCallback cb, final ErrorHandler eh) {
        Asyncfied.runAsyncfiable(new Asyncfied.Asyncfiable<StaticInfo>() {
            @Override
            public StaticInfo runAndReturn() throws IOException {
                L.i("query running for basic info");
                //return senzesFromBeacons(beacons, location);
                //return Network.queryBeacons(beacons, location);
                return Network.queryBasicInfo(apps, device);
            }

            @Override
            public void onReturn(StaticInfo result) {
                L.i("query returnning");
                //L.i(" - query senz: " + result.toString());
                cb.onStaticInfoReady(result);
            }

            @Override
            public void onError(Exception e) {
                L.e("query beacons error");
                eh.onError(e);
            }
        });
    }

    // It's an interface for user to define callback, when avoscloud server return the senz info.
    public interface SenzReadyCallback {
        public void onSenzReady(ArrayList<Senz> senzes);
    }
    // It's an interface for user to define callback, when avoscloud server return the static info.
    public interface StaticInfoReadyCallback {
        public void onStaticInfoReady(StaticInfo staticinfo);
    }
    // It's an interface for user to define callback, when send query request throw a error.
    public interface ErrorHandler {
        public void onError(Exception e);
    }

    /*static public ArrayList<Senz> senzesFromBeacons(Collection<Beacon> beacons, Location lastBeen) throws IOException {
        // Init a HashSet to store senzes' info.
        HashSet<Senz> result = new HashSet<Senz>();
        ArrayList<Beacon> toQueryServer = new ArrayList<Beacon>();
        // Search BeaconWithSenz in cacher by a list of beacons
        for (Beacon beacon : beacons) {
            BeaconWithSenz bws = Cache.lookupBeacon(beacon);
            // If the beacon's bws is not exist in cacher, then add this beacon to an arraylist of Beacons.
            if (bws == null)
                toQueryServer.add(beacon);
            // If the bws is exist in cacher, then add this bws's senz info into hashset.
            else
                result.add(bws.getSenz());
        }
        // Query on server with those Beacons(their bws is not exist in cacher).
        Collection<BeaconWithSenz> bwss = Network.queryBeacons(toQueryServer, lastBeen);
        // Add the query result into cacher.
        Cache.addBeaconsWithSenz(bwss);
        // Add the query result into hashset.
        for (BeaconWithSenz bws : bwss)
            result.add(bws.getSenz());

        // return the hashset.
        return new ArrayList<Senz>(result);
    }*/

    /*static public ArrayList<Senz> senzesFromLocation(Location location) throws IOException {
        // Init a HashSet to store senz' info.
        HashSet<Senz> result = new HashSet<Senz>();
        // Query on server with those Locations.
        ArrayList<BeaconWithSenz> bwss = Network.queryLocation(location);
        Cache.addBeaconsWithSenz(bwss);
        for (BeaconWithSenz bws : bwss)
            result.add(bws.getSenz());
        return new ArrayList<Senz>(result);
    }*/
}
