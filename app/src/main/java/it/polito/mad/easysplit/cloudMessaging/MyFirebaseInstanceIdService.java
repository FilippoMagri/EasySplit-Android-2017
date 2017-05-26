package it.polito.mad.easysplit.cloudMessaging;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.util.HashMap;

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {
    private static final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();
    private Context mContext;
    private static String TAG = "MyFirebaseIn";

    public MyFirebaseInstanceIdService() {}

    public MyFirebaseInstanceIdService(Context context) {
        mContext = context;
    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("firebase", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("refreshedToken", refreshedToken);
        editor.commit();
    }

    //Send device registration to server
    public void sendFCMRegistrationToServer() {
        SharedPreferences pref = mContext.getSharedPreferences("firebase",0);
        Log.d(TAG,pref.toString());
        if (pref !=null) {
            String refreshedToken = pref.getString("refreshedToken", null);

            HashMap<String, Object> childUpdates = new HashMap<>();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userId = user.getUid();

            TelephonyManager telephonyManager = (TelephonyManager)mContext.getSystemService( Context.TELEPHONY_SERVICE);
            String deviceId = telephonyManager.getDeviceId();

            Log.d(TAG,"DeviceId: "+deviceId);
            Log.d(TAG,"UserdId: "+userId);

            childUpdates.put("/users/" + userId + "/devices/" + deviceId, refreshedToken);
            mRoot.updateChildren(childUpdates);
        }
    }
}
