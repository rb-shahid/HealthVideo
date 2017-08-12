package com.byteshaft.healthvideo.interfaces;

import android.location.Location;

import java.io.File;

/**
 * Created by s9iper1 on 8/8/17.
 */

public interface OnLocationAcquired {
    void onLocationForNurse(Location location, File file, boolean currentLocation);
    void onLocationForAidWorker(Location location, String fileId, boolean currentLocation);
}
