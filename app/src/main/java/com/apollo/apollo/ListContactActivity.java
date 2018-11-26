package com.apollo.apollo;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


//activity for showing the contact list
public class ListContactActivity extends AppCompatActivity {

    private static final String TAG = "ListContactActivity";

    DatabaseHelper mDatabaseHelper;

    private ListView mListView;


    /*Create the Contact List activity*/
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_list);
        mListView = (ListView) findViewById(R.id.listView);
        mDatabaseHelper = new DatabaseHelper(this);
        populateListView();


    }

    /*Refresh the screen if activity is restarted*/
    @Override
    protected void onRestart() {
        super.onRestart();
        setContentView(R.layout.data_list);
        mListView = (ListView) findViewById(R.id.listView);
        mDatabaseHelper = new DatabaseHelper(this);
        populateListView();
    }

    /*Add contact and Back button handler, it just go back to the 1st screen*/
    public void backBtnOnClick(View v){
        finish();
    }


    /*populate the database into ListView*/
    private void populateListView() {
        Log.d(TAG, "populateListView: Displaying data in the ListView.");

        //get the data and append to a list
        Cursor data = mDatabaseHelper.getData();
        ArrayList<String> listData = new ArrayList<>();
        while(data.moveToNext()){
            //get the value from the database in column 1
            //then add it to the ArrayList
            listData.add(data.getString(1) + ": "+ data.getString(2));
        }

        if (listData.isEmpty()){
            toastMessage("Contact list is empty!!!");
            finish();
        }

        //create the list adapter and set the adapter
        ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listData);
        mListView.setAdapter(adapter);

        //set an onItemClickListener to the ListView, any click onto the row will pop up edit data activity

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i,long l) {
                String name = adapterView.getItemAtPosition(i).toString();
                name = name.substring(0, name.indexOf(':')).trim();
                toastMessage("Name is " + name);
                Log.d(TAG, "onItemClick: You Clicked on " + name);

                Cursor data = mDatabaseHelper.getItemID(name); //get the id associated with that name

                int itemID = -1;
                String itemPhone = "";
                while(data.moveToNext()){
                    itemID = data.getInt(0);
                    itemPhone = data.getString(1);
                }


                // small exception handling
                if(itemID > -1){
                    Log.d(TAG, "onItemClick: The ID is: " + itemID);
                    Intent editScreenIntent = new Intent(ListContactActivity.this, EditDataActivity.class);
                    editScreenIntent.putExtra("id",itemID);
                    editScreenIntent.putExtra("name",name);
                    editScreenIntent.putExtra("number",itemPhone);
                    startActivity(editScreenIntent);
                }
                else{
                    toastMessage("No ID associated with that name");
                }
            }
        });
    }

    /**
     * customizable toast
     * @param message
     */
    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }

}
