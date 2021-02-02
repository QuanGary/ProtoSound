package com.makeability.protosound;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.makeability.protosound.ui.home.models.AudioLabel;

import androidx.annotation.LongDef;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT;
import static com.makeability.protosound.utils.Constants.PREDICTION_CHANNEL_ID;

public class MainActivity extends AppCompatActivity {

	public static Socket mSocket;
	private static final String DEBUG_TAG = "NetworkStatusExample";
	public static final boolean TEST_MODEL_LATENCY = false;
	public static final boolean TEST_E2E_LATENCY = false;
	private static final String TEST_E2E_LATENCY_SERVER = "http://128.208.49.41:8789";
	private static final String MODEL_LATENCY_SERVER = "http://128.208.49.41:8790";
	private static final String DEFAULT_SERVER = "http://128.208.49.41:8788";
	private static final String TEST_SERVER = "http://128.208.49.41:5000";
	private static final String TAG = "MainActivity";
	public static boolean notificationChannelIsCreated = false;
	private String db = "";
	private Map<String, Long> soundLastTime = new HashMap<>();
	private List<String> timeLine = new ArrayList<>();
//	{
//		try {
//			mSocket = IO.socket(TEST_SERVER);
//			Log.i(TAG, "Does this thing really run?");
//		} catch (URISyntaxException e) {
//			Log.e(TAG, "Failed to init Socket");
//			e.printStackTrace();
//		}
//	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");

		List<Short> test = new ArrayList<>();
		test.add((short) 12);
		test.add((short) 23);
		test.add((short) 34);
		test.add((short) 45);
		test.add((short) 56);
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("data", new JSONArray(test));
			jsonObject.put("time", "" + System.currentTimeMillis());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		setContentView(R.layout.activity_main);
		BottomNavigationView navView = findViewById(R.id.nav_view);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
				R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
				.build();
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
		NavigationUI.setupWithNavController(navView, navController);

		checkNetworkConnection();
	}


	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy: ");
		super.onDestroy();
		mSocket.disconnect();
	}

	private Emitter.Listener onAudioLabelUIViewMessage = new Emitter.Listener() {
		@Override
		public void call(Object... args) {
			Log.i(TAG, "Received audio label event");
			JSONObject data = (JSONObject) args[0];
			String db = "1.0"; // TODO: Hard code this number for now so we don't have to redesign notification
			String audio_label;
			String accuracy = "1.0";
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalTime localTime = LocalTime.now();
			String time = formatter.format(localTime);
			try {
				audio_label = data.getString("label");
				accuracy = data.getString("confidence");
				db = data.getString("db");
			} catch (JSONException e) {
				return;
			}
			Log.i(TAG, "received sound label from Socket server: " + audio_label + ", " + accuracy);
			AudioLabel audioLabel = new AudioLabel(audio_label, accuracy, time, db,
					null);;
			if (soundLastTime.containsKey(audioLabel.label)) {
				if (System.currentTimeMillis() <= (soundLastTime.get(audioLabel.label) + 5 * 1000)) { //multiply by 1000 to get milliseconds
					Log.i(TAG, "Same sound appear in less than 5 seconds");
					return; // stop sending noti if less than 10 second
				}
			}
			timeLine.add(audioLabel.getTimeAndLabel());
			if (timeLine.size() > 500) {
				timeLine.remove(0);
			}
			soundLastTime.put(audioLabel.label, System.currentTimeMillis());
			ListView listView = (ListView) findViewById(R.id.listView);
			new Handler(Looper.getMainLooper()).post(() -> {
				ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_dialog_layout, timeLine);
				listView.setAdapter(adapter);
				listView.setSelection(adapter.getCount() - 1);
				adapter.notifyDataSetChanged();

			});
		}
	};

	private Emitter.Listener onAudioLabelNotificationMessage = new Emitter.Listener() {
		@Override
		public void call(final Object... args) {
			Log.i(TAG, "Received audio label event");
			JSONObject data = (JSONObject) args[0];
			String db = "1.0"; // TODO: Hard code this number for now so we don't have to redesign notification
			String audio_label;
			String accuracy = "1.0";
			String record_time = "";
			try {
				audio_label = data.getString("label");
				accuracy = data.getString("confidence");
				db = data.getString("db");
			} catch (JSONException e) {
				return;
			}
			Log.i(TAG, "received sound label from Socket server: " + audio_label + ", " + accuracy);
			AudioLabel audioLabel;
			audioLabel = new AudioLabel(audio_label, accuracy, null, db,
					null);
			createAudioLabelNotification(audioLabel);
		}
	};

	public void initSocket(String port) {
		mSocket.connect();
		mSocket.on("android_test_2", onTestMessage);

		// The server will return an audio_label to the phone with label
		mSocket.on("audio_data_s2c", onAudioLabelUIViewMessage);
		mSocket.on("training_complete", onTrainingCompleteMessage);

		mSocket.once(EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				new Handler(Looper.getMainLooper()).post(() -> {
					// Update UI here
					Log.i(TAG, "call: " + args);
					Log.d(TAG, "call: " + mSocket.connected());
					Button confirmPort = (Button) findViewById(R.id.confirm_port);
					confirmPort.setBackgroundColor(Color.GREEN);
					TextView tick = (TextView) findViewById(R.id.tick);
					tick.setText(R.string.port_connected);
					Button submit = (Button) findViewById(R.id.submit);
					submit.setText(R.string.submit_to_server);
				});

//				JSONObject data = (JSONObject) args[0];
//				String userPrototypeAvailable;
//				try {
//					userPrototypeAvailable = data.getString("label");
//					if (userPrototypeAvailable.equals("True")) {
//
//					}
//
//				} catch (JSONException e) {
//					Log.i(TAG, "JSON Exception failed: " + data.toString());
//					return;
//				}

			}
		});
		Log.d(TAG, "connected: " + mSocket.connected());
	}


	private Emitter.Listener onTestMessage = args -> {
		System.out.println(args[0]);
		Log.i(TAG, "Received socket event");
	};

	private Emitter.Listener onTrainingCompleteMessage = args -> {
		Log.i(TAG, "Received training complete!");
		Button submitButton = (Button) findViewById(R.id.submit);
		submitButton.setBackgroundColor(Color.GREEN);
		submitButton.setText(R.string.training_complete);
	};

	private void checkNetworkConnection() {
		ConnectivityManager connMgr =
				(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		boolean isWifiConn = false;
		boolean isMobileConn = false;
		for (Network network : connMgr.getAllNetworks()) {
			NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
			if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				isWifiConn |= networkInfo.isConnected();
			}
			if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
				isMobileConn |= networkInfo.isConnected();
			}
		}
		Log.d(DEBUG_TAG, "Wifi connected: " + isWifiConn);
		Log.d(DEBUG_TAG, "Mobile connected: " + isMobileConn);
	}

	private void createNotificationChannel() {
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(PREDICTION_CHANNEL_ID, PREDICTION_CHANNEL_ID, importance);
			channel.setDescription(PREDICTION_CHANNEL_ID);
			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			channel.enableVibration(true);
			channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			notificationManager.createNotificationChannel(channel);
		}
	}

	/**
	 *
	 * @param audioLabel
	 */
	public void createAudioLabelNotification(AudioLabel audioLabel) {
		// Unique notification for each kind of sound
		// TODO: supposed to have a unique id based on the label returned, but don't know what is the format yet, hard code as a constant for now
		final int NOTIFICATION_ID = getIntegerValueOfSound(audioLabel.label);

		// Disable same sound for 5 seconds
		if (soundLastTime.containsKey(audioLabel.label)) {
			if (System.currentTimeMillis() <= (soundLastTime.get(audioLabel.label) + 5 * 1000)) { //multiply by 1000 to get milliseconds
				Log.i(TAG, "Same sound appear in less than 5 seconds");
				return; // stop sending noti if less than 10 second
			}
		}
		soundLastTime.put(audioLabel.label, System.currentTimeMillis());

		Log.d(TAG, "generateBigTextStyleNotification()");
		if (!notificationChannelIsCreated) {
			createNotificationChannel();
			notificationChannelIsCreated = true;
		}
		int loudness = (int) Double.parseDouble(audioLabel.db);

		db = Integer.toString(loudness);
		//Log.i(TAG, "level" + audioLabel.db + " " + db);

		if(loudness > 70)
			db = "Loud, " + db;
		else if(loudness > 60)
			db = "Med, " + db;
		else
			db = "Soft, " + db;


		Intent intent = new Intent(this, MainActivity.class);       //Just go the MainActivity for now. Replace with other activity if you want more actions.
		String[] dataPassed = {audioLabel.label, Double.toString(audioLabel.confidence), audioLabel.time, audioLabel.db};         //Adding data to be passed back to the main activity
		intent.putExtra("audio_label", dataPassed);
		intent.setAction(Long.toString(System.currentTimeMillis()));
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(getApplicationContext(), PREDICTION_CHANNEL_ID)
				.setSmallIcon(R.drawable.circle_white)
				.setContentTitle(audioLabel.label)
				.setContentText("(" + db + " dB)")
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.setStyle(new NotificationCompat.BigTextStyle()
						.bigText("")
						.setSummaryText(""))
				.setAutoCancel(true) //Remove notification from the list after the user has tapped it
				.setContentIntent(pendingIntent);

		//NOTIFICATION ID depends on the sound and the location so a particular sound in a particular location is only notified once until dismissed
		Log.d(TAG, "Notification Id: " + NOTIFICATION_ID);
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
		notificationManager.notify(NOTIFICATION_ID, notificationCompatBuilder.build());

	}

	public static int getIntegerValueOfSound(String sound){
		int i = 0;
		for (char c : sound.toCharArray())
			i+=(int)c;
		return i;
	}
}