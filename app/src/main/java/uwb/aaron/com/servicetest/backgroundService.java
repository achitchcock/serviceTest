package uwb.aaron.com.servicetest;

import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.product.Model;
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

public class backgroundService extends Service { //implements TextureView.SurfaceTextureListener {

    private MediaPlayer player;
    private appRegistrar arc;
    private ResultReceiver rec;
    private int count = 0;

    private djiBackend djiBack;
    private FlightController flightController;

    private TextureView baseTV;   // change  to a texture object???

    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    protected DJICodecManager mCodecManager = null;
    private String TAG = "SERVICE_DRONE";

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
        //Log.d(TAG,"onStartCommand run in service.");
        switch (action){
            case "START_SERVICE":{
                new sdkLoader(getApplication());
                baseTV = new TextureView(getApplicationContext());
                rec = intent.getParcelableExtra("dataR");
                String recName= intent.getStringExtra("nameTag");
                Log.d(TAG,"received name="+recName);

                Log.d(TAG,"sending data back to activity");
                Bundle b = new Bundle();
                b.putString("DATA", "Service Started");
                try{
                    rec.send(0,b);
                }catch (Exception exc){
                    Log.d(TAG,exc.toString());
                }

                /*player = MediaPlayer.create(this,
                        Settings.System.DEFAULT_RINGTONE_URI);
                player.setLooping(true);
                player.start();
                */
                arc = new appRegistrar(getApplication());
                break;
            }
            case "LOAD_SDK":{
                // now completed on START_SERVICE
                //new sdkLoader(getApplication());
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
                    rec.send(0,b);
                }
                break;
            }
            case "CONNECT_DRONE":{
                Log.d(TAG,"calling setupDroneConnection");
                initFlightController();
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
            case "START_VIDEO":{
                //Camera camera = djiBack.getCameraInstance();
                //videoSetup();
                djiBack.initPreviewer();
                break;
            }
            case "STOP_VIDEO": {
                //uninitPreviewer();
                break;
            }
        }
        //showToast(action);
        return START_NOT_STICKY;
    }

    /*private void videoSetup(){
        showToast("Video setup called");
        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                Log.d(TAG, "onReceive: VIDEO RECEIVED: "+ size);
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                    Log.d(TAG, "onReceive: VIDEO SENT");
                }
            }
        };

        // part 2
        BaseProduct product = djiBack.getProductInstance();
        if (product == null || !product.isConnected()) {
            showToast("Disconnected");
        } else {
            //if (null != baseTV) {
            //    baseTV.setSurfaceTextureListener(this);
            //}
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
            }
        }
    }*/



    private void flightControllerStatus(){
        try {
            initFlightController();
        }catch (Exception e){
            Log.d(TAG, "failed to init flight controller");
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
            Log.d(TAG, "flightControllerStatus: NULL");
        }
        if (rec != null){
            Bundle b = new Bundle();
            b.putString("FC_STATUS", "Flight Controller status: "+ state);
            rec.send(0,b);
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
            djiBack.setContext(getApplication());
            djiBack.setResultReceiver(rec);
            djiBack.onCreate();

            Log.d(TAG, "djiBackend created");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(djiBack.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
        Log.d( TAG, "IntentFilter created" );
    }


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "broadcast receiver hit...");
            refreshSDKRelativeUI();
        }
    };

    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = djiBack.getProductInstance();
        String productText;
        String connectionStatus = "Status: ";

        if (null != mProduct && mProduct.isConnected()) {
            //Log.v(TAG, "refreshSDK: True");
            String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
            //mTextConnectionStatus.setText("Status: " + str + " connected");

            if (null != mProduct.getModel()) {
                productText = ("" + mProduct.getModel().getDisplayName());
                connectionStatus = "Status: " + str + " connected";
            } else {
                productText = ("Product Information");
            }
        } else {
            Log.v(TAG, "refreshSDK: False");
            //mBtnOpen.setEnabled(false);

            productText = "Product Information";
            connectionStatus = "Status: No Product Connected";
        }

        if (rec != null){
            Bundle b = new Bundle();
            b.putString("CONNECTION_STATUS",  connectionStatus);
            b.putString("PRODUCT",productText);
            rec.send(0,b);
        }
    }



    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy run in service");
        player.stop();
        try {
            unregisterReceiver(mReceiver);
        }catch (Exception exc){
            Log.d(TAG, "Receiver not regestered. No Problem.");
        }
        try{
            djiBack.uninitPreviewer();
            djiBack.onTerminate();
        }catch (Exception e){
            Log.d(TAG, "Previewer not created.  No Problem.");
        }

        showToast("Service stoped.");
        super.onDestroy();
    }

    private void showToast(final String toastMsg) {
        Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
    }


}
