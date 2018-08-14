package com.laoqiu.bglocation;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.app.ActivityManager;

import java.util.List;

public class RNALocationModule extends ReactContextBaseJavaModule {

    // React Class Name as called from JS
    public static final String REACT_CLASS = "RNALocation";
    // Unique Name for Log TAG
    public static final String TAG = RNALocationModule.class.getSimpleName();
    // Save last Location Provided
    private Location mLastLocation;
    private String locationProvider;
    private static final int TIMEOUT = 1000 * 10;

    //The React Native Context
    private ReactApplicationContext reactContext;
    //private Activity currentActivity = null;

    private LocationManager locationManager;
    private boolean isGPSEnabled;
    private boolean isNetworkEnabled;
    private LocationListener networkListener;
    private LocationListener gpsListener;

    // Constructor Method as called in Package
    public RNALocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        // Save Context for later use
        this.reactContext = reactContext;
    }


    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactMethod
    public void LocationListener() {
        //currentActivity = reactContext.getCurrentActivity();
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) reactContext.getSystemService(Context.LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // getting network status
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        // Define a listener that responds to location updates
        networkListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location loc) {
                // Called when a new location is found by the network location provider.
                if (loc != null) {
                    if (isBetterLocation(loc, mLastLocation)) {
                        sendPosition(loc);
                    }
                } else {
                    Log.i(TAG, "unable find location.");
                }
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        gpsListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location loc) {
                // Called when a new location is found by the network location provider.
                if (loc != null) {
                    if (isBetterLocation(loc, mLastLocation)) {
                        sendPosition(loc);
                    }
                } else {
                    Log.i(TAG, "unable find location.");
                }
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        if (isNetworkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
        }
        if (isGPSEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
        }
        mLastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        sendPosition(mLastLocation);

    }

    @ReactMethod
    public void getLastLocation() {
        sendPosition(mLastLocation);
    }

    public void sendPosition(Location loc) {
        mLastLocation = loc;

        try {
            double Longitude;
            double Latitude;
            double Accuracy;
            double Speed;
            int Altitude;

            // Receive Longitude / Latitude from (updated) Last Location
            Longitude = loc.getLongitude();
            Latitude = loc.getLatitude();
            Accuracy = (double) loc.getAccuracy();
            Speed = (double) loc.getSpeed();
            if (loc.hasAltitude()) {
                Altitude = 1;
            } else {
                Altitude = 0;
            }

            // Log.i(TAG, "Got new location. Lng: " +Longitude+" Lat: "+Latitude);
            if (!isAppOnForeground()) {
                // background location task
                Intent service = new Intent(getReactApplicationContext().getApplicationContext(), MyTaskService.class);
                Bundle params = new Bundle();

                params.putDouble("Longitude", Longitude);
                params.putDouble("Latitude", Latitude);
                params.putDouble("Accuracy", Accuracy);
                params.putDouble("Speed", Speed);
                params.putInt("Altitude", Altitude);
                service.putExtras(params);

                // start service
                getReactApplicationContext().getApplicationContext().startService(service);
            } else {
                // Create Map with Parameters to send to JS
                WritableMap params = Arguments.createMap();
                params.putDouble("Longitude", Longitude);
                params.putDouble("Latitude", Latitude);
                params.putDouble("Accuracy", Accuracy);
                params.putDouble("Speed", Speed);
                params.putInt("Altitude", Altitude);

                // Send Event to JS to update Location
                sendEvent(reactContext, "updateLocation", params);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Location services disconnected.");
        }
    }

    /*
     * Internal function for communicating with JS
     */
    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } else {
            Log.i(TAG, "Waiting for CatalystInstance...");
        }
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TIMEOUT;
        boolean isSignificantlyOlder = timeDelta < TIMEOUT;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
        // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 1;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }


    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
          return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private boolean isAppOnForeground() {
        /**
          We need to check if app is in foreground otherwise the app will crash.
         http://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
        **/
        ActivityManager activityManager = (ActivityManager) reactContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = 
        activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = reactContext.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == 
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
             appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
