package tw.tony.myapp;

/**
* Created by tony on 2017/09/08.
*/

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.R;


import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileOutputStream;


public class GcmIntentService extends IntentService {
	static String TAG = "GcmIntentService";
	static String PackageName = "tw.tony.myapp";
	static String TitleActual = "MyActual";
	static String TitleReport = "MyReport";
	static String Actual_File_Name = "last_actual.txt";
	static String MyPath;
	public static final int NOTIFICATION_ID_ACTUAL = 1;
	public static final int NOTIFICATION_ID_ETC = 10;
	private static int countDown = 0;	

	static boolean isUseAlarmTTS = true, isChangeSysVol = false;
	private static AudioManager am;
	private static int nowVol, preVol; // 要改的和原來的音量
	static boolean isUserStopAlarm = false, isUserResumeAlarm = false;
	
	final static String KEY_MSG_TO_SERVICE = "KEY_MSG_TO_SERVICE";
	final static String ACTION_MSG_TO_SERVICE = "MSG_TO_SERVICE";	
	final static String ACTION_STOP = "ACTION_STOP";
	
	MyReceiver myReceiver;
	
	public GcmIntentService() {
		super(TAG);
		// MyPath = getExternalFilesDir(null); // 這樣取不到正確的位置		
		// XE 的 internal 路徑，只有程式自己可以存取    
		MyPath = "/data/data/tw.tony.myapp/files/"; 				
	}
	
	// 為了要接收 通知 的按鈕 事件
	public class MyReceiver extends BroadcastReceiver	
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{											
			String action = intent.getAction();						
			if(action.equals(ACTION_MSG_TO_SERVICE)){
				String msg = intent.getStringExtra(KEY_MSG_TO_SERVICE);
				if(msg.equals(ACTION_STOP))
				isUserStopAlarm = true;				
				
				Log.d(TAG, "onReceive:" + msg);
			}
		}
	}
	
	@Override
	public void onCreate() {		
		myReceiver = new MyReceiver();
		super.onCreate();
	}

	@Override
	public void onDestroy() {        
		unregisterReceiver(myReceiver); // 要寫在這裡才不會發生錯誤
		super.onDestroy();
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {		
		
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);
		if (!extras.isEmpty()) {
			if (GoogleCloudMessaging.
					MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				Log.d(TAG, "Send error: " + extras.toString());
			} else if (GoogleCloudMessaging.
					MESSAGE_TYPE_DELETED.equals(messageType)) {
				Log.d(TAG, "Deleted messages on server: " + extras.toString());
			} else if (GoogleCloudMessaging.
					MESSAGE_TYPE_MESSAGE.equals(messageType)) {

				Log.i(TAG, "Received: " + extras.toString());
				String msg = extras.getString("message");
				
				Log.d(TAG, "saveGCM_msg");
				saveGCM_msg(msg); // 先存檔，以便 app 帶起後可以得到最新資料
				
				Log.d(TAG, "sendNotification");
				sendNotification(msg);                                
			}
		}
		
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	private void saveGCM_msg(String msg) {
		if(isActual(msg)) {
			try {
				FileOutputStream out = openFileOutput(Actual_File_Name, Context.MODE_PRIVATE);
				out.write(msg.getBytes());
				out.close();
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	}


	private void sendNotification(String msg) {

		int icon = getResources().getIdentifier("ic_launcher" , "drawable", getPackageName());
		long when = System.currentTimeMillis();

		final NotificationManager notificationManager =
		(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		PackageManager pm = getPackageManager();
		Intent mIntent = pm.getLaunchIntentForPackage(PackageName);
		mIntent.setClassName(this, "com.embarcadero.firemonkey.FMXNativeActivity");		
		mIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mIntent, 0);		
		//--------------------------------------------------
		Boolean actual = isActual(msg);
		
		// Tony : Title 將來應該由 Google Message 裡面的 Title 而定
		String Title;
		if(actual) Title = TitleActual;
		else Title = TitleReport;
		
		final NotificationCompat.Builder mBuilder =
		new NotificationCompat.Builder(this)
		.setSmallIcon(icon)  // icon 是必須的，不然通知不會顯現		
		.setContentTitle(Title)
		.setContentText(msg)
		.setAutoCancel(true)
		.setContentIntent(pendingIntent);

		Intent intentStop = new Intent();
		intentStop.setAction(ACTION_MSG_TO_SERVICE);
		intentStop.putExtra(KEY_MSG_TO_SERVICE, ACTION_STOP);
		PendingIntent pIntentStop = PendingIntent.getBroadcast(this, 0, intentStop, PendingIntent.FLAG_UPDATE_CURRENT);
		// 通知的按鈕使用廣播的方式 傳給 IntentService
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_MSG_TO_SERVICE);
		registerReceiver(myReceiver, intentFilter);  // 需要註冊才收得到廣播
		mBuilder.setDeleteIntent(pIntentStop);
			
					
						
		Log.d(TAG, String.format("countDown = %d", countDown));

		// avoid 2 more push messages comes
		notificationManager.cancel(NOTIFICATION_ID_ACTUAL);
		
		// Tony : 若放在 Thread 中，當前後收到推播時，會重覆播聲音
		// 但在 IntentService 內處理，下一筆修的推播來的時話，要等整個處理完了，才能再處理
	 
		MyPlayer myPlayer = new MyPlayer();
		
		if (countDown >= 1) // Tony 修正時間已過，還會叫一聲的 bug
		myPlayer.Play(); // Tony Test
		
		
		for (int count = 0; count <= countDown; count++)
		{
			if(isUserStopAlarm)							
			{
				isUserStopAlarm = false;
				myPlayer.Stop();
				// mBuilder.mActions.clear();								
			}
			
			mBuilder.setContentText(String.format("倒數：%d", countDown-count));
			mBuilder.setProgress(countDown, count, false);     														
			notificationManager.notify(NOTIFICATION_ID_ACTUAL, mBuilder.build());
			try {
				Thread.sleep(1000);							
			} catch (InterruptedException e) {
				Log.d(TAG, "sleep failure");
			}
		}
		
		myPlayer.Stop();
		
		// When the loop is finished, updates the notification				
		mBuilder.setContentTitle("已到達")
		.setContentText(String.format("曾倒數%d秒", countDown))
		.setContentInfo("Info")						
		.setProgress(0,0,false); // Removes the progress bar
		
		mBuilder.mActions.clear();
		notificationManager.notify(NOTIFICATION_ID_ACTUAL, mBuilder.build());



	}


	public class MyPlayer {
		private MediaPlayer mPlayer;
		private boolean isStop;
		
		public MyPlayer(){ mPlayer = new MediaPlayer();}
		
		public void Play(){
			
			if (isChangeSysVol)
			{
				am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				preVol = am.getStreamVolume(AudioManager.STREAM_MUSIC); // MediaPlayer 用的是這個
				am.setStreamVolume(AudioManager.STREAM_MUSIC, nowVol, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
			}
			
			try {
				//getExternalFilesDir
				File mp3 = new File(MyPath, "alarm.mp3"); 
								
				mPlayer.setDataSource(mp3.getPath());        
				mPlayer.prepare();
				
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
				@Override
				public void onCompletion(MediaPlayer mp) {
					mp.release();
					try {
						mp.prepare();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mp.start(); // repeat

				}
			});
			
			mPlayer.start();
			isStop = false;
		}
		
		public void Stop()
		{	
			if(!isStop)
			{
				mPlayer.stop();
				if(isChangeSysVol)
				am.setStreamVolume(AudioManager.STREAM_MUSIC, preVol, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
			    isStop = true;
			}		
			
		}
	} // public class MyPlayer

}

