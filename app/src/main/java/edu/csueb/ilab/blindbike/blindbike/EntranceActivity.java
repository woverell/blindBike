package edu.csueb.ilab.blindbike.blindbike;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mapquest.android.maps.GeoPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.mrunde.bachelorthesis.activities.NaviActivity;


public class EntranceActivity extends ActionBarActivity {

    /**
     * Coordinates of the destination to be sent to the NaviActivity
     */
    private double[] destination_coords;

    // True if location available, False otherwise
    private boolean location_available;

    // True if destination available, False otherwise
    private boolean destination_available;

    private Location currentLocation;

    private LocationManager locationManager;

    private LocationListener locationListener;

    private String str_currentLocation;

    private String str_destination;

    private String routeType;

    // GUI Variables
    private EditText edit_destination;

    private Button button_options;

    private Button button_go;

    private Button button_help;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrance);

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(getApplicationContext().LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                location_available = true;
                currentLocation = location;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        // Set the route type to fastest
        this.routeType = getApplicationContext().getString(R.string.ROUTE_TYPE);

        setupGUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        location_available = false; // since the app just resumed there will be no location
        destination_available = false; // no destination will be set when app resumes

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_entrance, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * This function will attempt to start the navigation activity
     * If no location can be found or if the destination cannot
     * be determined then it will use a Toast to inform the user
     * that they need to wait for a location or re-enter a
     * proper destination.
     */
    private void startNavigation(){
        if (location_available && destination_available) {
            // Stringify the destination coordinates
            str_destination = stringifyCoords(destination_coords[0], destination_coords[1]);
            // Get the current location
            str_currentLocation = stringifyCoords(currentLocation.getLatitude(), currentLocation.getLongitude());
            // Stop listening for gps
            locationManager.removeUpdates(locationListener);

            // Create an Intent to start the NaviActivity and hereby the
            // navigation
            Intent intent = new Intent(EntranceActivity.this,
                    NaviActivity.class);
            intent.putExtra("str_currentLocation", str_currentLocation);
            intent.putExtra("str_destination", str_destination);
            intent.putExtra("destination_lat", destination_coords[0]);
            intent.putExtra("destination_lng", destination_coords[1]);
            intent.putExtra("routeOptions", getRouteOptions());
            startActivity(intent);
        }
        if(!destination_available){
            Toast.makeText(EntranceActivity.this,
                    R.string.noDestinationEntered, Toast.LENGTH_SHORT)
                    .show();
        }else if(!location_available){
            Toast.makeText(EntranceActivity.this,
                    R.string.noLocationFound, Toast.LENGTH_SHORT)
                    .show();
        }else{
            Toast.makeText(EntranceActivity.this,
                    R.string.routeNotCalculated, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Sets up the GUI elements
     */
    private void setupGUI(){
        this.edit_destination = (EditText) findViewById(R.id.destinationEditText);

        this.button_go = (Button) findViewById(R.id.goButton);
        button_go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the entered destination
                String dest_text = edit_destination.getText().toString();

                if (dest_text.length() == 0) {
                    // If no destination entered then tell user
                    Toast.makeText(EntranceActivity.this,
                            R.string.noDestinationEntered, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    /*
                    // Start asyc SearchDestinationTask to get coordinates from geocoder
                    SearchDestinationTask destinationTask = new SearchDestinationTask();
                    destinationTask.execute(dest_text);
                    */
                    // Set destination coordinates
                    SearchDestination(dest_text);

                    // Start Navigation Attempt
                    startNavigation();
                }
            }
        });
    }

    /**
     * This function takes a latitude and longitude value and creates a string
     * that mapquest knows how to read.
     * @param latitude
     * @param longitude
     * @return String containing lat/long that mapquest can read
     */
    private String stringifyCoords(double latitude, double longitude){
        return "{latLng:{lat:" + latitude + ",lng:" + longitude + "}}";
    }

    private void SearchDestination(String... dest_text){
        String str_destination = dest_text[0];
        List<Address> addresses;
        try {
            // Create a geocoder to locate the destination
            Geocoder geocoder = new Geocoder(EntranceActivity.this,
                    Locale.getDefault());
            addresses = geocoder.getFromLocationName(str_destination,
                    R.integer.MAX_RESULTS);
            // Destination could be located
            Log.d("MainActivity", "Located destination sucessfully.");
            GeoPoint result = new GeoPoint(addresses.get(0).getLatitude(),
                    addresses.get(0).getLongitude());
            // Store result in dest_coords
            destination_coords = new double[] { result.getLatitude(),
                    result.getLongitude() };

            destination_available = true;
        } catch (IOException e1) {
            // Destination could not be located but try again once
            // because sometimes it works at the second try
            Log.d("EntranceActivity",
                    "First try to locate destination failed. Starting second try...");
            try {
                // Create a geocoder to locate the destination
                Geocoder geocoder = new Geocoder(EntranceActivity.this,
                        Locale.getDefault());
                addresses = geocoder.getFromLocationName(str_destination,
                        R.integer.MAX_RESULTS);
                // Destination could be located
                Log.d("MainActivity", "Located destination sucessfully.");
                GeoPoint result = new GeoPoint(addresses.get(0).getLatitude(),
                        addresses.get(0).getLongitude());
                // Store result in dest_coords
                destination_coords = new double[]{result.getLatitude(),
                        result.getLongitude()};

                destination_available = true;
            } catch (IOException e2) {
                // Seems like the destination could really not be
                // found, so send the user a message about the error
                Log.e("EntranceActivity",
                        "IO Exception in searching for destination. This is the error message: "
                                + e2.getMessage());
                Toast.makeText(EntranceActivity.this,
                        R.string.noDestinationFound, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    /**
     * This is a class to search for the destination asynchronously.
     *
     * @author Marius Runde
     */
    private class SearchDestinationTask extends
            AsyncTask<String, Void, GeoPoint> {

        /**
         * Progress dialog to inform the user about the searching process
         */
        private ProgressDialog progressDialog = new ProgressDialog(
                EntranceActivity.this);

        @Override
        protected void onPreExecute() {
            // Display progress dialog
            progressDialog.setMessage("Searching for destination...");
            progressDialog.show();
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    // Enable canceling the search
                    SearchDestinationTask.this.cancel(true);
                }
            });
        }

        @Override
        protected GeoPoint doInBackground(String... destination) {
            String str_destination = destination[0];
            List<Address> addresses;
            try {
                // Create a geocoder to locate the destination
                Geocoder geocoder = new Geocoder(EntranceActivity.this,
                        Locale.getDefault());
                addresses = geocoder.getFromLocationName(str_destination,
                        R.integer.MAX_RESULTS);
            } catch (IOException e1) {
                // Destination could not be located but try again once
                // because sometimes it works at the second try
                Log.d("MainActivity",
                        "First try to locate destination failed. Starting second try...");
                try {
                    // Create a geocoder to locate the destination
                    Geocoder geocoder = new Geocoder(EntranceActivity.this,
                            Locale.getDefault());
                    addresses = geocoder.getFromLocationName(str_destination,
                            R.integer.MAX_RESULTS);
                } catch (IOException e2) {
                    // Seems like the destination could really not be
                    // found, so send the user a message about the error
                    Log.e("MainActivity",
                            "IO Exception in searching for destination. This is the error message: "
                                    + e2.getMessage());
                    Toast.makeText(EntranceActivity.this,
                            R.string.noDestinationFound, Toast.LENGTH_SHORT)
                            .show();
                    return null;
                }
            }

            if (addresses.isEmpty()) {
                // Destination could not be located
                Toast.makeText(EntranceActivity.this, R.string.noDestinationFound,
                        Toast.LENGTH_SHORT).show();
                return null;
            } else {
                // Destination could be located
                Log.d("MainActivity", "Located destination sucessfully.");
                GeoPoint result = new GeoPoint(addresses.get(0).getLatitude(),
                        addresses.get(0).getLongitude());
                return result;
            }
        }

        @Override
        protected void onPostExecute(GeoPoint result) {
            // Dismiss progress dialog
            progressDialog.dismiss();

            // Check if the search was successful
            if (result != null) {
                // Store result in dest_coords
                destination_coords = new double[] { result.getLatitude(),
                        result.getLongitude() };

                destination_available = true;
            }

            // Start Navigation Attempt
            startNavigation();
        }
    }

    /**
     * Setup the route options and return them
     *
     * @return Route options as String
     */
    private String getRouteOptions() {
        JSONObject options = new JSONObject();

        try {
            // Set the units to kilometers
            String unit = "m";
            options.put("unit", unit);

            // Set the route type
            options.put("routeType", routeType);

            // Set the output shape format
            String outShapeFormat = "raw";
            options.put("outShapeFormat", outShapeFormat);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return options.toString();
    }
}
