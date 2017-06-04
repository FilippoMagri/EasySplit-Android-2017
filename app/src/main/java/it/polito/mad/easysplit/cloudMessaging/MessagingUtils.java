package it.polito.mad.easysplit.cloudMessaging;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MessagingUtils {
    public final static String AUTH_KEY_FCM = "AAAAbkzlrnw:APA91bHmAL4upMmgUiT9byDUDZKOXr5Skgk55PXKv0mGqmtMDscP-KFn1F-UltmVCXOYubYi-Wy57w1woFuGy8WiQ4BL_uZt6TZ-yDG-6aQanq4tVmk8reK-AXaxCYZRkWHRTj2JJJjH";
    public final static String API_URL_FCM = "https://fcm.googleapis.com/fcm/send";

    public MessagingUtils() {}

    //Send notifications to all members involved in the payment
    public static void sendPushUpNotifications(DatabaseReference mRoot, final String groupId, final String expenseName, Map<String, String> memberIds, final String message) {
        HashMap<String, String> membersToNotify = new HashMap<>(memberIds);
        String idUserLogged = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (membersToNotify.containsKey(idUserLogged)) {
            // Remove the user-logged from the notification list
            membersToNotify.remove(idUserLogged);
        }

        for (Map.Entry<String,String> entry:membersToNotify.entrySet()) {
            final String idMember = entry.getKey();
            final DatabaseReference userRef = mRoot.child("users").child(idMember);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot userSnap) {
                    if (! userSnap.hasChild("devices"))
                        return;
                    DataSnapshot devicesUserSnapShot = userSnap.child("devices");
                    for (DataSnapshot device : devicesUserSnapShot.getChildren()) {
                        String tokenNotification = device.getValue(String.class);
                        sendRealPushUpNotification(tokenNotification, message, expenseName, groupId.toString());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("MessagingUtils", "Database error: " + databaseError);
                }
            });
        }
    }

    public static void sendRealPushUpNotification (final String tokenNotification, final String notificationTitle,
                                                   final String notificationMessage, final String groupUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = "";
                    URL url = new URL(API_URL_FCM);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setUseCaches(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "key=" + AUTH_KEY_FCM);
                    conn.setRequestProperty("Content-Type", "application/json");

                    JSONObject json = new JSONObject();
                    try {
                        json.put("to", tokenNotification.trim());
                        json.put("priority","normal");
                        json.put("content_available",true);

                        JSONObject data = new JSONObject();
                        data.put("notificationTitle", notificationTitle);
                        data.put("notificationMessage", notificationMessage);
                        data.put("groupUri", groupUri);
                        data.put("groupTitle", "groupTitle");
                        json.put("data",data);
                        try {
                            OutputStreamWriter wr = new OutputStreamWriter(
                                    conn.getOutputStream());
                            wr.write(json.toString());
                            wr.flush();

                            BufferedReader br = new BufferedReader(new InputStreamReader(
                                    (conn.getInputStream())));

                            String output;
                            Log.d("EditExpense","Output from Server .... \n");
                            while ((output = br.readLine()) != null) {
                                Log.d("EditExpense",output);
                            }
                            result =  Integer.toString(conn.getResponseCode());
                        } catch (Exception e) {
                            e.printStackTrace();
                            result = Integer.toString(conn.getResponseCode());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
