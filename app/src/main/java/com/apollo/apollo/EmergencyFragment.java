package com.apollo.apollo;


import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Html;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class EmergencyFragment extends androidx.fragment.app.ListFragment {

    List<CharSequence> contactsList;
    LayoutInflater mInflater;

    public EmergencyFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        mInflater = inflater;

        return inflater.inflate(R.layout.data_list, container, false);
    }

    public void setList(Cursor data) {
        List<CharSequence> list = new ArrayList<>();

        while(data.moveToNext()){
            //get the value from the database in column 1
            //then add it to the ArrayList
            String formatted = "<font color=\"" + "#000000" + "\">" + data.getString(1)
                     + "<font color=\"" + "#F2F2F2" + "\">"
                    + data.getString(2) + "</font>";

            list.add(Html.fromHtml(formatted));
        }

        contactsList = list;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListAdapter adapter = new ArrayAdapter<CharSequence>(getContext(),
                R.layout.contact_row, contactsList);


        setListAdapter(adapter);
    }
}
