package com.apollo.apollo;

import android.annotation.SuppressLint;
import android.os.Parcel;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.search.DiscoveryResult;
import com.here.android.mpa.search.PlaceLink;

@SuppressLint("ParcelCreator")
public class DestinationSuggestion implements SearchSuggestion {
    private String name, address;
    private GeoCoordinate coordinate;


    public DestinationSuggestion(String name, String address, GeoCoordinate coordinate) {
        this.name = name;
        this.address = address;
        this.coordinate = coordinate;
    }

    @Override
    public String getBody() {
        return name + "\n" + address;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public GeoCoordinate getCoordinate() {
        return coordinate;
    }
}
