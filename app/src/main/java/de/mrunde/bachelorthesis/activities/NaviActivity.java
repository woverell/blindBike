package de.mrunde.bachelorthesis.activities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mapquest.android.maps.DefaultItemizedOverlay;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.LineOverlay;
import com.mapquest.android.maps.MapActivity;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;
import com.mapquest.android.maps.OverlayItem;
import com.mapquest.android.maps.RouteManager;

import de.mrunde.bachelorthesis.basics.RouteSegment;
import de.mrunde.bachelorthesis.instructions.DistanceInstruction;
import de.mrunde.bachelorthesis.instructions.ShapePointManager;
import edu.csueb.ilab.blindbike.blindbike.BB_Parameters;
import edu.csueb.ilab.blindbike.blindbike.CustomizeView;
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
import edu.csueb.ilab.blindbike.lightdetection.Traffic_light_Data;
import edu.csueb.ilab.blindbike.obstacleavoidance.ObstacleAvoidance;
import edu.csueb.ilab.blindbike.roadfollowing.GlobalRF;
import edu.csueb.ilab.blindbike.roadfollowing.LocalRF;

/**
 * This is the navigational activity which is started by the MainActivity. It
 * navigates the user from his current location to the desired destination.
 * 
 * @author Marius Runde
 */
public class NaviActivity extends MapActivity implements OnInitListener,
		LocationListener, CameraBridgeViewBase.CvCameraViewListener2,View.OnTouchListener {

	private static final String TAG = "OCVSample::Activity";
	private Mat mRgba;
	private Mat mGray;
	private CameraBridgeViewBase mOpenCvCameraView;

	private XCrossing xCrossing;
	private LightDetector mDetector;
	private GlobalRF globalRF;
	private LocalRF localRF;
	private ObstacleAvoidance obstacleAvoidance;

	private CustomizeView mMyCamera;
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

	//Calculate bounding box variables
	float Tmin=0,Tmax=0;
	double min_row_bound=0.0,max_row_bound=0.0;
	double min_row_bound_low=0.0,max_row_bound_low=0.0;
	// --- End of route and instruction objects ---

	/**
	 * TextToSpeech for audio output
	 */
	private TextToSpeech tts;

	private boolean mIsColorSelected = false;
	Mat mRgbabright;
	private Scalar mBlobColorRgba;
	private Scalar mBlobColorHsv;
	//private LightDetector mDetector;
	private Mat mSpectrum;
	private Size SPECTRUM_SIZE;
	private Scalar CONTOUR_COLOR;

	/**
	 * Store all logs of the <code>onLocationChanged()</code> and
	 * <code>updateInstruction()</code> methods in this String to display them
	 * on the application via the <code>OptionsMenu</code>
	 */
	private String debugger = "";

	//Edit Text that displayes the distance for debug purposes
	private EditText mydis;
	//Distance calculated in CM
	int dis = 0;
	private double dptl;

	// Light sensor variables
	private SensorManager mSensorManager;
	private Sensor mLightSensor;
	private Sensor mPressureSensor;
	private float mLightQuantity;
	private float altitude = 0;
	private int sig_flag =-1;


	//Array List to hold tld_obj object which hold lat and lng for next intersection
	ArrayList<Traffic_light_Data> tld_array_list;

	/**
	 	 * Shape point manager to store and manage shape points
	 	 */
	private ShapePointManager spm;

	/**
	 	 * Maximum distance to a shape point when it is considered as reached
	 	 */
		private final int SHAPE_POINT_DISTANCE_THRESHOLD = 15;

				/**
	 	 * The current desired bearing based on shape points of the route
	 	 * In compass degrees 0 to 360, 0 and 360 being north, 90 east
	 	 * 180 south, 270 west
	 	 */

		private double last_distance = 0;
		private boolean currentSPNotFound = true;
		private boolean candidateSPNotFound = true;
		private double candidateCurrentSPLat;
		private double candidateCurrentSPLng;

	//List<RouteSegment> is to house all the segments extracted
	List<RouteSegment> routeSegments;
	//To be used for testing the bounding box values
	private double test[][];

	//The elevation array with index equal to shape point array
	double elevation[];
	// The distance between the points
	double distance[];
	// ShapePoints from the elevation api call
	GeoPoint[] elevation_shapePoints;
	//segment_no for finding out which segment are we currently on
	int segment_no=0;

	// Vector Indices for global detection
	Vector indices;
	// Flag to skip the calculate_rows() call in OnCameraFrame when we have a general
	// search for a TL
	boolean skip_calculate_rows=false;

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
		globalRF = new GlobalRF();
		localRF = new LocalRF();
		obstacleAvoidance = new ObstacleAvoidance();

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

		//Obtain references to the sensormanager and the Light Sensor
		mSensorManager =(SensorManager)getSystemService(SENSOR_SERVICE);
		mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mPressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		SensorEventListener listener = new SensorEventListener() {

			@Override
			public void onSensorChanged(SensorEvent event) {
				// TODO Auto-generated method stub
				//mLightQuantity is the value of Brightness
				mLightQuantity = event.values[0];
				float presure = event.values[0];
				altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, presure);
				Log.i("CHRIS: Altitude ",String.valueOf(altitude));
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub

			}
		};

		// For getting focal length we use Customize view
		//mMyCamera= CustomizeView.class.cast(findViewById(R.id.tutorial1_activity_java_surface_view));
		//Register the listener with light sensor
		mSensorManager.registerListener(listener, mLightSensor,SensorManager.SENSOR_DELAY_UI);

		// Setup the whole GUI and map
		setupGUI();
		setupMapView();
		setupMyLocation();

		// Add the destination overlay to the map(puts flag on map)
		addDestinationOverlay(destination_lat, destination_lng);

		// Calculate the route
		//calculateRoute();

		// Get the guidance information and create the instructions
		getGuidance();

	//	mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_java_camera_view);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
		mMyCamera= CustomizeView.class.cast(findViewById(R.id.tutorial1_activity_java_surface_view));
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	/**
	 * Set up the GUI
	 */
	private void setupGUI() {
		this.tv_instruction = (TextView) findViewById(R.id.tv_instruction);
		this.iv_instruction = (ImageView) findViewById(R.id.iv_instruction);
        this.reroute_button = (Button) findViewById(R.id.reroute_button);

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

	public void addstatus(String t){
		this.mydis = (EditText)findViewById(R.id.mydtxt);
		mydis.setText(t, TextView.BufferType.EDITABLE); //results.toString());
	}

	/**
	 * Get the guidance information from MapQuest
	 */
	String url_elevation="",url_directions="";
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

		JSONObject result_elevation,result_directions;
		@Override
		protected JSONObject doInBackground(String... url) {
			// Get the data from the URL
			String output = "",output1="",output12="";
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
			JSONObject import_elevation = new JSONObject();
			try {
				response = httpclient.execute(new HttpGet(url[0]));
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					out.close();
					output = out.toString();
					//reuse objects for elevation
					try {
						// end of the output if needed to convert it to a JSONObject
						if (output.startsWith("renderBasicInformation(")) {
							output = output.substring(23, output.length());
						}
						if (output.endsWith(");")) {
							output = output.substring(0, output.length() - 2);
						}

						/*JSONObject result_inter = new JSONObject(output);
						//URL for elevation api MapQuest
						JSONObject import_elevation = result_inter.getJSONObject("guidance");*/

						//Get Session ID for the elevation
						url_directions = "https://open.mapquestapi.com/directions/v2/route?key="
								+ getResources().getString(R.string.apiKey)
								+ "&from="
								+ URLEncoder.encode(str_currentLocation, "UTF-8")
								+ "&to="
								+ URLEncoder.encode(str_destination, "UTF-8")
								+ "&narrativeType=text&fishbone=false&callback=renderBasicInformation";
/*

						//url for elevation
						url_elevation = "https://open.mapquestapi.com/elevation/v1/profile?key=" + getResources().getString(R.string.apiKey)
								+ "&inFormat=kvp&outFormat=json&unit=m&shapeFormat=raw"
								+ "&latLngCollection=" + import_elevation.getJSONArray("locations").getJSONObject(0).getJSONObject("latLng").getDouble("lat") + ","
								+ import_elevation.getJSONArray("locations").getJSONObject(0).getJSONObject("latLng").getDouble("lng")
								+ ","
								+ import_elevation.getJSONArray("locations").getJSONObject(1).getJSONObject("latLng").getDouble("lat") + ","
								+ import_elevation.getJSONArray("locations").getJSONObject(1).getJSONObject("latLng").getDouble("lng");
*/

					}
					catch(Exception je)
					{
						je.printStackTrace();
					}


					//Query for Session ID
					response = httpclient.execute(new HttpGet(url_directions));
					StatusLine statusLine12 =  response.getStatusLine();
					if(statusLine12.getStatusCode() == HttpStatus.SC_OK){
						ByteArrayOutputStream out12 = new ByteArrayOutputStream();
						response.getEntity().writeTo(out12);
						out12.close();
						output12 = out12.toString();
					}
					if (output12.startsWith("renderBasicInformation(")) {
						output12 = output12.substring(23, output12.length());
					}
					if (output12.endsWith(");")) {
						output12 = output12.substring(0, output12.length() - 2);
					}
					try {
						result_directions = new JSONObject(output12);
						import_elevation = result_directions.getJSONObject("route");
					}
					catch (JSONException je3)
					{
						je3.printStackTrace();
					}
					//Query for elevation

					//url for elevation
					String sess_ID=import_elevation.getString("sessionId");
					url_elevation = "https://open.mapquestapi.com/elevation/v1/profile?key=" + getResources().getString(R.string.apiKey)
							+ "&inFormat=kvp&outFormat=json"
							+ "&sessionId="+ sess_ID
							+ "&unit=m&shapeFormat=raw";

					response = httpclient.execute(new HttpGet(url_elevation));
					StatusLine statusLine1 =  response.getStatusLine();
					if(statusLine1.getStatusCode() == HttpStatus.SC_OK){
						ByteArrayOutputStream out11 = new ByteArrayOutputStream();
						response.getEntity().writeTo(out11);
						out11.close();
						output1 = out11.toString();

					}
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

			//Delete "RenderBasicInformation" stuff for the elevation object
			if (output1.startsWith("renderBasicInformation(")) {
				output1 = output1.substring(23, output1.length());
			}
			if (output1.endsWith(");")) {
				output1 = output1.substring(0, output1.length() - 2);
			}

			// Convert the output to a JSONObject
			try {
				JSONObject result = new JSONObject(output);

				result_elevation = new JSONObject(output1);
				if(result_elevation.length()!=0)
				{
					JSONArray elevationCollection = result_elevation.getJSONArray("elevationProfile");
					JSONArray elevation_shapePoint_Collection = result_elevation.getJSONArray("shapePoints");
					elevation = new double[elevationCollection.length()];
					distance = new double[elevationCollection.length()];
					elevation_shapePoints = new GeoPoint[elevation_shapePoint_Collection.length()/2];
					for(int i = 0; i < elevationCollection.length(); i++){
						elevation[i] = elevationCollection.getJSONObject(i).getDouble("height");
						distance[i] = elevationCollection.getJSONObject(i).getDouble("distance");
					}
					int k=0;
					for(int j = 0; j < elevation_shapePoint_Collection.length() - 1; j += 2){
						elevation_shapePoints[k] = new GeoPoint(elevation_shapePoint_Collection.getDouble(j),elevation_shapePoint_Collection.getDouble(j+1));
						k++;
					}
				}

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
		//REPLACE WITH READING FROM MAPQUEST DATA

		//getIntersectionPoints() will break the guidance object down and get
		//the lat and lng values for each Intersection [PERFECT]
		getIntersectionPoints(guidance);
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
		// res/ra  w/streetfurniture.json
		//REPLACE WITH READING FROM MAPQUEST DATA
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
		//REPACE WITH READING FROM MAPQUEST DATA
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

		//create shape point manager
		spm =new ShapePointManager(guidance);
		if(!spm.isImportSuccessful()){
			Toast.makeText(this,getResources().getString(R.string.jsonImportNotSuccessful),Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	/*
	* We are now going to extract all shape points that have an intersection
	* */

	public void getIntersectionPoints(JSONObject guidance) {
		tld_array_list=new ArrayList<Traffic_light_Data>();
		Traffic_light_Data tld_obj;

		int[] maneuvers;
		int[] linkIndexes;
		GeoPoint[] decisionPoints;
		double[] distances;
		int[] shapePointIndexes;

		// Extract the guidance information out of the raw JSON file
		try {
			JSONObject import_guidance = guidance.getJSONObject("guidance");

			// --- Get the maneuver types and link indexes ---
			// First store them in a temporal List
			List<Integer> tempManeuverList = new ArrayList<Integer>();
			List<Integer> tempLinkList = new ArrayList<Integer>();
			JSONArray guidanceNodeCollection = import_guidance.getJSONArray("GuidanceNodeCollection");
			for (int i = 0; i < guidanceNodeCollection.length(); i++) {
				if ((guidanceNodeCollection.getJSONObject(i)).has("maneuverType")) {
					tempManeuverList.add(guidanceNodeCollection.getJSONObject(i).getInt("maneuverType"));
					tempLinkList.add(guidanceNodeCollection.getJSONObject(i).getJSONArray("linkIds").getInt(0));
				}
			}
			// Then store them in an array
			maneuvers = new int[tempManeuverList.size()];
			linkIndexes = new int[tempLinkList.size()];
			for (int i = 0; i < maneuvers.length; i++) {
				maneuvers[i] = tempManeuverList.get(i);
				linkIndexes[i] = tempLinkList.get(i);
				tld_obj =  new Traffic_light_Data();
				tld_obj.setAltitude(0.0);
				tld_obj.setLat(0.0);
				tld_obj.setLng(0.0);
				tld_obj.setCategory("intersection");
				tld_obj.setCrossed(false);
				tld_obj.setManType(maneuvers[i]);
				tld_obj.setLinkId(linkIndexes[i]);
				tld_array_list.add(tld_obj);
			}

			// --- Get the distances and shape point indexes --- GuidanceLinkCollection
			JSONArray guidanceLinkCollection = import_guidance.getJSONArray("GuidanceLinkCollection");
			distances = new double[maneuvers.length];
			shapePointIndexes = new int[maneuvers.length];
			for (int i = 0; i < maneuvers.length ; i++) {
				if (guidanceLinkCollection.getJSONObject(tld_array_list.get(i).getLinkId())!=null) {
					//distances[i] = guidanceLinkCollection.getJSONObject("shapeIndex").getDouble("length");
					shapePointIndexes[i] = guidanceLinkCollection.getJSONObject(tld_array_list.get(i).getLinkId()).getInt("shapeIndex");

				}
			}
			int index=0;
			// --- Get the decision points ---
			JSONArray shapePoints = import_guidance.getJSONArray("shapePoints");
		//	decisionPoints = new GeoPoint[shapePoints.length() / 2];
			for(int i=0;i<shapePointIndexes.length;i++)
			{
				index = (shapePointIndexes[i] * 2);
				tld_array_list.get(i).setLat(shapePoints.getDouble(index));
				index++;
				tld_array_list.get(i).setLng(shapePoints.getDouble(index));
			}
			Log.i("CHRIS:","I reaached here!");



		} catch (JSONException e) {

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

		//Log all Intersections

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

		// Initialize OpenCV manager
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback); // CHRIS: What should the version actually be?
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		tts.shutdown();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
		mRgba = new Mat();
		mRgbabright = new Mat();
		mDetector = new LightDetector();
		mSpectrum = new Mat();
		mBlobColorRgba = new Scalar(255);
		mBlobColorHsv = new Scalar(255);
		SPECTRUM_SIZE = new Size(200, 64);
		CONTOUR_COLOR = new Scalar(255,0,0,255);
		indices = new Vector();
		test=new double[100][3];
		try {

			mMyCamera.setPreviewFPS(Double.valueOf(10000),Double.valueOf(11000));


		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	// This is a method for the general TL detection where we calculate min and max based on a distnace
	double d;
	public double Calculate_rows(int dis)
	{
		//trying values
		/*double min_row_bound=0.0,max_row_bound=0.0,sc_min=0.0,sc_max=0.0;
		double f=4.8;
		int lr=360;
		double r_min=487.68,r_max=579.12,d=4000,b=20; *///initially we take D=40 mts = 4000cm

		//exit sign
		double sc_min=0.0,sc_max=0.0;
		double f=4.8;
		int lr=360;		//center row of the image
	//	double r_min=10.00,r_max=70.00,d=1524,b=20;
	//	double r_min=180.00,r_max=240.00,d=2000,b=110;	//Dim of the note on the wall
		double r_min=480.00,r_max=700.00,b=110;	//Dim of the Traffic Light b=110
		d=dis;
		//d is assigned 3010 to avoid error with min_row_bound and max_row_bound &
		//To avoid mIsColorDetected flag to be true during initialization
		if(d==0)
			d=3010;
		//values: for d = 100 steps for 30 mts; 67 steps for 20 mts; 33 steps for 10 mts
		if(d<=3000 && d>=900) {
			// debug with set distance
			//d=1174; try 1182
			//d=1100;
			if(d==1903)	d=1900;
			if(d==1899)	d=1900;

			Tmin = (float) Math.toDegrees(Math.atan((r_min - b) / d));
			sc_min = f * (Math.tan(Tmin));
			Log.i("CHRIS Vals: ", String.valueOf(Math.tan(Tmin)));
			min_row_bound = sc_min + lr;

			Tmax = (float) Math.toDegrees(Math.atan(((r_max - b) / d)));
			sc_max = f * Math.tan(Tmax);
			Log.i("CHRIS Vals:", String.valueOf(Math.tan(Tmax)));
			max_row_bound = sc_max + lr;
			//invert values to get the max and min above 360
			min_row_bound = 720 - min_row_bound;
			max_row_bound = 720 - max_row_bound;
			//Error correction


			//-100 for lower light now trying -200
			min_row_bound_low = min_row_bound - 100;
			max_row_bound_low = max_row_bound - 100;

			min_row_bound = min_row_bound - 200;
			max_row_bound = max_row_bound - 200;

			///for 2500 as it has less rows in betwen &5000
			max_row_bound = max_row_bound + 100;
 			mIsColorSelected = true;
		}
		else
		{
			mIsColorSelected = false;
		}
		return 0;
	}
	public void onCameraViewStopped() {
		mRgba.release();
	}

	int testi=0;
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

		// ****** I have my detection module in here ***********//
		mRgba = inputFrame.rgba();
		//adding brightness for dark conditions
		//mRgba.convertTo(mRgbabright,-1,4,50);

		mRgbabright=mRgba.clone();
		int cols = mRgbabright.cols();
		int rows = mRgbabright.rows();

		//trying actual cropping
		/*String w=String.valueOf(mRgbabright.rows()/2);
		int wi=Integer.parseInt(w);
		Rect roi = new Rect(0,0,wi,mRgbabright.cols());
		Mat cropped = new Mat(mRgbabright, new Range(0,wi),new Range(0,mRgbabright.cols()));*/
		String w="";
		int wi=0;

		// Here we are calculating the min_row_bound and max_row_bound for a specific TL detection
		//where we know exactly that we are approaching an Intersection
		if(skip_calculate_rows==false) {
			 Calculate_rows(dis);
		}

		if((max_row_bound - min_row_bound)<0)
		{
			wi=(int)Math.abs(max_row_bound - min_row_bound);
			wi=wi+60;
		}
		else
		{
			wi=(int)Math.abs(max_row_bound - min_row_bound);
			wi+=60;
		}

		Rect roi = new Rect(0,0,wi,mRgbabright.cols());

//		Mat cropped = new Mat(mRgbabright, new Range(0,wi),new Range(0,mRgbabright.cols()));

		Mat touchedRegionHsv = new Mat();
		//touchedRegionRgba
		int row_start=0;

		//Done to get rid of negative values in min_row_bound and max_row_bound
		if(min_row_bound<0)
			row_start=Math.abs((int)min_row_bound);
		else
			row_start=(int)min_row_bound;

		int row_end=0;
		if(max_row_bound<0)
			row_end =Math.abs((int)max_row_bound);
		else
			row_end=(int)max_row_bound;


	//	Mat cropped_rect=mRgbabright.submat(row_start,row_end,100,700);//row_start,row_end,col_start,col_end
		//This switching of variables will create an error in submat function.
		//Hence to avoid the error we do swapping of the values.
		if(row_start>row_end)
		{
			row_start =row_start+row_end;
			row_end = row_start - row_end;
			row_start= row_start - row_end;
		}

		Mat cropped_rect=mRgbabright.submat(row_start,row_end,50,950);//row_start,row_end,col_start,col_end

		//Adding a rectangle to the JavaSurfaceView to see if the box moves as expected
		Core.rectangle(mRgbabright,new Point(50,min_row_bound),new Point(950,max_row_bound),new Scalar(255,0,0));

		//Converting to HSV
		Imgproc.cvtColor(cropped_rect, touchedRegionHsv, Imgproc.COLOR_BGR2HSV_FULL); //COLOR_RGB2HSV_FULL
		//touchedRegionHsv.copyTo(cropped_rect_low);
		// Calculate average color of touched region
		mBlobColorHsv = Core.sumElems(touchedRegionHsv);
		//touchedRect.width*touchedRect.height
		int pointCount = roi.width*roi.height;
		for (int i = 0; i < mBlobColorHsv.val.length; i++)
			mBlobColorHsv.val[i] /= pointCount;

		mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

		Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
				", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

		mDetector.setHsvColor(mBlobColorHsv);

		Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

	//	mIsColorSelected = true;

		//touchedRegionRgba.release();


		touchedRegionHsv.release();

		if (mIsColorSelected) {

			//Logging d, row_end and row_start values
			//test[testi][0] = d;
			//test[testi][1] = row_start;
			//test[testi][2] = row_end;

			mDetector.process(cropped_rect);
			List<MatOfPoint> contours = mDetector.getContours();
			Log.e(TAG, "Contours count: " + contours.size());
			Imgproc.drawContours(mRgbabright, contours, -1, CONTOUR_COLOR);

			Mat colorLabel = mRgbabright.submat(4, 68, 4, 68);
			colorLabel.setTo(mBlobColorRgba);

			Mat spectrumLabel = mRgbabright.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
			mSpectrum.copyTo(spectrumLabel);
			//Update the instructions to show red and Green lights on the TV instruction
			displayTLInstruction();
			//testi++;
		}

		return mRgbabright;
	}

	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

		return new Scalar(pointMatRgba.get(0, 0));
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Toast.makeText(v.getContext(),"Brightness Val: "+mLightQuantity,Toast.LENGTH_SHORT).show();
		float focal_length = mMyCamera.getFocalLength();
		Toast.makeText(v.getContext(), "Focal length: " + focal_length, Toast.LENGTH_SHORT).show();


		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Disable features of the MyLocationOverlay when in the background
		myLocationOverlay.disableMyLocation();
		// Disable the LocationManager when in the background
		lm.removeUpdates(this);
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
	}

	public int calculateDistance(double userLat, double userLng, double venueLat, double venueLng)
	{
		//Haversian Formula
		int r = 6371;
		double latDistance = Math.toRadians(userLat - venueLat);
		double lngDistance = Math.toRadians(userLng - venueLng);

		double a = Math.sin(latDistance/2) * Math.sin(latDistance/2) + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(venueLat)) * Math.sin(lngDistance/2) * Math.sin(lngDistance/2);
		double c = 2 * Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
		String x_dis = String.valueOf(c);
		x_dis = x_dis.substring(0, 6);
		c = Double.valueOf(x_dis);
		int mts =(int)(r * c);
		mts = mts / 10;
		return mts;
	}

	//This is a method which is a copy of the general method which will return min/max row bound for distance provided
	//will return min_row_bound if type=min row and return max_row_bound if type=max row
	public double Calculate_rows(int dis,String type)
	{

		double sc_min=0.0,sc_max=0.0;
		double f=4.8;
		int lr=360;		//center row of the image

		double r_min=480.00,r_max=700.00,b=110;	//Dim of the Traffic Light b=110
		d=dis;

		// debug with set distance
		//d=1174; try 1182
		//d=1100;
		if(d==1903)	d=1900;
		if(d==1899)	d=1900;

		Tmin = (float) Math.toDegrees(Math.atan((r_min - b) / d));
		sc_min = f * (Math.tan(Tmin));
		Log.i("CHRIS Vals: ", String.valueOf(Math.tan(Tmin)));
		min_row_bound = sc_min + lr;

		Tmax = (float) Math.toDegrees(Math.atan(((r_max - b) / d)));
		sc_max = f * Math.tan(Tmax);
		Log.i("CHRIS Vals:", String.valueOf(Math.tan(Tmax)));
		max_row_bound = sc_max + lr;
		//invert values to get the max and min above 360
		min_row_bound = 720 - min_row_bound;
		max_row_bound = 720 - max_row_bound;
		//Error correction


		//-100 for lower light now trying -200
		min_row_bound_low = min_row_bound - 100;
		max_row_bound_low = max_row_bound - 100;

		min_row_bound = min_row_bound - 200;
		max_row_bound = max_row_bound - 200;

		///for 2500 as it has less rows in betwen &5000
		max_row_bound = max_row_bound + 100;

		double ret=0;
		if(type.equalsIgnoreCase("min row"))
		{
			ret = min_row_bound;
		}
		else if(type.equalsIgnoreCase("max row"))
		{
			ret = max_row_bound;
		}
		mIsColorSelected = true;
		return ret;
	}

	GeoPoint tmp = null,tmp2=null;
	public Vector getIndices(int segment_no,double currentSPLat,double currentSPLng) {
		int flg=0;
		float[] distanceResultsA = new float[1];

		GeoPoint spArray[] = routeSegments.get(segment_no).getShapePoints();
		/*for (int i = 0; i < routeSegments.get(segment_no).getShapePoints().length;) {
			//Find the distance between current and next SP Point
			if(indices.size() > 0) {
				if (i <= (int) indices.lastElement()) {
					i = (int) indices.lastElement();
				}
			}
			double tmp1 = spArray[i].getLatitude();
			double tmp2 = spArray[i].getLongitude();
			double tmpLng = currentSPLng;
			Location.distanceBetween(currentSPLat, currentSPLng, spArray[i].getLatitude(), spArray[i].getLongitude(), distanceResultsA);

			if (distanceResultsA[0] > BB_Parameters.minDistanceToDetectLight && distanceResultsA[0] <= BB_Parameters.maxDistanceToDetectLight)
				indices.add(i);

			i++;
		}

		if(tmp2 != tmp)
		{
			tmp2 = tmp;
		}
		else{
			// we move our currentSPLat and Lng to next available index
			tmp = spArray[(int) indices.lastElement()+1];
			flg=1;
		}

		if(indices.size() > 0 && flg==0) {
			tmp = spArray[(int) indices.lastElement()];
			flg=0;
		}
		//Check if we have reached the end of the segment
		if (tmp.getLatitude()!=spArray[spArray.length-1].getLatitude() && tmp.getLongitude()!= spArray[spArray.length-1].getLongitude()) {
			getIndices(segment_no, tmp.getLatitude(), tmp.getLongitude());
		}*/

		for (int i = 0; i < routeSegments.get(segment_no).getShapePoints().length;i++) {
			//Find the distance between current and next SP Point

			double tmp1 = spArray[i].getLatitude();
			double tmp2 = spArray[i].getLongitude();
			double tmpLng = currentSPLng;
			Location.distanceBetween(currentSPLat, currentSPLng, spArray[i].getLatitude(), spArray[i].getLongitude(), distanceResultsA);

			if (distanceResultsA[0] > BB_Parameters.minDistanceToDetectLight && distanceResultsA[0] <= BB_Parameters.maxDistanceToDetectLight)
				indices.add(i);

		}
		return indices;
	}

	public int find_shapePoint(double currentSPLat,double currentSPLng){

		int index=0;

		for(int i=0;i<routeSegments.size();i++)
		{
			//Checking to see if it is in the very first segment
			if((routeSegments.get(i).getStartPoint()== null) && ((routeSegments.get(i).getEndPoint().getLatitude()== currentSPLat) && (routeSegments.get(i).getEndPoint().getLongitude() == currentSPLng)))
			{
				index= i;
				break;
			}
			if(routeSegments.get(i).getStartPoint()==null)
				i++;

			if((routeSegments.get(i).getStartPoint().getLatitude()==currentSPLat && routeSegments.get(i).getStartPoint().getLongitude()==currentSPLng) &&
					(routeSegments.get(i).getEndPoint().getLatitude()==currentSPLat && routeSegments.get(i).getEndPoint().getLongitude()==currentSPLng))
			{
				index = i;
				break;
			}
			else
			{
				GeoPoint[] tempArray = routeSegments.get(i).getShapePoints();
				for(int j=0;j<tempArray.length;j++)
				{
					if(tempArray[j].getLatitude() == currentSPLat && tempArray[j].getLongitude() == currentSPLng) {
						index = i;
						break;
					}

				}
				if(index!=0) break;
			}
		}
		return index;
	}
	@Override
	public void onLocationChanged(Location location) {
		debugger += "onLocationChanged() called...\n";

		double lat = location.getLatitude();
		double lng = location.getLongitude();

		// Update Current Location
		str_currentLocation = EntranceActivity.stringifyCoords(lat, lng);

		// Check if the instruction manager has been initialized already

		if (im != null) {

			// If the candidate shape point hasn't been found yet
			//Chris: Runs only the first time
			// THis is to set the Current and next Shape points for the first time
			if (this.candidateSPNotFound) {
				// Get the coordinates of the current shape point
				double currentSPLat = im.getCurrentSP().getLatitude();
				double currentSPLng = im.getCurrentSP().getLongitude();

				// Get the coordinates of the next shape point
				double nextSPLat = im.getNextSP().getLatitude();
				double nextSPLng = im.getNextSP().getLongitude();

				float[] distanceResultsA = new float[1], distanceResultsB = new float[1];

				Location.distanceBetween(lat, lng, currentSPLat, currentSPLng, distanceResultsA);
				Location.distanceBetween(lat, lng, nextSPLat, nextSPLng, distanceResultsB);
				float temp_distance_diff = distanceResultsB[0] - distanceResultsA[0];

				//Get the segments and store it in routeSegments
				routeSegments = im.getSegments();
				// loop through until at next closest shape point
				while (temp_distance_diff <= 0) {
					// go ahead to the next shape point
					im.goToNextSP();

					// update the current and next shape points
					currentSPLat = nextSPLat;
					currentSPLng = nextSPLng;
					nextSPLat = im.getNextSP().getLatitude();
					nextSPLng = im.getNextSP().getLongitude();

					// calc the distance from user to current and next sp
					Location.distanceBetween(lat, lng, currentSPLat, currentSPLng, distanceResultsA);
					Location.distanceBetween(lat, lng, nextSPLat, nextSPLng, distanceResultsB);

					// store the distance difference
					temp_distance_diff = distanceResultsB[0] - distanceResultsA[0];
				}

				this.candidateCurrentSPLat = currentSPLat;
				this.candidateCurrentSPLng = currentSPLng;
				this.candidateSPNotFound = false;
			}

			//Chris: Runs only the first time
			// If the current shape point hasn't been found yet
			if (this.currentSPNotFound) {
				float[] distanceResults = new float[1];
				Location.distanceBetween(lat, lng, this.candidateCurrentSPLat, this.candidateCurrentSPLng, distanceResults);


				// If this is not our first time testing this candidate
				if (this.last_distance >= 0) {
					// if our current distance to the candidate is greater than it was last location update
					if (distanceResults[0] > this.last_distance) {
						// then this candidate is our current shape point, so we have found the current SP
						this.currentSPNotFound = false;
					} else {
						// otherwise the previous candidate was our current shape point and we found the current SP
						im.goToPreviousSP();
						this.currentSPNotFound = false;
					}
				}

				this.last_distance = distanceResults[0];

			} else { // Otherwise we know the current shape point
				double currentSPLat, currentSPLng, nextSPLat, nextSPLng;


				// Calculate the distance to the next shape point
				float[] distanceResults = new float[1];
				Location.distanceBetween(lat, lng, im.getNextSP().getLatitude(), im.getNextSP().getLongitude(), distanceResults);

				// Shift to next shape point if within threshold of current shape point
				// or if next shape point is closer than the current shape point (this
				// should never happen unless we have gotten past the current shape point)

				//CHRIS:: We should do our coding for finding 10 points before intersection
				//here and then count down while approaching the intersection lat lng.

				//Create a copy of the segments that were created in the Route File
				//List<RouteSegment> segmentscopy = new ArrayList<RouteSegment>();
				//segmentscopy =im.getSegments();
				//spm.getSegmentswithShapePoints(im.getCurrentSP().getLatitude(),im.getCurrentSP().getLongitude(),segmentscopy,im.getDecisionPoints());

				im.getShapePoints();
				// Always runs untill no shape points are left
				if (im.hasShapePointsLeft()) {
					if (distanceResults[0] < SHAPE_POINT_DISTANCE_THRESHOLD) {

						im.goToNextSP(); // shift to the next shape point

						// update the lat and long
						currentSPLat = im.getCurrentSP().getLatitude();
						currentSPLng = im.getCurrentSP().getLongitude();
						nextSPLat = im.getNextSP().getLatitude();
						nextSPLng = im.getNextSP().getLongitude();
					}
					currentSPLat = im.getCurrentSP().getLatitude();
					currentSPLng = im.getCurrentSP().getLongitude();
						//Look for our intersection here
						//Step1: find the segment that would contain our current Point **find_shapePoint() does that
						//We shd have a flag here that is ::crossed=undefined initially to call find_shapePoint()
						//then crossed::true if we have crossed an intersection and we call find_shapePoint()
						//crossed::false from the point we have found the segment to the point that we have crossed it.
						segment_no =  find_shapePoint(currentSPLat,currentSPLng);

						GeoPoint spArray[] = routeSegments.get(segment_no).getShapePoints();
						float[] distance_tl = new float[1];
						Location.distanceBetween(lat, lng, routeSegments.get(segment_no).getEndPoint().getLatitude(), routeSegments.get(segment_no).getEndPoint().getLongitude(), distance_tl);
						//This means that we are in range of the last pt in the segment ergo: An Intersection
						if(distance_tl[0]<=40)
						{
							//Here we will do our usual TL detection and count down till we cross

							//Calculate distance and display
							skip_calculate_rows = false;
							dis = calculateDistance(lat, lng, routeSegments.get(segment_no).getEndPoint().getLatitude(), routeSegments.get(segment_no).getEndPoint().getLongitude());
							String t = String.valueOf(dis);
							addstatus(t);
							//Chris: when we cross set the flag to crossed and then do find_shapepoint to find the new segment
							//else we donot need to keep searching for the shapePoint in a segment once we have a position.
						}
						//Here we are going to start detecting unknown intersection by focusing on indices
						else
						{
							//Here we will do Lynn's detection assuming there might be intersections and we
							//donot know about
							//Step 1: get the indices of the points that are closer to our current point
							// Method to find all indices within a distance range for searching all the time
							getIndices(segment_no,currentSPLat,currentSPLng);
							if(indices.size()>0)
							{
								//Find the min and max boundary for our current point
									//find the distance and convert to CM
								float[] distance_between_pts1 = new float[1];
								float[] distance_between_pts2 = new float[1];
								Location.distanceBetween(currentSPLat, currentSPLng, spArray[(int)indices.get(0)].getLatitude(),spArray[(int)indices.get(0)].getLongitude(), distance_between_pts1);

								if(indices.size()==1)
								{
									// Here i am checking if there is only one index (only one point in range) then we give a dummy distance to the second to get the max_row
									distance_between_pts2[0] = distance_between_pts1[0]>20?10:40;
								}
								else {
									//Here we have another point in range and thus would pass the second distance
									Location.distanceBetween(currentSPLat, currentSPLng, spArray[(int) indices.get(1)].getLatitude(), spArray[(int) indices.get(1)].getLongitude(), distance_between_pts2);
								}
								skip_calculate_rows = true;
									//Convert distances to cm and get min row
								double min_row= Calculate_rows((int)(distance_between_pts1[0]*100),"min row");
								double max_row= Calculate_rows((int)(distance_between_pts2[0]*100), "max row");

								//Here we can just assign values to min_row_bound and max_row_bound
								//And set a flag to jump over the Calculate_rows() in OnCameraFrame()
								min_row_bound = min_row;
								max_row_bound = max_row;

								//Here check if currentSpLat and Lng are the same as Lat nd Lng from the array
								//if so then delete the index
								if(currentSPLat == spArray[(int)indices.get(0)].getLatitude() && currentSPLng ==  spArray[(int)indices.get(0)].getLongitude()) {
									indices.remove(0);
								}
								if( spArray[(int)indices.get(1)]!=null) {
									if (currentSPLat == spArray[(int) indices.get(1)].getLatitude() && currentSPLng == spArray[(int) indices.get(1)].getLongitude()) {
										indices.remove(1);
									}
								}
							}

						}
						// bearing calculation
						//double X = Math.cos(nextSPLat) * Math.sin(nextSPLng - currentSPLng);
						//double Y = (Math.cos(currentSPLat) * Math.sin(nextSPLat)) - (Math.sin(currentSPLat) * Math.cos(nextSPLat) * Math.cos(nextSPLng - currentSPLng));



				}

			}
			// Check if the instruction manager has been initialized already

				// Get the coordinates of the next decision point
				double dp1Lat = im.getCurrentInstruction().getDecisionPoint()
						.getLatitude();
				double dp1Lng = im.getCurrentInstruction().getDecisionPoint()
						.getLongitude();

				double dp2Lat = 0;
				double dp2Lng = 0;
				if (im.isOnLastInstruction()) {
					// WILLIAM: I don't know right now
					dp2Lat = dp1Lat;
					dp2Lng = dp1Lng;
					//return;
				} else {
					// Get the coordinates of the decision point after next
					dp2Lat = im.getNextInstructionLocation().getLatitude();
					dp2Lng = im.getNextInstructionLocation().getLongitude();
				}
				// Get the coordinates of the decision point after next
				//double dp2Lat = im.getNextInstructionLocation().getLatitude();
				//double dp2Lng = im.getNextInstructionLocation().getLongitude();

				// Calculate the distance to the next decision point
				float[] results = new float[1];
				float[] tlresult = new float[1];
				Location.distanceBetween(lat, lng, dp1Lat, dp1Lng, results);
				double distanceDP1 = results[0];
				if (lastDistanceDP1 == 0) {
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
				Location.distanceBetween(lat, lng, dp2Lat, dp2Lng, results);    //in meters
				double distanceDP2 = results[0];
				if (lastDistanceDP2 == 0) {
					lastDistanceDP2 = distanceDP2;
				}

				//	String t= String.valueOf(distanceDP2);//+ String.valueOf(results[0]); //String.valueOf(results[0])
				//	addstatus(t); //checking the distance in mts
				// Log the distances
		/*		int f = 1;
				if (sig_flag == -1) {
					f = im.findSignal();
					sig_flag = 1;
				}
				double dptllat = 0, dptllong = 0;*/

				//	GeoPoint nexttl = new GeoPoint((int) (im.tllat * 1e6), (int) (im.ltlong * 1e6));

				//	GeoPoint l = new GeoPoint((int) (lat * 1e6), (int) (lng * 1e6));

			//	GeoPoint nexttl = new GeoPoint((int) (im.tllat * 1e6), (int) (im.ltlong * 1e6));

			//	GeoPoint l = new GeoPoint((int) ((37.654012) * 1e6), (int) ((-122.053441) * 1e6));
				/*if (f == 1) {
					//	Toast.makeText(getApplicationContext(), "Found signal at ! " + nexttl.getLatitude() + "long: " + nexttl.getLongitude(), Toast.LENGTH_SHORT).show();
					//	Location.distanceBetween(dptllat, dptllong, lat, lng, tldis);

					//Old method with alot of error

*//*				double x = (nexttl.getLongitude() - l.getLongitude()) * Math.cos((l.getLatitude() + nexttl.getLatitude()) / 2);
				double y = (nexttl.getLatitude() - l.getLatitude());
				double dis = Math.sqrt(x * x + y * y) * r;

				String x_dis = String.valueOf(dis);
				x_dis = x_dis.substring(2, 6);
				// Was having an error of "Invalid Int exception", Thus removing any spaces in the x_dis string
				x_dis = x_dis.replaceAll("\\D+","");
				int new_dis = Integer.parseInt(x_dis);
				//	int new_dis=Integer.parseInt(String.valueOf(dis).substring(3));
				String t = String.valueOf(new_dis);*//*

					//Haversine Formula
					//37.654012, -122.053441
			//		dis = calculateDistance(lat, lng, im.tllat, im.ltlong);
			//		String t = String.valueOf(dis);
			//		addstatus(t);
				}*/

				String distancesString = "LastDistanceDP1: " + lastDistanceDP1
						+ " | distanceDP1: " + distanceDP1 + " | LastDistanceDP2: "
						+ lastDistanceDP2 + " | distanceDP2: " + distanceDP2;
				debugger += distancesString + "\n";
				Log.e("Navi.onLocationChanged", distancesString);


				// If we have been within NODE_WINDOW_DISTANCE
				// of the next node
				if (distanceDP1 < getApplicationContext().getResources().getInteger(R.integer.NODE_WINDOW_DISTANCE)) {
					beenWithinNodeWindow = true;
				}
				// Check the distances with the stored ones
				// CASE 1: This is the handle when we can get close to
				// our decision point like on city roads, but not
				// highways where you merge off
				if (distanceDP1 < MAX_DISTANCE_TO_DECISION_POINT) {
					// Distance to decision point is less than
					// MAX_DISTANCE_TO_DECISION_POINT

					if (im.isOnLastInstruction()) {
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
					if (im.isOnLastInstruction()) {
						// Tell User they have arrived and stop navigation
						haveArrivedInstruction();
						stopNavigation();
						return;
					}
					updateInstruction();

			}
		}
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
		this.iv_instruction.setImageDrawable(getResources().getDrawable(Maneuver.getDrawableId(instruction.getManeuverType())));
		// --- End of update the instruction view ---

		// --- Update the landmarks on the map (if available) ---
		// Remove previous landmark (if available)
		if (this.map.getOverlays().size() > this.INDEX_OF_LANDMARK_OVERLAY) {
			this.map.getOverlays().remove(this.INDEX_OF_LANDMARK_OVERLAY);
		}

		// Add new local landmark (if available)
		if (instruction.getClass().equals(LandmarkInstruction.class)) {
			Landmark newLocalLandmark = ((LandmarkInstruction) instruction).getLocal();
			OverlayItem oi_newLocalLandmark = new OverlayItem(newLocalLandmark.getCenter(), newLocalLandmark.getTitle(),newLocalLandmark.getCategory());
			MyDefaultItemizedOverlay newLocalLandmarkOverlay = new MyDefaultItemizedOverlay(getResources().getDrawable(LandmarkCategory.getDrawableId(newLocalLandmark.getCategory())));
			newLocalLandmarkOverlay.addItem(oi_newLocalLandmark);
			this.map.getOverlays().add(this.INDEX_OF_LANDMARK_OVERLAY,newLocalLandmarkOverlay);
		}

		// Add new global landmark (if available)
		if (instruction.getClass().equals(GlobalInstruction.class)) {
			Landmark newGlobalLandmark = ((GlobalInstruction) instruction).getGlobal();
			OverlayItem oi_newGlobalLandmark = new OverlayItem(
					newGlobalLandmark.getCenter(),
					newGlobalLandmark.getTitle(),
					newGlobalLandmark.getCategory());
			MyDefaultItemizedOverlay newGlobalLandmarkOverlay = new MyDefaultItemizedOverlay(getResources().getDrawable(LandmarkCategory.getDrawableId(newGlobalLandmark.getCategory())));
			newGlobalLandmarkOverlay.addItem(oi_newGlobalLandmark);
			this.map.getOverlays().add(this.INDEX_OF_LANDMARK_OVERLAY,newGlobalLandmarkOverlay);
		}

		// --- End of updating map ---

		// Speak out the verbal instruction
		speakInstruction();
	}

	private void displayTLInstruction()
	{

		String flg=mDetector.getTl_flag();
		if(flg.equalsIgnoreCase("RED"))
		{
			try {
			if(d<=3000 && d>1500) {

					String nextVerbalInstruction = "Detected Red Light, Slow Down";
					//Load red signal
					// Display the verbal instruction

					TextView tv_ins = (TextView) findViewById(R.id.tv_instruction);
					tv_ins.setText(nextVerbalInstruction);
					// Get the corresponding instruction image and display it
					ImageView iv_ins = (ImageView) findViewById(R.id.iv_instruction);
					iv_ins.setImageDrawable(getResources().getDrawable(R.drawable.tl_red_light));

				}
				else if(d<=1500 && d>900)
				{
					String nextVerbalInstruction = "Detected Red Light, Stop!";
					//Load red signal
					// Display the verbal instruction

					TextView tv_ins = (TextView) findViewById(R.id.tv_instruction);
					tv_ins.setText(nextVerbalInstruction);
					// Get the corresponding instruction image and display it
					ImageView iv_ins = (ImageView) findViewById(R.id.iv_instruction);
					iv_ins.setImageDrawable(getResources().getDrawable(R.drawable.tl_red_light));

				}
				else if(d<=900)
				{
					String nextVerbalInstruction = "Detected Red Light, Stop!";
					//Load red signal
					// Display the verbal instruction

					TextView tv_ins = (TextView) findViewById(R.id.tv_instruction);
					tv_ins.setText(nextVerbalInstruction);
					// Get the corresponding instruction image and display it
					ImageView iv_ins = (ImageView) findViewById(R.id.iv_instruction);
					iv_ins.setImageDrawable(getResources().getDrawable(R.drawable.tl_red_light));

				}
				else
				{
					mDetector.setTl_flag("");
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		if(flg.equalsIgnoreCase("GREEN"))
		{
			try {
				if(d<=3000 && d>900) {
					String nextVerbalInstruction = "Detected Green Light, Proceed with caution";
					//Load Green Signal
					// Display the verbal instruction
					TextView tv_ins1 = (TextView) findViewById(R.id.tv_instruction);
					tv_ins1.setText(nextVerbalInstruction);
					// Get the corresponding instruction image and display it
					ImageView iv_ins = (ImageView) findViewById(R.id.iv_instruction);
					iv_ins.setImageDrawable(getResources().getDrawable(R.drawable.tl_green_light));
				}
				else if(d<=900)
				{
					String nextVerbalInstruction = "Proceed across Intersection with Caution!";
					//Load red signal
					// Display the verbal instruction

					TextView tv_ins = (TextView) findViewById(R.id.tv_instruction);
					tv_ins.setText(nextVerbalInstruction);
					// Get the corresponding instruction image and display it
					ImageView iv_ins = (ImageView) findViewById(R.id.iv_instruction);
					iv_ins.setImageDrawable(getResources().getDrawable(R.drawable.tl_green_light));

				}
				else
				{
					mDetector.setTl_flag("");
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		//Chris: Maybe we shd put a flag here that
		//if RED then show red signal, If GREEN then show green and AMBER for amber
		// --- End of updating map ---

		// Speak out the verbal instruction
		//speakInstruction();
	}

	/**
	 * Speak out the current instruction
	 */
	private void speakInstruction() {
		tts.setSpeechRate((float) 0.85);
		tts.speak(tv_instruction.getText().toString(),TextToSpeech.QUEUE_FLUSH, null);
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


