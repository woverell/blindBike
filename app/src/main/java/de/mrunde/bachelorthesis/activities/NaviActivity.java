package de.mrunde.bachelorthesis.activities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.app.DialogFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.hardware.Camera.Size;

import de.mrunde.bachelorthesis.instructions.ShapePointManager;
import edu.csueb.ilab.blindbike.blindbike.BB_Parameters;

import com.mapquest.android.maps.DefaultItemizedOverlay;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.LineOverlay;
import com.mapquest.android.maps.MapActivity;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;
import com.mapquest.android.maps.OverlayItem;
import com.mapquest.android.maps.RouteManager;

import de.mrunde.bachelorthesis.instructions.DistanceInstruction;
import edu.csueb.ilab.blindbike.blindbike.EntranceActivity;
import edu.csueb.ilab.blindbike.blindbike.R;
import de.mrunde.bachelorthesis.basics.Landmark;
import de.mrunde.bachelorthesis.basics.LandmarkCategory;
import de.mrunde.bachelorthesis.basics.Maneuver;
import de.mrunde.bachelorthesis.basics.MyDefaultItemizedOverlay;
import de.mrunde.bachelorthesis.instructions.GlobalInstruction;
import de.mrunde.bachelorthesis.instructions.Instruction;
import de.mrunde.bachelorthesis.instructions.InstructionManager;
import de.mrunde.bachelorthesis.instructions.LandmarkInstruction;
import edu.csueb.ilab.blindbike.intersection.XCrossing;
import edu.csueb.ilab.blindbike.lightdetection.LightDetector;
import edu.csueb.ilab.blindbike.obstacleavoidance.ObstacleAvoidance;
import edu.csueb.ilab.blindbike.roadfollowing.GlobalRF;
import edu.csueb.ilab.blindbike.roadfollowing.LocalRF;
import edu.csueb.ilab.blindbike.blindbike.CustomizeView;

/**
 * This is the navigational activity which is started by the MainActivity. It
 * navigates the user from his current location to the desired destination.
 * 
 * @author Marius Runde
 */
public class NaviActivity extends MapActivity implements OnInitListener,
		LocationListener, CameraBridgeViewBase.CvCameraViewListener2,View.OnTouchListener, SensorEventListener {

	private static final String TAG = "OCVSample::Activity";
	private Mat mRgba;
	private Mat mGray;
	private CustomizeView mOpenCvCameraView;

	private XCrossing xCrossing;
	private LightDetector lightDetector;
	private GlobalRF globalRF;
	private LocalRF localRF;
	private ObstacleAvoidance obstacleAvoidance;

	// device sensor manager
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mMagnetometer;

	private float[] mLastAccelerometer = new float[3];
	private float[] mLastMagnetometer = new float[3];
	private boolean mLastAccelerometerSet = false;
	private boolean mLastMagnetometerSet = false;
	private float[] mR = new float[9];
	private float[] mOrientation = new float[3];
	private float mCurrentDegree = 0f;


	private List<Size> mResolutionList; //list of supported resolutions by camera

	// --- The indexes of the overlays ---
	/**
	 * Index of the local landmark overlay
	 */
	private final int INDEX_OF_LANDMARK_OVERLAY = 3;

	// --- End of indexes ---

	// --- The graphical user interface (GUI) ---
	/**
	 * Instruction view (verbal)
	 */
	private TextView tv_instruction;

	/**
	 * text view to show the desired bearing
	 */
	private TextView bearingTextView;

	/**
	 * text view to show the measured bearing
	 */
	private TextView measuredBearingTextView;

	/**
	 * text view to show the merge direction
	 */
	private TextView directionsTextView;

	/**
	 * Instruction view (image)
	 */
	private ImageView iv_instruction;

	/**
	 * Map view
	 */
	private MapView map;

	/**
	 * An overlay to display the user's location
	 */
	private MyLocationOverlay myLocationOverlay;

    // Reroute Button
    private Button reroute_button;

	// --- End of GUI ---

	// --- The route and instruction objects ---
	/**
	 * Current location as String (for RouteManager only!)
	 */
	private String str_currentLocation;

	/**
	 * Destination as String (for RouteManager and as title of destination
	 * overlay)
	 */
	private String str_destination;

	/**
	 * text to display for instruction to user for staying near edge of road
	 */
	private String directionsText;

	/**
	 * Latitude of the destination
	 */
	private double destination_lat;

	/**
	 * Longitude of the destination
	 */
	private double destination_lng;

	/**
	 * Route manager for route calculation
	 */
	private RouteManager rm;

	/**
	 * Route options (already formatted as a String)
	 */
	private String routeOptions;

	/**
	 * Instruction manager that creates instructions
	 */
	private InstructionManager im;

	/**
	 * Location manager to monitor the user's location
	 */
	private LocationManager lm;

	/**
	 * Shape point manager to store and manage shape points
	 */
	private ShapePointManager spm;

	/**
	 * Location provider of the LocationManager
	 */
	private String provider;

	/**
	 * Minimum distance for a route segment to use a NowInstruction
	 */
	private final int MIN_DISTANCE_FOR_NOW_INSTRUCTION = 100;

	/**
	 * The NowInstruction will be returned at this distance before reaching the
	 * next decision point
	 */
	private final int DISTANCE_FOR_NOW_INSTRUCTION = 48;

	/**
	 * Variable to control if the usage of a NowInstruction has been checked
	 */
	private boolean nowInstructionChecked = false;

	/**
	 * Variable to control if a NowInstruction will be used
	 */
	private boolean nowInstructionUsed = false;

	/**
	 * Maximum distance to a decision point when it is supposed to be reached
	 */
	private final int MAX_DISTANCE_TO_DECISION_POINT = 32;

	/**
	 * The current desired bearing based on shape points of the route
	 * In compass degrees 0 to 360, 0 and 360 being north, 90 east
	 * 180 south, 270 west
	 */
	private double desiredBearing = -1;

	/**
	 * The current bearing measured from the phone compass sensor
	 */
	private double currentBearing = -1;

	/**
	 * This stores the previous distance to the candidate shape point
	 */
	private double last_distance_to_candidateSP = 0;

	/**
	 * This will be true when the current shape point is found
	 */
	private boolean currentSPFound = false;

	/**
	 * This will be true when the candidate shape point is found
	 */
	private boolean candidateSPFound = false;

	/**
	 * This stores the candidate shape point
	 */
	private GeoPoint candidateSP;

	private boolean beenWithinNodeWindow = false;

	/**
	 * Store the last distance between the next decision point and the current
	 * location. Is set to 0 when the instruction is updated.
	 */
	private double lastDistanceDP1 = 0;

	/**
	 * Store the last distance between the decision point after next and the
	 * current location. Is set to 0 when the instruction is updated.
	 */
	private double lastDistanceDP2 = 0;

	/**
	 * Counts the distance changes between the next decision point, the decision
	 * point after next and the current location. Is set to 0 after the
	 * instruction has been updated. The instruction is updated when the counter
	 * reaches its maximum negative value (<code>(-1) * MAX_COUNTER_VALUE</code>
	 * ) or the next decision point has been reached. If the counter reaches the
	 * maximum positive value (<code>MAX_COUNTER_VALUE</code>), the whole
	 * guidance will be updated.
	 */
	private int distanceCounter = 0;

	/**
	 * Maximum value for the <code>distanceCounter</code>.
	 */
	private final int MAX_COUNTER_VALUE = 5;

	// --- End of route and instruction objects ---

	private boolean haveSavedAlready; // used for test_mode in BB_Parameters for saving image to phone gallery

	/**
	 * TextToSpeech for audio output
	 */
	private TextToSpeech tts;

	private Mat dummy_Image;

	/**
	 * Store all logs of the <code>onLocationChanged()</code> and
	 * <code>updateInstruction()</code> methods in this String to display them
	 * on the application via the <code>OptionsMenu</code>
	 */
	private String debugger = "";

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i(TAG, "OpenCV loaded successfully");
					mOpenCvCameraView.enableView();
					mOpenCvCameraView.setOnTouchListener(NaviActivity.this);
				} break;
				case LoaderCallbackInterface.INIT_FAILED:
					Log.i("Test","Init Failed");
					break;
				case LoaderCallbackInterface.INSTALL_CANCELED:
					Log.i("Test","Install Cancelled");
					break;
				case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
					Log.i("Test","Incompatible Version");
					break;
				case LoaderCallbackInterface.MARKET_ERROR:
					Log.i("Test","Market Error");
					break;
				default:
				{
					Log.i("Test", "OpenCV Manager Install");
					super.onManagerConnected(status);
				} break;
			}
		}
	};

	/**
	 * This method is called when the application has been started
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if(getResources().getBoolean(R.bool.DEVELOPMENT_MODE)) {
            setContentView(R.layout.navi);
        }else
        {
            setContentView(R.layout.navi);
        }

		// Setup Detector Instance
		xCrossing = new XCrossing();
		lightDetector = new LightDetector();

		// Get the route information from the intent
		Intent intent = getIntent();
		this.str_currentLocation = intent.getStringExtra("str_currentLocation");
		this.str_destination = intent.getStringExtra("str_destination");
		this.destination_lat = intent.getDoubleExtra("destination_lat", 0.0);
		this.destination_lng = intent.getDoubleExtra("destination_lng", 0.0);
		this.routeOptions = intent.getStringExtra("routeOptions");

		// Initialize the TextToSpeech
		tts = new TextToSpeech(this, this);

		// Initialize the LocationManager
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Choose the location provider
		Criteria criteria = new Criteria();
		provider = lm.getBestProvider(criteria, false);
		Location location = lm.getLastKnownLocation(provider);

		if (location != null) {
			Log.e("Test", "Provider " + provider + " has been selected.");
			onLocationChanged(location);
		} else {
			Log.e("Test", "Location not available");
		}

		// Setup the whole GUI and map
		setupGUI();
		setupMapView();
		setupMyLocation();

		// Initialize sensor manager
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		// Add the destination overlay to the map(puts flag on map)
		addDestinationOverlay(destination_lat, destination_lng);

		// Calculate the route
		//calculateRoute();

		// Get the guidance information and create the instructions
		getGuidance();

		mOpenCvCameraView = (CustomizeView) findViewById(R.id.opencv_java_camera_view);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);

		//mOpenCvCameraView.mCamera is null
	}

	/**
	 * Set up the GUI
	 */
	private void setupGUI() {
		this.tv_instruction = (TextView) findViewById(R.id.tv_instruction);
		this.iv_instruction = (ImageView) findViewById(R.id.iv_instruction);
        this.reroute_button = (Button) findViewById(R.id.reroute_button);
		this.bearingTextView = (TextView) findViewById(R.id.bearingTextView);
		this.measuredBearingTextView = (TextView) findViewById(R.id.measuredBearingTextView);
		this.directionsTextView = (TextView) findViewById(R.id.directionsTextView);
        reroute_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Call updateGuidance to restart the activity
				updateGuidance();
			}
		});
	}



	/**
	 * Set up the map and disable user interaction
	 */
	private void setupMapView() {
		this.map = (MapView) findViewById(R.id.map);
		map.setBuiltInZoomControls(true); // WILLIAM: Toggling to see difference
		map.setClickable(false);
		map.setLongClickable(false);
	}

	/**
	 * Set up a MyLocationOverlay and execute the runnable once a location has
	 * been fixed
	 */
	private void setupMyLocation() {
		// Check if the GPS is enabled
		if (!((LocationManager) getSystemService(LOCATION_SERVICE))
				.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			// Open dialog to inform the user that the GPS is disabled
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getString(R.string.gpsDisabled));
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.openSettings,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Open the location settings if it is disabled
							Intent intent = new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							startActivity(intent);
						}
					});
			builder.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Dismiss the dialog
							dialog.cancel();
						}
					});

			// Display the dialog
			AlertDialog dialog = builder.create();
			dialog.show();
		}

		// Set up the myLocationOverlay
		this.myLocationOverlay = new MyLocationOverlay(this, map);
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.setMarker(
				getResources().getDrawable(R.drawable.my_location), 0);
		myLocationOverlay.runOnFirstFix(new Runnable() {

			@Override
			public void run() {
				GeoPoint currentLocation = myLocationOverlay.getMyLocation();
				map.getController().animateTo(currentLocation);
				map.getController().setZoom(18);
				map.getOverlays().add(myLocationOverlay);
				myLocationOverlay.setFollowing(true); // WILLIAM: This should set the map to follow the user
			}
		});
	}

	/**
	 * Add the destination overlay to the map
	 * 
	 * @param lat
	 *            Latitude of the destination
	 * @param lng
	 *            Longitude of the destination
	 */
	private void addDestinationOverlay(double lat, double lng) {
		// Create a GeoPoint object of the destination
		GeoPoint destination = new GeoPoint(lat, lng);

		// Create the destination overlay
		OverlayItem oi_destination = new OverlayItem(destination,
				"Destination", str_destination);
		final DefaultItemizedOverlay destinationOverlay = new DefaultItemizedOverlay(
				getResources().getDrawable(R.drawable.destination_flag));
		destinationOverlay.addItem(oi_destination);

		// Add the overlay to the map
		map.getOverlays().add(destinationOverlay);
	}


	/**
	 * Get the guidance information from MapQuest
	 */
	private void getGuidance() {
		// Create the URL to request the guidance from MapQuest
		String url;
		try {
			url = "https://open.mapquestapi.com/guidance/v1/route?key="
					+ getResources().getString(R.string.apiKey)
					+ "&from="
					+ URLEncoder.encode(str_currentLocation, "UTF-8")
					+ "&to="
					+ URLEncoder.encode(str_destination, "UTF-8")
					+ "&narrativeType=text&fishbone=false&callback=renderBasicInformation";
            Log.v("WILLIAM:", "WILLIAM  " + url);

		} catch (UnsupportedEncodingException e) {
			Log.e("NaviActivity",
					"Could not encode the URL. This is the error message: "
							+ e.getMessage());
			return;
		}

		// Get the data. The instructions are created afterwards.
		GetJsonTask jsonTask = new GetJsonTask();
		jsonTask.execute(url);
	}

	/**
	 * This is a class to get the JSON file asynchronously from the given URL.
	 * 
	 * @author Marius Runde
	 */
	private class GetJsonTask extends AsyncTask<String, Void, JSONObject> {

		/**
		 * Progress dialog to inform the user about the download
		 */
		private ProgressDialog progressDialog = new ProgressDialog(
				NaviActivity.this);

		/**
		 * Count the time needed for the data download
		 */
		private int downloadTimer;

		@Override
		protected void onPreExecute() {
			// Display progress dialog
			progressDialog.setMessage("Downloading guidance...");
			progressDialog.show();
			progressDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					// Cancel the download when the "Cancel" button has been
					// clicked
					GetJsonTask.this.cancel(true);
				}
			});

			// Set timer to current time
			downloadTimer = Calendar.getInstance().get(Calendar.SECOND);
		}

		@Override
		protected JSONObject doInBackground(String... url) {
			// Get the data from the URL
			String output = "";
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
			try {
				response = httpclient.execute(new HttpGet(url[0]));
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					out.close();
					output = out.toString();
				} else {
					// Close the connection
					response.getEntity().getContent().close();
					throw new IOException(statusLine.getReasonPhrase());
				}
			} catch (Exception e) {
				Log.e("GetJsonTask",
						"Could not get the data. This is the error message: "
								+ e.getMessage());
				return null;
			}

			// Delete the "renderBasicInformation" stuff at the beginning and
			// end of the output if needed to convert it to a JSONObject
			if (output.startsWith("renderBasicInformation(")) {
				output = output.substring(23, output.length());
			}
			if (output.endsWith(");")) {
				output = output.substring(0, output.length() - 2);
			}

			// Convert the output to a JSONObject
			try {
				JSONObject result = new JSONObject(output);
				return result;
			} catch (JSONException e) {
				Log.e("GetJsonTask",
						"Could not convert output to JSONObject. This is the error message: "
								+ e.getMessage());
				return null;
			}
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			// Dismiss progress dialog
			progressDialog.dismiss();

			// Write the time needed for the download into the log
			downloadTimer = Calendar.getInstance().get(Calendar.SECOND)
					- downloadTimer;
			Log.i("GetJsonTask", "Completed guidance download in "
					+ downloadTimer + " seconds");

			// Check if the download was successful
			if (result == null) {
				// Could not receive the JSON
				Toast.makeText(NaviActivity.this,
						getResources().getString(R.string.routeNotCalculated),
						Toast.LENGTH_SHORT).show();
				// Finish the activity to return to MainActivity
				finish();
			} else {
				// Create the instructions
				createInstructions(result);

				// Draw the route and display the first instruction
				drawRoute(result);
			}
		}
	}

	/**
	 * Create the instructions for the navigation
	 * 
	 * @param guidance
	 *            The guidance information from MapQuest
	 */
	private void createInstructions(JSONObject guidance) {
		// Load the landmarks as a JSONObject from res/raw/landmarks.json
		InputStream is = getResources().openRawResource(R.raw.landmarks);
		JSONObject landmarks = null;
		try {
			String rawJson = IOUtils.toString(is, "UTF-8");
			landmarks = new JSONObject(rawJson);
		} catch (Exception e) {
			// Could not load landmarks
			Log.e("NaviActivity",
					"Could not load landmarks. This is the error message: "
							+ e.getMessage());
		}

		// Load the street furniture as a JSONArray from
		// res/raw/streetfurniture.json
		is = getResources().openRawResource(R.raw.streetfurniture);
		JSONArray streetFurniture = null;
		try {
			String rawJson = IOUtils.toString(is, "UTF-8");
			streetFurniture = new JSONArray(rawJson);
		} catch (Exception e) {
			// Could not load street furniture
			Log.e("NaviActivity",
					"Could not load street furniture. This is the error message: "
							+ e.getMessage());
		}

		// Load the intersections as a JSONArray from res/raw/intersections.json
		is = getResources().openRawResource(R.raw.intersections);
		JSONArray intersections = null;
		try {
			String rawJson = IOUtils.toString(is, "UTF-8");
			intersections = new JSONArray(rawJson);
		} catch (Exception e) {
			// Could not load intersections
			Log.e("NaviActivity",
					"Could not load intersections. This is the error message: "
							+ e.getMessage());
		}

		// Create the instruction manager
		im = new InstructionManager(guidance, landmarks, streetFurniture,
				intersections);
		// Check if the import was successful
		if (im.isImportSuccessful()) {
			// Create the instructions
			im.createInstructions();
		} else {
			// Import was not successful
			Toast.makeText(this,
					getResources().getString(R.string.jsonImportNotSuccessful),
					Toast.LENGTH_SHORT).show();
			// Finish the activity to return to MainActivity
			finish();
		}

		// Create shape point manager
		spm = new ShapePointManager(guidance);
		if(!spm.isImportSuccessful()){
			Toast.makeText(this,
					getResources().getString(R.string.jsonImportNotSuccessful),
					Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	/**
	 * Draw the route with the given shapePoints from the guidance information
	 * 
	 * @param json
	 *            The guidance information from MapQuest
	 */
	private void drawRoute(JSONObject json) {
		// Set custom line style
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLUE);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(5);

		// Initialize the route overlay
		List<GeoPoint> shapePoints = new ArrayList<GeoPoint>(
				Arrays.asList(this.im.getShapePoints()));
		LineOverlay drawnRoute = new LineOverlay(paint);
		drawnRoute.setData(shapePoints);

		// Add the drawn route to the map
		map.getOverlays().add(drawnRoute);
		Log.d("NaviActivity", "Route overlay added");

		if (!im.isImportSuccessful()) {
			// Import was not successful
			Toast.makeText(NaviActivity.this,
					getResources().getString(R.string.jsonImportNotSuccessful),
					Toast.LENGTH_SHORT).show();
			// Finish the activity to return to the MainActivity
			finish();
		} else {
			do {
				// Route is not displayed yet
			} while (!this.isRouteDisplayed());

			// Get the first instruction and display it
			displayInstruction(im.getInstruction(0));
		}
	}

	@Override
	public void onBackPressed() {
		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.closeActivity_title)
				.setMessage(R.string.closeActivity_message)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								finish();
							}
						}).setNegativeButton("No", null).show();
	}

	@Override
	protected boolean isRouteDisplayed() {
		if (this.map.getOverlays().size() > 1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Called when the OptionsMenu is created
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.navi, menu);
		return true;
	}

	/**
	 * Called when an item of the OptionsMenu is clicked
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_allInstructions:
			// Create an array of all verbal instructions
			String[] allInstructions = im.getVerbalInstructions();

			// Display all instructions in a list
			AlertDialog.Builder builder1 = new AlertDialog.Builder(
					NaviActivity.this);
			builder1.setTitle(R.string.allInstructions);
			builder1.setItems(allInstructions, null);

			AlertDialog alertDialog1 = builder1.create();
			alertDialog1.show();
			return true;
		case R.id.menu_debugger:
			// Display all stored logs in a list
			AlertDialog.Builder builder2 = new AlertDialog.Builder(
					NaviActivity.this);
			builder2.setTitle(R.string.menu_debugger);
			builder2.setMessage(debugger);

			AlertDialog alertDialog2 = builder2.create();
			alertDialog2.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		// Enable features of the MyLocationOverlay
		myLocationOverlay.enableMyLocation();
		// Request location updates at startup every 500ms for changes of 1m
		lm.requestLocationUpdates(provider, 500, 1, this);
		super.onResume();

		// for the system's orientation sensor registered listeners
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);

		this.haveSavedAlready = false;

		// Initialize OpenCV manager
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback); // CHRIS: What should the version actually be?

		//this.mOPenCVCameraView.MCamera is still null here
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		//do we maybe setup the camera parameters here??? this.mOPenCVCameraView.mCamera is valid
		this.mOpenCvCameraView.setClosestResolution(BB_Parameters.idealResolutionLow_width, BB_Parameters.idealResolutionLow_height, BB_Parameters.idealResolutionHigh_width, BB_Parameters.idealResolutionHigh_width);

		mGray = new Mat();
		mRgba = new Mat();

		globalRF = new GlobalRF(this);
		localRF = new LocalRF();
		obstacleAvoidance = new ObstacleAvoidance();

		if(BB_Parameters.test_mode){
			this.dummy_Image = new Mat();
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inScaled = false;
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), BB_Parameters.testImage, o);
			Utils.bitmapToMat(bmp, this.dummy_Image); // URL: http://stackoverflow.com/questions/17390289/convert-bitmap-to-mat-after-capture-image-using-android-camera
			if(BB_Parameters.batch_testing)
				batchTestImages();
		}


		/*try {
			this.mOpenCvCameraView.setPreviewFPS(1000, 2000);
		}catch(Exception e1){
			Log.e("FPS", "FPS SETTING ERROR");
		}*/
	}

	public void onCameraViewStopped() {
	}

	public static Bitmap getBitmapFromAsset(Context context, String filePath) {
		AssetManager assetManager = context.getAssets();

		InputStream istr;
		Bitmap bitmap = null;
		try {
			istr = assetManager.open(filePath);
			bitmap = BitmapFactory.decodeStream(istr);
		} catch (IOException e) {
			// handle exception
		}

		return bitmap;
	}

	private void batchTestImages(){
		String[] listOfFiles = new String[10];
		String filename;
		int idOfImage;
		try {
			listOfFiles = getAssets().list("imgs_to_process");
		}catch(IOException any) {
			// nothing
			listOfFiles[0] = "si_4";
			listOfFiles[1] = "si_7";
		}
		// Loop Through All Images in Resources/drawable folder
		this.dummy_Image = new Mat();
		for(int i = 0; i < listOfFiles.length; i++) {
			filename = listOfFiles[i];
			Bitmap bmp = getBitmapFromAsset(getApplicationContext(), "imgs_to_process/" + filename);
			Utils.bitmapToMat(bmp, this.dummy_Image); // URL: http://stackoverflow.com/questions/17390289/convert-bitmap-to-mat-after-capture-image-using-android-camera
			this.dummy_Image.copyTo(mRgba);

			Log.v("INSTRUCTION", filename + " :" + globalRF.processFrame(mRgba,0,0, filename + "_classLabels")); // process the frame

			saveMatToFile(mRgba, filename + "_processed"); // save the image
		}
	}

	/**
	 * This method takes as params an image stored in a Mat object,
	 * creates a jpeg image from the Mat and saves it in the Pictures
	 * Gallery of the phone with the name passed in params.
	 * @param toSave
	 * @param filename
	 */
	private void saveMatToFile(Mat toSave, String filename){
		Bitmap bitmap = Bitmap.createBitmap(toSave.cols(), toSave.rows(), Bitmap.Config.ARGB_8888);
		org.opencv.android.Utils.matToBitmap(toSave, bitmap);

		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File file = new File(path, filename + ".jpeg");
		if (file.exists ()) file.delete ();
		try {
			FileOutputStream out = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
			out.flush();
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
		Log.i("FrameProc", "New Frame Processing");

		if(BB_Parameters.test_mode){
			this.dummy_Image.copyTo(mRgba);
		}else{
			mRgba = inputFrame.rgba();
		}


		//int width = mRgba.width();
		//int height = mRgba.height();
		//Log.i("NaviActivity", "width: " + width + "height: " + height + "\n");
		// Step 2: Any preprocessing/noise removal???
		// Step 3: Do we only process every x frames???
			// We would figure out x is a function of x = f(maximum bike speed, maximum distance between frames)
			// if(c == x) {process frame}
			// c++;
			// if(c > x) {set c = 1}

		// Step 4: Performance Monitoring
		// Maybe we should have some kind of timer function so we know which frames we are processing and
		// write out to a log file to monitor performance
		// NOTE: Looking at the org.opencv.android.FpsMeter

		// CALL ROAD FOLLOWING(William)
		// and update the directions to be given to the user
		this.directionsText = globalRF.processFrame(mRgba, this.desiredBearing, this.currentBearing, "");
		// If there is something to speak then speak it
		if(this.directionsText.length()>1)
			tts.speak(this.directionsText, TextToSpeech.QUEUE_FLUSH, null);
		/*
		PERSPECTIVE WARP TESTING
		Mat src_mat=new Mat(4,1,CvType.CV_32FC2);
		Mat dst_mat=new Mat(4,1, CvType.CV_32FC2);

		// Source points
		src_mat.put(0,0, 0.0, mRgba.height(),mRgba.width(),mRgba.height(),210, 140, 120, 140);
		dst_mat.put(0,0, 0.0, mRgba.height(), mRgba.width(), mRgba.height(), mRgba.width(), 0, 0.0, 0.0);
		Mat perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
		Mat dst=mRgba.clone();
		Imgproc.warpPerspective(mRgba, dst, perspectiveTransform, dst.size());*/
		if(BB_Parameters.test_mode && BB_Parameters.save_to_gallery && !this.haveSavedAlready) {
			saveMatToFile(mRgba, "test_mode_processed"); // save the image
			this.haveSavedAlready = true;
		}

		return mRgba;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Disable features of the MyLocationOverlay when in the background
		myLocationOverlay.disableMyLocation();
		// Disable the LocationManager when in the background
		lm.removeUpdates(this);

		mSensorManager.unregisterListener(this, mAccelerometer);
		mSensorManager.unregisterListener(this, mMagnetometer);

		// Disable camera view
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onInit(int status) {
		// Initialize the TextToSpeech engine
		if (status == TextToSpeech.SUCCESS) {
			tts.setLanguage(Locale.ENGLISH);
		} else {
			tts = null;
			Log.e("MainActivity",
					"Failed to initialize the TextToSpeech engine");
		}


		//mOPenCVCameraView.mCamera is still null here

	}

	@Override
	public void onLocationChanged(Location location) {
		debugger += "onLocationChanged() called...\n";

		// set the lat and lng from the gps new location
		double lat = location.getLatitude();
		double lng = location.getLongitude();

		// Update Current Location
		str_currentLocation = EntranceActivity.stringifyCoords(lat, lng);

		// Check if the instruction manager has been initialized already
		if (im != null) {

			this.directionsTextView.setText(this.directionsText); // update the display of the instruction to the user

			// If the candidate shape point hasn't been found yet, the candidate SP is the closest shape point
			// to the current location of the user
			// However this is not necessarily the current SP, if you are closest to the next shape point (the one the user is
			// traveling towards) rather than the shape point just passed we need to account for that,
			// see if(!this.currentSPFound) block, which sets the correct currentSP and nextSP
			if (!this.candidateSPFound) {
				this.candidateSP = im.goToClosestSP(lat, lng);
				this.candidateSPFound = true;
			}

			// If the current shape point hasn't been found yet
			// What this code is doing is testing whether we are heading towards the candidate or away from the candidate
			// If we are approaching the candidate then set the previous shape point as currentSP and the candidate as the
			// nextSP, if we are leaving the candidate then set the candidate as currentSP and the next SP as nextSP
			if(!this.currentSPFound && this.candidateSPFound){
				float[] distanceResults = new float[1];
				Location.distanceBetween(lat, lng, this.candidateSP.getLatitude(), this.candidateSP.getLongitude(), distanceResults);


				// If this is not our first time testing this candidate
				if (this.last_distance_to_candidateSP >= 0) {
					// if our current distance to the candidate is greater than it was last location update
					if (distanceResults[0] > this.last_distance_to_candidateSP) {
						// then this candidate is our current shape point, so we have found the current SP
						this.currentSPFound = true;
					} else {
						// otherwise the previous candidate was our current shape point and we found the current SP
						im.goToPreviousSP();
						this.currentSPFound = true;
					}
				}

				this.last_distance_to_candidateSP = distanceResults[0];

			// Otherwise we know the current shape point, so now just check and see if we are within
			// BB_Parameters.SHAPE_POINT_DISTANCE_THRESHOLD of the next shape point, if we are then update the currentSP
			// and nextSP
			} else if(this.currentSPFound) {
				double currentSPLat, currentSPLng, nextSPLat, nextSPLng;

				// Calculate the distance to the next shape point
				float[] distanceResults = new float[1];
				Location.distanceBetween(lat, lng, im.getNextSP().getLatitude(), im.getNextSP().getLongitude(), distanceResults);

				// Shift to next shape point if within threshold of current shape point
				// or if next shape point is closer than the current sape point (this
				// should never happen unless we have gotten past theh current shape point)

				if(im.hasShapePointsLeft()){
					if (distanceResults[0] < BB_Parameters.SHAPE_POINT_DISTANCE_THRESHOLD) {

						im.goToNextSP(); // shift to the next shape point

						// get the current and next shape point
						currentSPLat = im.getCurrentSP().getLatitude();
						currentSPLng = im.getCurrentSP().getLongitude();
						nextSPLat = im.getNextSP().getLatitude();
						nextSPLng = im.getNextSP().getLongitude();

						// calculate the bearing
						double X = Math.cos(nextSPLat) * Math.sin(nextSPLng - currentSPLng);
						double Y = (Math.cos(currentSPLat) * Math.sin(nextSPLat)) - (Math.sin(currentSPLat) * Math.cos(nextSPLat) * Math.cos(nextSPLng - currentSPLng));

						this.desiredBearing = (Math.toDegrees(Math.atan2(X, Y)) + 360) % 360;
						this.bearingTextView.setText(Double.toString(this.desiredBearing)); // display the bearing
					}
				}else{
					// if no shape points left set bearing to -1 to signify no bearing available
					this.desiredBearing = -1;
				}
			}

			// Get the coordinates of the next decision point
			double dp1Lat = im.getCurrentInstruction().getDecisionPoint()
					.getLatitude();
			double dp1Lng = im.getCurrentInstruction().getDecisionPoint()
					.getLongitude();

			double dp2Lat = 0;
			double dp2Lng = 0;

			if(im.isOnLastInstruction()){
				// WILLIAM: I don't know right now
				dp2Lat = dp1Lat;
				dp2Lng = dp1Lng;
				//return;
			}else{
				// Get the coordinates of the decision point after next
				dp2Lat = im.getNextInstructionLocation().getLatitude();
				dp2Lng = im.getNextInstructionLocation().getLongitude();
			}
			// Get the coordinates of the decision point after next
			//double dp2Lat = im.getNextInstructionLocation().getLatitude();
			//double dp2Lng = im.getNextInstructionLocation().getLongitude();

			// Calculate the distance to the next decision point
			float[] results = new float[1];
			Location.distanceBetween(lat, lng, dp1Lat, dp1Lng, results);
			double distanceDP1 = results[0];
			if(lastDistanceDP1 == 0){
				lastDistanceDP1 = distanceDP1;
			}

			// Check whether a now instruction must be used (only once for each
			// route segment)
			if (nowInstructionChecked == false
					&& distanceDP1 >= MIN_DISTANCE_FOR_NOW_INSTRUCTION) {
				nowInstructionUsed = true;
			}
			nowInstructionChecked = true;

			// Calculate the distance to the decision point after next
			Location.distanceBetween(lat, lng, dp2Lat, dp2Lng, results);
			double distanceDP2 = results[0];
			if(lastDistanceDP2 == 0){
				lastDistanceDP2 = distanceDP2;
			}

			// Log the distances
			String distancesString = "LastDistanceDP1: " + lastDistanceDP1
					+ " | distanceDP1: " + distanceDP1 + " | LastDistanceDP2: "
					+ lastDistanceDP2 + " | distanceDP2: " + distanceDP2;
			debugger += distancesString + "\n";
			Log.v("Navi.onLocationChanged", distancesString);


			// If we have been within NODE_WINDOW_DISTANCE
			// of the next node
			if(distanceDP1 < getApplicationContext().getResources().getInteger(R.integer.NODE_WINDOW_DISTANCE)){
				beenWithinNodeWindow = true;
			}
			// Check the distances with the stored ones
            // CASE 1: This is the handle when we can get close to
            // our decision point like on city roads, but not
            // highways where you merge off
			if (distanceDP1 < MAX_DISTANCE_TO_DECISION_POINT) {
				// Distance to decision point is less than
				// MAX_DISTANCE_TO_DECISION_POINT

				if(im.isOnLastInstruction()){
					// Tell User they have arrived and stop navigation
					haveArrivedInstruction();
					stopNavigation();
					return;
				}
				updateInstruction();
			} else if (distanceDP1 < DISTANCE_FOR_NOW_INSTRUCTION
					&& nowInstructionUsed == true) {
				// Distance to decision point is less than
				// DISTANCE_FOR_NOW_INSTRUCTION and decreasing, so a now
				// instruction is prompted to the user
				updateNowInstruction();
				// Set variable nowInstructionUsed to false, so that the now
				// instruction is only used once
				nowInstructionUsed = false;
			} else if (distanceDP1 > lastDistanceDP1
					&& distanceDP2 < lastDistanceDP2
					&& beenWithinNodeWindow) {
				// The distance to the next decision point has increased and the
				// distance to the decision point after next has decreased and
				// user has been within the node window
				//lastDistanceDP1 = distanceDP1;
				//lastDistanceDP2 = distanceDP2;
				distanceCounter++;

				String logMessage = "distanceCounter: " + distanceCounter;
				debugger += logMessage + "\n";
				Log.v("Navi.onLocationChanged", logMessage);
			} else if (distanceDP1 > lastDistanceDP1) {
				// Distance to the next decision point and the decision point
				// after next has increased (can lead to a driving error)
				//lastDistanceDP1 = distanceDP1;
				//lastDistanceDP2 = distanceDP2;
				distanceCounter--;

				String logMessage = "distanceIncreaseCounter: "
						+ distanceCounter;
				debugger += logMessage + "\n";
				Log.v("Navi.onLocationChanged", logMessage);
			}

			// Update the last distances
			lastDistanceDP1 = distanceDP1;
			lastDistanceDP2 = distanceDP2;

			// Check if the whole guidance needs to be reloaded due to a driving
            // error (user seems to go away from both the decision point and the
            // decision point after next)
			if (distanceCounter < (-1 * MAX_COUNTER_VALUE)) {
				updateGuidance();
			}
			// Check if the instruction needs to be updated
            // CASE 1 Extended: This handles the case where we are merging off like
            // a highway where we can't get exactly close to decision
            // point 1
			if (distanceCounter > MAX_COUNTER_VALUE) {
				if(im.isOnLastInstruction()){
					// Tell User they have arrived and stop navigation
					haveArrivedInstruction();
					stopNavigation();
					return;
				}
				updateInstruction();
			}
			//This is to keep a track of the marker as it moves
			GeoPoint gp = new GeoPoint(lat,lng);
			map.getController().animateTo(gp);
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor == mAccelerometer) {
			System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
			mLastAccelerometerSet = true;
		} else if (event.sensor == mMagnetometer) {
			System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
			mLastMagnetometerSet = true;
		}
		if (mLastAccelerometerSet && mLastMagnetometerSet) {
			SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
			SensorManager.getOrientation(mR, mOrientation);
			float azimuthInRadians = mOrientation[0];
			double azimuthInDegress = (Math.toDegrees(azimuthInRadians)+360)%360;

			this.currentBearing = (azimuthInDegress + 90) % 360;
			this.measuredBearingTextView.setText(Double.toString(this.currentBearing)); // display the bearing
		}

	}
		@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy){

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Do nothing here
	}

	@Override
	public void onProviderEnabled(String provider) {
		// Do nothing here
	}

	@Override
	public void onProviderDisabled(String provider) {
		// Do nothing here
	}

	/**
	 * Called when the next decision point has been reached to update the
	 * current instruction to the following instruction.
	 */
	private void updateInstruction() {
		String logMessage = "Updating Instruction...";
		debugger += logMessage + "\n";
		Log.i("NaviActivity", logMessage);

		// Reset the distances, their counters, and the NowInstruction
		// controllers
		lastDistanceDP1 = 0;
		lastDistanceDP2 = 0;
		distanceCounter = 0;
		nowInstructionChecked = false;
		nowInstructionUsed = false;
		beenWithinNodeWindow = false;

		// Get the next instruction and display it
		Instruction nextInstruction = im.getNextInstruction();
		displayInstruction(nextInstruction);

	}

	private void haveArrivedInstruction(){
		// WILLIAM: Create new instance of class Instruction that says we have arrived
		// Instruction i = new Instruction(im.getCurrentInstruction().getDecisionPoint() ,24);
		Instruction i = new DistanceInstruction(im.getCurrentInstruction().getDecisionPoint(),24, (int) lastDistanceDP1);

		// Reset the distances, their counters, and the NowInstruction
		// controllers
		lastDistanceDP1 = 0;
		lastDistanceDP2 = 0;
		distanceCounter = 0;
		nowInstructionChecked = false;
		nowInstructionUsed = false;

		displayInstruction(i);
	}

	private void stopNavigation(){
		// This is where we have to close down the navigation and have to leave the GUI
		// in a state that shows they have arrived
		// Because once we start the opencv stuff we have to tell opencv to stop taking pictures
		// Possibility: Go back to previous activity, but let user know they have arrived (count 30 seconds then go back?)
		// Possibility: Turn off opencv and then they have to hit a button to go back to previous activity

		showDialog();
	}

	/**
	 * Called when a new instruction shall be displayed. Also the map is being
	 * updated so that the old landmarks are removed and the new ones are
	 * displayed.
	 * 
	 * @param instruction
	 *            The instruction to be displayed
	 */
	private void displayInstruction(Instruction instruction) {
		// --- Update the instruction view ---
		// Get the next verbal instruction
		String nextVerbalInstruction = instruction.toString();
		// Display the verbal instruction
		this.tv_instruction.setText(nextVerbalInstruction);

		// Get the corresponding instruction image and display it
		this.iv_instruction.setImageDrawable(getResources().getDrawable(
				Maneuver.getDrawableId(instruction.getManeuverType())));
		// --- End of update the instruction view ---

		// --- Update the landmarks on the map (if available) ---
		// Remove previous landmark (if available)
		if (this.map.getOverlays().size() > this.INDEX_OF_LANDMARK_OVERLAY) {
			this.map.getOverlays().remove(this.INDEX_OF_LANDMARK_OVERLAY);
		}

		// Add new local landmark (if available)
		if (instruction.getClass().equals(LandmarkInstruction.class)) {
			Landmark newLocalLandmark = ((LandmarkInstruction) instruction)
					.getLocal();
			OverlayItem oi_newLocalLandmark = new OverlayItem(
					newLocalLandmark.getCenter(), newLocalLandmark.getTitle(),
					newLocalLandmark.getCategory());
			MyDefaultItemizedOverlay newLocalLandmarkOverlay = new MyDefaultItemizedOverlay(
					getResources().getDrawable(
							LandmarkCategory.getDrawableId(newLocalLandmark
									.getCategory())));
			newLocalLandmarkOverlay.addItem(oi_newLocalLandmark);
			this.map.getOverlays().add(this.INDEX_OF_LANDMARK_OVERLAY,
					newLocalLandmarkOverlay);
		}

		// Add new global landmark (if available)
		if (instruction.getClass().equals(GlobalInstruction.class)) {
			Landmark newGlobalLandmark = ((GlobalInstruction) instruction)
					.getGlobal();
			OverlayItem oi_newGlobalLandmark = new OverlayItem(
					newGlobalLandmark.getCenter(),
					newGlobalLandmark.getTitle(),
					newGlobalLandmark.getCategory());
			MyDefaultItemizedOverlay newGlobalLandmarkOverlay = new MyDefaultItemizedOverlay(
					getResources().getDrawable(
							LandmarkCategory.getDrawableId(newGlobalLandmark
									.getCategory())));
			newGlobalLandmarkOverlay.addItem(oi_newGlobalLandmark);
			this.map.getOverlays().add(this.INDEX_OF_LANDMARK_OVERLAY,
					newGlobalLandmarkOverlay);
		}
		// --- End of updating map ---

		// Speak out the verbal instruction
		speakInstruction();
	}

	/**
	 * Speak out the current instruction
	 */
	private void speakInstruction() {
		tts.setSpeechRate((float) 0.85);
		tts.speak(tv_instruction.getText().toString(),
				TextToSpeech.QUEUE_FLUSH, null);
	}

	/**
	 * Called when the next decision point will be reached in
	 * <code>DISTANCE_FOR_NOW_INSTRUCTION</code> and a
	 * <code>NowInstruction</code> is used to update the current instruction to
	 * the instruction. The map is not changed as in the
	 * <code>updateInstruction</code> method.
	 */
	private void updateNowInstruction() {
		// Get the now instruction
		Instruction nowInstruction = im.getNowInstruction();

		// --- Update the instruction view ---
		// Get the verbal instruction
		String verbalInstruction = nowInstruction.toString();
		// Display the verbal instruction
		this.tv_instruction.setText(verbalInstruction);

		// The instruction image stays the same so nothing has to be done here

		// Speak out the verbal instruction
		speakInstruction();
	}

	/**
	 * Update the complete guidance. This method is called when a driving error
	 * has occurred.
	 */
	private void updateGuidance() {
		// Inform the user about updating the guidance
		Log.i("NaviActivity", "Updating guidance...");
		tts.setSpeechRate((float) 1);
		tts.speak("Updating guidance", TextToSpeech.QUEUE_FLUSH, null);

		// update the intent with the most recent location
		Intent updatedIntent = getIntent();
		updatedIntent.putExtra("str_currentLocation", str_currentLocation);

		// Restart the activity
		finish();
		startActivity(updatedIntent);
	}

	void showDialog() {
		DialogFragment newFragment = NaviDialogue
				.newInstance(R.string.navi_dialogue_text);
		newFragment.show(getFragmentManager(), "dialog");
	}

	/*
	This is called when the user clicks the ok button of the NaviDialogue
	 */
	public void doPositiveClick() {
		finish(); // End the activity
		Log.i("NaviActivity", "Positive click!");
	}

	/*
	This is called when the user clicks the reroute button of the NaviDialogue
	 */
	public void doNegativeClick() {
		updateGuidance(); // Perform reroute from current location
		Log.i("NaviActivity", "Negative click!");
	}
}
