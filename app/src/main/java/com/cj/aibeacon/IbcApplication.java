package com.cj.aibeacon;

import android.app.Application;
import android.util.Log;

import com.sensoro.cloud.SensoroManager;

/**
 * Created by w.pitt on 2016/5/12.
 */
public class IbcApplication extends Application {

    private static final String TAG = IbcApplication.class.getSimpleName();
    public SensoroManager sensoroManager;

    @Override
    public void onCreate() {
        initSensoro();
        super.onCreate();
    }

    private void initSensoro() {
        sensoroManager = SensoroManager.getInstance(getApplicationContext());
        sensoroManager.setCloudServiceEnable(false);
        sensoroManager.addBroadcastKey("01Y2GLh1yw3+6Aq0RsnOQ8xNvXTnDUTTLE937Yedd/DnlcV0ixCWo7JQ+VEWRSya80yea6u5aWgnW1ACjKNzFnig==");
        try {
            sensoroManager.startService();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onTerminate() {
        if (sensoroManager != null) {
            sensoroManager.stopService();
        }
        super.onTerminate();
    }
}
