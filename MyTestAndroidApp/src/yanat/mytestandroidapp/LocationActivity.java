package yanat.mytestandroidapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.w3c.dom.Document;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.Session;
import com.facebook.android.Facebook;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class LocationActivity extends FragmentActivity implements
		LocationListener, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	// store the co-ordinates of the location the user entered
	private double latitude;
	private double longitude;

	// store the location object of the destination that the user enters
	private Location dLocation;

	private LocationClient mLocationClient;
	private Location mCurrentLocation;

	// Define an object that holds accuracy and frequency parameters
	private LocationRequest mLocationRequest;

	/*
	 * Note if updates have been turned on. Starts out as "false"; is set to
	 * "true" in the method handleRequestSuccess of LocationUpdateReceiver.
	 */
	boolean mUpdatesRequested;

	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;

	// Update frequency in seconds
	public static final int UPDATE_INTERVAL_IN_SECONDS = 30;

	// Update frequency in milliseconds
	private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
			* UPDATE_INTERVAL_IN_SECONDS;

	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 20;

	// A fast frequency ceiling in milliseconds
	private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND
			* FASTEST_INTERVAL_IN_SECONDS;

	// request code to send to Google Play Services
	// This code is returned in Activity.onResult
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	// the google map object
	private GoogleMap map;

	// the Document object to draw the directions from google map
	private Document document;

	// the GMapV2Direction object to get the directions
	private GMapV2Direction md;

	private LatLng from, to;

	String add;

	// the facebook app ID
	private static String APP_ID = "295664620583278";

	/*
	 * Initialize the Activity
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location);

		// Create the LocationRequest object
		mLocationRequest = LocationRequest.create();

		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		// Set the update interval to 5 seconds
		mLocationRequest.setInterval(UPDATE_INTERVAL);

		// Set the fastest update interval to 1 second
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

		// Start with updates turned off
		mUpdatesRequested = true;

		// To track the user's preference, store it in your app's
		// SharedPreferences in onPause() and retrieve it in onResume().
		Intent intent = getIntent();
		add = intent.getStringExtra("address");

		// Use the geocoder class to covert the destination address into the
		// destination coordinates
		Geocoder geocoder = new Geocoder(this, Locale.getDefault());

		try {
			List<Address> addresses = geocoder.getFromLocationName(add, 1);
			if (addresses.size() > 0) {
				latitude = addresses.get(0).getLatitude();
				longitude = addresses.get(0).getLongitude();
				dLocation = new Location("DESTINATION");
				dLocation.setLatitude(latitude);
				dLocation.setLongitude(longitude);
				mLocationClient = new LocationClient(this, this, this);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		mLocationClient = new LocationClient(this, this, this);
	}

	/*
	 * Called when the Activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// Connect the client.
		mLocationClient.connect();
	}

	/*
	 * Called when the Activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		// Disconnecting the client invalidates it.
		mLocationClient.disconnect();

		super.onStop();
	}

	// Called when the activity resumes
	@Override
	protected void onResume() {
		super.onResume();
		mUpdatesRequested = true;
		mLocationClient.connect();
		if (mCurrentLocation != null) {
			initializeMap();
		}
	}

	/*
	 * Called when the activity pauses
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/*
	 * Handle results returned to this Activity by other Activities started with
	 * startActivityForResult(). In particular, the method onConnectionFailed()
	 * in LocationUpdateRemover and LocationUpdateRequester may call
	 * startResolutionForResult() to start an Activity that handles Google Play
	 * services problems. The result of this call returns here, to
	 * onActivityResult.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		// Choose what to do based on the request code
		switch (requestCode) {

		// If the request code matches the code sent in onConnectionFailed
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:

			switch (resultCode) {
			// If Google Play services resolved the problem
			case Activity.RESULT_OK:

				// Log the result
				Log.d("APPTAG", "Error Here 1");
				break;

			// If any other result was returned by Google Play services
			default:
				// Log the result
				Log.d("APPTAG", "Error Here 2");
				break;
			}

			// If any other request code was received
		default:
			// Report that this Activity received an unknown requestCode
			Log.d("APPTAG", "Error Here 3");

			break;
		}
	}

	// Method to check if the google play services are indeed available or not
	private boolean servicesConnected() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("Location Updates", "Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			// Get the error dialog from Google Play services
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
					resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

			// If Google Play services can provide an error dialog
			if (errorDialog != null) {
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				// Set the dialog in the DialogFragment
				errorFragment.setDialog(errorDialog);
				// Show the error dialog in the DialogFragment
				errorFragment.show(getSupportFragmentManager(),
						"Location Updates");
			}
			return false;
		}
	}

	@Override
	public void onLocationChanged(Location location) {

		// update the current location
		mCurrentLocation = location;

		// Display the latitude
		Log.d("Current Latitude",
				String.valueOf(mCurrentLocation.distanceTo(dLocation)));

		float distance = mCurrentLocation.distanceTo(dLocation);

		if (2 > distance) {
			sendNotification("You have reached the coffee shop");
			postFeed("http://www.androidhive.info/2012/03/android-facebook-connect-tutorial/");
		} else if (100 > distance) {
			sendNotification("You are less than 100m away from the coffee shop");
			postFeed("http://www.androidhive.info/2012/03/android-facebook-connect-tutorial/");
		}

		// Call the function to draw the map
		initializeMap();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (result.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				result.startResolutionForResult(this,
						CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
			showErrorDialog(result.getErrorCode());
		}
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		// Display the connection status
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();

		// Get the last location of the user
		mCurrentLocation = mLocationClient.getLastLocation();

		if (mUpdatesRequested) {
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
			Log.d("Current Latitude2",
					String.valueOf(mCurrentLocation.getLatitude()));
			// Call the function to draw the map
			initializeMap();
		}
	}

	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}

	private void initializeMap() {

		md = new GMapV2Direction();

		map = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();

		from = new LatLng(mCurrentLocation.getLatitude(),
				mCurrentLocation.getLongitude());
		to = new LatLng(latitude, longitude);

		map.clear();

		map.addMarker(new MarkerOptions().position(from).title(
				"Current Location"));

		map.addMarker(new MarkerOptions().position(to).title("Destination"));

		// Move the camera instantly to akgec with a zoom of 5.
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(from, 5));

		// Zoom in, animating the camera.
		map.animateCamera(CameraUpdateFactory.zoomTo(5), 2000, null);

		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			new MapDraw().execute();
		} else {
			map.addPolyline(new PolylineOptions().add(from, to).width(4)
					.color(Color.BLACK));
		}

	}

	/*
	 * the function to send the notification
	 */
	private void sendNotification(String message) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("My notification").setContentText(message);

		// Sets an ID for the notification
		int mNotificationId = 001;
		// Gets an instance of the NotificationManager service
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify(mNotificationId, mBuilder.build());
	}

	/*
	 * post to the users timeline that they have reached the coffee shop
	 */
	private void postFeed(String message) {
		Bundle params = new Bundle();
		params.putString("name", "An example parameter");
		params.putString("link", message);

		WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(this,
				Session.getActiveSession(), params)).setOnCompleteListener(
				new OnCompleteListener() {

					@Override
					public void onComplete(Bundle values,
							FacebookException error) {
						// TODO Auto-generated method stub

					}
				}).build();
		feedDialog.show();
	}

	/**
	 * Show a dialog returned by Google Play services for the connection error
	 * code
	 * 
	 * @param errorCode
	 *            An error code returned from onConnectionFailed
	 */
	private void showErrorDialog(int errorCode) {

		// Get the error dialog from Google Play services
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
				this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

		// If Google Play services can provide an error dialog
		if (errorDialog != null) {

			// Create a new DialogFragment in which to show the error dialog
			ErrorDialogFragment errorFragment = new ErrorDialogFragment();

			// Set the dialog in the DialogFragment
			errorFragment.setDialog(errorDialog);

			// Show the error dialog in the DialogFragment
			errorFragment.show(getSupportFragmentManager(), "Error");
		}
	}

	// Define a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment {
		// Global field to contain the error dialog
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	// The private class which draws the directions in the map
	private class MapDraw extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			document = md.getDocument(from, to, GMapV2Direction.MODE_DRIVING);
			return null;
		}

		protected void onPostExecute(String url) {

			ArrayList<LatLng> directionPoint = md.getDirection(document);
			PolylineOptions rectLine = new PolylineOptions().width(3).color(
					Color.RED);

			for (int i = 0; i < directionPoint.size(); i++) {
				rectLine.add(directionPoint.get(i));
			}

			map.addPolyline(rectLine);

		}
	}
}
