package tw.tony.myapp;

import android.app.Notification;
import android.app.Service;
import android.os.Binder;
import android.os.IBinder;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.app.NotificationManager;
import android.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileOutputStream;

import static java.lang.Thread.sleep;

/**
 * Created by tony on 2017/5/11.
 */

public class MyService extends Service {
    
    private MyBinder mBinder = new MyBinder();
	
	static String TAG = "MyService";
	static String PackageName = "tw.tony.myapp";
  
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        Log.d(TAG, "MyService thread id is " + android.os.Process.myTid());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand()");

    int icon = getResources().getIdentifier("ic_launcher" , "drawable", getPackageName());
    long when = System.currentTimeMillis();

    final NotificationManager notificationManager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    PackageManager pm = getPackageManager();
    Intent mIntent = pm.getLaunchIntentForPackage(PackageName);
    mIntent.setClassName(this, "com.embarcadero.firemonkey.FMXNativeActivity");
    

    mIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mIntent, 0);
    
    // Tony : Title 將來應該由 Google Message 裡面的 Title 而定
    String Title = "MyTitle";
        
    final NotificationCompat.Builder mBuilder =
      new NotificationCompat.Builder(this)
        .setSmallIcon(icon)  // icon 是必須的，不然通知不會顯現
        .setContentTitle(Title)
        .setContentText("ContentText")
        .setAutoCancel(true)
        .setContentIntent(pendingIntent);
				
        
		int Intensity = 1;
		int countDown = 0;		
		mBuilder.setContentText("常駐服務執行中");
        
		startForeground(1, mBuilder.build());
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    class MyBinder extends Binder {

        public void doMyWork() {
            Log.d(TAG, "doMyWork()");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //while (true) {
                      //  Log.d(TAG, "run");
                       // try {
                            //sleep(1000);
                       // } catch (InterruptedException e) {
                       //     e.printStackTrace();
                       // }
                    //}
                }
            }).start();

        }

    }

}
