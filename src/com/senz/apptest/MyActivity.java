/***********************************************************************************************************************
 * @Project     : Senz+ App
 * @author      : Woodie
 * @CreateTime  : Thur, Oct 23, 2014
 * @Version     : 0.0.2
 * @Description : It's an application of Senz+ SDK which created by zhzhzoo. I extracted the src code from sdk JAR, and
 *                put it into this project on purpose that debug the sdk code and promote the function.
 *                In this project, I only code one MainActivity as the main .And in this main, I follow the instruction,
 *                which written by zhzhzoo on his github (https://github.com/zhzhzoo/Senz-Android-SDK), Instantiation a
 *                SenzManager Object and call it's initiation, then start Listening.
 *                I will continue updating the code.
 ***********************************************************************************************************************/
package com.senz.apptest;

import android.app.Activity;
import android.os.Bundle;
import java.util.List;

import android.os.RemoteException;
import android.util.Log;
import com.senz.core.SenzManager;
import com.senz.core.Senz;
import com.senz.core.POI;
import com.senz.core.TOI;

public class MyActivity extends Activity {

    private final static String TAG = "SenzApp";
    private SenzManager senzManager = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // Instantiation the SenzManager Object.
        SenzManager senzManager = new SenzManager(this);
        // Init the SenzManager
        try {
            senzManager.init();
        }
        catch (Exception e) {
            Log.e(TAG, "unable to initialize senz manager!", e);
        }
        // Start the SenzManager's listening
        try {
            senzManager.startTelepathy(new SenzManager.TelepathyCallback() {
                @Override
                // When the Beacon device is nearby
                public void dicoverSenz(List<Senz> senzes) {
                    Log.i(TAG,"Discover a new Senz!");
                    Log.i(TAG,"senz: doing -> " + senzes.get(0)._doing() + " where -> " + senzes.get(0)._where() + " when -> " + senzes.get(0)._when());
                }
                @Override
                public void discoverPOI(POI poi) {
                    Log.i(TAG,"Discover a new POI!");
                    Log.i(TAG,"POI: at -> " + poi._at + "   poi group -> " + poi._poi_group);
                }
                @Override
                public void discoverTOI(TOI toi) {
                    Log.i(TAG,"Discover a new TOI!");
                    Log.i(TAG,"POI: while -> " + toi._while + "   when -> " + toi._when);
                }
            });
        }
        catch (Exception e) {
            Log.d(TAG, "unable to start telepathy", e);
        }
    }
}
