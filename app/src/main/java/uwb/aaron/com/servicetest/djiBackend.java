package uwb.aaron.com.servicetest;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.product.Model;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;

import com.secneo.sdk.Helper;



public class djiBackend extends Application implements TextureView.SurfaceTextureListener{
    public static final String FLAG_CONNECTION_CHANGE = "activationDemo_connection_change";

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback;
    private BaseProduct.BaseProductListener mDJIBaseProductListener;
    private BaseComponent.ComponentListener mDJIComponentListener;
    private static BaseProduct mProduct;
    public Handler mHandler;

    private Application instance;

    private String TAG = "BACKEND_DRONE";
    private TextureView baseTV;
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    protected DJICodecManager mCodecManager = null;
    private ResultReceiver rec;


    private int vid = 0;
    public void setContext(Application application) {
        instance = application;
    }
    public void setResultReceiver(ResultReceiver res){rec = res;}

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    public djiBackend() {

    }

    /**
     * This function is used to get the instance of DJIBaseProduct.
     * If no product is connected, it returns null.
     */
    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static boolean isHandHeldConnected() {
        return getProductInstance() != null && getProductInstance() instanceof HandHeld;
    }

    public static synchronized Camera getCameraInstance() {

        if (getProductInstance() == null) return null;

        Camera camera = null;

        if (getProductInstance() instanceof Aircraft){
            camera = ((Aircraft) getProductInstance()).getCamera();

        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }

        return camera;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: DJIBACKEND LOG TESTING");
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());

        mDJIComponentListener = new BaseComponent.ComponentListener() {

            @Override
            public void onConnectivityChange(boolean isConnected) {
                notifyStatusChange();
            }

        };
        mDJIBaseProductListener = new BaseProduct.BaseProductListener() {

            @Override
            public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {

                if (newComponent != null) {
                    newComponent.setComponentListener(mDJIComponentListener);
                }
                notifyStatusChange();
            }

            @Override
            public void onConnectivityChange(boolean isConnected) {

                notifyStatusChange();
            }

        };

        baseTV = new TextureView(getApplicationContext());
        baseTV.setMinimumWidth(160);
        baseTV.setMinimumHeight(90);

        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (vid < 10){
                    Log.d(TAG, "onReceive: VIDEO RECEIVED: " + size);
                    vid += 1;
                }
                Bundle b = new Bundle();
                b.putByteArray("VIDEO_BUFF", videoBuffer);
                b.putInt("BUFF_SIZE", size);
                //b.putString("BYTE_BUFFER", );
                try{
                    rec.send(0,b);
                }catch (Exception exc){
                    Log.d(TAG,exc.toString());
                }

                /*if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                    Log.d(TAG, "onReceive: VIDEO SENT");
                }else{
                    Log.d(TAG, "onReceive: CODEC MANAGER NULL. TRYING TO INIT");
                    onSurfaceTextureAvailable(baseTV.getSurfaceTexture(),160,90);
                }*/
            }
        };


        
    }
     /* Video functions */


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            Log.d(TAG, "onSurfaceTextureAvailable: init codecmanager");
            mCodecManager = new DJICodecManager(getApplicationContext().getApplicationContext(), surface, width, height);
        }
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureUpdated: TEXTURE UPDATED");
    }

    public void initPreviewer() {
        Log.d(TAG, "initPreviewer: PREVIEW INIT");
        BaseProduct product = getProductInstance();
        if (product == null || !product.isConnected()) {
            //showToast("DISCONNECTED...");
            Log.d(TAG, "initPreviewer: DISCONNECTED");
        } else {
            if (null != baseTV) {
                Log.d(TAG, "initPreviewer: basetvlistener");
                baseTV.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                Log.d(TAG, "initPreviewer: videocallback");
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
            }
        }
    }
    public void uninitPreviewer() {
        Log.d(TAG, "uninitPreviewer: RUN");
        Camera camera = getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
        }
    }



    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            getApplicationContext().sendBroadcast(intent);
        }
    };

}
