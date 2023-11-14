package com.github.tvbox.osc.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.blankj.utilcode.util.LogUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.constant.Constants;
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MBox")
                .setContentText("播放中")
                .setSmallIcon(R.drawable.app_icon)
                .setContentIntent(getPendingIntentActivity());

        // 创建通知栏操作
        NotificationCompat.Action previousAction = buildNotificationAction(
                R.drawable.ic_play_pre, "上一集", getPendingIntent(Constants.PIP_BOARDCAST_ACTION_PREV));
        NotificationCompat.Action pauseAction = buildNotificationAction(
                R.drawable.ic_pause, "暂停", getPendingIntent(Constants.PIP_BOARDCAST_ACTION_PLAYPAUSE));
        NotificationCompat.Action nextAction = buildNotificationAction(
                R.drawable.ic_play_next, "下一集", getPendingIntent(Constants.PIP_BOARDCAST_ACTION_NEXT));

        // 将通知栏操作添加到通知中
        builder.addAction(previousAction);
        builder.addAction(pauseAction);
        builder.addAction(nextAction);

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
        videoView.start();
        return START_NOT_STICKY;
    }

    private NotificationCompat.Action buildNotificationAction(int iconResId, String title, PendingIntent intent) {
        // 创建通知栏操作
        return new NotificationCompat.Action.Builder(iconResId, title, intent).build();
    }

    private PendingIntent getPendingIntentActivity() {
        Intent intent = new Intent(this, DetailActivity.class);
        return PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public static PendingIntent getPendingIntent(int actionCode) {
        return PendingIntent.getBroadcast(App.getInstance(), actionCode, new Intent("PIP_VOD_CONTROL").putExtra("action", actionCode).setPackage(App.getInstance().getPackageName()),PendingIntent.FLAG_UPDATE_CURRENT);
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
