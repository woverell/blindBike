package edu.csueb.ilab.blindbike.blindbike;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
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
        setContentView(R.layout.activity_entrance_landscape);

        // Set the route type to fastest
        this.routeType = getApplicationContext().getString(R.string.ROUTE_TYPE);

        setupGUI();
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
     * Sets up the GUI elements
     */
    private void setupGUI(){
        this.edit_destination = (EditText) findViewById(R.id.destinationEditText);

        this.button_go = (Button) findViewById(R.id.goButton);
        button_go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_go.getText() == "") {
                    // Get the entered destination
                    String dest_text = edit_destination.getText().toString();

                    if (dest_text.length() == 0) {
                        // If no destination entered then tell user
                        Toast.makeText(EntranceActivity.this,
                                R.string.noDestinationEntered, Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        // Start asyc SearchDestinationTask to get coordinates from geocoder
                        SearchDestinationTask destinationTask = new SearchDestinationTask();
                        destinationTask.execute(dest_text);
                    }
                }else {
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

                // Update the button text to Go


                /*
                    // Create the destination overlay
                    addDestinationOverlay(result);

                    // If the route has been calculated before change the text
                    // of the button so the route has to be calculated again and
                    // clear the route from the RouteManager
                    if (btn_calculate.getText() == getResources().getString(
                            R.string.start)) {
                        btn_calculate.setText(R.string.calculate);
                        rm.clearRoute();
                    }
                */
            }
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
