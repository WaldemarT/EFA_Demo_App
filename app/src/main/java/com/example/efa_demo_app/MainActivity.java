package com.example.efa_demo_app;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.efa_demo_app.Helpers.Journey;
import com.example.efa_demo_app.Helpers.JourneyAdapter;
import com.example.efa_demo_app.Helpers.Trip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // StopFinder Request URI Example: http://smartmmi.demo.mentz.net/smartmmi/XML_STOPFINDER_REQUEST?outputFormat=rapidJson&type_sf=any&name_sf=mto
    public String stopFinderURI = "http://smartmmi.demo.mentz.net/smartmmi/XML_STOPFINDER_REQUEST?outputFormat=rapidJson&type_sf=any&name_sf=";
    public String stopFinderURIfinal;

    // Trip Request URI Example: http://smartmmi.demo.mentz.net/smartmmi/XML_TRIP_REQUEST2?outputFormat=rapidJson&type_sf=any&type_origin=stop&name_origin=de:08212:89&type_destination=stop&name_destination=de:08212:5203
    public String tripRequestURIbegin = "http://smartmmi.demo.mentz.net/smartmmi/XML_TRIP_REQUEST2?outputFormat=rapidJson&type_sf=any&type_origin=stop&name_origin=";
    public String tripRequestURIend = "&type_destination=stop&name_destination=";
    public String tripRequestURIfinal;

    // Stop List Request
    public String stopListRequestURI = "http://smartmmi.demo.mentz.net/sl3/XML_STOPLIST_REQUEST?stopListSubnetwork=kvv&outputFormat=rapidJSON";
    public Button buttonStopListRequest;
    public TextView resultStopListRequest;
    public StringBuffer stopListResult;

    // Origin
    public LinearLayout linLayStartHS;
    public TextView editTextOrigin;
    public TextView requestEFAOrigin;
    public TextView resultEFAOrigin;
    public Button buttonSearchOrigin;
    public String selectedOriginItem;
    public String selectedOriginStationID;

    // General Parameters
    private RequestQueue mQeue;
    public HashMap<String, String> data;
    public PopupMenu popupMenu;

    // Destination
    public LinearLayout linLayZielHS;
    public TextView editTextDestination;
    public TextView requestEFADestination;
    public TextView resultEFADestination;
    public Button buttonSearchDestination;
    public String selectedDestinationItem;
    public String selectedDestinationStationID;

    // Trip Request
    public LinearLayout linLayTripRequest;
    public Button buttonTripRequest;
    //public ArrayAdapter myAdapter;

    // Trips list
    private List<Journey> journeyList = new ArrayList<>();
    private List<Trip> allTrips;
    private ListView tripsListView;

    // Fixing Footpath Bug
    private String transportationName;

    // Save files to external storage
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tripsListView = findViewById(R.id.listv);

        // Origin
        linLayStartHS = findViewById(R.id.linLayStartHS);
        editTextOrigin = findViewById(R.id.editTextOrigin);
        buttonSearchOrigin = findViewById(R.id.buttonSearchOrigin);
        requestEFAOrigin = findViewById(R.id.requestOriginTextView);
        resultEFAOrigin = findViewById(R.id.responseOriginTextView);
        resultEFAOrigin.setMovementMethod(new ScrollingMovementMethod());

        // General Parameters
        mQeue = Volley.newRequestQueue(this);
        data = new HashMap<>();

        // Destination
        linLayZielHS = findViewById(R.id.linLayZielHS);
        editTextDestination = findViewById(R.id.editTextDestination);
        buttonSearchDestination = findViewById(R.id.buttonSearchDestination);
        requestEFADestination = findViewById(R.id.requestDestinationTextView);
        resultEFADestination = findViewById(R.id.responseDestinationTextView);
        resultEFADestination.setMovementMethod(new ScrollingMovementMethod());

        // Trip Request
        linLayTripRequest = findViewById(R.id.linLayTripRequest);
        buttonTripRequest = findViewById(R.id.buttonTripRequest);

        // Stop List Request
        buttonStopListRequest = findViewById(R.id.buttonStopListRequest);
        resultStopListRequest = findViewById(R.id.resultStopListRequest);
        resultStopListRequest.setMovementMethod(new ScrollingMovementMethod());
        stopListResult = new StringBuffer("Test");

        // Origin
        buttonSearchOrigin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultEFAOrigin.setText("");
                popupMenu = new PopupMenu(MainActivity.this, buttonSearchOrigin);
                String origin = String.valueOf(editTextOrigin.getText());
                stopFinderURIfinal = stopFinderURI + origin;
                requestEFAOrigin.setText(stopFinderURIfinal);

                // Tutorial https://www.youtube.com/watch?v=y2xtLqP8dSQ
                jsonParseOrigin();
            }
        });

        // Destination
        buttonSearchDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultEFADestination.setText("");
                popupMenu = new PopupMenu(MainActivity.this, buttonSearchDestination);
                String origin = String.valueOf(editTextDestination.getText());
                stopFinderURIfinal = stopFinderURI + origin;
                requestEFADestination.setText(stopFinderURIfinal);

                // Tutorial https://www.youtube.com/watch?v=y2xtLqP8dSQ
                jsonParseDestination();
            }
        });

        // TripRequest
        buttonTripRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                tripRequestURIfinal = tripRequestURIbegin + selectedOriginStationID + tripRequestURIend + selectedDestinationStationID;
                Log.d("TripRequest: ", tripRequestURIfinal);

                linLayStartHS.setVisibility(View.GONE);
                linLayZielHS.setVisibility(View.GONE);
                linLayTripRequest.setVisibility(View.GONE);
                //Toast.makeText(MainActivity.this, "Triggers Trip Request: " + selectedOriginStationID + " to " + selectedDestinationStationID, Toast.LENGTH_LONG).show();
                tripRequest();
            }
        });
        //myAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);

        // Stop List Request
        buttonStopListRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("StopListRequest: ", stopListRequestURI);
                stopListRequest();
            }
        });
    }

    // Origin
    private void jsonParseOrigin() {

        String url = stopFinderURIfinal;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("locations");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject location = jsonArray.getJSONObject(i);

                                String stationID = location.getString("id");
                                String stationName = location.getString("name");
                                int stationMatchQuality = location.getInt("matchQuality");

                                data.put(stationName, stationID);

                                popupMenu.getMenu().add(stationName);
                                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        selectedOriginItem = item.getTitle().toString();
                                        editTextOrigin.setText(selectedOriginItem);
                                        Log.d("selectedOriginTitle ", selectedOriginItem);
                                        // DEBUG
                                        //Toast.makeText(MainActivity.this, selectedOriginItem, Toast.LENGTH_SHORT).show();

                                        selectedOriginStationID = data.get(selectedOriginItem);
                                        if (selectedOriginStationID != null) {
                                            Log.d("selectedOriginID ", selectedOriginStationID);
                                        } else {
                                            Toast.makeText(MainActivity.this, "please be more specific", Toast.LENGTH_SHORT).show();
                                        }
                                        return false;
                                    }
                                });

                                // DEBUG
                                resultEFAOrigin.append(stationName + "; " + stationID + "; " + (stationMatchQuality) + "\n\n");
                            }
                            //Log.d("HashMapAll: ", String.valueOf(data));
                            popupMenu.show();

                            // set LinearLayout visible for user to select destination station
                            linLayZielHS.setVisibility(View.VISIBLE);

                        } catch (JSONException e) {
                            Log.e("ERROR", e.toString());
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQeue.add(request);
    }

    // Destination
    private void jsonParseDestination() {

        String url = stopFinderURIfinal;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("locations");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject location = jsonArray.getJSONObject(i);

                                String stationID = location.getString("id");
                                String stationName = location.getString("name");
                                int stationMatchQuality = location.getInt("matchQuality");

                                data.put(stationName, stationID);

                                popupMenu.getMenu().add(stationName);
                                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        selectedDestinationItem = item.getTitle().toString();
                                        editTextDestination.setText(selectedDestinationItem);
                                        Log.d("selectedDestTitle ", selectedDestinationItem);
                                        // DEBUG
                                        //Toast.makeText(MainActivity.this, selectedOriginItem, Toast.LENGTH_SHORT).show();

                                        selectedDestinationStationID = data.get(selectedDestinationItem);
                                        if (selectedDestinationStationID != null) {
                                            Log.d("selectedDestID ", selectedDestinationStationID);
                                        } else {
                                            Toast.makeText(MainActivity.this, "please be more specific", Toast.LENGTH_SHORT).show();
                                        }
                                        return false;
                                    }
                                });

                                // DEBUG
                                resultEFADestination.append(stationName + "; " + stationID + "; " + (stationMatchQuality) + "\n\n");
                            }
                            //Log.d("HashMapAll: ", String.valueOf(data));
                            popupMenu.show();

                            // set Linear Layout visible for user to start TripRequest
                            linLayTripRequest.setVisibility(View.VISIBLE);

                        } catch (JSONException e) {
                            Log.e("ERROR", e.toString());
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQeue.add(request);
    }

    // Origin
    public void tripRequest() {

        String url = tripRequestURIfinal;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArrayJourneys = response.getJSONArray("journeys");
                            for (int i = 0; i < jsonArrayJourneys.length(); i++) {
                                JSONObject journey = jsonArrayJourneys.getJSONObject(i);
                                Log.d("test", journey.toString());

                                allTrips = new ArrayList<>();

                                // legs node is JSON Array
                                JSONArray jsonArrayLegs = journey.getJSONArray("legs");
                                for (int j = 0; j < jsonArrayLegs.length(); j++) {
                                    JSONObject leg = jsonArrayLegs.getJSONObject(j);

                                    //int duration = leg.getInt("duration");
                                    //Log.d("TripRequest_LEGS ", String.valueOf(duration));

                                    // origin node is JSON Object
                                    JSONObject jsonObjectOrigin = leg.getJSONObject("origin");
                                    String originDepartureTime = jsonObjectOrigin.getString("departureTimePlanned");

                                    //Log.d("TripRequest_DepartTime ", String.valueOf(originDepartureTime));
                                    // convert String to date
                                    DateFormat formatDepartureTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.GERMANY);
                                    Date originDepartureDate = formatDepartureTime.parse(originDepartureTime);
                                    originDepartureDate.getTime();
                                    Log.d("TripRequest_DepartTime ", String.valueOf(originDepartureDate));

                                    // destination node is JSON Object
                                    JSONObject jsonObjectDestination = leg.getJSONObject("destination");
                                    String originArrivalTime = jsonObjectDestination.getString("arrivalTimePlanned");
                                    //Log.d("TripRequest_ArriveTime ", String.valueOf(originArrivalTime));
                                    // convert String to date
                                    DateFormat formatArrivalTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.GERMANY);
                                    Date originArrivalDate = formatArrivalTime.parse(originArrivalTime);
                                    Log.d("TripRequest_ArriveTime ", String.valueOf(originArrivalDate));

                                    // calculating trip duration "travelTime"
                                    long travelTime = originArrivalDate.getTime() - originDepartureDate.getTime();
                                    Log.d("TripRequest_TravelTime ", String.valueOf(originArrivalDate.getTime() - originDepartureDate.getTime()));
                                    long minutes = travelTime / 60000;
                                    Log.d("TripRequest_TravelTime ", minutes + " Minuten");

                                    // transportation node is JSON Object
                                    JSONObject jsonObjectTransportation = leg.getJSONObject("transportation");

                                    try {
                                        transportationName = jsonObjectTransportation.getString("name");
                                        Log.d("TryCatchBlock", transportationName);
                                    } catch (Exception e) {
                                        Log.d("TryCatchBlock", String.valueOf(e));
                                        // transportation node is JSON Object
                                        JSONObject jsonObjectProduct = jsonObjectTransportation.getJSONObject("product");
                                        transportationName = jsonObjectProduct.getString("name");
                                    }

                                    Log.d("TripRequest_TRANSPORT ", String.valueOf(transportationName));

                                    allTrips.add(new Trip(String.valueOf(originDepartureDate), String.valueOf(originArrivalDate), minutes, String.valueOf(transportationName)));
                                    Log.d("test", String.valueOf(allTrips.size()));
                                }

                                journeyList.add(new Journey(allTrips));

                                //end of a journey element
                                Log.d("TripRequest_Journey_# ", String.valueOf(i));


                                // Tutorial: https://stackoverflow.com/questions/47129961/how-to-parsing-multi-dimensional-json-data-array-in-android-studio
                            }


                        } catch (JSONException e) {
                            Log.e("ERROR", e.toString());
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        JourneyAdapter journeyAdapter = new JourneyAdapter(getApplicationContext(), journeyList);
                        tripsListView.setAdapter(journeyAdapter);

                        // adding onItemClickListener to trigger AlarmDialog
                        tripsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                final Object o = tripsListView.getItemAtPosition(position);
                                //Toast.makeText(getBaseContext(),String.valueOf(o),Toast.LENGTH_SHORT).show();

                                new AlertDialog.Builder(MainActivity.this)
                                        //.setTitle("Travel Companion Service")
                                        .setTitle("Aktivieren der Reisebegleitung")
                                        //.setMessage("Set the selected trip as actual trip?")
                                        .setMessage("Möchten Sie die Reisebegleitung für gewählte Route aktivieren?")

                                        // Specifying a listener allows you to take an action before dismissing the dialog.
                                        // The dialog is automatically dismissed when a dialog button is clicked.
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                // Continue with delete operation
                                                Toast.makeText(getBaseContext(), "Reisebegleitung für gewählte Reise nach: " + selectedDestinationItem + " aktiviert", Toast.LENGTH_LONG).show();
                                            }
                                        })

                                        // A null listener allows the button to dismiss the dialog and take no further action.
                                        .setNegativeButton(android.R.string.no, null)
                                        .setIcon(android.R.drawable.ic_dialog_map)
                                        .show();
                            }
                        });

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQeue.add(request);
    }

    // Stop List Request
    public void stopListRequest() {

        String url = stopListRequestURI;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArrayLocations = response.getJSONArray("locations");
                            for (int i = 0; i < jsonArrayLocations.length(); i++) {
                                JSONObject location = jsonArrayLocations.getJSONObject(i);
                                //Log.d("StopListRequest", String.valueOf(location));

                                String stationID = location.getString("id");
                                String stationNameMinor = location.getString("name");
                                //Log.d("StopListRequest", stationNameMinor + "," + stationID);

                                JSONObject jsonObjectParent = location.getJSONObject("parent");

                                String stationNameMajor = jsonObjectParent.getString("name");
                                //Log.d("StopListRequest", String.valueOf(stationNameMajor));

                                stopListResult.append(stationID + "," + stationNameMajor + "," + stationNameMinor + "\n");
                                Log.d("hallo", String.valueOf(stopListResult));
                                resultStopListRequest.append(stationID + ", " + stationNameMajor + ", " + stationNameMinor + "\n");
                                //Log.d("StopListRequest", stationID + ", " + stationNameMajor + ", " + stationNameMinor);
                            }

                            // Writes StringBuffer stopListResult to file stations_WT.json on external Storage (Documents)
                            // Check whether this app has write external storage permission or not.
                            int writeExternalStoragePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            // If do not grant write external storage permission.
                            if (writeExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                                // Request user to grant write external storage permission.
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
                            }
                            // Save email_public.txt file to /storage/emulated/0/DCIM folder
                            String publicDcimDirPath = ExternalStorageUtil.getPublicExternalStorageBaseDir(Environment.DIRECTORY_DOCUMENTS);

                            File newFile = new File(publicDcimDirPath, "stations_WT.json");

                            FileWriter fw = new FileWriter(newFile);

                            fw.write(String.valueOf(stopListResult));

                            fw.flush();

                            fw.close();

                            Toast.makeText(getApplicationContext(), "Save to public external storage success. File Path " + newFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

                        } catch (JSONException e) {
                            Log.e("ERROR", e.toString());
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQeue.add(request);
    }
}
