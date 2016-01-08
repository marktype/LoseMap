package net.wyxj.vehicle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.MKGeneralListener;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKMapTouchListener;
import com.baidu.mapapi.map.MKMapViewListener;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.platform.comapi.basestruct.GeoPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private static final int MENU_VEHICLE = 0x00000001;
    public static final int END_VEHICLE = 0x00000010;
    public static final int UPDATE_TEXT = 0x00000001;

    // 此处定义全局常量
    /** 最短距离，进行弯道选择判断时要求距离必须小于这个数字才算有效*/
    public static final double MinCurveEntryDist = 100.0;
    public static final double MinCurveRadius = 10.0;
    public static final double MaxCurveRadius = 1000.0;
    public static final float TextShowTextSize = 25;
    //kx5EfhPrZDubx397qwBA4WG6
    private String accessKey = "cEGR9hPNydNcSqSnf2GGG5zI";
    private UI ui;
    public ShowDialog sd;
    public AppData appData;
    public RoadData roadData;
    public Resources resource;
    public BMapManager mMapManager = null; // 定义管理sdk的对象
    public MapView mMapView = null; // 定义mapview对象
    public MapController mMapController = null;
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyBDLocationListener();
    public LocationData mLocationData = null;
    public MyLocationOverlay myLocationOverlay = null;
    public TextView textShow = null;
    public Handler handler = null;
    public SensorManager sensorManager = null;
    public boolean isCut = false;
    public int cutSpeed = 0;
    public int cutId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化地图管理器
        mMapManager = new BMapManager(getApplication());
        mMapManager.init(accessKey, new MyMKGeneralListener());
        // 初始化各种与地图无关的成员变量
        appInit();

        regeisterSensorManager();

        textShow = (TextView) findViewById(R.id.textShow);
        textShow.setTextSize(TextShowTextSize);
        // 初始化mapview对象，并且设置显示缩放控件
        mMapView = (MapView) findViewById(R.id.bmapsView);

        mMapView.setBuiltInZoomControls(true);
        mMapView.showScaleControl(true);
        mMapView.setDoubleClickZooming(false);
        // 定义地图控件，获取mapview的控制，并把地图范围定位在宿舍
        mMapView.regMapViewListener(mMapManager, new MyMKMapViewListener());
        mMapView.regMapTouchListner(new MyMKMapTouchListener());
        mMapController = mMapView.getController();
        // 初始化locationClient服务
        clientInit();
        // 此处启动，将会在第一次定位后自动关闭
        mLocationClient.start();
        // 使用上一次退出时的位置数据作为初始化的地点
        mMapController.animateTo(new GeoPoint(appData.lastLatitude,
                appData.lastLongitude));
        mMapController.setZoom(appData.lastZoom);

        // 此处进行定时器设置，定时器每隔500ms执行一次
        handler = new Handler(){
            public void handleMessage(Message msg){
                if(msg.what != MainActivity.UPDATE_TEXT ){
                    return ;
                }
                // 截图
                if(isCut){
                    cutSpeed ++;
                    if(cutSpeed %8 == 1){
                        mMapView.getCurrentMap();
                    }
                }

                if(!mLocationClient.isStarted() || appData.isFirst ){
                    appData.textShowStrings[0] = "未启动导航功能";
                    appData.textShowStrings[1] = "";
                    textShow.setTextColor(Color.BLACK);
                    appData.updateTextShow();
                    mMapController.setRotation(0);
                    return ;
                }

                GeoPoint curPos = new GeoPoint(appData.lastLatitude,appData.lastLongitude);
                double radius = roadData.getRadius(curPos);
                double curSpeed,safeSpeed;

                // 返回的应该是米每秒
                safeSpeed = appData.getSafeSpeed(radius);
                if(safeSpeed > 120.0/3.6 ){
                    safeSpeed = 120.0/3.6;
                }

                // 该速度为 千米每小时
                curSpeed = appData.lastSpeed/3.6f;

                appData.textShowStrings[0] = "当前车速为："+(int)(curSpeed*3.6)+"km/h";
                if(curSpeed <= safeSpeed){
                    appData.textShowStrings[1] = "安全车速为："+(int)(safeSpeed*3.6)+"km/h";
                    textShow.setTextColor(Color.BLUE);
                }else {
                    appData.textShowStrings[1] = "安全车速为："+(int)(safeSpeed*3.6)+"km/h"+"\n您已超速!请迅速减速";
                    textShow.setTextColor(Color.RED);
                    Toast.makeText(MainActivity.this, "您已超过安全车速，请迅速减速！" , Toast.LENGTH_LONG).show();
                }
                appData.updateTextShow();

            }
        };

        // 启动定时器
        new Timer().schedule(new TimerTask(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Message msg = new Message();
                msg.what = MainActivity.UPDATE_TEXT;
                handler.sendMessage(msg);
            }
        }, 500, 500);

    }

    public LocationClientOption getLocationOption() {
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setAddrType("all");// 返回的定位结果包含地址信息
        option.setCoorType("bd09ll");// 返回的定位结果是百度经纬度,默认值gcj02
        option.setScanSpan(1000);// 设置发起定位请求的间隔时间为5000ms
        option.disableCache(true);// 禁止启用缓存定位
        option.setPoiNumber(0); // 最多返回POI个数
        option.setPoiDistance(1000); // poi查询距离
        option.setPoiExtraInfo(false); // 是否需要POI的电话和地址等详细信息
        return option;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // 处理后退键事件
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (ui.currentMode == UI.MENU) {
                ui.initMain();
                return true;
            } else {
                sd.showExitDialog();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 进行当前Activity初始化工作，包括如下工作 1.初始化 dataRead 以及 dataWrite 两个保存应用数据的类
     * 2.创建三个重要的类的实例，以及初始化当前应用资源实例
     */
    public void appInit() {
        resource = this.getResources();
        // 代表所有控件，包括其初始化工作
        ui = new UI(this);
        sd = new ShowDialog(this);
        appData = new AppData(this);
        roadData = new RoadData(this);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
    }

    public void clientInit() {
        // 初始化定位服务
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.setAK(accessKey);
        mLocationClient.registerLocationListener(myListener);
        LocationClientOption option = getLocationOption();
        mLocationClient.setLocOption(option);

        mLocationData = new LocationData();
        mLocationData.latitude = (double) (appData.lastLatitude) / 1E6;
        mLocationData.longitude = (double) (appData.lastLongitude) / 1E6;
        mLocationData.accuracy = appData.lastAccuracy;
        // 定位图层初始化
        myLocationOverlay = new MyLocationOverlay(mMapView);
        // 设置定位数据
        myLocationOverlay.setData(mLocationData);
        myLocationOverlay.setMarker(getResources().getDrawable(
                R.drawable.loading_point_medium));
        // 添加定位图层
        mMapView.getOverlays().add(myLocationOverlay);
        myLocationOverlay.enableCompass();
        // 修改定位数据后刷新图层生效
        mMapView.refresh();
    }

    public void saveCurrentScreen(){
        //1.构建Bitmap
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int w = point.x;
        int h = point.y;

        Bitmap Bmp = Bitmap.createBitmap( w, h, Config.ARGB_8888 );

        //2.获取屏幕
        View decorview = this.getWindow().getDecorView();
        decorview.setDrawingCacheEnabled(true);
        Bmp = decorview.getDrawingCache();

        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File( sdcard.toString() + "/vehicle");
        if(dir.exists() == false){
            dir.mkdir();
        }
        appData.lastId ++;
        File file = new File(dir.toString() + "/cut" + appData.lastId + ".png");
        try {
            file.createNewFile();
        } catch (IOException e) {
            Log.v("file","create fail");
            return ;
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.v("file","find fail");
            return ;
        }

        Bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        Log.v("file","succeed write: cut"+appData.lastId+".png");
        try {
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.v("file","end fail");
            return ;
        }

    }

    public void regeisterSensorManager(){

//		sensorManager.registerListener(new SensorEventListener(){
//			@Override
//			public void onAccuracyChanged(Sensor sensor, int accuracy) {
//			}
//			@Override
//			public void onSensorChanged(SensorEvent event) {
//				appData.lastDirection = event.values[0];
//			}
//		}, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_navi:
                // 启动导航跟踪功能，默认是关闭的
                if (!mLocationClient.isStarted()) {
                    mLocationClient.start();
                    menuItem.setTitle("关闭导航");
                } else {
                    mLocationClient.stop();
                    menuItem.setTitle("启动导航");
                }
                break;
            case R.id.menu_vehicle:
                // 启动菜单设置用的activity
                entryMenuVehicle();
                break;
            case R.id.menu_cut:
                if (isCut==false) {
                    isCut = true;
                    menuItem.setTitle("关闭截图");
                } else {
                    isCut = false;
                    menuItem.setTitle("启动截图");
                }
                break;
            default:
        }
        return true;
    }

    /**
     * 启动用于车辆参数配置设置的activity
     */
    public void entryMenuVehicle() {
        // 启动另一个 Activity,即进入学习模式。
        Intent intent = new Intent(this, VehicleActivity.class);
        Bundle bundle = new Bundle();
        bundle.putDouble("mass", appData.mass);
        bundle.putDouble("sprung", appData.sprung);
        bundle.putDouble("track",appData.track);
        bundle.putDouble("roll",appData.roll);
        bundle.putDouble("height",appData.height);
        bundle.putDouble("suspension",appData.suspension);
        bundle.putDouble("nonsense1",appData.nonsense1);
        bundle.putDouble("nonsense2",appData.nonsense2);
        intent.putExtras(bundle);
        this.startActivityForResult(intent, MainActivity.MENU_VEHICLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == MainActivity.MENU_VEHICLE
                && resultCode == MainActivity.END_VEHICLE) {
            Bundle bundle = intent.getExtras();
            appData.mass = bundle.getDouble("mass", 18300);
            appData.sprung = bundle.getDouble("sprung", 14414);
            appData.track = bundle.getDouble("track", 2.1);
            appData.roll = bundle.getDouble("roll", 0.8);
            appData.height = bundle.getDouble("height", 1);
            appData.suspension = bundle.getDouble("suspension", 300000);
            appData.nonsense1 = bundle.getDouble("nonsense1", 5);
            appData.nonsense2 = bundle.getDouble("nonsense2", 487050);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }


    // 注意在onResume、onDestroy和onPause中控制mapview和地图管理对象的状态
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        mMapView.onResume();
        if (mMapManager != null) {
            mMapManager.start();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        mMapView.destroy();
        if (mMapManager != null) {
            mMapManager.destroy();
            mMapManager = null;
        }
        if (mLocationClient.isStarted()) {
            mLocationClient.stop();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        mMapView.onPause();
        if (mMapManager != null) {
            mMapManager.stop();
        }
        super.onPause();
    }

    public class MyMKMapViewListener implements MKMapViewListener {
        @Override
        public void onClickMapPoi(MapPoi arg0) {
        }

        @Override
        public void onGetCurrentMap(Bitmap map) {
            File sdcard = Environment.getExternalStorageDirectory();
            File dir = new File( sdcard.toString() + "/vehicle");
            if(dir.exists() == false){
                dir.mkdir();
            }
            appData.lastId ++;
            File file = new File(dir.toString() + "/cut" + appData.lastId + ".png");
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.v("file","create fail");
                return ;
            }
            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                Log.v("file","find fail");
                return ;
            }
            map.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            Log.v("file","succeed write: cut"+appData.lastId+".png");
            try {
                fOut.flush();
                fOut.close();
            } catch (IOException e) {
                Log.v("file","end fail");
                return ;
            }
        }

        @Override
        public void onMapAnimationFinish() {
        }

        @Override
        public void onMapLoadFinish() {
        }

        @Override
        public void onMapMoveFinish() {
        }

    }

    public class MyMKMapTouchListener implements MKMapTouchListener {

        @Override
        public void onMapClick(GeoPoint arg0) {
            mMapController.animateTo(arg0);
        }

        @Override
        public void onMapDoubleClick(GeoPoint arg0) {
            mMapController.setZoom(mMapView.getZoomLevel()+1);
        }

        @Override
        public void onMapLongClick(GeoPoint arg0) {
        }

    }

    public class MyBDLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // TODO Auto-generated method stub
            if (location == null) {
                return;
            }
            // 将获取的位置数据存储到属性 mLocationData中
            mLocationData.latitude = location.getLatitude();
            mLocationData.longitude = location.getLongitude();
            // 如果不显示定位精度圈，将accuracy赋值为0即可
            mLocationData.accuracy = location.getRadius();
            mLocationData.direction = location.getDerect();
            // 如果属于卫星定位，就获取当前的运动速度
            if(location.getLocType() == BDLocation.TypeGpsLocation){
                appData.lastSpeed = location.getSpeed();
            }else{
                appData.lastSpeed = 0;
            }

            appData.lastAccuracy = mLocationData.accuracy;
            appData.lastZoom = mMapView.getZoomLevel();
            appData.lastLatitude = (int) (mLocationData.latitude * 1E6);
            appData.lastLongitude = (int) (mLocationData.longitude * 1E6);

            // 将定位数据设置到定位图层里
            myLocationOverlay.setData(mLocationData);
            if( appData.isFirst == false ){
                //mMapController.setRotation((int)appData.lastDirection);
                //myLocationOverlay.setMarker(getResources().getDrawable(
                //		R.drawable.navi_map_gps_locked));
            }
            // 更新图层数据执行刷新后生效
            mMapView.refresh();

            // 此处的目的是根据此时的方向来旋转地图

            GeoPoint curPoint = new GeoPoint(
                    (int) (location.getLatitude() * 1e6), (int) (location.getLongitude() * 1e6));
            mMapController.animateTo( curPoint );
            // 如果程序是启动后第一次定位，那么就应该关闭定位
            if(appData.isFirst == true){
                mLocationClient.stop();
                appData.isFirst = false;
            }

            appData.lastLocID ++;
            // 每十次输出一次地理位置调试信息
            if( appData.lastLocID %5 == 1){
                Log.v("current location", curPoint.toString() );
            }

            // 下列代码用于判断是否位于弯道路口处
            roadData.setCurrentPoint(curPoint);
        }

        @Override
        public void onReceivePoi(BDLocation arg0) {
        }

    }

    public class MyMKGeneralListener implements MKGeneralListener {
        @Override
        public void onGetNetworkState(int iError) {
        }

        @Override
        public void onGetPermissionState(int iError) {
        }
    }

}
