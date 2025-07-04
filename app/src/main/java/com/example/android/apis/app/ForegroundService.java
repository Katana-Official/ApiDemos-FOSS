/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.apis.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.example.android.apis.R;

/**
 * This is an example of implementing an application service that can
 * run in the "foreground".  It shows how to code this to work well by using
 * the improved Android 2.0 APIs when available and otherwise falling back
 * to the original APIs.  Yes: you can take this exact code, compile it
 * against the Android 2.0 SDK, and it will run against everything down to
 * Android 1.0.
 */
public class ForegroundService extends Service {
    static final String ACTION_FOREGROUND = "com.example.android.apis.FOREGROUND";
    static final String ACTION_FOREGROUND_WAKELOCK = "com.example.android.apis.FOREGROUND_WAKELOCK";
    static final String ACTION_BACKGROUND = "com.example.android.apis.BACKGROUND";
    static final String ACTION_BACKGROUND_WAKELOCK = "com.example.android.apis.BACKGROUND_WAKELOCK";

    private NotificationManager mNM;

    private PowerManager.WakeLock mWakeLock;
    private Handler mHandler = new Handler();
    private Runnable mPulser = new Runnable() {
        @Override public void run() {
            Log.i("ForegroundService", "PULSE!");
            mHandler.postDelayed(this, 5*1000);
        }
    };

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        handleDestroy();
        // Make sure our notification is gone.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(R.string.foreground_service_started);
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final boolean usingWakelock = ACTION_FOREGROUND_WAKELOCK.equals(intent.getAction());
        if (ACTION_FOREGROUND.equals(intent.getAction()) || usingWakelock) {
            // In this sample, we'll use the same text for the ticker and the expanded notification
            CharSequence text = getText(R.string.foreground_service_started);

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, Controller.class), PendingIntent.FLAG_IMMUTABLE);

            int showMode = (usingWakelock)
                    ? Notification.FOREGROUND_SERVICE_IMMEDIATE
                    : Notification.FOREGROUND_SERVICE_DEFERRED;
            // Set the info for the views that show in the notification panel.  In the
            // wakelock flow, also force the notification to display immediately.
            Notification.Builder notificationBuilder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.stat_sample)  // the status icon
                    .setTicker(text)  // the status text
                    .setWhen(System.currentTimeMillis())  // the time stamp
                    .setContentTitle(getText(R.string.alarm_service_label))  // the label
                    .setContentText(text)  // the contents of the entry
                    .setContentIntent(contentIntent);  // The intent to send when clicked
//                    .setForegroundServiceBehavior(showMode)
//                    .build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notificationBuilder.setForegroundServiceBehavior(
                        showMode
                );
            }

            startForeground(R.string.foreground_service_started, notificationBuilder.build());

        } else if (ACTION_BACKGROUND.equals(intent.getAction())
                || ACTION_BACKGROUND_WAKELOCK.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(R.string.foreground_service_started);
            }
        }

        if (ACTION_FOREGROUND_WAKELOCK.equals(intent.getAction())
                || ACTION_BACKGROUND_WAKELOCK.equals(intent.getAction())) {
            if (mWakeLock == null) {
                mWakeLock = getSystemService(PowerManager.class).newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK, "wake-service");
                mWakeLock.acquire();
            } else {
                releaseWakeLock();
            }
        }

        mHandler.removeCallbacks(mPulser);
        mPulser.run();

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    void handleDestroy() {
        releaseWakeLock();
        mHandler.removeCallbacks(mPulser);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    // ----------------------------------------------------------------------

    /**
     * <p>Example of explicitly starting and stopping the {@link ForegroundService}.
     * 
     * <p>Note that this is implemented as an inner class only keep the sample
     * all together; typically this code would appear in some separate class.
     */
    public static class Controller extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setContentView(R.layout.foreground_service_controller);

            // Watch for button clicks.
            Button button = (Button)findViewById(R.id.start_foreground);
            button.setOnClickListener(mForegroundListener);
            button = (Button)findViewById(R.id.start_foreground_wakelock);
            button.setOnClickListener(mForegroundWakelockListener);
            button = (Button)findViewById(R.id.start_background);
            button.setOnClickListener(mBackgroundListener);
            button = (Button)findViewById(R.id.start_background_wakelock);
            button.setOnClickListener(mBackgroundWakelockListener);
            button = (Button)findViewById(R.id.stop);
            button.setOnClickListener(mStopListener);
            button = (Button)findViewById(R.id.start_foreground_2);
            button.setOnClickListener(mForegroundListener2);
            button = (Button)findViewById(R.id.stop_2);
            button.setOnClickListener(mStopListener2);
            button = (Button)findViewById(R.id.start_foreground_2_alarm);
            button.setOnClickListener(mForegroundAlarmListener);
        }

        private OnClickListener mForegroundListener = new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ForegroundService.ACTION_FOREGROUND);
                intent.setClass(Controller.this, ForegroundService.class);
                startService(intent);
            }
        };

        private OnClickListener mForegroundWakelockListener = new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ForegroundService.ACTION_FOREGROUND_WAKELOCK);
                intent.setClass(Controller.this, ForegroundService.class);
                startService(intent);
            }
        };

        private OnClickListener mBackgroundListener = new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ForegroundService.ACTION_BACKGROUND);
                intent.setClass(Controller.this, ForegroundService.class);
                startService(intent);
            }
        };

        private OnClickListener mBackgroundWakelockListener = new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ForegroundService.ACTION_BACKGROUND_WAKELOCK);
                intent.setClass(Controller.this, ForegroundService.class);
                startService(intent);
            }
        };

        private OnClickListener mStopListener = new OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(Controller.this,
                        ForegroundService.class));
            }
        };

        private OnClickListener mForegroundListener2 = new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ForegroundService.ACTION_FOREGROUND);
                intent.setClass(Controller.this, ForegroundService2.class);
                startService(intent);
            }
        };

        private OnClickListener mForegroundAlarmListener = new OnClickListener() {
            public void onClick(View v) {
                final Context ctx = Controller.this;

                final Intent intent = new Intent(ForegroundService.ACTION_FOREGROUND);
                intent.setClass(ctx, ForegroundService2.class);

                PendingIntent pi = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pi = PendingIntent.getForegroundService(ctx, 0, intent, 0);
                }
                AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                if (pi != null) {
                    am.setExact(AlarmManager.ELAPSED_REALTIME,
                            SystemClock.elapsedRealtime() + 15_000,
                            pi);
                }
                Log.i("ForegroundService", "Starting service in 15 seconds");
            }
        };

        private OnClickListener mStopListener2 = new OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(Controller.this,
                        ForegroundService2.class));
            }
        };

    }
}
