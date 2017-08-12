package com.byteshaft.healthvideo.utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.interfaces.OnLocationAcquired;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;

/**
 * Created by s9iper1 on 8/8/17.
 */

public class GetLocation implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private String fileId;
    private int locationCounter = 0;
    private OnLocationAcquired onLocationAcquired;
    private boolean forNurse = false;
    private File file;

    public GetLocation(OnLocationAcquired onLocationAcquired) {
        this.onLocationAcquired = onLocationAcquired;
    }

    public void acquireLocation(String fileId, boolean forNurse, File file) {
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        locationCounter = 0;
        this.forNurse = forNurse;
        if (forNurse) {
            this.file = file;
        } else {
            this.fileId = fileId;
            Log.e("TAG", "File id" + fileId);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(AppGlobals.getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("TAG", "onConnectionSuspended");
        if (getLastKnownLocation() != null) {
            if (forNurse) {
                onLocationAcquired.onLocationForNurse(getLastKnownLocation(), file, true);
            } else {
                onLocationAcquired.onLocationForAidWorker(getLastKnownLocation(), fileId, true);
            }
        }


    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("TAG", "onConnectionFailed");

    }

    public void startLocationUpdates() {
        long INTERVAL = 0;
        long FASTEST_INTERVAL = 0;
        if (ActivityCompat.checkSelfPermission(AppGlobals.getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(AppGlobals.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("TAG", "Location changed called" + "Lat " + location.getLatitude() + ", Lng " + location.getLongitude());
        if (locationCounter >= 1) {
            stopLocationUpdate();
            if (forNurse) {
                onLocationAcquired.onLocationForNurse(location, file, true);
            } else {
                onLocationAcquired.onLocationForAidWorker(location, fileId, true);
            }
        }
        locationCounter++;
    }

    private void stopLocationUpdate() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private Location getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(AppGlobals.getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(AppGlobals.getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }
}
