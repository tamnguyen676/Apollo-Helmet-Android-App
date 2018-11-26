package com.apollo.apollo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.database.Cursor;


public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final String TABLE_NAME = "people_table";
    private static final String COL1 = "ID";
    private static final String COL2 = "name";
    private static final String COL3 = "number";


    public DatabaseHelper(Context context){
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL2 +" TEXT, "+  COL3 + " TEXT)";
        db.execSQL(createTable);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean addData(String name, String number) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL2, name);
        contentValues.put(COL3, number);

        Log.d(TAG, "addData: Adding " + name + ": " + number +" to " + TABLE_NAME);

        long result = db.insert(TABLE_NAME, null, contentValues);


        //if date as inserted incorrectly it will return -1
        if (result == -1 ) {
            return false;
        } else {
            return true;
        }
    }


    /**
     * Returns all the data from database
     * @return
     */
    public Cursor getData(){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }
    public Cursor getPhoneNumber(){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT number FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    /**
     * Returns only the ID that matches the name passed in
     * @param name
     * @return
     */
    public Cursor getItemID(String name){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL1 + ","+ COL3 + " FROM " + TABLE_NAME +
                " WHERE " + COL2 + " = '" + name + "'";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    /**
     * Updates the name field
     * @param newName
     * @param id
     * @param oldName
     */
    public void updateName(String newName, String newNumber,int id, String oldName, String oldNumber){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_NAME + " SET " + COL2 +
                " = '" + newName + "'," +  COL3 + " = '" + newNumber
                + "' WHERE " + COL1 + " = '" + id + "'" +
                " AND " + COL2 + " = '" + oldName + "'" + " AND " + COL3 + " = '" + oldNumber + "'";
        Log.d(TAG, "updateName: query: " + query);
        Log.d(TAG, "updateName: Setting name to " + newName);
        Log.d(TAG, "updateName: Setting phone number to " + newNumber);
        db.execSQL(query);
    }

    /**
     * Delete from database
     * @param id
     * @param name
     */
    public void deleteName(int id, String name, String number){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME + " WHERE "
                + COL1 + " = '" + id + "'" +
                " AND " + COL2 + " = '" + name   + "'" +
                " AND " + COL3 + " = '" + number + "'";
        Log.d(TAG, "deleteName: query: " + query);
        Log.d(TAG, "deleteName: Deleting " + name + " from database.");
        Log.d(TAG, "deleteName: Deleting " + number + " from database.");
        db.execSQL(query);
    }





}
