package com.cj.aibeacon;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cj.aibeacon.tools.BeaconsFragment;
import com.cj.aibeacon.tools.DetailFragment;
import com.cj.aibeacon.tools.TTFIcon;
import com.sensoro.beacon.kit.Beacon;
import com.sensoro.beacon.kit.BeaconManagerListener;
import com.sensoro.cloud.SensoroManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends FragmentActivity {
    public static final String BEACON = "beacon";
    public CopyOnWriteArrayList<Beacon> beacons;
    /*
     * Sensoro Manager
     */
    SensoroManager sensoroManager;
    RelativeLayout containLayout;
    public static final String TAG_FRAG_BEACONS = "TAG_FRAG_BEACONS";
    public static final String TAG_FRAG_DETAIL = "TAG_FRAG_DETAIL";
    public static final String TAG_FRAG_DISTANCE = "TAG_FRAG_DISTANCE";
    public static final String TAG_FRAG_RANGE = "TAG_FRAG_RANGE";
    public static final String TAG_FRAG_LIGHT = "TAG_FRAG_LIGHT";
    public static final String TAG_FRAG_TEMPERATURE = "TAG_FRAG_TEMPERATURE";
    public static final String TAG_FRAG_MOVE = "TAG_FRAG_MOVE";
    public static final String TAG_FRAG_NOTIFICATION = "TAG_FRAG_NOTIFICATION";
    public FragmentManager fragmentManager;
    public DetailFragment detailFragment;
    public ActionBar actionBar;
    public TTFIcon freshIcon;
    public TTFIcon infoIcon;
    public TextView actionBarTitle;
    private IbcApplication app;
    private BeaconManagerListener beaconManagerListener;
    public Handler handler = new Handler();
    public Runnable runnable;
    public LayoutInflater inflater;
    public RelativeLayout actionBarLayout;
    public RelativeLayout actionBarMainLayout;
    public ArrayList<OnBeaconChangeListener> beaconListeners;
    public SharedPreferences sharedPreferences;

    public NotificationManager notificationManager;
    public static final int NOTIFICATION_ID = 0;
    public String beaconFilter;
    private BeaconsFragment beaconsFragment;
    public String matchFormat;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initCtrl();
        initActionBar();
        showFragment(0);
        initSensoroListener();
        initRunnable();
        initBroadcast();
    }


    private void initBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    if (state == BluetoothAdapter.STATE_ON) {
                        startSensoroService();
                    }
                }
            }
        }, filter);
    }


    private void initRunnable() {
        runnable = new Runnable() {
            @Override
            public void run() {
                updateGridView();
                handler.postDelayed(this, 2000);
            }
        };
    }

    private void initCtrl() {
        containLayout = (RelativeLayout) findViewById(R.id.activity_main_container);
        fragmentManager = getSupportFragmentManager();
        inflater = getLayoutInflater();
        app = (IbcApplication) getApplication();
        matchFormat = "%s-%04x-%04x";
        sensoroManager = app.sensoroManager;
        beacons = new CopyOnWriteArrayList<Beacon>();
        beaconListeners = new ArrayList<OnBeaconChangeListener>();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        sharedPreferences = getPreferences(Activity.MODE_PRIVATE);
    }

    private void initActionBar() {
        actionBar = getActionBar();
        if (actionBarMainLayout == null) {
            actionBarMainLayout = (RelativeLayout) inflater.inflate(R.layout.title_main, null);
            freshIcon = (TTFIcon) actionBarMainLayout.findViewById(R.id.actionbar_main_fresh);
            freshIcon.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (fragmentManager.findFragmentByTag(MainActivity.TAG_FRAG_BEACONS) != null) {
                        beacons.clear();
                        updateGridView();
                    }
                }
            });
            infoIcon = (TTFIcon) actionBarMainLayout.findViewById(R.id.actionbar_main_info);
        }

        if (actionBarLayout == null) {
            actionBarLayout = (RelativeLayout) inflater.inflate(R.layout.title_main, null);
            actionBarTitle = (TextView) findViewById(R.id.actionbar_title);
        }
    }

    /*
 * update the grid
 */
    private void updateGridView() {
        if (beaconsFragment == null) {
            return;
        }
        if (!beaconsFragment.isVisible()) {
            return;
        }
        if (beacons.size() > 0) {
            Beacon minBeacon = getMinBeacon(beacons);
            if (currentBeacon == null || (currentBeacon.getSerialNumber() != minBeacon.getSerialNumber())) {
                //根据SN 去做对应操作
                startOption(minBeacon.getSerialNumber());
            }
            currentBeacon = minBeacon;
        }
        beaconsFragment.notifyFresh();
    }

    public String mBeaconSN[] = new String[]{"0117C534585F", "0117C53F8D28", "0117C5312F68", "0117C533A521", "0117C53F9384"};

    private void startOption(String serialNumber) {
        showView(serialNumber);
    }

    private void showView(String sn) {
        System.out.println("范围最小的是：--------" + sn);
        Toast.makeText(MainActivity.this, "范围最小的是：--------" + sn, Toast.LENGTH_SHORT).show();

        beaconsFragment.setImage(sn);
    }


    Beacon currentBeacon;

    private Beacon getMinBeacon(List<Beacon> beacon1) {
        double min = beacon1.get(0).getAccuracy();
        int minBeacon = 0;
        for (int i = 1; i < beacon1.size(); i++) {
            if (min > beacon1.get(i).getAccuracy()) {
                minBeacon = i;
                min = beacon1.get(i).getAccuracy();
            }
        }

        return beacon1.get(minBeacon);
    }

    @Override
    protected void onResume() {
        boolean isBTEnable = isBlueEnable();
        if (isBTEnable) {
            //开启sensoroservice
            startSensoroService();
        }
        handler.post(runnable);
        super.onResume();
    }


    private void showFragment(int fragmentID) {
        beaconsFragment = new BeaconsFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.activity_main_container, beaconsFragment, TAG_FRAG_BEACONS);
        transaction.commit();
        setTitle(R.string.yunzi);
    }

    /*
     * Start sensoro service.
	 */
    private void startSensoroService() {
        // set a tBeaconManagerListener.
        sensoroManager.setBeaconManagerListener(beaconManagerListener);
        try {
            sensoroManager.startService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSensoroListener() {
        /*
         * beacons has bean scaned in this scanning period.
         *//*
          * beaconsFragment.isVisible()
          *//*
           * Add the update beacons into the grid.
           *//*
            * filter
            *//*
             * A new beacon appears.
             *//*
              * show notification
              *//*
               * A beacon disappears.
               *//*
                * show notification
                */
        beaconManagerListener = new BeaconManagerListener() {
            @Override
            public void onUpdateBeacon(final ArrayList<Beacon> arg0) {
                /*
                 * beacons has bean scaned in this scanning period.
				 */
                if (beaconsFragment == null) {
                    beaconsFragment = (BeaconsFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAG_BEACONS);
                }
                if (beaconsFragment == null) {
                    return;
                }
                /*
                 * beaconsFragment.isVisible()
				 */
                if (beaconsFragment.isVisible()) {
                    /*
                     * Add the update beacons into the grid.
					 */

                    beacons.clear();
                    beacons.addAll(arg0);

//                    for (Beacon beacon : arg0) {
//                        //判断距离如果距离不相同  beacon是刚获得的
//                        if (beacons.contains(beacon)) {
//                            continue;
//                        }
//                        /*
//                         * filter
//						 */
//                        if (TextUtils.isEmpty(beaconFilter)) {
//                            beacons.add(beacon);
//                        } else {
//                            String matchString = String.format(matchFormat, beacon.getSerialNumber(), beacon.getMajor(), beacon.getMinor());
//                            if (matchString.contains(beaconFilter)) {
//                                beacons.add(beacon);
//                            }
//                        }
//                    }
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        for (OnBeaconChangeListener listener : beaconListeners) {
                            if (listener == null) {
                                continue;
                            }
                            listener.onBeaconChange(arg0);
                        }
                    }
                });

            }

            @Override
            public void onNewBeacon(Beacon arg0) {
                /*
                 * A new beacon appears.
				 */
                String key = getKey(arg0);
                boolean state = sharedPreferences.getBoolean(key, false);
                if (state) {
                    /*
                     * show notification
					 */
                    showNotification(arg0, true);
                }
                System.out.println("进入 ---区域" + arg0.getSerialNumber());
            }

            @Override
            public void onGoneBeacon(Beacon arg0) {
                /*
                 * A beacon disappears.
				 */
                String key = getKey(arg0);
                boolean state = sharedPreferences.getBoolean(key, false);
                if (state) {
                    /*
                     * show notification
					 */
                    showNotification(arg0, false);
                }

                System.out.println("离开 ---区域" + arg0.getSerialNumber());
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public String getKey(Beacon beacon) {
        if (beacon == null) {
            return null;
        }
        String key = beacon.getProximityUUID() + beacon.getMajor() + beacon.getMinor() + beacon.getSerialNumber();

        return key;
    }

    /*
 * Register beacon change listener.
 */
    public void registerBeaconChangerListener(OnBeaconChangeListener onBeaconChangeListener) {
        if (beaconListeners == null) {
            return;
        }
        beaconListeners.add(onBeaconChangeListener);
    }

    /*
         * Unregister beacon change listener.
         */
    public void unregisterBeaconChangerListener(OnBeaconChangeListener onBeaconChangeListener) {
        if (beaconListeners == null) {
            return;
        }
        beaconListeners.remove(onBeaconChangeListener);
    }


    private void showNotification(final Beacon beacon, final boolean isIn) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Notification.Builder builder = new Notification.Builder(getApplicationContext());
                String context = null;
                if (isIn) {
                    context = String.format("IN:%s", beacon.getSerialNumber());
                } else {
                    context = String.format("OUT:%s", beacon.getSerialNumber());
                }
                builder.setTicker(context);
                builder.setContentText(context);
                builder.setWhen(System.currentTimeMillis());
                builder.setAutoCancel(true);
                builder.setContentTitle(getString(R.string.app_name));
                builder.setSmallIcon(R.drawable.ic_launcher);
                builder.setDefaults(Notification.DEFAULT_SOUND);
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(pendingIntent);

                Notification notification = builder.build();

                notificationManager.notify(NOTIFICATION_ID, notification);
                // NotificationManager nm = (NotificationManager)
                // getSystemService(Context.NOTIFICATION_SERVICE);
                // Notification n = new Notification(R.drawable.ic_launcher,
                // "Hello,there!", System.currentTimeMillis());
                // n.flags = Notification.FLAG_AUTO_CANCEL;
                // // n.flags = Notification.FLAG_NO_CLEAR; //
                // ������֪ͨ����ɾ��
                // Intent i = new Intent(getApplicationContext(),
                // MainActivity.class);
                // i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                // Intent.FLAG_ACTIVITY_NEW_TASK);
                // // PendingIntent
                // PendingIntent contentIntent =
                // PendingIntent.getActivity(getApplicationContext(),
                // R.string.app_name, i, PendingIntent.FLAG_UPDATE_CURRENT);
                //
                // n.setLatestEventInfo(getApplicationContext(), "Hello,there!",
                // "Hello,there,I'm john.", null);
                // nm.notify(R.string.app_name, n);
                // nm.cancel(R.string.app_name); // ȡ��֪ͨ

            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;

    private boolean isBlueEnable() {
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        boolean status = bluetoothAdapter.isEnabled();
        if (!status) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(intent);
                }
            }).setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).setTitle(R.string.ask_bt_open);
            builder.show();
        }

        return status;
    }


    /*
 * Beacon Change Listener.Use it to notificate updating of beacons.
 */
    public interface OnBeaconChangeListener {
        public void onBeaconChange(ArrayList<Beacon> beacons);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable);
        super.onPause();
    }
}
