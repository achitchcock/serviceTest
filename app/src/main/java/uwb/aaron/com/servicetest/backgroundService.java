package uwb.aaron.com.servicetest;

import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.camera.Camera;
import dji.sdk.products.HandHeld;
import uwb.aaron.com.servicestarterlib.dataReceiver;
import uwb.aaron.com.servicetest.sdkLoader;
import dji.common.util.CallbackUtils;

import com.secneo.sdk.Helper;

import java.util.Calendar;

/**
 * Created by Aaron on 4/21/2018.
 */

public class backgroundService extends Service {

    private MediaPlayer player;
    private appRegistrar arc;
    private ResultReceiver rec;
    private int count = 0;

    private djiBackend djiBack;
    private FlightController flightController;

    //protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    protected DJICodecManager mCodecManager = null;


    public backgroundService() {
        super();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        Log.d("SERVICE_DRONE","onStartCommand run in service.");
        switch (action){
            case "START_SERVICE":{
                //Helper.install(getApplication());
                rec = intent.getParcelableExtra("dataR");
                String recName= intent.getStringExtra("nameTag");
                Log.d("SERVICE DRONE:","received name="+recName);

                Log.d("SERVICE_DRONE","sending data back to activity");
                Bundle b = new Bundle();
                b.putString("DATA", "Service Started");
                try{
                    rec.send(1,b);
                }catch (Exception exc){
                    Log.d("SERVICE_DRONE: ",exc.toString());
                }

                player = MediaPlayer.create(this,
                        Settings.System.DEFAULT_RINGTONE_URI);
                player.setLooping(true);
                player.start();
                break;
            }
            case "LOAD_SDK":{
                new sdkLoader(getApplication());
                //arc = new appRegistrar(getApplication());
                break;
            }
            case "REGISTER":{
                arc = new appRegistrar(getApplication());
                break;
            }
            case "GET_STATUS":{
                if (rec != null){
                    Bundle b = new Bundle();
                    b.putString("DATA", "status retreived: "+ count);
                    count += 1;
                    rec.send(1,b);
                }
                break;
            }
            case "CONNECT_DRONE":{
                Log.d("SERVICE_DRONE","calling setupDroneConnection");
                setupDroneConnection();
                break;
            }

            case "REFRESH_DJI":{
                refreshSDKRelativeUI();
                break;
            }

            case "LAND":{
                //initFlightController();
                land();
                break;
            }
            case "TAKE_OFF":{
                //showToast("takeoffBtn clicked");
                //initFlightController();
                takeOff();
                break;
            }
            case "FC_STATUS":{
                flightControllerStatus();
                break;
            }

        }
        //getApplication();
        //return super.onStartCommand(intent, flags, startId);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        action,
                        Toast.LENGTH_SHORT).show();
            }
        });



        return START_STICKY;
    }

    private void videoSetup(){
        // The callback for receiving the raw H264 video data for camera live view
        /*mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };*/
    }



    private void flightControllerStatus(){
        try {
            initFlightController();
        }catch (Exception e){
            Log.d("SERVICE_DRONE", "failed to init flight controller");
        }
        String state = "";
        if( flightController != null){

            FlightControllerState st = flightController.getState();
            if(st.isIMUPreheating()==true){
                state += "| IMU: Preheating. ";
            }else{
                state += "| IMU: ready";
            }
            state += "|  Flight Mode: " + st.getFlightModeString();
            state += "\nGPS Signal Level: " + st.getGPSSignalLevel();
            state += "| GPS Satelite count:" + st.getSatelliteCount();
            state += "| Motors on: " + st.areMotorsOn();


        }else {
            Log.d("SERVICE_DRONE", "flightControllerStatus: NULL");
        }
        if (rec != null){
            Bundle b = new Bundle();
            b.putString("FC_STATUS", "Flight Controller status: "+ state);
            rec.send(3,b);
        }
    }

    private void initFlightController(){
        BaseProduct base = djiBack.getProductInstance();//DJISDKManager.getInstance().getProduct();
        if(base instanceof Aircraft){
            Aircraft myCraft = (Aircraft)base;
            flightController = myCraft.getFlightController();//DemoApplication.getFlightController();
            if (flightController == null) {
                return;
            }
        }else{
            showToast("Not aircraft.");
            return;
        }
    }



    private void takeOff(){
        if (flightController == null) {
            showToast("Flightcontroller Null");
            return;
        }

        CommonCallbacks.CompletionCallback take = new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (null == djiError) {
                    showToast("Takeoff started!");
                } else {
                    showToast(djiError.getDescription());
                }
            }
        };

        flightController.startTakeoff(null);
            //flightController.turnOnMotors();
    }

    private void land(){
        if (flightController == null) {
            showToast("Flightcontroller Null");
            return;
        }
        flightController.startLanding(null);
    }


    private void setupDroneConnection(){
        if(djiBack == null){
            djiBack = new djiBackend();
            //djiBack.setContext(getApplication());
            djiBack.onCreate();

            Log.d("SERVICE_DRONE", "djiBackend created");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(djiBack.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
        Log.d( "SERVICE_DRONE", "IntentFilter created" );
    }


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("SERVICE_DRONE", "broadcast receiver hit...");
            refreshSDKRelativeUI();
        }
    };

    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = djiBack.getProductInstance();
        String productText;
        String connectionStatus = "Status: ";

        if (null != mProduct && mProduct.isConnected()) {
            Log.v("SERVICE_DRONE", "refreshSDK: True");
            String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
            //mTextConnectionStatus.setText("Status: " + str + " connected");

            if (null != mProduct.getModel()) {
                productText = ("" + mProduct.getModel().getDisplayName());
                connectionStatus = "Status: " + str + " connected";
            } else {
                productText = ("Product Information");
            }
        } else {
            Log.v("SERVICE_DRONE", "refreshSDK: False");
            //mBtnOpen.setEnabled(false);

            productText = "Product Information";
            connectionStatus = "Status: No Product Connected";
        }

        if (rec != null){
            Bundle b = new Bundle();
            b.putString("CONNECTION_STATUS",  connectionStatus);
            b.putString("PRODUCT",productText);
            rec.send(2,b);
        }
    }

    @Override
    public void onDestroy() {
        Log.d("SERVICE_DRONE","onDestroy run in service");
        player.stop();
        try {
            unregisterReceiver(mReceiver);
        }catch (Exception exc){
            Log.d("SERVICE_DRONE", exc.toString());
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "Service stoped.",
                        Toast.LENGTH_SHORT).show();
            }
        });


        super.onDestroy();
    }

    private void showToast(final String toastMsg) {
        Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
    }


}
