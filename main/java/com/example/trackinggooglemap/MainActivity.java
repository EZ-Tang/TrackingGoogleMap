package com.example.trackinggooglemap;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;



// Android Map Tracking by Eric Tang
//
// The following Youtube videos/Forums were used in this project:
// https://www.youtube.com/watch?v=eiexkzCI8m8 (How to Implement Google Map in Android Studio | GoogleMap | Android Coding)
// https://www.youtube.com/watch?v=ari3iD-3q8c (Android Get Current Location | Latitude, Longitude | Address | FusedLocationProvider API, Geocoder)
// https://stackoverflow.com/questions/30249920/how-to-draw-path-as-i-move-starting-from-my-current-location-using-google-maps
// https://developers.google.com/maps/documentation/android-sdk/get-api-key#restrict_key
// And a bunch of others minor websites...

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    public static final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    GoogleMap map;
    double longitude;
    double latitude;

    LocationManager locationManager;
    boolean newMap = true; // check if this is a new map or if the map has just been cleared
    int marker = 1;

    private ArrayList<LatLng> points; //Polyline
    Polyline line; //Polyline

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION); //prompts user for location permission

        points = new ArrayList<LatLng>(); // For Polyline
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        /* Prevents error from being raised by not checking the permission.
        locationManager.requestLocationUpdates(...) does nothing without permission.*/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, MainActivity.this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        /* Adds marker to where the user is standing (prompted by the button) */
        Button addButton = (Button) findViewById(R.id.buttonAdd);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getCurrentLocation();
                if (!newMap) {
                    LatLng addLocation = new LatLng(latitude, longitude);
                    map.addMarker(new MarkerOptions().position(addLocation).title("Marker #" + marker));
                    map.moveCamera(CameraUpdateFactory.newLatLng(addLocation));
                    marker = marker + 1;
                }
            }
        });

        /* Clears the map*/
        Button clearButton = (Button) findViewById(R.id.buttonClear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                newMap = true;
                points = new ArrayList<LatLng>();
                marker = 1;
                if (map != null) //checks if there is a map
                    map.clear();
                getCurrentLocation();
            }
        });

    }

    /* Checks if the user has given the app location permission */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0)
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Please give this harmless app permission! :D", Toast.LENGTH_SHORT).show();
            }
    }

    public void getCurrentLocation() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }

        else if (map != null){
            map.setMyLocationEnabled(true); //enables user location
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, MainActivity.this);
            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            super.onLocationResult(locationResult);
                            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                    .removeLocationUpdates(this);
                            if (locationResult != null && locationResult.getLocations().size() > 0) {
                                int latestLocationIndex = locationResult.getLocations().size() - 1;
                                latitude = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                                longitude = locationResult.getLocations().get(latestLocationIndex).getLongitude();

                                if (newMap) { //if the map is newly opened, set an initial marker
                                    LatLng startLocation = new LatLng(latitude, longitude);
                                    map.addMarker(new MarkerOptions().position(startLocation).title("Starting Location"));
                                    map.moveCamera(CameraUpdateFactory.newLatLng(startLocation));
                                    newMap = false;
                                }
                            }
                        }


                    }, Looper.getMainLooper());
        }
    }

    /*Sets map to googleMap*/
    @Override
    public void onMapReady(GoogleMap googleMap){
        map = googleMap;
    }


    @Override
    /*Everytime the location changes, add a Polyline to simulate the movement*/
    public void onLocationChanged(Location loc) {
        if (latitude != 0 || longitude != 0) {
            getCurrentLocation();
            LatLng latLng = new LatLng(latitude, longitude);
            points.add(latLng); // adds all polylines into the points arrayList
            PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
            for (int i = 0; i < points.size(); i++) {
                LatLng point = points.get(i);
                options.add(point);
            }
            line = map.addPolyline(options); //add Polyline
        }
    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
