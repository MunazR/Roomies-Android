package com.munaz.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Used to access database
 */
public class DbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ROOMIES";
    private static final int DATABASE_VERSION = 1;

    public static final String GROUP_TABLE_NAME = "Groups";
    public static final String USER_TABLE_NAME = "Users";
    public static final String CHORES_TABLE_NAME = "Chores";

    public static final String USER_COLUMN_ID = "Id";
    public static final String USER_COLUMN_FIRST_NAME = "First_Name";
    public static final String USER_COLUMN_LAST_NAME = "Last_Name";
    public static final String USER_COLUMN_DISPLAY_NAME = "Display_Name";
    public static final String USER_COLUMN_PROFILE_PICTURE_URL = "Profile_Picture_Url";

    public static final String GROUP_COLUMN_ID = "Id";
    public static final String GROUP_COLUMN_OWNER = "Owner";
    public static final String GROUP_COLUMN_MEMBERS = "Members";
    public static final String GROUP_COLUMN_INVITED = "Invited";
    public static final String GROUP_COLUMN_CHORES = "Chores";

    public static final String CHORES_COLUMN_ID = "Id";
    public static final String CHORES_COLUMN_TITLE = "Title";
    public static final String CHORES_COLUMN_ASSIGNED_TO = "AssignedTo";

    private static final String USER_TABLE_CREATE =
            "CREATE TABLE " + USER_TABLE_NAME + " (" +
                    USER_COLUMN_ID + " TEXT PRIMARY KEY, " +
                    USER_COLUMN_FIRST_NAME + " TEXT, " +
                    USER_COLUMN_LAST_NAME + " TEXT, " +
                    USER_COLUMN_DISPLAY_NAME + " TEXT, " +
                    USER_COLUMN_PROFILE_PICTURE_URL + " TEXT)";

    private static final String GROUP_TABLE_CREATE =
            "CREATE TABLE " + GROUP_TABLE_NAME + " ( " +
                    GROUP_COLUMN_ID + " TEXT PRIMARY KEY, " +
                    GROUP_COLUMN_OWNER + " TEXT, " +
                    GROUP_COLUMN_MEMBERS + " TEXT, " +
                    GROUP_COLUMN_INVITED + " TEXT, " +
                    GROUP_COLUMN_CHORES + " TEXT)";

    private static final String CHORES_TABLE_CREATE =
            "CREATE TABLE " + CHORES_TABLE_NAME + " ( " +
                    CHORES_COLUMN_ID + " TEXT PRIMARY KEY, " +
                    CHORES_COLUMN_TITLE + " TEXT, " +
                    CHORES_COLUMN_ASSIGNED_TO + " TEXT)";

    DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(USER_TABLE_CREATE);
        db.execSQL(GROUP_TABLE_CREATE);
        db.execSQL(CHORES_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query = "DROP TABLE IF EXISTS ";
        db.execSQL(query + GROUP_TABLE_NAME);
        db.execSQL(query + USER_TABLE_NAME);
        db.execSQL(query + CHORES_TABLE_NAME);
        onCreate(db);
    }
}
