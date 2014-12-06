package com.senz.service;

import android.app.AlarmManager;
import android.app.Service;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Message;
import android.os.Messenger;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.Parcel;
import android.os.Looper;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Bundle;
import android.os.SystemClock;
import android.location.Location;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.ArrayList;

import android.text.format.Time;
import com.senz.core.StaticInfo;
import com.senz.service.TelepathyPeriod;
import com.senz.service.GPSInfo;
import com.senz.network.Query;
import com.senz.utils.FixedQueue;
import com.senz.utils.L;
import com.senz.core.Beacon;
import com.senz.core.Senz;
import com.senz.core.BeaconWithSenz;
import com.senz.utils.Writer;

/***********************************************************************************************************************
 * @ClassName:   SenzService
 * @Author:      zhzhzoo
 * @CommentBy:   Woodie
 * @CommentAt:   Mon, Oct 27, 2014
 * @Description: It is a *Service* to communicate with SenzManager. It will be instantiated after binding service.
 *               - Defined Intent (It will be sent to broadcast receiver), Pending Intent (It will wrapper the Intent),
 *                         Broadcast Receiver (It will receive the notification, and call onReceive())
 *               -
 *               -
 ***********************************************************************************************************************/
public class SenzService extends Service {

    // The definition of Message type
    public static final int MSG_START_TELEPATHY = 1;
    public static final int MSG_STOP_TELEPATHY = 2;
    public static final int MSG_SENZ_RESPONSE = 3;
    public static final int MSG_STATICINFO_RESPONSE = 6;
    public static final int MSG_ERROR_RESPONSE = 4;
    public static final int MSG_SET_SCAN_PERIOD = 5;

    // Sensor Manager
    private SensorInfo mSensorInfo;
    // Device Info
    private DeviceInfo mDeviceInfo;
    // App Info
    private AppInfo mAppInfo;

    //Writer
    private Writer gyroWriter = null;
    private Writer acceWriter = null;
    private Writer lightWriter = null;


    // The Intent wrappered by following PendingIntent.
    private static final Intent START_SCAN_INTENT = new Intent("startScan");
    private static final Intent AFTER_SCAN_INTENT = new Intent("afterScan");
    private static final Intent LOOK_NEARBY_INTENT = new Intent("lookNearby");

    // *PendingIntent* can be seen as an Intent object wrapper.
    // This class is used to handle things about to happen.
    // It is usually obtained through :
    //     *getActivity*, *getBroadcast* or *getService*
    // of the instance pendingintent
    private PendingIntent mStartScanBroadcastPendingIntent;
    private PendingIntent mAfterScanBroadcastPendingIntent;
    private PendingIntent mLookNearbyBroadcastPendingIntent;

   /*
    * About *Broadcast Receiver*
    * Broadcast Receiver uses for receiving and processing a broadcast notification. Most of the broadcast
    * is a system initiated, such as geographic transformation, lack of electricity, the call letters and the
    * like. A program may also broadcast notification. Program can have any number of broadcast receivers respond
    * to the notification that it thinks important.
    * Typically an application or system of our own in certain events (the battery is low, the calls to SMS)
    * will broadcast an Intent, we can use the Broadcast Receiver register an Intent to listen to them and get
    * the data of Intent.
    */
    // They are used to receiving the notification (We have defined those notifications, actually the notification is a intent)
    // If they receive a notification(intent), they will call function - onReceive().
    // - TIPS: Receiver is active only while onReceive running. It is not inactive until onReceive return.
    //         So, some time-cosuming operation should be in another single thread.(This operation should be done by a
    //         Service)
    private BroadcastReceiver mBluetoothBroadcastReceiver;
    private BroadcastReceiver mStartScanBroadcastReceiver;
    private BroadcastReceiver mAfterScanBroadcastReceiver;
    private BroadcastReceiver mLookNearbyBroadcastReceiver;

    private final Messenger mMessenger;
    private final BluetoothAdapter.LeScanCallback mLeScanCallback;
    private Messenger mReplyTo;
    private AlarmManager mAlarmManager;
    private BluetoothAdapter mAdapter;

    private ConcurrentHashMap<Beacon, Boolean> mBeaconsInACycle;
    private ConcurrentHashMap<Beacon, Boolean> mBeaconsNearBy;
    private TelepathyPeriod mTelepathyPeriod;
    private Runnable mAfterScanTask;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mScanning;
    private boolean mStarted;
    private GPSInfo mGPSInfo;
    private Location mLocation;
    private GPSInfo.GPSInfoListener mGPSInfoListener;
    private SensorInfo.SensorHandler mSensorHandler;

    public SenzService() {
        // Instantiation of Messenger which used to communicate with SenzManager.
        this.mMessenger       = new Messenger(new IncomingHandler());
        this.mLeScanCallback  = new InternalLeScanCallback();
        this.mBeaconsInACycle = new ConcurrentHashMap();
        this.mBeaconsNearBy   = new ConcurrentHashMap();
        this.mTelepathyPeriod = new TelepathyPeriod(TimeUnit.SECONDS.toMillis(1L),
                                                    TimeUnit.SECONDS.toMillis(0L),
                                                    TimeUnit.MINUTES.toMillis(30L));
        this.mStarted         = this.mScanning = false;
        // GPS listener defined by user. It's method will be invoked when location info is changed.
        this.mGPSInfoListener = new InternalGPSInfoListener();
        // Sensor Handler defined by user. It's methods will be invoked when Sensor info is changed.
        this.mSensorHandler   = new InternalSensorHandler();
    }

    public void onCreate() {
        super.onCreate();

        L.i("Creating service ...");

        this.mGPSInfo = new GPSInfo(this);
        // Get the device info.
        this.mDeviceInfo = new DeviceInfo(this);
        // Get the app list in the device.
        this.mAppInfo = new AppInfo(this);
        // Instantiation of AlarmManager.
        // - It broadcasts a notification after a setting time if you call set().
        this.mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // Instantiation of BluetoothManager.
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.mAdapter = bluetoothManager.getAdapter();

        // Instantiation of interface - Runnable
        this.mAfterScanTask = new AfterScanTask();
        this.mHandlerThread = new HandlerThread("SenzServiceThread", Process.THREAD_PRIORITY_BACKGROUND);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());

        // Instantiation of Broadcast Receiver
        // It define how to respond notice when Broadcast Receiver receive a notice
        this.mBluetoothBroadcastReceiver = createBluetoothBroadcastReceiver();
        this.mStartScanBroadcastReceiver = createStartScanBroadcastReceiver();
        this.mAfterScanBroadcastReceiver = createAfterScanBroadcastReceiver();
        this.mLookNearbyBroadcastReceiver = createLookNearbyBroadcastReceiver();

        // Register the Broadcast Receiver.
        // It binds Intent(notification) to Broadcast Receiver.
        registerReceiver(this.mBluetoothBroadcastReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
        registerReceiver(this.mStartScanBroadcastReceiver, new IntentFilter("startScan"));
        registerReceiver(this.mAfterScanBroadcastReceiver, new IntentFilter("afterScan"));
        registerReceiver(this.mLookNearbyBroadcastReceiver, new IntentFilter("lookNearby"));

        // Instantiation of PendingIntent (We call getBroadcast() to instantiate the PendingIntent).
        // - PendingIntent is instantiated by calling getActivity, getBroadcast, or getService.
        // - Because PendingIntent has current app's context, external app can also run the Intent which is wrappered in
        //   PendingIntent just like current app does even if the current app is not exist.
        this.mStartScanBroadcastPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 233, START_SCAN_INTENT, 0);
        this.mAfterScanBroadcastPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 233, AFTER_SCAN_INTENT, 0);
        this.mLookNearbyBroadcastPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 233, LOOK_NEARBY_INTENT, 0);

        // GPS start listening.
        this.mGPSInfo.start(this.mGPSInfoListener);

        // Sensor
        this.mSensorInfo = new SensorInfo(this,this.mSensorHandler);

        askUserStaticInfo();
        // Writer
        //this.gyroWriter = new Writer("gyro.txt");
        //this.acceWriter = new Writer("acce.txt");
        //this.gyroWriter.writeFileSdcard("{");
        //this.acceWriter.writeFileSdcard("{");
    }

    public void onDestroy() {

        L.i("Destroying service ...");

        // Unregister BroadcastReceiver
        unregisterReceiver(this.mBluetoothBroadcastReceiver);
        unregisterReceiver(this.mStartScanBroadcastReceiver);
        unregisterReceiver(this.mAfterScanBroadcastReceiver);
        unregisterReceiver(this.mLookNearbyBroadcastReceiver);

        if (this.mAdapter != null) {
            stopScanning();
        }

        // Write file over.
        //this.acceWriter.writeFileSdcard("}");
        //this.gyroWriter.writeFileSdcard("}");
        // GPS stop listening.
        this.mGPSInfo.end();

        // Sensor stop listening.
        this.mSensorInfo.unregisterSensor();

        this.mHandlerThread.quit();

        super.onDestroy();
    }

   /*
    * @Function:    < onBind >
    * @CommentBy:   Woodie
    * @CommentAt:   Thur, Oct 30, 2014
    * @Description: If SenzManager bindService, then this function will be invoked.
    *               It will return an IBinder to Client(SenzManager)'s onServiceConnected(), so Client can instantiate a
    *               Massenger. to communicate with SenzService.
    */
    public IBinder onBind(Intent intent) {
        return this.mMessenger.getBinder();
    }

    // Ask for user's static info
    private void askUserStaticInfo() {
        L.i("Ask for User's Static infomation");
        final Message response = Message.obtain(null, MSG_STATICINFO_RESPONSE);
        // It will send query request to avoscloud.
        // - send:    Location (got by GPS)
        // - receive: Senz Info (from AVOSCloud Server)
        Query.staticInfoFromBasicInfoAsync(
            mAppInfo.appList,
            mDeviceInfo,
            new Query.StaticInfoReadyCallback() {
                @Override
                public void onStaticInfoReady(StaticInfo staticinfo) {
                    // put senz info into msg which will be sent back to SenzManager.
                    response.getData().putParcelable("staticinfo", staticinfo);
                    try {
                        // Send msg back to SenzManager.
                        L.i("Static info query complete");
                        mReplyTo.send(response);
                    }
                    catch (RemoteException e) {
                        L.e("Error while delivering responses", e);
                    }
                }
            },
            new Query.ErrorHandler() {
                @Override
                public void onError(Exception e) {

                }
            }
        );
    }

   /*
    * @Function:    < Broadcast Notification(Intent) >
    * @CommentBy:   Woodie
    * @CommentAt:   Thur, Oct 30, 2014
    * @Description: Following functions are used to broadcast a various of Notifications to Broadcast Receiver.
    */
    // Broadcast AfterScan notification
    private void startScanning() {
        //L.d("Start Scanning");
        if (this.mScanning) {
            L.d("Scanning already in progress, not starting another");
            return;
        }
        if (!this.mStarted)
            return;
        if (!this.mAdapter.isEnabled()) {
            // TODO: tell manager about the exception
            return;
        }
        if (!this.mAdapter.startLeScan(this.mLeScanCallback)) {
            // TODO: tell manager about the exception
            return;
        }
        this.mScanning = true;
        removeAllCallbacks();
        //L.d("[SET ALARM] startscanning");
        setAlarm(this.mAfterScanBroadcastPendingIntent, this.mTelepathyPeriod.scanMillis);
    }

    // Broadcast LookNearby notification
    private void lookNearby() {
        L.i("Look for nearby GPS Location");
        // Cancel LookNearby Intent which already exists.
        this.mAlarmManager.cancel(this.mLookNearbyBroadcastPendingIntent);

        final Message response = Message.obtain(null, MSG_SENZ_RESPONSE);
        // It will send query request to avoscloud.
        // - send:    Location (got by GPS)
        // - receive: Senz Info (from AVOSCloud Server)
        Query.senzesFromLocationAsync(
            // Location is a useful para, others are useless.(It will pass to Asyncfied.runAsyncfiable)
            this.mLocation,
            // Defined operation after Asyncfied return.(It will pass to Asyncfied.runAsyncfiable)
            new Query.SenzReadyCallback() {
                // Here the para senzes is the result which is got back from avoscloud server.
                @Override
                public void onSenzReady(ArrayList<Senz> senzes) {
                    // put senz info into msg which will be sent back to SenzManager.
                    response.getData().putParcelableArrayList("senzes", senzes);
                    try {
                        // Send msg back to SenzManager.
                        L.i("Location query complete, got " + senzes.size() + " senzes");
                        mReplyTo.send(response);
                    }
                    catch (RemoteException e) {
                        L.e("Error while delivering responses", e);
                    }
                }
            },
            // Defined operation when throw an error.(It will pass to Asyncfied.runAsyncfiable)
            new Query.ErrorHandler() {
                // on error, wait 1 min then query again
                @Override
                public void onError(Exception e) {
                    //L.i("[SET ALARM] looknearby");
                    //setAlarm(SenzService.this.mLookNearbyBroadcastPendingIntent, TimeUnit.MINUTES.toMillis(1));
                }
            });
    }

    private class AfterScanTask implements Runnable {
        @Override
        public void run() {
            // Stop Scanning Bluetooth.
            SenzService.this.stopScanning();

            ArrayList<Beacon> beacons = new ArrayList<Beacon>();
            final Message response = Message.obtain(null, MSG_SENZ_RESPONSE);
            for (Map.Entry<Beacon, Boolean> e : SenzService.this.mBeaconsInACycle.entrySet())
                beacons.add(e.getKey());
            if(beacons.size() > 0) {
                // It will send query request to avoscloud.
                // - send:    Location (got by GPS), beacons (got by bluetooth)
                // - receive: Senz Info (from AVOSCloud Server)
                Query.senzesFromBeaconsAsync(
                        // It's a para that is sent to avoscloud server
                        beacons,
                        // It's a para that is sent to avoscloud server
                        SenzService.this.mLocation,
                        // Defined operation after Asyncfied return.(It will pass to Asyncfied.runAsyncfiable)
                        new Query.SenzReadyCallback() {
                            // Here the para senzes is the result which is got back from avoscloud server.
                            @Override
                            public void onSenzReady(ArrayList<Senz> senzes) {
                                // put senz info into msg which will be sent back to SenzManager.
                                response.getData().putParcelableArrayList("senzes", senzes);
                                try {
                                    // Send msg back to SenzManager.
                                    L.i("Beacon query complete, got " + senzes.size() + " senzes");
                                    mReplyTo.send(response);
                                } catch (RemoteException e) {
                                    L.e("Error while delivering responses", e);
                                }
                                // clear cache in mBeaconsInACycle(bluetooth's info)
                                SenzService.this.mBeaconsInACycle.clear();
                                //if (!SenzService.this.mStarted)
                                //    return;
                                //SenzService.this.setAlarmStart();
                            }
                        },
                        //
                        new Query.ErrorHandler() {
                            // on error resume next
                            @Override
                            public void onError(Exception e) {
                                L.e("query error", e);
                                //SenzService.this.setAlarmStart();
                            }
                        }
                );// -------Query.senzesFromBeaconsAsync
            }
            else
            {
                L.i("There is no Beacon device.");
            }
            SenzService.this.setAlarmStart();
        }
    }

    // It's the bluetooth's callback
    private class InternalLeScanCallback implements BluetoothAdapter.LeScanCallback
    {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            //L.d("[Internall Callback] Get beacons info - " + device);
            Beacon beacon = Beacon.fromLeScan(device, rssi, scanRecord);
            if (beacon == null) {
                L.v("Device" + device + "is not a beacon");
                return;
            }
            SenzService.this.mBeaconsInACycle.put(beacon, true);
        }
    }

    // It's the GPSinfo's callback
    private class InternalGPSInfoListener implements GPSInfo.GPSInfoListener
    {
        @Override
        public void onGPSInfoChanged(Location location) {
            SenzService.this.mLocation = location;
            //L.i("GPS info changed: " + location);
            lookNearby();
        }
    }

    // It's the SensorInfo's callback
    private class InternalSensorHandler implements SensorInfo.SensorHandler
    {
        @Override
        // It will be invoked when Acce data changed,
        // And the code run in AcceHandler is in another thread.
        public void AcceHandler(float Acce[])
        {
            //acceWriter.writeAcceToFile(Acce);
        }
        // It will be invoked when Gyro data changed,
        // And the code run in GyroHandler is in another thread.
        @Override
        public void GyroHandler(float Gyro[])
        {
            //gyroWriter.writeGyroToFile(Gyro);
        }
        // It will be invoked when Light data changed,
        // And the code run in LightHandler is in another thread.
        @Override
        public void LightHandler(float Light)
        {

        }
    }

    // It will remove all callbacks.
    private void stopScanning() {
        try {
            this.mScanning = false;
            removeAllCallbacks();
            this.mAdapter.stopLeScan(this.mLeScanCallback);
        }
        catch (Exception e) {
            L.wtf("BluetoothAdapter throws unexpected exception", e);
        }
    }

    // Including :
    // - Removing handler's callbacks.
    // - Canceling AlarmManager's all Notification Intent.
    private void removeAllCallbacks()
    {
        this.mHandler.removeCallbacks(this.mAfterScanTask);
        this.mAlarmManager.cancel(this.mAfterScanBroadcastPendingIntent);
        this.mAlarmManager.cancel(this.mStartScanBroadcastPendingIntent);
    }

   /*
    * @Function:    < Instantiation of BroadcastReceiver >
    * @CommentBy:   Woodie
    * @CommentAt:   Thur, Oct 30, 2014
    * @Description: The following definition of BroadcastReceivers mainly define onReceive().
    *               The onReceive() will be triggered when BroadcastReceiver receive the corresponding notifications.
    * @Hint:        If you need do some time-consuming task, you should run the task in another thread by a service.
    */
    // Bluetooth - BroadcastReceiver
    private BroadcastReceiver createBluetoothBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction())) {
                    switch(intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1)) {
                        case BluetoothAdapter.STATE_ON:
                            SenzService.this.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (SenzService.this.mStarted) {
                                        //L.i("[Broadcast Receiver] Bluetooth ON - start scanning");
                                        SenzService.this.startScanning();
                                    }
                                }
                            });
                            break;

                        case BluetoothAdapter.STATE_OFF:
                            SenzService.this.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //L.i("[Broadcast Receiver] Bluetooth OFF - stop scanning");
                                    SenzService.this.stopScanning();
                                }
                            });
                            break;
                    }
                }
            }
        };
    }

    // AfterScan - BroadcastReceiver
    // - Start a thread to run "AfterScan" Task.
    private BroadcastReceiver createAfterScanBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //L.i("[Broadcast Receiver] after scan!");
                SenzService.this.mHandler.post(SenzService.this.mAfterScanTask);
            }
        };
    }

    // StartScan - BroadcastReceiver
    // - Start a thread to restart scanning.
    private BroadcastReceiver createStartScanBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SenzService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //L.i("[Broadcast Receiver] start scanning!");
                        SenzService.this.startScanning();
                    }
                });
            }
        };
    }

    // LookNearby - BroadcastReceiver
    // - Start a thread to "look nearby"(actually, it sends a http request to server, which para is location)
    private BroadcastReceiver createLookNearbyBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SenzService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //L.i("[Broadcast Receiver] look nearby!");
                        SenzService.this.lookNearby();
                    }
                });
            }
        };
    }

    /*private BroadcastReceiver createStartScanBroadCastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SenzService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SenzService.this.lookNearby();
                    }
                });
            }
        };
    }*/


    // It will bind a Notification Intent to AlarmManager.
    private void setAlarm(PendingIntent pendingIntent, long delayMillis) {
        this.mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayMillis, pendingIntent);
        //L.i("[ALARM] Wait " + delayMillis + " ms");
    }


    private void setAlarmStart() {
        if (this.mTelepathyPeriod.waitMillis == 0L){
            this.startScanning();
        }
        else{
            //L.i("[SET ALARM] setAlarmStart");
            this.setAlarm(this.mStartScanBroadcastPendingIntent, this.mTelepathyPeriod.waitMillis);
        }
    }



    // It is used to instantiate the Messenger.
    // The IncomingHandle defines what SenzService will do when SenzService receive corresponding msg.
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;
            final Bundle bundle = msg.getData();
            final Messenger replyTo = msg.replyTo;
            SenzService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    switch (what) {
                        case MSG_START_TELEPATHY:
                            //L.i("[MSG] Starting telepathy");
                            SenzService.this.mStarted = true;
                            SenzService.this.mReplyTo = replyTo;
                            startScanning();
                            break;
                        case MSG_STOP_TELEPATHY:
                            //L.i("[MSG] Stopping telepathy");
                            SenzService.this.mStarted = false;
                            stopScanning();
                            break;
                        case MSG_SET_SCAN_PERIOD:
                            bundle.setClassLoader(TelepathyPeriod.class.getClassLoader());
                            SenzService.this.mTelepathyPeriod = (TelepathyPeriod) bundle.getParcelable("telepathyPeriod");
                            //L.i("[MSG] Setting scan period: " + SenzService.this.mTelepathyPeriod);
                            break;
                    }
                }
            });
        }
    }
}
