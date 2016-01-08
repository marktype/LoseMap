package net.wyxj.vehicle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.io.File;


/**
 * 数据处理类
 */
public class AppData {

    private SharedPreferences dataRead;
    private SharedPreferences.Editor dataWrite;

    /** 标识程序定位是不是启动后的第一次定位*/
    public boolean isFirst;
    /** 标识定位的流水号，*/
    public int lastLocID;
    public float lastSpeed;
    public int lastId;
    public String[] textShowStrings = new String[2];

    // 上一次退出时存储的地理位置数据
    public int lastLatitude;
    public int lastLongitude;
    public float lastZoom;
    public float lastAccuracy;
    public float lastDirection;

    // 汽车参数
    public double mass;
    public double sprung;
    public double track;
    public double roll;
    public double height;
    public double suspension;
    public double nonsense1;
    public double nonsense2;

    private MainActivity mainActivity;

    public AppData(MainActivity main) {
        mainActivity = main;
        // 获取 studying 的私有数据读取句柄
        dataRead = mainActivity.getSharedPreferences("vehicle", Context.MODE_PRIVATE);
        // 初始化 dataWrite 用于写入应用数据
        dataWrite = dataRead.edit();
        isFirst = true;
        lastLocID = 0;
        lastSpeed = 0;
        lastDirection = 0;
        read();
        Log.v("start info","read app data successful");
    }

    /**
     *
     */
    public void read() {
        lastLatitude = dataRead.getInt("lastLatitude", 39915000);
        lastLongitude = dataRead.getInt("lastLongitude", 116404000);
        lastZoom = dataRead.getFloat("lastZoom", 16);
        lastAccuracy = dataRead.getFloat("lastAccuracy", 1500);

        lastId = dataRead.getInt("lastId", 0);

        mass = Double.parseDouble( dataRead.getString("mass", "18300") );
        sprung = Double.parseDouble( dataRead.getString("sprung", "14414") );
        track = Double.parseDouble( dataRead.getString("track", "2.1") );
        roll = Double.parseDouble( dataRead.getString("roll", "0.8") );
        height = Double.parseDouble( dataRead.getString("height","1") );
        suspension = Double.parseDouble( dataRead.getString("suspension", "300000") );
        nonsense1 = Double.parseDouble( dataRead.getString("nonsense1", "5") );
        nonsense2 = Double.parseDouble( dataRead.getString("nonsense2", "487050") );

    }

    /**
     *
     */
    public void write() {
        // 获取最新的数据
        lastAccuracy = mainActivity.mLocationData.accuracy;
        lastZoom = mainActivity.mMapView.getZoomLevel();
        lastLatitude = (int) (mainActivity.mLocationData.latitude * 1E6);
        lastLongitude = (int) (mainActivity.mLocationData.longitude * 1E6);

        dataWrite.putInt("lastLatitude", lastLatitude);
        dataWrite.putInt("lastLongitude", lastLongitude);
        dataWrite.putFloat("lastZoom", lastZoom);
        dataWrite.putFloat("lastAccuracy", lastAccuracy);

        dataWrite.putInt("lastId", lastId);

        dataWrite.putString("mass", String.valueOf(mass) );
        dataWrite.putString("sprung", String.valueOf(sprung) );
        dataWrite.putString("track", String.valueOf(track) );
        dataWrite.putString("roll", String.valueOf(roll) );
        dataWrite.putString("height", String.valueOf(height) );
        dataWrite.putString("suspension", String.valueOf(suspension) );

        dataWrite.commit();
    }

    public void writeToBundle(Bundle bundle) {
    }

    public void readFromBundle(Bundle bundle) {
    }

    public void loadFormFile(File file) {
    }

    // 安全车速计算方法
    public float getSafeSpeed(double radius) {
        if( radius == 0.0){
            return 120.0f/3.6f;
        }
        float speed = 0;
        double gravity = 9.81;

        double t1 = mass*gravity*track*radius;
        double t2 = 2*sprung*height;
        double t3 = t1/t2;

        double t4 = sprung*gravity*roll*roll;
        double t5 = height*(suspension-sprung*gravity*roll);
        double t6 = 1 + t4/t5;

        double t7 = t3/t6;
        speed = (float) Math.sqrt(t7);

        // 对速度进行一定程度的缩小
        speed = 0.6f*speed;
        return speed;
    }

    public void updateTextShow() {
        String newString = null;
        if (textShowStrings[0] != null) {
            newString = textShowStrings[0];
        } else {
            newString = "";
        }
        for (int i = 1; i < textShowStrings.length; i++) {
            if (textShowStrings[i] != null) {
                newString = newString + "\n" + textShowStrings[i];
            }
        }
        mainActivity.textShow.setText(newString);
    }

}