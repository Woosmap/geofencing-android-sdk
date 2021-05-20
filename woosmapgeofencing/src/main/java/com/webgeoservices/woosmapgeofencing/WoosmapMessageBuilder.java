package com.webgeoservices.woosmapgeofencing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

import static com.webgeoservices.woosmapgeofencing.WoosmapSettings.getNotificationDefaultUri;

public class WoosmapMessageBuilder {

    private final Context context;
    private Class<?> cls = null;
    private int message_icon;

    public WoosmapMessageBuilder(Context context) {
        this.context = context;
        this.setIconFromManifestVariable();
    }

    public WoosmapMessageBuilder(Context context, Class<?> cls) {
        this.context = context;
        this.cls = cls;
        this.setIconFromManifestVariable();
    }

    private void setIconFromManifestVariable() {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            this.message_icon = bundle.getInt("woosmap.messaging.default_notification_icon", R.drawable.ic_local_grocery_store_black_24dp);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            this.message_icon = R.drawable.ic_local_grocery_store_black_24dp;
        }
    }

    /**
     * Set the notification's small icon
     *
     * @param icon
     */
    public void setSmallIcon(int icon) {
        this.message_icon = icon;
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param datas FCM message body received.
     */
    public void sendWoosmapNotification(WoosmapMessageDatas datas) {
        Log.d("WGS_Message", datas.messageBody);

        NotificationManager mNotificationManager =
                (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);

        String CHANNEL_ID = WoosmapSettings.WoosmapNotificationChannelID;
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Style style;


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this.context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_local_grocery_store_black_24dp)
                        //.setSmallIcon(this.message_icon)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(4);

        if (datas.icon_url != null) {
            mBuilder.setLargeIcon(getBitmapFromURL(datas.icon_url));
        }

        if (datas.title != null && datas.type != null && (datas.imageUrl != null || datas.longText != null)) {
            if (datas.type.equals("image") && datas.imageUrl != null) {
                Bitmap largeIcon = getBitmapFromURL(datas.imageUrl);
                style = new NotificationCompat.BigPictureStyle().bigPicture(largeIcon).setSummaryText(datas.messageBody);
                mBuilder.setContentText(datas.messageBody);
                mBuilder.setContentTitle(datas.title);
                mBuilder.setStyle(style);
            } else if (datas.type.equals("text") && datas.longText != null) {
                style = new NotificationCompat.BigTextStyle()
                        .bigText(datas.longText);
                mBuilder.setContentText(datas.messageBody);
                mBuilder.setContentTitle(datas.title);
                mBuilder.setStyle(style);
            }
        } else {
            mBuilder.setContentTitle(datas.title);
            mBuilder.setContentText(datas.messageBody);
        }

        Intent resultIntent;
        if (this.cls != null) {
            resultIntent = new Intent(this.context, this.cls);
        } else {
            resultIntent = new Intent(Intent.ACTION_VIEW);
            if (datas.open_uri != null) {
                resultIntent.setData(Uri.parse(datas.open_uri));
            } else {
                Log.d("WGS_Notification", "Try to open empty URI");
                resultIntent.setData(Uri.parse(getNotificationDefaultUri(this.context)));
            }
        }
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.putExtra(WoosmapSettings.WoosmapNotification, datas.notificationId);
        Log.d("WGS_Notification", "notif: " + datas.notificationId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        Notification notification = mBuilder.build();
        Objects.requireNonNull(mNotificationManager).notify(new Random().nextInt(20), notification);
    }

    private static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
