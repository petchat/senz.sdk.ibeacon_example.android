package com.senz.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.Message;
import android.os.Messenger;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.Looper;
import android.os.IBinder;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import com.senz.core.Senz;
import com.senz.service.SenzService;
import com.senz.exception.SenzException;
import com.senz.utils.L;
import com.senz.filter.Filter;

/***********************************************************************************************************************
 * @ClassName:   SenzManager
 * @Author:      zhzhzoo
 * @CommentBy:   Woodie
 * @CommentAt:   Mon, Oct 27, 2014
 * @Description: It is a *Client* to communicate with SenzService. Users should Instantiate it first! And following is
 *               some user interface.
 *               # TelepathyCallback:
 *                     It has a callback, named TelepathyCallback, used to report the senz info.
 *                     a. onDiscover(): used in function respondSenz(). It will be called when SenzService
 *                        send MSG_TELEPATHY_RESPONSE message.
 *                     b. onLeave(): used in function reportUnseenAndUpdateTime(), It will be called when
 *                        SenzService send MSG_TELEPATHY_RESPONSE message.
 *               # getLastDiscoveredSenzes :
 *                     Users can call this public function when they has instantiated the SenzManager,
 *                     tryed init(), and tryed startTelepathy(). It will return an senz array named mLastDiscovered.
 *               # Init:
 *                     User should try it after instantiation of SenzManager,
 *                     It check the bluetooth device status. Then bind service to SenzService.
 *               # startTelepathy:
 *                     Users should call it if they need start listening.
 *                     It will send a MSG_START_TELEPATHY message and a Handler(Used to receive respond from SenzService)
 *                     to SenzService.
 *               # stopTelepathy:
 *                     Users should call it if they need stop listening.
 *                     It will send a MSG_STOP_TELEPATHY message and a Handler(Used to receive respond from SenzService)
 *                     to SenzService.
 *               # end:
 *                     Users should call it if they need release all resources.
 *                     It will unbind Service.
 ***********************************************************************************************************************/
public class SenzManager {

    private Context mContext;
    // Callback
    private TelepathyCallback mTelepathyCallback;
    private ErrorHandler mErrorHandler;
    // Used to send massage to SenzService
    // The message type definition is in SenzService (incomingHandler)
    private Messenger mServiceMessenger;
    // Used to receive message from SenzService
    // The message type definition is in SenzManager (incomingHandler)
    private Messenger mIncomingMessenger;
    // It mainly used to get IBinder from SenzService and instantiate a Messenger with the IBinder.
    // This Messenger used to send message to SenzService.
    private ServiceConnection mServiceConnection;

    private boolean mStarted;
    private HashMap<Senz, Long> mLastSeen = new HashMap<Senz, Long>();
    private Filter mFilter = new Filter(this.mContext);
    // It stores senz info
    private ArrayList<Senz> mLastDiscovered;

    public SenzManager(Context context) {
        this.mContext = context;
        // Instantiate the implemention of ServiceConnection.
        this.mServiceConnection = new InternalServiceConnection();
        // Instantiate the Messenger that used to get message from SenzService
        this.mIncomingMessenger = new Messenger(new IncomingHandler());

        this.mLastDiscovered = new ArrayList<Senz>();
    }

    public boolean checkPermissions() {
        PackageManager pm = this.mContext.getPackageManager();
        int bluetoothPermission = pm.checkPermission("android.permission.BLUETOOTH", this.mContext.getPackageName());
        int bluetoothAdminPermission = pm.checkPermission("android.permission.BLUETOOTH_ADMIN", this.mContext.getPackageName());
        
        L.i("bluetooth:" + bluetoothPermission + " bluetooth_admin:" + bluetoothAdminPermission);
        return bluetoothPermission == 0 && bluetoothAdminPermission == 0;
    }

    public boolean checkService() {
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = new Intent(this.mContext, SenzService.class);
        List resolveInfo = pm.queryIntentServices(intent, 65536);

        return resolveInfo.size() > 0;
    }

    public boolean hasBluetooth() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public boolean bluetoothEnabled() {
        if (!checkPermissions()) {
            L.e("AndroidManifest.xml does not contain android.permission.BLUETOOTH or android.permission.BLUETOOTH_ADMIN permissions.");
            return false;
        }
        if (!checkService()) {
            L.e("SenzService may be not declared in AndroidManifest.xml.");
            return false;
        }
        BluetoothManager bluetoothManager = (BluetoothManager) this.mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    /*
     * @Function:    < init >
     * @CommentBy:   Woodie
     * @CommentAt:   Thur, Oct 24, 2014
     * @Description: First, It will call hasBluetooth() and bluetoothEnabled() to check device's bluetooth state;
     *               Second, Check whether the connection exists; (mServiceMessenger != null ?)
     *               Third, It binds service to SenzService, it will lead Android sys to call SenzService's onBind(),
     *               then onBind() will return a IBinder object to SenzManager, the IBinder can be catched by SenzManager's
     *               implemention of the interface - ServiceConnection 's onServiceConnected(). onServiceConnected()
     *               will use this IBinder to instantiate a Messenger, which used to communicate with SenzService.
     */
    public void init() throws SenzException {
        L.i("initializing senz manager");

        if (!this.hasBluetooth())
            throw new SenzException("No Bluetooth!");
        if (!this.bluetoothEnabled())
            throw new SenzException("Bluetooth not enabled!");

        if (isConnected())
            return;

        boolean bound = this.mContext.bindService(new Intent(this.mContext, SenzService.class),
                                                  this.mServiceConnection,
                                                  Context.BIND_AUTO_CREATE);
        if (!bound) {
            L.e("Could not bind service: make sure that com.senz.sdk.service.SenzService is declared in AndroidManifest.xml");
            throw new SenzException("Can't bind service");
        }
    }

   /*
    * @Function:    < end >
    * @CommentBy:   Woodie
    * @CommentAt:   Mon, Oct 27, 2014
    * @Description: Release all of resources and unbind service from SenzService.
    */
    public void end() {
        if (!isConnected())
            return;
        this.mContext.unbindService(this.mServiceConnection);
        this.mServiceMessenger = null;
    }

    public boolean isConnected() {
        return this.mServiceMessenger != null;
    }

    /*
     * @Function:    < startTelepathy >
     * @CommentBy:   Woodie
     * @CommentAt:   Mon, Oct 27, 2014
     * @Description: The main function of startTelepathy is passed a user-defined callback function - interface TelepathyCallback.
     *               The member of TelepathyCallback named onDiscover will be called in *respondSenz()*.
     *               The member of TelepathyCallback named onLeave will be called in *reportUnseenAndUpdateTime()*.
     *               Then call internalStartTelepathy() to instantiate message and send this message to SenzService.
     */
    public void startTelepathy(TelepathyCallback cb) throws RemoteException, SenzException {
        if (cb == null)
            throw new NullPointerException();
        this.mTelepathyCallback = cb;
        this.mStarted = true;

        if (isConnected())
            internalStartTelepathy();
    }

   /*
    * @Function:    < internalStartTelepathy >
    * @CommentBy:   Woodie
    * @CommentAt:   Mon, Oct 27, 2014
    * @Description: Instantiate a Massage object, which obtain a MSG_START_TELEPATHY message and attach a replyto Message
    *               object, and send this message to SenzService to start listening!
    *               It's the first message that SenzManager send to SenzService.
    */
    private void internalStartTelepathy() {
        Message startTelepathyMsg = Message.obtain(null, SenzService.MSG_START_TELEPATHY);
        startTelepathyMsg.replyTo = this.mIncomingMessenger;
        try {
            this.mServiceMessenger.send(startTelepathyMsg);
        }
        catch (RemoteException e) {
            L.e("Error sending start telepathy message: ", e);
        }
    }

    public void stopTelepathy() throws RemoteException {
        this.mStarted = false;
        if (!isConnected())
            return; // Stopping, OK to ignore no connection
        Message stopTelepathyMsg = Message.obtain(null, SenzService.MSG_STOP_TELEPATHY);
        stopTelepathyMsg.replyTo = this.mIncomingMessenger;
        try {
            this.mServiceMessenger.send(stopTelepathyMsg);
        }
        catch (RemoteException e) {
            L.e("Error sending stop telepathy message: ", e);
        }
    }



    private void respondSenz(final ArrayList<Senz> senzes) {
        L.i("[SenzManager] Senzes Discovered count:" + senzes.size());
        this.mLastDiscovered = senzes;
        // If there is senz
        if(senzes.size() > 1) {
            senzes.remove(senzes.size() - 1);
            this.mTelepathyCallback.dicoverSenz(senzes);
        }
        // If there is POI or TOI
        else if(senzes.size() == 1)
        {
            if(senzes.get(0)._where() != "null" && senzes.get(0)._group() != "null"){
                POI poi = new POI(senzes.get(0)._where(), senzes.get(0)._group());
                this.mTelepathyCallback.discoverPOI(poi);
            }
            if(senzes.get(0)._while() != "null" && senzes.get(0)._when() != "null"){
                TOI toi = new TOI(senzes.get(0)._while(), senzes.get(0)._when());
                this.mTelepathyCallback.discoverTOI(toi);
            }
        }
    }

    private void respondError(String reason) {
        if (mErrorHandler != null)
            mErrorHandler.onError(new SenzException(reason));
        else
            L.d("Unhandled error: " + reason);
    }

   /*
    * @Class:       < IncomingHandler >
    * @CommentBy:   Woodie
    * @CommentAt:   Mon, Oct 27, 2014
    * @Description: If there is no IncomingHandler, then SenzManager won't receive message from SenzService.
    *               If we need receive SenzService's respond, we should instantiate a Handler, and use this instantiation
    *               of Handler to generate a Messenger.
    *               Finally, we send this Messenger as mServiceMessenger's replyTo to SenzService.
    *               So SenzManager can send message to SenzManager through this Messenger.
    */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SenzService.MSG_SENZ_RESPONSE:
                    ArrayList<Senz> senzes = msg.getData().getParcelableArrayList("senzes");
                    SenzManager.this.respondSenz(senzes);
                    break;
                case SenzService.MSG_STATICINFO_RESPONSE:
                    StaticInfo staticInfo = msg.getData().getParcelable("staticinfo");
                    L.i("User's gender is " + staticInfo.getGender());
                    break;
                case SenzService.MSG_ERROR_RESPONSE:
                    String reason = msg.getData().getString("reason");
                    SenzManager.this.respondError(reason);
                    break;
                default:
                    L.d("Unknown message: " + msg);
                    break;
            }
        }
    }

   /*
    * @Class:       < InternalServiceConnection >
    * @CommentBy:   Woodie
    * @CommentAt:   Mon, Oct 27, 2014
    * @Description: The implemention of ServiceConnection. This interface 's instantiation will be triggered while onbind
    *               SenzService of unbind SenzService.
    *               When onbinding SenzService, It will get IBinder from SenzService, and use the IBinder to generate
    *               a instantiation of Messenger to communicate with SenzService.
    * @Hint:        The instantiation of InternalServiceConnection used to init the bind (It's a para of bindService()).
    */
    private class InternalServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SenzManager.this.mServiceMessenger = new Messenger(service);
            if (SenzManager.this.mStarted)
                internalStartTelepathy();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            L.e("Service disconnected... " + name);
            SenzManager.this.mServiceMessenger = null;
        }
    }

   /*
    * @interface:   < TelepathyCallback >
    * @CommentBy:   Woodie
    * @CommentAt:   Mon, Oct 27, 2014
    * @Description: Left to the user-defined interface.
    *               The member of TelepathyCallback named onDiscover will be called in *respondSenz()*.
    *               The member of TelepathyCallback named onLeave will be called in *reportUnseenAndUpdateTime()*.
    */
    public interface TelepathyCallback {
        public void dicoverSenz(List<Senz> senzes);
        public void discoverPOI(POI poi);
        public void discoverTOI(TOI toi);
    }

    public interface ErrorHandler {
        public void onError(SenzException e);
    }

    // Log the senzes and report the unseen senzes.
    /*private void reportUnseenAndUpdateTime(ArrayList<Senz> senzes) {
        // Get current time
        long now = System.currentTimeMillis();
        ArrayList<Senz> unseens = new ArrayList<Senz>();
        // Get senzes which service returned, and they are remembered as "key" by SenzManager in mLastSeen.
        // And log the current time as "value" in mLastSeen.
        for (Senz senz : senzes)
            this.mLastSeen.put(senz, now);
        //L.i("[SenzManager] Senzes LastSeen count:" + mLastSeen.size());
        // Check every senzes in mLastSeen,
        // If someone's timestamp is over 20s, then it will be added in unseen.
        // These senzes in unseen stand for those left senzes.
        for (Entry<Senz, Long> entry : this.mLastSeen.entrySet())
            if (entry.getValue() - now > TimeUnit.SECONDS.toMillis(20))
                unseens.add(entry.getKey());
        // Remove all left senzes in mLastSeen.
        for (Senz senz : unseens)
            this.mLastSeen.remove(senz);
        // Call the callback which defined by users.
        //if(unseens.size() > 0) {
        //    this.mTelepathyCallback.onLeave(unseens);
        //}
    }*/
}
