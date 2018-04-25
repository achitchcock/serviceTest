package uwb.aaron.com.servicetest;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.secneo.sdk.Helper;

/**
 * Created by Aaron on 4/15/2018.
 */

public class sdkLoader {
    public sdkLoader(Application app){
        Toast.makeText(app.getApplicationContext(), "SDK loading...", Toast.LENGTH_SHORT).show();
        Helper.install(app);
        Toast.makeText(app.getApplicationContext(), "SDK Loaded", Toast.LENGTH_SHORT).show();

    }
}
