package com.ayoubmkdm.github.smartfarming;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener {
    private static final String TAG = "MapsActivity";
    public static final float DEFAULT_ZOOM = 15f;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int PERMISSIONS_GRANTED = 0;
    private static final int LOCATIONS_PERMISSIONS_NOT_GRANTED = 111;
    private static final int GPS_NOT_ACTIVATED = 222;
    //Vars
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean mLocationPermissionGranted = false;
    private Location mCurrentLocation;
    private boolean markerClicked;
    private LocationManager mLocationManager;
    //Widgets
    private GoogleMap mMap;
    private PolygonOptions mOptions;
    private Polygon mPolygon;
    private BottomAppBar mBottomAppBar;
    private LinearLayout mBottomSheet;
    private BottomSheetBehavior mBottomSheetBehavior;

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        solvePermissions(isPermissionsSet());
        getDeviceLocation();
        mMap.setMyLocationEnabled(true);
        //TODO clean the code
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);

        markerClicked = false;
    }

    @Override
    public void onMapClick(LatLng point) {
        Toast.makeText(this, point.toString(), Toast.LENGTH_LONG).show();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(point));
        markerClicked = false;
    }

    @Override
    public void onMapLongClick(LatLng point) {
        Toast.makeText(this, "New marker added @ " + point.toString(),
                Toast.LENGTH_LONG).show();

        mMap.addMarker(new MarkerOptions().position(point).title(point.toString()));

        markerClicked = false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (markerClicked) {
            if (mPolygon != null) {
                mPolygon.remove();
                mPolygon = null;
            }
            mOptions.add(marker.getPosition());
            mOptions.strokeColor(Color.rgb(255,214,0));
            mOptions.fillColor(Color.argb(150,255,255,82));
            mPolygon = mMap.addPolygon(mOptions);
        } else {
            if (mPolygon != null) {
                mPolygon.remove();
                mPolygon = null;
            }
            mOptions = new PolygonOptions().add(marker.getPosition());
            markerClicked = true;
        }
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //Initialization
        mBottomAppBar = findViewById(R.id.maps_bottom_bar);
        mBottomSheet = findViewById(R.id.tools_bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        //get the data from the intent
        mLocationPermissionGranted = getIntent().getBooleanExtra(MainActivity.IS_PERMISSIONS_GRANTED,
                false);
        mBottomAppBar.setOnClickListener(view -> {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        solvePermissions(isPermissionsSet());
        initGoogleMaps();
    }

    private void solvePermissions(int permissionCode) {
        switch (permissionCode){
            case GPS_NOT_ACTIVATED:
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                break;
            case LOCATIONS_PERMISSIONS_NOT_GRANTED:
                goBackAndRequestPermissions();
                break;
            default: return;
        }
    }

    private int isPermissionsSet() {
        if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED){
            return LOCATIONS_PERMISSIONS_NOT_GRANTED;
        }else if(!isGpsActivated(this)){
            return GPS_NOT_ACTIVATED;
        }else {
            return PERMISSIONS_GRANTED;
        }
    }

    private void goBackAndRequestPermissions() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void initGoogleMaps() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceCurrentLocation: getting the device current location");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionGranted) {
                Task<Location> location = mFusedLocationClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "getDeviceLocation: current location detected");
                        mCurrentLocation = task.getResult();
                        if (mCurrentLocation != null) {
                            moveCamera(new LatLng(mCurrentLocation.getLatitude(),
                                    mCurrentLocation.getLongitude()), DEFAULT_ZOOM);
                            //TODO code need to be cleaned
                            // the @var mOptions default setting is not clickable, if the clickable option is true
                            // that means that the @method drawAPolygonAroundTheCurrentPlace executed
                            // and we already drawn a polygon
//                            boolean isThereAPolygonOnTheMap = mOptions.isClickable();
                            //when there is a polygon on the map there is no need to draw anther one
//                            if (!isThereAPolygonOnTheMap)    drawAPolygonAroundTheCurrentPlace();

                        } else {
                            Log.d(TAG, "getDeviceLocation: current location is unknown"
                                    + task.getException());
                            Toast.makeText(this, "Impossible d'obtenir l'emplacement actuel de l'appareil",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.d(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
            Toast.makeText(this, "Une erreur s'est produite lors de la tentative " +
                    "d'obtention de votre position \n" + e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void drawAPolygonAroundTheCurrentPlace() {
        //TODO add a refresh button to draw another polygon in different location
        mOptions.clickable(true).add(new LatLng(mCurrentLocation.getLatitude() - 0.001, mCurrentLocation.getLongitude() - 0.001)
                ,new LatLng(mCurrentLocation.getLatitude() + 0.001, mCurrentLocation.getLongitude() - 0.001)
                ,new LatLng(mCurrentLocation.getLatitude() + 0.001, mCurrentLocation.getLongitude() + 0.001)
                ,new LatLng(mCurrentLocation.getLatitude() - 0.001, mCurrentLocation.getLongitude() + 0.001))
                .fillColor(Color.argb(150,255,255,82))
                .strokeColor(Color.rgb(255,214,0));
        //add a polygon to the map
        Polygon polygon = mMap.addPolygon(mOptions);
        // Store a data object with the polygon, used here to indicate an arbitrary type.
        polygon.setTag("parcel");
    }

    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG, "moveCamera: to lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    public boolean isGpsActivated(Context context){
        mLocationManager = (LocationManager)context.getSystemService(context.LOCATION_SERVICE);
        assert mLocationManager != null;
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}










