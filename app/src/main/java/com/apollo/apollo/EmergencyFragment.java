package com.apollo.apollo;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.ContactsContract;
import android.text.Html;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;

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
    private TextView noContactsText;
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
        noContactsText = view.findViewById(R.id.noContactsText);

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

            list.add(data.getString(1) + "\n" + data.getString(2));
        }

        contactsList = list;
    }

    public void setDatabaseHelper(DatabaseHelper dbHelper) {
        databaseHelper = dbHelper;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (contactsList.isEmpty()) noContactsText.setVisibility(View.VISIBLE);
        else noContactsText.setVisibility(View.GONE);

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

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getContext());
        }
        builder.setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String name = contactsList.get(pos).toString().split("\n")[0];
                        Cursor data = databaseHelper.getItemID(name); //get the id associated with that name

                        int itemID = -1;
                        String itemPhone = "";
                        while(data.moveToNext()){
                            itemID = data.getInt(0);
                            itemPhone = data.getString(1);
                        }

                        databaseHelper.deleteName(itemID);
                        contactsList.remove(name + "\n" + itemPhone);

                        adapter = new ArrayAdapter<>(getContext(),
                                R.layout.contact_row, contactsList);


                        setListAdapter(adapter);

                        Toast.makeText(getActivity(), name + "\n" + itemPhone + " was deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
