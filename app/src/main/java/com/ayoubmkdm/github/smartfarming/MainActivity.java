package com.ayoubmkdm.github.smartfarming;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 666;
    protected static final String IS_PERMISSIONS_GRANTED = "is_permissions_granted";
    //Vars
    private Boolean mLocationPermissionGranted = false;
    //Widgets
    private Button mButtonEnter;
    private ProgressBar mPbWaitForInitializing;

    @Override
    protected void onResume() {
        super.onResume();
        if (isServicesOk()) {
            initLocationPermissions();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initializing widgets
        mButtonEnter = findViewById(R.id.button_enter);
        mPbWaitForInitializing = findViewById(R.id.progressBar_wait_for_initializing);
        //wait for 2 seconds to check for permissions and services ...
        timeTheMainActivity();
        //show the button after 2 seconds from showing the activity
        showEnterButton();
        mButtonEnter.setOnClickListener(view -> {
            showProgressBar();
            if (mLocationPermissionGranted){
                startTheMapActivity();
            }else {
                Toast.makeText(MainActivity.this, "permissions for getting location are denied",
                        Toast.LENGTH_LONG).show();
                //TODO request permissions
                hideProgressBar();
            }
        });
    }

    // Check if the google services version allowed to run google maps
    public boolean isServicesOk(){
        Log.d(TAG, "isServicesOk: checking google services version");
        int available = GoogleApiAvailability.getInstance().
                isGooglePlayServicesAvailable(MainActivity.this);
        if (available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServicesOk: version is ok, connection succeeded");
            return true;
        }else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //In case of a resolvable error
            Log.d(TAG, "isServicesOk: error occurred but we can resolve it");
            resolveGoogleServicesError(available);
        }else {
            Log.d(TAG, "isServicesOk: Services not Ok");
            closeTheApp("Votre appareil ne peut pas effectuer de demandes de carte");
        }
        return false;
    }

    private void resolveGoogleServicesError(int available) {
        Log.d(TAG, "resolveGoogleServicesError: launching google play services dialog");
        DialogInterface.OnCancelListener onCancelingDialog = dialog1 -> {
            Log.d(TAG, "isServicesOk: error resolvable but the user canceled");
            closeTheApp("L'application ne fonctionne pas, il y a " +
                    "eu une erreur sur les services google play: vérifiez s'ils sont désactivés ou démodé");
        };
        Dialog dialog = GoogleApiAvailability.getInstance().
                getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST, onCancelingDialog);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void showEnterButton() {
        mButtonEnter.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideProgressBar();
            }
        }, 2000);
    }

    private void timeTheMainActivity() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mLocationPermissionGranted){
                    startTheMapActivity();
                }
            }
        },2000);
    }

    //Verify permissions
    public void initLocationPermissions(){
        Log.d(TAG, "initLocationPermissions: initializing permissions");
        String[] permissions = {FINE_LOCATION, COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this, FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED){
            mLocationPermissionGranted = true;
//            initGoogleMaps();
//            startTheMapActivity();
        } else {
            ActivityCompat.requestPermissions(this, permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    //This method is called in case the user didn't accept the location permissions
    private void requestDenied() {
        Log.d(TAG, "requestDenied: the request is denied .. closing the app");
        closeTheApp("Vous ne pouvez pas continuer lorsque l'application n'a pas " +
                "l'autorisation d'utiliser l'emplacement de l'appareil");
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: getting the requests results");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE :
                if (grantResults.length > 0){
                    for (int result : grantResults){
                        if (result != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            requestDenied();
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                    //initialize the map
//                    initGoogleMaps();
//                    startTheMapActivity();
                }
        }
    }

    private void startTheMapActivity(){
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(IS_PERMISSIONS_GRANTED,mLocationPermissionGranted);
        startActivity(intent);
    }

    //this method is for canceling the app in case the user don't have requirement:
    // play services version or denied permissions
    private void closeTheApp(String closingMessage) {
        Toast.makeText(this, closingMessage ,Toast.LENGTH_LONG).show();
        this.finishAffinity();
    }

    private void showProgressBar(){
        mButtonEnter.setVisibility(View.GONE);
        mPbWaitForInitializing.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar(){
        mButtonEnter.setVisibility(View.VISIBLE);
        mPbWaitForInitializing.setVisibility(View.GONE);
    }
}