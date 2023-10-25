package com.github.tvbox.osc.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.blankj.utilcode.util.LogUtils;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.player.MyVideoView;
import com.github.tvbox.osc.player.controller.VodController;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.MainActivity;

import xyz.doikki.videoplayer.player.AbstractPlayer;

public class PlayService extends Service {

    private static MyVideoView videoView;

    public static void start(MyVideoView controller) {
        PlayService.videoView = controller;
        ContextCompat.startForegroundService(App.getInstance(), new Intent(App.getInstance(), PlayService.class));
    }

    public static void stop() {
        App.getInstance().stopService(new Intent(App.getInstance(), PlayService.class));
    }


    private static final String CHANNEL_ID = "MyChannelId";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("My Service")
                .setContentText("Running in the background")
                .setContentIntent(getPendingIntentActivity());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
        videoView.start();
        return START_NOT_STICKY;
    }

    private PendingIntent getPendingIntentActivity() {
        Intent intentMain = new Intent(this, MainActivity.class);
        Intent intentPlayer = new Intent(this, DetailActivity.class);
        Intent[] intents = {intentMain, intentPlayer};
        return PendingIntent.getActivities(this, 1, intents, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onDestroy() {
        LogUtils.d("PlayService onDestroy");
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
