package com.example.pondmatev1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database
    private static final String DATABASE_NAME = "UserDB";
    private static final int DATABASE_VERSION = 2;

    // User Table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_FULLNAME = "fullname";
    private static final String COLUMN_ADDRESS = "address";
    private static final String COLUMN_USERTYPE = "usertype";

    // Pond Table
    private static final String TABLE_PONDS = "ponds";
    private static final String COLUMN_POND_NAME = "name";
    private static final String COLUMN_POND_BREED = "breed";
    private static final String COLUMN_FISH_COUNT = "fish_count";
    private static final String COLUMN_COST_PER_FISH = "cost_per_fish";
    private static final String COLUMN_DATE_STARTED = "date_started";
    private static final String COLUMN_DATE_HARVEST = "date_harvest";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Users table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_FULLNAME + " TEXT, " +
                COLUMN_ADDRESS + " TEXT, " +
                COLUMN_USERTYPE + " TEXT)";
        db.execSQL(CREATE_USERS_TABLE);

        // Ponds table
        String CREATE_POND_TABLE = "CREATE TABLE " + TABLE_PONDS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_POND_NAME + " TEXT, " +
                COLUMN_POND_BREED + " TEXT, " +
                COLUMN_FISH_COUNT + " INTEGER, " +
                COLUMN_COST_PER_FISH + " REAL, " +
                COLUMN_DATE_STARTED + " TEXT, " +
                COLUMN_DATE_HARVEST + " TEXT)";
        db.execSQL(CREATE_POND_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PONDS);
        onCreate(db);
    }

    // ------------------- USER OPERATIONS -------------------

    public boolean addUser(String username, String password, String fullname, String address, String userType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_FULLNAME, fullname);
        values.put(COLUMN_ADDRESS, address);
        values.put(COLUMN_USERTYPE, userType);

        try {
            long result = db.insertOrThrow(TABLE_USERS, null, values);
            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }
    }

    public boolean checkUserCredentials(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);

        boolean matchFound = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return matchFound;
    }

    public Cursor getUserInfo(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS, null,
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null);
    }

    public String getUserType(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT usertype FROM " + TABLE_USERS + " WHERE username = ?", new String[]{username});
        if (cursor.moveToFirst()) {
            String userType = cursor.getString(0);
            cursor.close();
            return userType;
        } else {
            cursor.close();
            return null;
        }
    }

    public boolean isUserExistsByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE username = ?", new String[]{username});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    // ------------------- POND OPERATIONS -------------------

    public void insertPond(PondModel pond) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_POND_NAME, pond.getName());
        values.put(COLUMN_POND_BREED, pond.getBreed());
        values.put(COLUMN_FISH_COUNT, pond.getFishCount());
        values.put(COLUMN_COST_PER_FISH, pond.getCostPerFish());
        values.put(COLUMN_DATE_STARTED, pond.getDateStarted());
        values.put(COLUMN_DATE_HARVEST, pond.getDateHarvest());

        db.insert(TABLE_PONDS, null, values);
        db.close();
    }

    public Cursor getAllPonds() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PONDS, null);
    }

    // ------------------- CARETAKER OPERATIONS -------------------
    public ArrayList<CaretakerModel> getCaretakersList() {
        ArrayList<CaretakerModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT id, username, password, fullname, address FROM users WHERE usertype = 'Caretaker'", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String username = cursor.getString(1);
                String password = cursor.getString(2);
                String fullname = cursor.getString(3);
                String address = cursor.getString(4);
                list.add(new CaretakerModel(id, username, password, fullname, address));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }


}

