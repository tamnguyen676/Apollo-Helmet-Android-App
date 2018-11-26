package com.apollo.apollo;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class EditDataActivity extends AppCompatActivity {

    private static final String TAG = "EditDataActivity";

    private Button btnSave,btnDelete;
    private EditText editable_item;   //change editable item to name
    private EditText editable_number;

    DatabaseHelper mDatabaseHelper;

    private String selectedName;
    private String selectedNumber;
    private int selectedID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_data_layout);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnDelete = (Button) findViewById(R.id.btnDelete);
        editable_item = (EditText) findViewById(R.id.editable_item);
        editable_number = (EditText) findViewById(R.id.editable_number);
        mDatabaseHelper = new DatabaseHelper(this);

        //get the intent extra from the ListDataActivity
        Intent receivedIntent = getIntent();

        //now get the itemID we passed as an extra
        selectedID = receivedIntent.getIntExtra("id",-1); //NOTE: -1 is just the default value

        //now get the name we passed as an extra
        selectedName = receivedIntent.getStringExtra("name");

        //now get the phone number we passed as an extra
        selectedNumber = receivedIntent.getStringExtra("number");


        //set the text to show the current selected name
        editable_item.setText(selectedName);
        editable_number.setText(selectedNumber);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name   = editable_item.getText().toString();
                String number = editable_number.getText().toString();

                //exception handling for inserting new data
                if(!name.equals("") && !number.equals("")){
                    mDatabaseHelper.updateName(name,number,selectedID,selectedName,selectedNumber);
                    toastMessage("Contact Saved");
                }else{
                    toastMessage("You must enter a name and a valid phone number");
                }
                finish();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDatabaseHelper.deleteName(selectedID,selectedName,selectedNumber);
                editable_item.setText("");
                editable_number.setText("");
                toastMessage("removed from database");
                finish();
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

