package com.byteshaft.healthvideo.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.serializers.NurseDetails;

import java.util.ArrayList;


public class DatabaseHelpers extends SQLiteOpenHelper {

    public DatabaseHelpers(Context context) {
        super(context, DatabaseConstants.DATABASE_NAME, null, DatabaseConstants.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseConstants.TABLE_CREATE);
        Log.i(AppGlobals.getLogTag(getClass()), "Database created !!!");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS" + DatabaseConstants.TABLE_NAME);
        onCreate(db);
    }

    public void createNewEntry(NurseDetails nurseDetails) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseConstants.DEVICE_ID, nurseDetails.getDeviceId());
        values.put(DatabaseConstants.FILE_ID, nurseDetails.getFileId());
        values.put(DatabaseConstants.IS_CURRENT_LOCATION, nurseDetails.isCurrentLocation());
        values.put(DatabaseConstants.LOCATION, nurseDetails.getLocation());
        values.put(DatabaseConstants.TIME_STAMP, nurseDetails.getTimeStamp());
        sqLiteDatabase.insert(DatabaseConstants.TABLE_NAME, null, values);
        Log.i(AppGlobals.getLogTag(getClass()), "created New Entry");
        sqLiteDatabase.close();
    }

    public ArrayList<NurseDetails> getAllNursesDetails() {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        String query = "SELECT * FROM " + DatabaseConstants.TABLE_NAME + " ORDER BY " +
                DatabaseConstants.IS_CURRENT_LOCATION + " DESC";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        ArrayList<NurseDetails> arrayList = new ArrayList<>();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(
                    DatabaseConstants.ID_COLUMN));
            String deviceId = cursor.getString(cursor.getColumnIndex(
                    DatabaseConstants.DEVICE_ID));
            boolean isCurrentLocation = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex(
                    DatabaseConstants.IS_CURRENT_LOCATION)));
            int fileId = cursor.getInt(cursor.getColumnIndex(
                    DatabaseConstants.FILE_ID));
            String location = cursor.getString(cursor.getColumnIndex(
                    DatabaseConstants.LOCATION));
            String timeStamp = cursor.getString(cursor.getColumnIndex(
                    DatabaseConstants.TIME_STAMP));
            if (deviceId != null) {
                NurseDetails nurseDetails = new NurseDetails();
                nurseDetails.setId(id);
                nurseDetails.setFileId(fileId);
                nurseDetails.setTimeStamp(timeStamp);
                nurseDetails.setCurrentLocation(isCurrentLocation);
                nurseDetails.setDeviceId(deviceId);
                nurseDetails.setLocation(location);
                arrayList.add(nurseDetails);
            }

        }
        sqLiteDatabase.close();
        return arrayList;
    }

    public void deleteNurse(String value) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        sqLiteDatabase.delete(DatabaseConstants.TABLE_NAME, DatabaseConstants.ID_COLUMN +
                "=?", new String[]{value});
        sqLiteDatabase.close();
    }
}
