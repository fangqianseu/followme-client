package com.dji.importSDKDemo;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.tv.TvTrackInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.OnMapClickListener;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.Polygon;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.common.error.DJIError;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.rx.schedulers.NewThreadScheduler;

import static dji.common.mission.waypoint.WaypointMissionState.READY_TO_EXECUTE;

public class MainActivity extends FragmentActivity {

    protected static final String TAG = "fq";

    private MapView mapView;
    private AMap aMap;

    private TextView mstate, message, mToast, mLng, mLat, mAlt, mDis, mySpeed;
    private Button bltooth, locate, clear;
    private Button start, stop;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private Marker droneMarker = null;

    private float mSpeed = 7.0f;
    private double followradius = -1;
    private double distance = 0;

    private double realLocationLat = 181, realLocationLng = 181, realLocationAlt = -1;
    private double serviceLocationLat = 181, serviceLocationLng = 181, serviceLocationAlt = -1;

    private double missionlat = 0, missionLng = 0, missionAlt = 0;

    private List<Waypoint> waypointList = new ArrayList<>();
    private List<LatLng> linedraw = new ArrayList<>();
    private List<Polyline> Polylinelist = new ArrayList<>();
    private Queue<MyLocationPoint> myLocationPointList = new LinkedList<>();

    private int missionfailetime = 0;

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    private boolean servicemissionisstart = true;

    static String BlueToothAddress = "null";
    private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    private clientThread clientConnectThread = null;
    private BluetoothSocket socket = null;
    private BluetoothDevice device = null;
    private readThread mreadThread = null;

    @Override
    protected void onResume() {
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        removeListener();
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view) {
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string) {
//        MainActivity.this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
//            }
//        });
        Message msg = new Message();
        msg.obj = string;
        msg.what = 1;
        LinkDetectedHandler.sendMessage(msg);

    }

    private void initUI() {


        mstate = (TextView) findViewById(R.id.ConnectStatusTextView);
//        message = (TextView) findViewById(R.id.message);
        mToast = (TextView) findViewById(R.id.mytoast);

        mLat = (TextView) findViewById(R.id.Lat);
        mLng = (TextView) findViewById(R.id.Lng);
        mAlt = (TextView) findViewById(R.id.Alt);
        mDis = (TextView) findViewById(R.id.Dis);
        mySpeed = (TextView) findViewById(R.id.Speed);

//        bltooth = (Button) findViewById(R.id.bluetooth);
//        locate = (Button) findViewById(R.id.locate);
//        clear = (Button) findViewById(R.id.clear);
//        start = (Button) findViewById(R.id.start);
//        stop = (Button) findViewById(R.id.stop);
//
//
//        bltooth.setOnClickListener(this);
//        locate.setOnClickListener(this);
//        clear.setOnClickListener(this);
//        start.setOnClickListener(this);
//        stop.setOnClickListener(this);

        leftCenterButton();
    }

    private void leftCenterButton() {
        int redActionButtonSize = getResources().getDimensionPixelSize(
                R.dimen.red_action_button_size);
        int redActionButtonMargin = getResources().getDimensionPixelOffset(
                R.dimen.action_button_margin);
        int redActionButtonContentSize = getResources().getDimensionPixelSize(
                R.dimen.red_action_button_content_size);
        int redActionButtonContentMargin = getResources()
                .getDimensionPixelSize(R.dimen.red_action_button_content_margin);

        int redActionMenuRadius = getResources().getDimensionPixelSize(
                R.dimen.red_action_menu_radius);
        int blueSubActionButtonSize = getResources().getDimensionPixelSize(
                R.dimen.blue_sub_action_button_size);
        int blueSubActionButtonContentMargin = getResources()
                .getDimensionPixelSize(
                        R.dimen.blue_sub_action_button_content_margin);

        ImageView fabIconStar = new ImageView(this);
//        fabIconStar.setImageResource(R.drawable.ic_action_new_light);

        // 设置菜单按钮Button的宽、高，边距
        FloatingActionButton.LayoutParams starParams = new FloatingActionButton.LayoutParams(
                redActionButtonSize, redActionButtonSize);
        starParams.setMargins(redActionButtonMargin, redActionButtonMargin,
                redActionButtonMargin, redActionButtonMargin);
        fabIconStar.setLayoutParams(starParams);

        // 设置菜单按钮Button里面图案的宽、高，边距
        FloatingActionButton.LayoutParams fabIconStarParams = new FloatingActionButton.LayoutParams(
                redActionButtonContentSize, redActionButtonContentSize);
        fabIconStarParams.setMargins(redActionButtonContentMargin,
                redActionButtonContentMargin, redActionButtonContentMargin,
                redActionButtonContentMargin);

        final FloatingActionButton leftCenterButton = new FloatingActionButton.Builder(
                this).setContentView(fabIconStar, fabIconStarParams)
                .setPosition(FloatingActionButton.POSITION_LEFT_CENTER)
                .setLayoutParams(starParams).build();

        SubActionButton.Builder lCSubBuilder = new SubActionButton.Builder(this);
//        lCSubBuilder.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_white)
//        );

        //设置菜单中图标的参数
        FrameLayout.LayoutParams blueContentParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        blueContentParams.setMargins(blueSubActionButtonContentMargin,
                blueSubActionButtonContentMargin,
                blueSubActionButtonContentMargin,
                blueSubActionButtonContentMargin);

        lCSubBuilder.setLayoutParams(blueContentParams);

        //设置布局参数
        FrameLayout.LayoutParams blueParams = new FrameLayout.LayoutParams(blueSubActionButtonSize,
                blueSubActionButtonSize);
        lCSubBuilder.setLayoutParams(blueParams);

        final ImageView lcIcon1 = new ImageView(this);
        final ImageView lcIcon2 = new ImageView(this);
        final ImageView lcIcon3 = new ImageView(this);
        final ImageView lcIcon4 = new ImageView(this);

        lcIcon1.setImageResource(R.drawable.bluetooth);
        lcIcon2.setImageResource(R.drawable.locate);
        lcIcon3.setImageResource(R.drawable.start);
        lcIcon4.setImageResource(R.drawable.stop);

        //setStartAngle(70).setEndAngle(-70)设置扩展菜单的位置
        final FloatingActionMenu leftCenterMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(lCSubBuilder.setContentView(lcIcon1, blueContentParams).build())
                .addSubActionView(lCSubBuilder.setContentView(lcIcon2, blueContentParams).build())
                .addSubActionView(lCSubBuilder.setContentView(lcIcon3, blueContentParams).build())
                .addSubActionView(lCSubBuilder.setContentView(lcIcon4, blueContentParams).build())
                .setRadius(redActionMenuRadius).setStartAngle(-70).setEndAngle(70)
                .attachTo(leftCenterButton).build();

        leftCenterMenu.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
            @Override
            public void onMenuOpened(FloatingActionMenu floatingActionMenu) {
                lcIcon1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startclient();
                    }
                });

                lcIcon2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateDroneLocation();
                        cameraUpdate();

                        linedraw.add(new LatLng(droneLocationLat, droneLocationLng));

                    }
                });

                lcIcon3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        waypointList.clear();
                        mystartmission();
                    }
                });

                lcIcon4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stopWaypointMission();
                        waypointList.clear();
                        waypointMissionBuilder.waypointList(waypointList);
                        myLocationPointList.clear();
                    }
                });
            }

            @Override
            public void onMenuClosed(FloatingActionMenu floatingActionMenu) {

            }
        });
    }

    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
        }

        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        aMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Shenzhen"));
        aMap.moveCamera(CameraUpdateFactory.newLatLng(shenzhen));

    }

    private void initbuletooth() {

        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                BlueToothAddress = device.getAddress();
                mstate.setText(device.getName() + ":" + BlueToothAddress);
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);


        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        initMapView();
        initUI();
        addListener();


        initbuletooth();

    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange() {
        initFlightController();
    }

    private void initFlightController() {

        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            realLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            realLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                            realLocationAlt = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();

                            LatLng after = CoordinateUtil.toGCJ02Point(realLocationLat, realLocationLng);
                            droneLocationLat = after.latitude;
                            droneLocationLng = after.longitude;

                            updateDroneLocation();
                        }
                    });

        }
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {
            Log.e(TAG, "onDownloadUpdate: ");
        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {
            Log.e(TAG, "onUploadUpdate: ");

            if (uploadEvent.getProgress() != null
                    && uploadEvent.getProgress().isSummaryUploaded
                    && uploadEvent.getProgress().uploadedWaypointIndex == (waypointMissionBuilder.getWaypointList().size() - 1)) {

                startWaypointMission();
            }
        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {
//            Log.e(TAG, "onExecutionUpdate: ");

            MyLocationPoint service = new MyLocationPoint(serviceLocationLat, serviceLocationLng, serviceLocationAlt);
            MyLocationPoint client = new MyLocationPoint(realLocationLat, realLocationLng, realLocationAlt);

            distance = Distance(service, client);

//            Log.i(TAG, "onExecutionStart: distance" + distance + "raduis +" + followradius);

        }

        @Override
        public void onExecutionStart() {
            Log.e(TAG, "onExecutionStart: ");
        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {

            if (followradius > 0) {
                if (distance > followradius) {
                    float newspeed = (float) (mSpeed * 1.2);
                    mSpeed = (newspeed > 15.0f ? 15.0f : newspeed);
                } else {
                    mSpeed = (float) (mSpeed * 0.8);
                }
            }
            Log.e("fq onExecutionFinish", "finished ");
            Timer mytimer = new Timer();
            mytimer.schedule(new TimerTask() {
                                 public void run() {
                                     myaftermissionfinished();
                                 }
                             }
                    , 200);

            setResultToToast("任务执行结束: " + (error == null ? "成功!" : error.getDescription()));
        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }


    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation() {

        java.text.DecimalFormat df1 = new java.text.DecimalFormat("0.00000");
        java.text.DecimalFormat df2 = new java.text.DecimalFormat("0.00");


        String mylat = df1.format(realLocationLat);
        String myLng = df1.format(realLocationLng);
        String myAlt = df2.format(realLocationAlt);
        String mydistance = df2.format(distance);
        String myspeed = df2.format(mSpeed);

        Message msg2 = new Message();
        String info = myLng + "-" + mylat + "-" + myAlt + "-" + mydistance + "-" + myspeed;
        msg2.obj = info;
        msg2.what = 2;
        LinkDetectedHandler.sendMessage(msg2);

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        final MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);
                }
            }
        });
    }


//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.bluetooth: {
//                startclient();
//                break;
//            }
//            case R.id.locate: {
//                updateDroneLocation();
//                cameraUpdate();
//
//                linedraw.add(new LatLng(droneLocationLat, droneLocationLng));
//
//                break;
//            }
//            case R.id.clear: {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        aMap.clear();
//                    }
//
//                });
//
//
//                updateDroneLocation();
//                break;
//            }
//            case R.id.start: {
//                waypointList.clear();
//                mystartmission();
//                break;
//            }
//            case R.id.stop: {
//                stopWaypointMission();
//                waypointList.clear();
//                waypointMissionBuilder.waypointList(waypointList);
//                myLocationPointList.clear();
//                break;
//            }
//            default:
//                break;
//        }
//    }

    private void startclient() {
        device = mBtAdapter.getRemoteDevice(BlueToothAddress);
        clientConnectThread = new clientThread();
        clientConnectThread.start();
    }

    private void cameraUpdate() {
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);

    }


    private void SettingDialog() {

        Log.d("fq SettingDialog", "begin");

//        mSpeed = 10.0f;
        mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
        mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;


        int size = myLocationPointList.size();

        if (size == 0) {
            myaftermissionfinished();
            return;
        }

        Log.d("fq SettingDialog", "begin " + size + " before");


        if (followradius > 0) {
            size = (size > 4 ? 4 : size);
        } else {
            size = (size > 8 ? 8 : size);
        }

        Log.d("fq SettingDialog", "begin " + size + " after");


        if (size < 2) {
            MyLocationPoint point = myLocationPointList.peek();
            myLocationPointList.offer(new MyLocationPoint(point.getLatitude(), point.getLongitude(), point.getAltitude() + 1));
            size++;
        }

        Log.d("fq before ", myLocationPointList.toString());

        waypointList.clear();

        for (int i = 0; i < size; i++) {

            MyLocationPoint point = myLocationPointList.poll();


            Waypoint mWaypoint = new Waypoint(point.getLatitude(), point.getLongitude(), (float) point.getAltitude() + 1.0f);

            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
            } else {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
            }
        }

        Log.d("fq  after ", myLocationPointList.toString());

        waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
//

    }


    private void configWayPointMission() {

        Log.d("fq configWayMission", "begin : mspeed" + mSpeed);

        if (waypointMissionBuilder == null) {

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        } else {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("载入任务成功");
            Log.d(TAG, "loadWaypoint succeeded ");

//            if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(getWaypointMissionOperator().getCurrentState())
//                    || WaypointMissionState.READY_TO_UPLOAD.equals(getWaypointMissionOperator().getCurrentState())) {

            uploadWayPointMission();

//            }
            Log.d(TAG, String.valueOf(getWaypointMissionOperator().getCurrentState()));

        } else {
            setResultToToast("载入任务失败 " + error.getDescription());
        }

    }

    private void uploadWayPointMission() {

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {

                Log.d("fq uploadMission", "begin");

                if (error == null) {


                    setResultToToast("任务上传成功！");

                } else {
                    Log.e("fq", "Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }


            }
        });

    }

    private void startWaypointMission() {

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {

                Log.d("fq startMission", "begin ");


                if (error != null) {

                    Log.d("fq startMission", "misstake " + error.getDescription());

                    mymissionstarterror();

                    return;
                }
                drawline();
                missionfailetime = 0;

                setResultToToast("任务开始: " + (error == null ? "成功" : error.getDescription()));

            }
        });

    }

    private void stopWaypointMission() {

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {

                Log.d("fq stopMission", "begin");

                setResultToToast("任务结束: " + (error == null ? "成功" : error.getDescription()));
            }
        });

    }

    private void drawline() {
        Log.e("fq", "drawline: begin");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                LatLng last = null;

                if (waypointMissionBuilder.getWaypointList().size() > 0) {

                    for (int i = 0; i < waypointMissionBuilder.getWaypointList().size(); i++) {
                        Waypoint point = waypointMissionBuilder.getWaypointList().get(i);
                        LatLng after = CoordinateUtil.toGCJ02Point(point.coordinate.getLatitude(), point.coordinate.getLongitude());

                        if (i == waypointMissionBuilder.getWaypointList().size() - 1) {
                            last = after;
                        }

                        linedraw.add(after);
                    }
                }

                Polyline polyline = aMap.addPolyline(new PolylineOptions().
                        addAll(linedraw).width(10).color(Color.argb(255, 255, 72, 56)));
                Polylinelist.add(polyline);

                linedraw.clear();
                linedraw.add(last);
            }
        });


    }

    // 开启线程作为客户端
    private class clientThread extends Thread {
        public void run() {
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID
                        .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                // 连接
                Message msg2 = new Message();
                msg2.obj = "正在连接。。。" ;
                msg2.what = 0;
                LinkDetectedHandler.sendMessage(msg2);

                socket.connect();

                Message msg = new Message();
                msg.obj = "已连接到服务器.";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);

                // 开始接收数据
                mreadThread = new readThread();
                mreadThread.start();
            } catch (Exception e) {
                // TODO: handle exception
                Message msg = new Message();
                msg.obj = "连接服务端异常！断开连接重新试一试。";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);
            }
        }
    }

    private Handler LinkDetectedHandler = new Handler() {
        public void handleMessage(Message msg) {

            if (0 == msg.what) {
                mstate.setText((String) msg.obj);
            } else {
                if (1 == msg.what) {
                    mToast.setText((String) msg.obj);
                } else {
                    String[] s = ((String) msg.obj).split("-");
                    mLng.setText(s[0]);
                    mLat.setText(s[1]);
                    mAlt.setText(s[2]);
                    mDis.setText(s[3]);
                    mySpeed.setText(s[4]);
                }
            }
        }
    };

    private void mystartmission() {

        Log.d("fq startmissionHandler", "begin");

        if (servicemissionisstart == false && myLocationPointList.size() == 0) {
            setResultToToast("任务结束");
            return;
        }

        SettingDialog();
        configWayPointMission();
    }

    ;

    private void mymissionstarterror() {
        Log.d("fq startMission", "again ");

        missionfailetime++;

        if (missionfailetime < 3) {
            Log.e(TAG, "handleMessage: " + missionfailetime);

            Timer myTimer = new Timer();
            myTimer.schedule(new TimerTask() {
                                 public void run() {
                                     startWaypointMission();
                                 }
                             }
                    , 500);

        } else {
            missionfailetime = 0;

            waypointList.clear();

            Timer myTimer = new Timer();
            myTimer.schedule(new TimerTask() {
                                 public void run() {
                                     mystartmission();
                                 }
                             }
                    , 300);

        }

    }

    private void myaftermissionfinished() {

        waypointList.clear();

        if (servicemissionisstart == true || myLocationPointList.size() > 0) {
            Log.d("fq after stop", "start again.");
            mystartmission();
        } else {
            stopWaypointMission();
        }

    }


    private void servicemissionstop() {

        Message msg = new Message();
        msg.obj = "主无人机任务结束.";
        msg.what = 0;
        LinkDetectedHandler.sendMessage(msg);
        Log.d("fq servicchange", "service end");

        servicemissionisstart = false;
    }

    private void dealwithData(String in) {
        if ("end".equals(in)) {
            Log.d("fq dealwith end", "");
            servicemissionstop();
            return;
        }

        String[] gottondata = in.split("-");
        if (gottondata.length != 3) {
            setResultToToast("数据错误");
        }

        if (gottondata.length == 4) {
            mSpeed = Float.parseFloat(gottondata[1]);
            followradius = Double.parseDouble(gottondata[3]);
            Log.i(TAG, "speed and followradius set successfully." + mSpeed + " -" + followradius);
            return;
        }

        missionlat = Double.parseDouble(gottondata[0]);
        missionLng = Double.parseDouble(gottondata[1]);
        missionAlt = Double.parseDouble(gottondata[2]);

        MyLocationPoint point = new MyLocationPoint(missionlat, missionLng, missionAlt);
        myLocationPointList.offer(point);

        serviceLocationLat = missionlat;
        serviceLocationLng = missionLng;
        serviceLocationAlt = missionAlt;


    }

    // 开启线程读取数据
    private class readThread extends Thread {
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream in = null;
            try {
                in = socket.getInputStream();
            } catch (Exception e) {
                // TODO: handle exception
            }
            while (true) {
                try {
                    if ((bytes = in.read(buffer)) > 0) {
                        byte[] buf = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf[i] = buffer[i];
                        }
                        String s = new String(buf);

//                        Log.d("fq readthread", s);

                        dealwithData(s);
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    try {
                        in.close();
                    } catch (Exception e2) {
                        // TODO: handle exception
                    }
                    break;
                }
            }
        }
    }

    /**
     * 计算地球上任意两点(经纬度)距离
     *
     * @return 返回距离 单位：米
     */
    private double Distance(MyLocationPoint point1, MyLocationPoint point2) {

        double lat1 = point1.getLatitude();
        double long1 = point1.getLongitude();
        double alt1 = point1.getAltitude();

        double lat2 = point2.getLatitude();
        double long2 = point2.getLongitude();
        double alt2 = point1.getAltitude();

        double a, b, R;
        R = 6378137; // 地球半径
        lat1 = lat1 * Math.PI / 180.0;
        lat2 = lat2 * Math.PI / 180.0;
        a = lat1 - lat2;
        b = (long1 - long2) * Math.PI / 180.0;

        double d, h;
        double sa2, sb2;
        sa2 = Math.sin(a / 2.0);
        sb2 = Math.sin(b / 2.0);

        d = 2 * R * Math.asin(Math.sqrt(sa2 * sa2 + Math.cos(lat1) * Math.cos(lat2) * sb2 * sb2));
        h = alt1 - alt2;

        return Math.sqrt(d * d + h * h);
    }
}
