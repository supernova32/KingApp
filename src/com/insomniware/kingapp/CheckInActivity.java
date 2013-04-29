package com.insomniware.kingapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class CheckInActivity extends Activity {
	
	/**
	 * Keep track of the task to ensure we can cancel it if requested.
	 */
	private CheckInTask mCheckTask = null;
	private String room_info = null;
	private String nUsers;
	private ArrayList<HashMap<String, String>> mList;
	private ListView mListView;
	private int option;
	private String room_hash;

	// UI references.
	private TextView mRoomInfo;
	private TextView mUsersInfo;
	private View mCheckInResultView;
	private View mCheckInStatusView;
	private TextView mCheckInStatusMessageView;
	private String mQR;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_check_in);
		mCheckInResultView = findViewById(R.id.check_in_result);
		mCheckInStatusView = findViewById(R.id.check_in_status);
		mRoomInfo = (TextView) findViewById(R.id.room_info);
		mUsersInfo = (TextView) findViewById(R.id.numberOfUsers);
		mCheckInStatusMessageView = (TextView) findViewById(R.id.check_in_status_message);
		mListView = (ListView) findViewById(R.id.courseList);
		// Show the Up button in the action bar.
		setupActionBar();
		Bundle b = getIntent().getExtras();
		option = b.getInt("selected");
		room_hash = b.getString("room_hash");
		if (option == 0){
    		IntentIntegrator integrator = new IntentIntegrator(this);
    		integrator.initiateScan();
		} else if (option == 1) {
			performGPSCheckIn();
		} else if (option == 2) {
			setTitle("Room Info");
			changeStatusText();
			mRoomInfo.setText(b.getString("room_name"));
			showProgress(true);
			mCheckTask = new CheckInTask();
			mCheckTask.execute((Void) null);
		}
	}

	private void performGPSCheckIn() {
		if (mCheckTask != null) {
			return;
		}
		showProgress(true);
		Toast.makeText(this, "Feature not implemented yet", Toast.LENGTH_LONG).show();
		
	}
	
	private void showError(String msn) {
		Toast.makeText(this, msn, Toast.LENGTH_LONG).show();		
	}
	
	private void changeStatusText() {
		mCheckInStatusMessageView.setText("Fetching room info");
	}
	
	private void performQRCheckIn(String qr_result) {
		mQR = qr_result;
		if (mCheckTask != null) {
			return;
		}
		showProgress(true);
		mCheckTask = new CheckInTask();
		mCheckTask.execute((Void) null);		
	}
	
	private HashMap<String, String> fetchRoomInfo(String room_hash) throws JSONException, IOException {
		mList = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> map = new HashMap<String, String>();
		JSONObject jsonobj = new JSONObject();
		jsonobj.put("room_hash", room_hash);
		ConnectionHelper conn = new ConnectionHelper("room_info", jsonobj);
		JSONObject recvdjson = conn.performRequest();
		if (recvdjson.has("message")) {
			return null;
		}
		nUsers = recvdjson.getString("users");
		JSONObject room = recvdjson.getJSONObject("room");
		JSONObject stats = recvdjson.getJSONObject("stats");
		if (room.has("building_id")) {
			room_info = "Room: "+room.getString("building_id")+"/"+room.getString("number");			
		}
		Iterator<?> iter = stats.keys();
		while (iter.hasNext()) {
	        String key = (String) iter.next();
	        String value = null;
	        try {
	            value = stats.getString(key);
	        } catch (JSONException e) {
	            // Something went wrong!
	        }
	        map.put("course", key);
	        map.put("number", value);
	    }
		return map;
	}
	
	private int messageEval(String msn) {
		int return_value = 3;
		if (msn.contentEquals("OK")) {
			return_value = 0;					
		} else if (msn.contentEquals("Distance error")) {
			return_value = 1;
		} else if (msn.contentEquals("Duplicate")) {
			return_value = 2;
		} else if (msn.contentEquals("Record not found")) {
			return_value = 5;			
		}
		return return_value;
	}
	
	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mCheckInStatusView.setVisibility(View.VISIBLE);
			mCheckInStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mCheckInStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mCheckInResultView.setVisibility(View.VISIBLE);
			mCheckInResultView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mCheckInResultView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mCheckInStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mCheckInResultView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		//TextView dummyTextView = (TextView) findViewById(R.id.result_text);
		IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (result != null) {
			// handle scan result
			String contents = result.getContents();
			performQRCheckIn(contents);
			//dummyTextView.setText(contents);
		} else {
			//dummyTextView.setText("Result unsuccessful");			
		}
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.check_in, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_settings:
	        Intent intent = new Intent(this, SettingsActivity.class);
	        startActivity(intent);
	        break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class CheckInTask extends AsyncTask<Void, Void, Integer> {
		HashMap<String, String> localMap;
		
		@Override
		protected Integer doInBackground(Void... params) {
			
			int return_value = 3;
			
			JSONObject jsonobj = new JSONObject();
			ConnectionHelper conn;
			
			try {
				if (option == 2){
					return_value = 4;
					localMap = fetchRoomInfo(room_hash);							
				} else if (mQR != null){					 
					jsonobj.put("room_hash", mQR);
					Location loc = MainPageActivity.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					jsonobj.put("latitude", loc.getLatitude());
					jsonobj.put("longitude", loc.getLongitude());
					conn = new ConnectionHelper("check_in", jsonobj);
					JSONObject recvdjson = conn.performRequest();
					if (recvdjson.has("message")) {
						String msn = recvdjson.getString("message");
						return_value = messageEval(msn);					
					}
					localMap = fetchRoomInfo(mQR);				
					
				} else {					
					conn = new ConnectionHelper("hidden_check", jsonobj);					
				}
				
			} catch (JSONException ex) {
				ex.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return return_value;
		}

		@Override
		protected void onPostExecute(final Integer status) {
			changeStatusText();
			mCheckTask = null;
			showProgress(false);
			if (room_info != null){
				mRoomInfo.setText(room_info);						
			}
			mUsersInfo.setText("Users checked in: " + nUsers);
			mList.add(localMap);
			SimpleAdapter sa = new SimpleAdapter(
	    	        getBaseContext(),
	    	        mList,
	    	        R.layout.location_details,
	    	        new String[] { "course", "number"},
	    	        new int[] { R.id.locationName, R.id.locationDetails});
			mListView.setAdapter(sa);
			
			switch(status) {
				case 0: 
					showError("Check in successful!");
					break;
				case 1: 
					//Distance error
					showError("You are not where you say you are!");
					break;
				case 2:
					//Duplicate
					showError("You've already been here!");
					break;
				case 3:
					//Unknown error
					showError("Houston we have a problem!");
					finish();
					break;
				case 5:
					showError("Wrong QR Code. Try again!");
					finish();
					break;
			}
		}

		@Override
		protected void onCancelled() {
			mCheckTask = null;
			showProgress(false);
		}
	}

}
