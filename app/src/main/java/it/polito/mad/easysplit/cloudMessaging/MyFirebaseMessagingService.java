package it.polito.mad.easysplit.cloudMessaging;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

import it.polito.mad.easysplit.Group;
import it.polito.mad.easysplit.GroupDetailsActivity;
import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.Utils;

/**
 * Created by fil on 25/05/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    //Default Value Set to One
    public Integer mNotificationId = 1;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            String notificationMessage="",groupUri="",groupTitle="";
            ArrayMap<String,String> map = (ArrayMap<String,String>) remoteMessage.getData();
            for (Map.Entry<String,String> entry: map.entrySet()) {
                String key = entry.getKey();
                switch (key) {
                    case "notificationMessage":
                        notificationMessage = entry.getValue();
                        break;
                    case "groupUri":
                        groupUri = entry.getValue();
                        break;
                    case "groupTitle":
                        groupTitle = entry.getValue();
                        break;
                    default:
                        break;
                }
            }
            // Check if every important field of the payload is present
            if (!notificationMessage.equals("")&&!groupUri.equals("")&&!groupTitle.equals("")) {
                createSimpleNotification(notificationMessage,groupUri,groupTitle);
            }
        }
    }

    private void createSimpleNotification(String body,String groupUri,String groupTitle) {
        long[] vibrate = { 0, 100, 200, 300 };
        Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap largeIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.ic_monetization_on_red_900_36dp);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setLargeIcon(largeIcon)
                        .setSmallIcon(R.drawable.ic_monetization_on_red_900_36dp)
                        .setContentTitle(groupTitle).setDefaults(Notification.DEFAULT_ALL)
                        .setContentText(body).setPriority(Notification.PRIORITY_HIGH)
                        .setSound(uri)
                        .setVibrate(vibrate);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, GroupDetailsActivity.class);
        resultIntent.setData(Utils.getUriFor(Utils.UriType.GROUP, groupUri));
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(Group.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mNotificationId allows you to update the notification later on.
        mNotificationId = getSaltInteger();
        mNotificationManager.notify(mNotificationId, mBuilder.build());
        lightOnTheScreen();
    }

    private void lightOnTheScreen() {
        PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn= pm.isScreenOn();
        if(isScreenOn==false)
        {
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.ON_AFTER_RELEASE,"MyLock");
            wl.acquire(10000);
            PowerManager.WakeLock wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyCpuLock");
            wl_cpu.acquire(10000);
            wl.release();
            wl_cpu.release();
        }
    }

    protected Integer getSaltInteger() {
        String SALTCHARS = "1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 3) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        Integer saltInteger = new Integer(salt.toString());
        return saltInteger;
    }
}
