
package com.spade2021.floatapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {

    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";

    public interface FrameListener {
        void onFrame(Bitmap bitmap);
        void onStopped(String message);
    }

    public static FrameListener listener;

    private static final String CHANNEL_ID = "float_screen_channel";
    private static final int NOTIFICATION_ID = 501;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private Handler handler;
    private int width;
    private int height;
    private int density;

    private long lastFrameTime;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra(
                EXTRA_RESULT_CODE, 0
        );

        Intent resultData = getIntentExtra(intent, EXTRA_RESULT_DATA);

        if (resultData == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            Notification notification = createNotification();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                );
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }

            startCapture(resultCode, resultData);

        } catch (Exception e) {
            notifyStopped("Gagal memulai Live Screen");
            stopCapture();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private Intent getIntentExtra(Intent intent, String key) {
        if (Build.VERSION.SDK_INT >= 33) {
            return intent.getParcelableExtra(key, Intent.class);
        }

        return intent.getParcelableExtra(key);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "FloatApp Live Screen",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("FloatApp Live Screen")
                .setContentText("Berbagi layar sedang aktif")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build();
    }

    private void startCapture(int resultCode, Intent resultData) {

        android.util.DisplayMetrics metrics =
                getResources().getDisplayMetrics();

        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        MediaProjectionManager manager =
                (MediaProjectionManager)
                        getSystemService(MEDIA_PROJECTION_SERVICE);

        mediaProjection = manager.getMediaProjection(
                resultCode,
                resultData
        );

        if (mediaProjection == null) {
            throw new IllegalStateException("MediaProjection tidak tersedia");
        }

        mediaProjection.registerCallback(
                new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        stopCapture();
                        notifyStopped("Live Screen dihentikan");
                        stopSelf();
                    }
                },
                handler
        );

        imageReader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
        );

        imageReader.setOnImageAvailableListener(
                this::readFrame,
                handler
        );

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "FloatAppScreen",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                handler
        );
    }

    private void readFrame(ImageReader reader) {
        Image image = null;

        try {
            image = reader.acquireLatestImage();

            if (image == null) return;

            long now = System.currentTimeMillis();

            if (now - lastFrameTime < 500) return;

            lastFrameTime = now;

            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();

            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();

            int rowPadding =
                    rowStride - pixelStride * width;

            int paddedWidth =
                    width + rowPadding / pixelStride;

            Bitmap padded = Bitmap.createBitmap(
                    paddedWidth,
                    height,
                    Bitmap.Config.ARGB_8888
            );

            buffer.rewind();
            padded.copyPixelsFromBuffer(buffer);

            Bitmap frame = Bitmap.createBitmap(
                    padded,
                    0,
                    0,
                    width,
                    height
            );

            padded.recycle();

            FrameListener currentListener = listener;

            if (currentListener != null) {
                currentListener.onFrame(frame);
            } else {
                frame.recycle();
            }

        } catch (Exception ignored) {
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    private void notifyStopped(String message) {
        FrameListener currentListener = listener;

        if (currentListener != null) {
            handler.post(() -> currentListener.onStopped(message));
        }
    }

    private void stopCapture() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(null, null);
            imageReader.close();
            imageReader = null;
        }

        if (mediaProjection != null) {
            mediaProjection = null;
        }
    }

    @Override
    public void onDestroy() {
        stopCapture();
        listener = null;

        stopForeground(true);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
              }
      
