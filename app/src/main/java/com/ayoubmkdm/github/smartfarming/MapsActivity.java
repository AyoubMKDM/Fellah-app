package com.ayoubmkdm.github.smartfarming;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Switch;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener, View.OnClickListener {
    private static final String TAG = "MapsActivity";
    private static final float DEFAULT_ZOOM = 15f;
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
    private RelativeLayout mLayoutInfosOpened;
    private LinearLayout mLayoutInfosClosed;
    private BottomSheetBehavior mBottomSheetBehavior;
    private ImageButton mButtonToolsMenu;
    private ImageButton mButtonCloseInfosLayout;
    private ImageButton mButtonOpenInfosLayout;
    private LinearLayout mBottomSheet;
    private ImageButton mButtonMyLocation;
    private ImageButton mButtonSearch;
    private ImageButton mButtonDragMarker;
    private ImageButton mButtonDeleteMarker;
    private ImageButton mButtonSaveMyWork;
    private Switch mSwitchDeleteMarker;
    private EditText mSearchField;
    private LinearLayout mLayoutAbout;
    private boolean isDeletingMarkerOn = false;


    // TODO add the mOptions and mPolygon to the savedInstanceState Bundle
    @Override
    protected void onResume() {
        super.onResume();
        solvePermissions(isPermissionsSet());
        initGoogleMaps();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //Initialization
        mBottomSheet = findViewById(R.id.tools_bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        mLayoutInfosClosed = findViewById(R.id.layout_infos_closed);
        mLayoutInfosOpened = findViewById(R.id.layout_infos_opened);
        mButtonCloseInfosLayout = findViewById(R.id.maps_button_img_close);
        mButtonOpenInfosLayout = findViewById(R.id.maps_button_img_open);
        mButtonToolsMenu = findViewById(R.id.maps_button_img_tool_menu);
        mButtonToolsMenu = findViewById(R.id.maps_button_img_tool_menu);
        mButtonMyLocation = findViewById(R.id.maps_button_find_my_location);
        mButtonSearch = findViewById(R.id.maps_button_search);
        mButtonDeleteMarker = findViewById(R.id.maps_button_img_delete_marker);
        mButtonSaveMyWork = findViewById(R.id.maps_button_img_save_work);
        mSwitchDeleteMarker = findViewById(R.id.maps_switch_delete_marker);
        mLayoutAbout = findViewById(R.id.maps_layout_about);
        mSearchField = findViewById(R.id.edittext_search_field);
        // TODO clean this peace of code
        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            ImageButton buttonClear = findViewById(R.id.search_button_clear_field);
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonClear.setOnClickListener(v -> {
                    buttonClear.setVisibility(View.GONE);
                    mSearchField.setText("");
                    mSearchField.requestFocus();
                });
                if (!s.toString().isEmpty()) buttonClear.setVisibility(View.VISIBLE);
                if (before == 0 && count == 1 && s.charAt(start) == '\n') {
                    geolocate(s.toString());
                    mSearchField.setText(s.subSequence(0, s.length()-1));
                    mSearchField.clearFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        //get the data from the intent
        mLocationPermissionGranted = getIntent().getBooleanExtra(MainActivity.IS_PERMISSIONS_GRANTED,
                false);
        handelInfoBarClicks();
        handelBottomMenuClicks();
        //BottomSheet utility
        mBottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback());
    }

    private void geolocate(String query) {
        Log.d(TAG, "geolocate: geolocationg");
        Geocoder geocoder = new Geocoder(this);
        List<Address> results = new ArrayList();
        try {
            results = geocoder.getFromLocationName(query,1);
        }catch (IOException e){
            Log.d(TAG, "geolocate: IOException: " + e.getMessage());
        }
        if (results.size() > 0) {
            Address result = results.get(0);
            Log.d(TAG, "geolocate: Result:" + result.toString());
            LatLng resultLatlng = new LatLng(result.getLatitude(), result.getLongitude());
            hideSoftKeyboard();
            moveCamera(resultLatlng, DEFAULT_ZOOM);
        }else {
            Toast.makeText(this, R.string.maps_no_results_found, Toast.LENGTH_SHORT).show();
            hideSoftKeyboard();
        }
    }

    private void handelInfoBarClicks() {
        mButtonCloseInfosLayout.setOnClickListener(this);
        mButtonOpenInfosLayout.setOnClickListener(this);
        mButtonToolsMenu.setOnClickListener(this);
    }

    private void handelBottomMenuClicks() {
        mButtonSearch.setOnClickListener(this);
        mButtonMyLocation.setOnClickListener(this);
        mButtonDeleteMarker.setOnClickListener(this);
    }

    /************************** GOOGLE MAPS CALLBACK *******************************/
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Places.initialize(getApplicationContext(), String.valueOf(R.string.google_places_key));
        mMap = googleMap;
        solvePermissions(isPermissionsSet());
        if (isGpsActivated(this)) {
            getDeviceLocation();
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        //TODO clean the code
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMarkerDragListener(this);

        markerClicked = false;
    }

    @Override
    public void onMapClick(LatLng point) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(point));
        markerClicked = false;
    }

    @Override
    public void onMapLongClick(LatLng point) {
        Toast.makeText(this, getString(R.string.maps_new_marker_added) ,
                Toast.LENGTH_LONG).show();
        /*+
                        String.format("Latitude %.4f \n", point.latitude) +
                        String.format("Longitude %.4f ", point.longitude) */

        mMap.addMarker(new MarkerOptions().position(point).title(point.toString()).draggable(true));
        markerClicked = false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (mPolygon != null) {
            mPolygon.remove();
            mPolygon = null;
        }
        if (isDeletingMarkerOn){
            LatLng latLng = marker.getPosition();
            marker.remove();
            if (mPolygon != null) {
                if (mOptions.getPoints().indexOf(latLng) > 0)   mOptions.getPoints().remove(mOptions.getPoints().indexOf(latLng));
                mPolygon = mMap.addPolygon(mOptions);
            }
        }else {
            if (markerClicked) {
                mOptions.add(marker.getPosition());
                mOptions.strokeColor(Color.rgb(255, 214, 0));
                mOptions.fillColor(Color.argb(150, 255, 255, 82));
                mPolygon = mMap.addPolygon(mOptions);
            } else {
                mOptions = new PolygonOptions().add(marker.getPosition());
                markerClicked = true;
            }
        }
        return true;
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
                            Toast.makeText(this, R.string.maps_permissions_denied,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.d(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
            //TODO Create a string resource var for the Toas mnessage
            Toast.makeText(this, getString(R.string.maps_security_exception) + e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG, "moveCamera: to lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        hideSoftKeyboard();
    }

    private boolean isGpsActivated(Context context){
        mLocationManager = (LocationManager)context.getSystemService(context.LOCATION_SERVICE);
        assert mLocationManager != null;
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        //TODO Create a string resource var for the Toas mnessage
        Toast.makeText(this, getString(R.string.maps_drag_message), Toast.LENGTH_LONG).show();
        if (mPolygon != null) {
            mPolygon.remove();
            mOptions = new PolygonOptions();
        }
    }

    @Override
    public void onMarkerDrag(Marker marker) {}

    @Override
    public void onMarkerDragEnd(Marker marker) {}

    /************************* BOTTOM SHEET CALLBACK ***********************************/
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback() {
        return new BottomSheetBehavior.BottomSheetCallback(){
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {}
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mButtonToolsMenu.setRotation(slideOffset * 180);
            }
        };
    }

    /******************************* CLICKS HANDELING ***********************************/
    private LinearLayout mLayoutAboutDelete;
    private RelativeLayout mLayoutSearch;
    @Override
    public void onClick(View v) {
        mLayoutAboutDelete = findViewById(R.id.maps_layout_about_delet_markers);
        mLayoutSearch = findViewById(R.id.maps_layout_searchbar);
        switch (v.getId()){
            case R.id.maps_button_img_close:
                closeTheInfoBar();
                break;
            case R.id.maps_button_img_open:
                openTheInfoBar();
                break;
            case R.id.maps_button_img_tool_menu:
                expandAndHideBottomSheet();
                break;
            case  R.id.maps_button_img_delete_marker:
                deleteFunctionality();
                break;
            case R.id.maps_button_find_my_location:
                getDeviceLocation();
                break;
            case R.id.maps_button_search:
                openTheSearchBar();
                break;
        }
    }

    private void closeTheInfoBar() {
        mLayoutInfosOpened.setVisibility(View.GONE);
        mLayoutInfosClosed.setVisibility(View.VISIBLE);
    }

    private void openTheInfoBar() {
        mLayoutInfosClosed.setVisibility(View.GONE);
        mLayoutInfosOpened.setVisibility(View.VISIBLE);
    }

    private void expandAndHideBottomSheet() {
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED){
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }else {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void deleteFunctionality() {
        mLayoutSearch.setVisibility(View.GONE);
        if (mLayoutInfosOpened.getVisibility() == View.GONE)    openTheInfoBar();
        if (mSwitchDeleteMarker.isChecked()) {
            mLayoutAboutDelete.setVisibility(View.GONE);
            mLayoutAbout.setVisibility(View.VISIBLE);
            mSwitchDeleteMarker.setChecked(false);
        }else{
            mLayoutAboutDelete.setVisibility(View.VISIBLE);
            mLayoutAbout.setVisibility(View.GONE);
            mSwitchDeleteMarker.setChecked(true);
        }
        isDeletingMarkerOn = mSwitchDeleteMarker.isChecked();
    }

    private void openTheSearchBar() {
        mLayoutAboutDelete.setVisibility(View.GONE);
        if (mSwitchDeleteMarker.isChecked()) mSwitchDeleteMarker.setChecked(false);
        if (mLayoutInfosOpened.getVisibility() == View.GONE)    openTheInfoBar();
        if (mLayoutSearch.getVisibility() == View.GONE) {
            mLayoutAbout.setVisibility(View.GONE);
            mLayoutSearch.setVisibility(View.VISIBLE);
        }else{
            mLayoutAbout.setVisibility(View.VISIBLE);
            mLayoutSearch.setVisibility(View.GONE);
        }
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }
}