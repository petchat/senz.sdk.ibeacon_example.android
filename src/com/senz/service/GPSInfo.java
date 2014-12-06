package com.senz.service;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import com.senz.utils.L;

/***********************************************************************************************************************
 * @ClassName:   GPSInfo
 * @Author:      zhzhzoo
 * @CommentBy:   Woodie
 * @CommentAt:   Thur, Nov 6, 2014
 * @Description: It's a GPS manager.
 *               - First, it inits a location manager.
 *               - Second, it inits a provider with the location manager.
 *               - Third, it starts Location Service(Listening GPS) and report current location info once immediatelly.
 *               - When Location is changed, It will report current location.
 ***********************************************************************************************************************/

public class GPSInfo {

	public static final String TAG = GPSInfo.class.getSimpleName();
	
	private LocationManager locationManager;
	private String provider;
	private GPSInfoListener GPSListener;

    // Init GPS provider.
	public GPSInfo(Context ctx) {
        if (ctx == null) {
            L.d("yes");
        }
        // Init locationManager.
		locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        // Init provider with locationManager.
		provider = selectProvider();
	}

    // It's the user - interface to start listening GPS.
	public void start(GPSInfoListener ltn) {
		GPSListener = ltn;
        // Trigger the callback which defined by user named GPSInfoListener.onGPSInfoChanged()
        notifyAbout(locationManager.getLastKnownLocation(provider));
        // update once at a highest rate.
		locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
	}

    // Stop listening location update.
	public void end() {
		locationManager.removeUpdates(locationListener);
	}

	private String selectProvider() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		return locationManager.getBestProvider(criteria, true);
	}

    // It's a interface for user to define.
    // The method in following interface will be triggered in different event.
	private final LocationListener locationListener = new LocationListener () {
		// If location changed , it will call notifyAbout().(Actually is onGPSInfoChanged)
		@Override
		public void onLocationChanged(Location location) {
            notifyAbout(location);
		}
		
		@Override
		public void onProviderDisabled(String provider) {
		}
		
		@Override
		public void onProviderEnabled(String provider) {
		}
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

    // It will be called when
    // - the GPS started.
    // - the Location is changed
	private void notifyAbout(Location location) {
		GPSListener.onGPSInfoChanged(location);
	}

    // It's a user - interface to define callback.
    public interface GPSInfoListener {
        public void onGPSInfoChanged(Location location);
    }
}
