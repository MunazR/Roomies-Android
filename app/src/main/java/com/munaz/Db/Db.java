package com.munaz.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.munaz.model.Group;
import com.munaz.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class for database
 */
public class Db {
    private static Db mInstance = null;
    private static Context mCtx = null;
    private static SQLiteDatabase mDb = null;

    private Db(Context context) {
        mCtx = context;
        mDb = new DbHelper(context).getWritableDatabase();
    }

    public static synchronized Db getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Db(context);
        }
        return mInstance;
    }

    public Group getGroup() {
        Cursor groupCursor
                = mDb.query(DbHelper.GROUP_TABLE_NAME, null, null, null, null, null, null);

        if (groupCursor.getCount() == 0) {
            return null;
        }

        groupCursor.moveToFirst();

        String id = groupCursor.getString(0);
        String ownerId = groupCursor.getString(1);
        String[] memberIds = groupCursor.getString(2).split(",");

        User owner = getUser(ownerId);
        List<User> members = new ArrayList<>();

        for (int i = 0; i < memberIds.length; i++) {
            members.add(getUser(memberIds[i]));
        }

        return new Group(id, owner, members);
    }

    public void insertGroup(Group group) {
        // Clear db of any old groups
        mDb.delete(DbHelper.GROUP_TABLE_NAME, null, null);
        mDb.delete(DbHelper.USER_TABLE_NAME, null, null);

        StringBuilder memberIds = new StringBuilder();
        for (User user : group.members) {
            insertUser(user);
            memberIds.append(user.id + ",");
        }

        ContentValues values = new ContentValues();
        values.put(DbHelper.GROUP_COLUMN_ID, group.id);
        values.put(DbHelper.GROUP_COLUMN_OWNER, group.owner.id);
        values.put(DbHelper.GROUP_COLUMN_MEMBERS, memberIds.toString());

        mDb.insert(DbHelper.GROUP_TABLE_NAME, null, values);
    }

    public void insertUser(User user) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.USER_COLUMN_ID, user.id);
        values.put(DbHelper.USER_COLUMN_FIRST_NAME, user.firstName);
        values.put(DbHelper.USER_COLUMN_LAST_NAME, user.lastName);
        values.put(DbHelper.USER_COLUMN_DISPLAY_NAME, user.displayName);
        values.put(DbHelper.USER_COLUMN_PROFILE_PICTURE_URL, user.profilePictureUrl);

        mDb.insert(DbHelper.USER_TABLE_NAME, null, values);
    }

    public User getUser(String userId) {
        String[] args = { userId };
        Cursor userCursor
                = mDb.query(DbHelper.USER_TABLE_NAME, null, "ID = ?", args, null, null, null);

        if (userCursor.getCount() == 0) {
            return null;
        }

        userCursor.moveToFirst();

        String id = userCursor.getString(0);
        String firstName = userCursor.getString(1);
        String lastName = userCursor.getString(2);
        String displayName = userCursor.getString(3);
        String profilePictureUrl = userCursor.getString(4);

        return new User(id, firstName, lastName, displayName, profilePictureUrl);
    }
}
