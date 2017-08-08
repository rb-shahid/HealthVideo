package com.byteshaft.healthvideo.database;


public class DatabaseConstants {

    public static final String DATABASE_NAME = "health_video.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "nurse_records";
    public static final String DEVICE_ID = "device_id";
    public static final String FILE_ID = "file_id";
    public static final String LOCATION = "location";
    public static final String TIME_STAMP = "time_stamp";
    public static final String ID_COLUMN = "ID";
    public static final String IS_CURRENT_LOCATION = "is_current_location";

    public static final String TABLE_CREATE =
            "CREATE TABLE " +
                    TABLE_NAME + "(" +
                    ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DEVICE_ID + " TEXT , " +
                    LOCATION + " TEXT , " +
                    TIME_STAMP + " TEXT , " +
                    FILE_ID + " TEXT, " +
                    IS_CURRENT_LOCATION + " TEXT" + " ) ";
}
