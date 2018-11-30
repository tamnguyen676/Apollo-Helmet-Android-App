package com.apollo.apollo;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.ContactsContract;
import android.text.Html;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class EmergencyFragment extends androidx.fragment.app.ListFragment {

    private String TAG = "EmergencyFragment";

    private List<CharSequence> contactsList;
    private FloatingActionButton addButton;
    private DatabaseHelper databaseHelper;
    private ListAdapter adapter;
    static final int PICK_CONTACT = 2;

    public EmergencyFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.data_list, container, false);


        addButton = view.findViewById(R.id.addContactButton);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT);
            }
        });

        return view;
    }

    public void setList(Cursor data) {
        List<CharSequence> list = new ArrayList<>();

        while(data.moveToNext()){
            //get the value from the database in column 1
            //then add it to the ArrayList
            String formatted = "<font color=\"" + "#000000" + "\">" + data.getString(1)
                     + "<br>" + "<font color=\"" + "#F2F2F2" + "\">"
                    + data.getString(2) + "</font>";

            list.add(Html.fromHtml(formatted));
        }

        contactsList = list;
    }

    public void setDatabaseHelper(DatabaseHelper dbHelper) {
        databaseHelper = dbHelper;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ArrayAdapter<CharSequence>(getContext(),
                R.layout.contact_row, contactsList);


        setListAdapter(adapter);
    }

    /**
     * Adds name and phone number into SQL database
     * @param newNameEntry Name of contact
     * @param newNumberEntry Phone number of contact
     */
    public void addData(String newNameEntry, String newNumberEntry) {
        boolean insertData = databaseHelper.addData(newNameEntry, newNumberEntry);

        if (insertData) {
            Log.d(TAG, "Contact inserted!");
//            toastMessage("Data Successfully Inserted!");
        } else {
            Log.w(TAG, "Could not insert data!");
//            toastMessage("Something went wrong");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "In onActivityResult");
        // After user has picked contact from their contact manager, add the result to the database
        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            Cursor cursor = getActivity()
                    .getApplicationContext()
                    .getContentResolver()
                    .query(contactUri, null, null, null, null);

            cursor.moveToFirst();
            int phoneNumIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int nameIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

            Log.d(TAG, "Added "
                    + cursor.getString(nameIndex) + ": "
                    + cursor.getString(phoneNumIndex));

            addData(cursor.getString(nameIndex), cursor.getString(phoneNumIndex));

        }
    }
}
