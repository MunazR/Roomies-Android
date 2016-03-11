package com.munaz.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.munaz.model.Chore;
import com.munaz.model.Expense;
import com.munaz.model.Group;
import com.munaz.model.PantryItem;
import com.munaz.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class for database
 */
public class Db {
    private static Db mInstance = null;
    private static SQLiteDatabase mDb = null;

    private Db(Context context) {
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
        User owner = getUser(ownerId);

        String[] memberIds = groupCursor.getString(2).split(",");
        List<User> members = new ArrayList<>();

        for (String memberId : memberIds) {
            members.add(getUser(memberId));
        }

        List<User> invited = new ArrayList<>();
        String inviteIdsConcat = groupCursor.getString(3);
        if (!inviteIdsConcat.equals("")) {
            String[] invitedIds = inviteIdsConcat.split(",");

            for (String invitedId : invitedIds) {
                invited.add(getUser(invitedId));
            }
        }

        List<Chore> chores = new ArrayList<>();
        String choreIdsConcat = groupCursor.getString(4);
        if (!choreIdsConcat.equals("")) {
            String[] choreIds = choreIdsConcat.split(",");

            for (String choreId : choreIds) {
                chores.add(getChore(choreId));
            }
        }

        List<Expense> expenses = new ArrayList<>();
        String expenseIdsConcat = groupCursor.getString(5);
        if (!expenseIdsConcat.equals("")) {
            String[] expenseIds = expenseIdsConcat.split(",");

            for (String expenseId : expenseIds) {
                expenses.add(getExpense(expenseId));
            }
        }

        List<PantryItem> pantryItems = new ArrayList<>();
        String pantryItemIdsConcat = groupCursor.getString(6);
        if (!pantryItemIdsConcat.equals("")) {
            String[] pantryItemIds = pantryItemIdsConcat.split(",");

            for (String pantryItemId : pantryItemIds) {
                pantryItems.add(getPantryItem(pantryItemId));
            }
        }

        groupCursor.close();

        return new Group(id, owner, members, invited, chores, expenses, pantryItems);
    }

    public void insertGroup(Group group) {
        emptyDb();

        StringBuilder memberIds = new StringBuilder();
        for (User user : group.members) {
            insertUser(user);
            memberIds.append(user.id);
            memberIds.append(",");
        }

        StringBuilder invitedIds = new StringBuilder();
        for (User user : group.invited) {
            insertUser(user);
            invitedIds.append(user.id);
            invitedIds.append(",");
        }

        StringBuilder choreIds = new StringBuilder();
        for (Chore chore : group.chores) {
            insertChore(chore);
            choreIds.append(chore.id);
            choreIds.append(",");
        }

        StringBuilder expenseIds = new StringBuilder();
        for (Expense expense : group.expenses) {
            insertExpense(expense);
            expenseIds.append(expense.id);
            expenseIds.append(",");
        }

        StringBuilder pantryItemIds = new StringBuilder();
        for (PantryItem pantryItem : group.pantryItems) {
            insertPantryItem(pantryItem);
            pantryItemIds.append(pantryItem.id);
            pantryItemIds.append(",");
        }

        ContentValues values = new ContentValues();
        values.put(DbHelper.GROUP_COLUMN_ID, group.id);
        values.put(DbHelper.GROUP_COLUMN_OWNER, group.owner.id);
        values.put(DbHelper.GROUP_COLUMN_MEMBERS, memberIds.toString());
        values.put(DbHelper.GROUP_COLUMN_INVITED, invitedIds.toString());
        values.put(DbHelper.GROUP_COLUMN_CHORES, choreIds.toString());
        values.put(DbHelper.GROUP_COLUMN_EXPENSES, expenseIds.toString());
        values.put(DbHelper.GROUP_COLUMN_PANTRY, pantryItemIds.toString());

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
        String[] args = {userId};
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

        userCursor.close();

        return new User(id, firstName, lastName, displayName, profilePictureUrl);
    }

    public void insertChore(Chore chore) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.CHORES_COLUMN_ID, chore.id);
        values.put(DbHelper.CHORES_COLUMN_TITLE, chore.title);
        values.put(DbHelper.CHORES_COLUMN_ASSIGNED_TO, chore.assignedTo);

        mDb.insert(DbHelper.CHORES_TABLE_NAME, null, values);
    }

    public Chore getChore(String choreId) {
        String[] args = {choreId};
        Cursor choreCursor
                = mDb.query(DbHelper.CHORES_TABLE_NAME, null, "ID = ?", args, null, null, null);

        if (choreCursor.getCount() == 0) {
            return null;
        }

        choreCursor.moveToFirst();

        String id = choreCursor.getString(0);
        String title = choreCursor.getString(1);
        String assignedTo = choreCursor.getString(2);

        choreCursor.close();

        return new Chore(id, title, assignedTo);
    }

    public void insertExpense(Expense expense) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.EXPENSES_COLUMN_ID, expense.id);
        values.put(DbHelper.EXPENSES_COLUMN_TITLE, expense.title);
        values.put(DbHelper.EXPENSES_COLUMN_AMOUNT, expense.amount);
        values.put(DbHelper.EXPENSES_COLUMN_EXPENSED_BY, expense.expensedBy);

        mDb.insert(DbHelper.EXPENSES_TABLE_NAME, null, values);
    }

    public Expense getExpense(String expenseId) {
        String[] args = {expenseId};
        Cursor expenseCusor
                = mDb.query(DbHelper.EXPENSES_TABLE_NAME, null, "ID = ?", args, null, null, null);

        if (expenseCusor.getCount() == 0) {
            return null;
        }

        expenseCusor.moveToFirst();

        String id = expenseCusor.getString(0);
        String title = expenseCusor.getString(1);
        int amount = expenseCusor.getInt(2);
        String expensedBy = expenseCusor.getString(3);

        expenseCusor.close();

        return new Expense(id, title, amount, expensedBy);
    }

    public void insertPantryItem(PantryItem pantryItem) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.PANTRY_COLUMN_ID, pantryItem.id);
        values.put(DbHelper.PANTRY_COLUMN_TITLE, pantryItem.title);

        mDb.insert(DbHelper.PANTRY_TABLE_NAME, null, values);
    }

    public PantryItem getPantryItem(String pantryItemId) {
        String[] args = {pantryItemId};
        Cursor pantryItemCursor
                = mDb.query(DbHelper.PANTRY_TABLE_NAME, null, "ID = ?", args, null, null, null);

        if (pantryItemCursor.getCount() == 0) {
            return null;
        }

        pantryItemCursor.moveToFirst();

        String id = pantryItemCursor.getString(0);
        String title = pantryItemCursor.getString(1);

        pantryItemCursor.close();

        return new PantryItem(id, title);
    }

    public void emptyDb() {
        mDb.delete(DbHelper.GROUP_TABLE_NAME, null, null);
        mDb.delete(DbHelper.USER_TABLE_NAME, null, null);
        mDb.delete(DbHelper.CHORES_TABLE_NAME, null, null);
        mDb.delete(DbHelper.EXPENSES_TABLE_NAME, null, null);
        mDb.delete(DbHelper.PANTRY_TABLE_NAME, null, null);
    }
}
