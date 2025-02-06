package com.example.service.download.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.service.download.get.DownloadManager;
import com.example.service.download.get.DownloadManagerImpl;
import com.example.service.download.get.DownloadMission;
import com.example.service.utils.Utils;
import java.io.File;
import java.util.HashSet;
import java.util.Stack;


public class DownloadManagerService extends Service implements DownloadMission.MissionListener {

    private static final String TAG = "MyAppDownloadManagerService";

    private static final String DOWNLOADING_CHANNEL_ID = "DOWNLOADING";
    private static final String DOWNLOADING_CHANNEL_NAME = "Downloading";

    private NotificationManagerCompat notificationManager;
    public static final Stack<String> listUrlDownloading = new Stack<>();
    private DMBinder mBinder;
    private DownloadManager mManager;

    private long mLastTimeStamp = System.currentTimeMillis();

    /**
     * hash code của các file đang tải, dùng để hiện thông báo
     */
    private final HashSet<Integer> downloadingNotificationId = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();

        mBinder = new DMBinder();
        if (mManager == null) {
            String path = getPathDownload().getPath();
            mManager = new DownloadManagerImpl(this, path);
        }

        notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelCompat notificationChannel = new NotificationChannelCompat.Builder(
                DOWNLOADING_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).setName(DOWNLOADING_CHANNEL_NAME).build();
            if (notificationManager.getNotificationChannel(DOWNLOADING_CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        for (int i = 0; i < mManager.getCount(); i++) {
        mManager.pauseMission(i);
    }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void showNotification() {
        for (int i = 0; i < mManager.getCount(); i++) {
        DownloadMission downloadMission = mManager.getMission(i);
        int notificationId = (int) downloadMission.timestamp;
        String notification = downloadMission.notification;

        // Hiện thông báo đang tải
        if (downloadMission.running) {
            double progress = downloadMission.done * 1D / downloadMission.length * 100D;
            String contentText;

            if (progress >= 100D) {
                if (downloadMission.hasShowMergingNotification) {
                    continue;
                } else {
                    contentText = "Merging";
                    downloadMission.hasShowMergingNotification = true;
                }
            } else {
                contentText = String.format("%.1f%%", progress);
            }

            NotificationCompat.Builder progressNotificationBuilder = createProgressNotificationBuilder()
                .setContentTitle(downloadMission.name)
                .setContentText(contentText)
                .setProgress(100, (int) progress, false)
                .setSmallIcon(android.R.drawable.stat_sys_download_done);

            notificationManager.notify(notificationId, progressNotificationBuilder.build());
            downloadingNotificationId.add(notificationId);
        }
        // Hiện thông báo tải xong
        else if (downloadMission.finished
            && downloadingNotificationId.contains(notificationId)
        ) {
            new Handler(Looper.getMainLooper()).post(() ->
            Toast.makeText(
                this,
                    notification,
                Toast.LENGTH_SHORT
            ).show());
            listUrlDownloading.remove(downloadMission.url);
            // Gửi một broadcast hiển thị quảng cáo
//                sendBroadcast(new Intent(ACTION_SHOW_ADS));
            // Notify file vừa tải vào media store
            String downloadSuccessPath = downloadMission.location
                    + File.separator
            + downloadMission.name;
            sendBroadcastDownloadedFile(downloadSuccessPath);

            NotificationCompat.Builder progressNotificationBuilder = createProgressNotificationBuilder()
                .setContentTitle(downloadMission.name)
                .setContentText(notification)
                .setSmallIcon(android.R.drawable.stat_sys_download_done);
            notificationManager.cancel(notificationId);
            notificationManager.notify(notificationId, progressNotificationBuilder.build());

//                Log.i(TAG, "downloadingNotificationId before: " + downloadingNotificationId);
            downloadingNotificationId.remove(notificationId);
//                Log.i(TAG, "downloadingNotificationId after: " + downloadingNotificationId);
        }
    }
    }

    private NotificationCompat.Builder createProgressNotificationBuilder() {
        NotificationCompat.Builder progressNotificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            progressNotificationBuilder = new NotificationCompat.Builder(
                    this,
            DOWNLOADING_CHANNEL_ID
            );
        } else {
            progressNotificationBuilder = new NotificationCompat.Builder(this);
        }
        return progressNotificationBuilder;
    }

    @Override
    public void onProgressUpdate(long done, long total) {
        long now = System.currentTimeMillis();
        long delta = now - mLastTimeStamp;

        if (delta > 2000) {
            postUpdateMessage();
            mLastTimeStamp = now;
        }
    }

    @Override
    public void onFinish() {
        postUpdateMessage();
    }

    @Override
    public void onError(int errCode) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
        this,
        "Error",
        Toast.LENGTH_SHORT
        ).show());
        postUpdateMessage();
    }

    private void postUpdateMessage() {
        showNotification();
    }

    private void sendBroadcastDownloadedFile(String downloadedPath) {
        Log.i(TAG, "sendBroadcastDownloadedFile: " + downloadedPath);

        MediaScannerConnection.scanFile(
            this,
            new String[]{downloadedPath}, new String[]{"audio/*"},
            new MediaScannerConnection.MediaScannerConnectionClient() {

                @Override
                public void onMediaScannerConnected() {

                }

                @Override
                public void onScanCompleted(String path, Uri uri) {
                    // Gửi Broadcast update
                    new Handler(Looper.getMainLooper()).postDelayed(
                        () -> sendBroadcast(new Intent(Utils.ACTION_FINISH_DOWNLOAD)),
                    1000
                    );
                }
            }
        );
    }

    // Wrapper of DownloadManager
    public class DMBinder extends Binder {
        public DownloadManager getDownloadManager() {
            return mManager;
        }

        public void onMissionAdded(DownloadMission mission) {
            mission.addListener(DownloadManagerService.this);
            postUpdateMessage();
        }

        public void onMissionRemoved(DownloadMission mission) {
            mission.removeListener(DownloadManagerService.this);
            postUpdateMessage();
        }
    }

    public static File getPathDownload() {
        File rootFile;
        if (Build.VERSION.SDK_INT >= 30) {
            rootFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            rootFile = Environment.getExternalStorageDirectory();
        }
        return new File(rootFile + "/MusicDownload");
    }
}
