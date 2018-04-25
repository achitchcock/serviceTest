package uwb.aaron.com.servicetest;


import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;


public class appRegistrar {

    private Application parent;
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private final String TAG;
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static BaseProduct mProduct;
    private Handler mHandler;




    public appRegistrar(Application app){
        parent = app;
        TAG = parent.getClass().getName();
        //if(Looper.myLooper() == null) {
        //    Looper.prepare();
        //}
        //mHandler = new Handler(parent.getMainLooper());
        //showToast(TAG);
        //checkAndRequestPermissions();

        startSDKRegistration();

    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    //showToast("registering, pls wait...");
                    Log.d("LOOK:::::::","registering, pls wait...");
                    DJISDKManager.getInstance().registerApp(parent.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                showToast("Register Success");

                                Log.d("LOOK:::::::","register success");
                                DJISDKManager.getInstance().startConnectionToProduct();
                            } else {
                                //showToast("Register sdk fails, please check the bundle id and network connection!");
                                Log.d("LOOK:::::::","registering sdk failed.");
                            }
                            Log.v(TAG, djiError.getDescription());
                        }
                        @Override
                        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
                            mProduct = newProduct;
                            if(mProduct != null) {
                                mProduct.setBaseProductListener(mDJIBaseProductListener);
                            }
                            notifyStatusChange();
                        }
                    });
                }
            });
        }
    }

    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private void notifyStatusChange() {
        try{
            mHandler.removeCallbacks(updateRunnable);
            mHandler.postDelayed(updateRunnable, 500);
        }catch (Exception exc){

        }

    }
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            parent.sendBroadcast(intent);
        }
    };

    private void showToast(final String toastMsg) {
        Toast.makeText(parent.getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
    }
}

