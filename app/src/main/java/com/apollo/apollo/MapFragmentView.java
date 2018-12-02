/*
 * Copyright (c) 2011-2018 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apollo.apollo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.Router;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.mpa.urbanmobility.Alert;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

/**
 * This class encapsulates the properties and functionality of the Map view.It also triggers a
 * turn-by-turn navigation from HERE Burnaby office to Langley BC.There is a sample voice skin
 * bundled within the SDK package to be used out-of-box, please refer to the Developer's guide for
 * the usage.
 */
public class MapFragmentView implements DistanceCalculator {
    private MapFragment m_mapFragment;
    private Activity m_activity;
    private Map m_map;
    private NavigationManager m_navigationManager;
    private GeoBoundingBox m_geoBoundingBox;
    private Route m_route;
    private boolean m_foregroundServiceStarted;
    private PositioningManager m_positioningManager;
    private GeoCoordinate m_geoCoordinate;
    private Maneuver maneuver;
    private double lastDistance;
    private FloatingSearchView floatingSearchView;
    private ConnectedThreadHolder connectedThreadHolder;

    private static String TAG = "MapFragmentView";

    public MapFragmentView(Activity activity,
                           FloatingSearchView floatingSearchView,
                           ConnectedThreadHolder connectedThread) {
        m_activity = activity;
        this.floatingSearchView = floatingSearchView;
        this.connectedThreadHolder = connectedThread;

        initMapFragment();
    }

    // Google has deprecated android.app.Fragment class. It is used in current SDK implementation.
    // Will be fixed in future SDK version.
    @SuppressWarnings("deprecation")
    private MapFragment getMapFragment() {
        return (MapFragment) m_activity.getFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
        /* Locate the mapFragment UI element */
        m_mapFragment = getMapFragment();

        // Set path of isolated disk cache
        String diskCacheRoot = Environment.getExternalStorageDirectory().getPath()
                + File.separator + ".isolated-here-maps";
        // Retrieve intent name from manifest
        String intentName = "";
        try {
            ApplicationInfo ai = m_activity.getPackageManager().getApplicationInfo(m_activity.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            intentName = bundle.getString("INTENT_NAME");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to find intent name, NameNotFound: " + e.getMessage());
        }

        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(diskCacheRoot, intentName);
        if (!success) {
            // Setting the isolated disk cache was not successful, please check if the path is valid and
            // ensure that it does not match the default location
            // (getExternalStorageDirectory()/.here-maps).
            // Also, ensure the provided intent name does not match the default intent name.
        } else {
            if (m_mapFragment != null) {
            /* Initialize the MapFragment, results will be given via the called back. */
                m_mapFragment.init(new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {

                        if (error == Error.NONE) {
                            m_map = m_mapFragment.getMap();

                            m_positioningManager = PositioningManager.getInstance();
                            m_positioningManager.start(PositioningManager.LocationMethod.GPS);
                            updatePos();

                            m_map.setCenter(new GeoCoordinate(m_geoCoordinate.getLatitude(), m_geoCoordinate.getLongitude()),
                                    Map.Animation.NONE);
                            //Put this call in Map.onTransformListener if the animation(Linear/Bow)
                            //is used in setCenter()
                            m_map.setZoomLevel(13.2);
                        /*
                         * Get the NavigationManager instance.It is responsible for providing voice
                         * and visual instructions while driving and walking
                         */
                            m_navigationManager = NavigationManager.getInstance();
                            m_navigationManager.setDistanceUnit(NavigationManager.UnitSystem.IMPERIAL_US);
                        } else {
                            Toast.makeText(m_activity,
                                    "ERROR: Cannot initialize Map with error " + error,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }
    }

    public void updatePos() {
        m_geoCoordinate = m_positioningManager.getLastKnownPosition().getCoordinate();
        Log.d(TAG, m_geoCoordinate.getLatitude() + " " + m_geoCoordinate.getLongitude());
    }

    private void createRoute(GeoCoordinate coordinate) {
        /* Initialize a CoreRouter */

        /* Initialize a RoutePlan */
        final RoutePlan routePlan = new RoutePlan();

        /*
         * Initialize a RouteOption.HERE SDK allow users to define their own parameters for the
         * route calculation,including transport modes,route types and route restrictions etc.Please
         * refer to API doc for full list of APIs
         */
        RouteOptions routeOptions = new RouteOptions();
        /* Other transport modes are also available e.g Pedestrian */
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        /* Disable highway in this route. */
        routeOptions.setHighwaysAllowed(true);
        /* Calculate the shortest route available. */
        routeOptions.setRouteType(RouteOptions.Type.SHORTEST);
        /* Calculate 1 route. */
        routeOptions.setRouteCount(1);
        /* Finally set the route option */
        routePlan.setRouteOptions(routeOptions);

        routePlan.addWaypoint(new RouteWaypoint(m_geoCoordinate));
        routePlan.addWaypoint(new RouteWaypoint(coordinate));

        calculateRoute(routePlan);
    }

    private void calculateRoute(RoutePlan routePlan) {
        CoreRouter coreRouter = new CoreRouter();

        Log.d(TAG, "Right before calculate route");
        /* Trigger the route calculation,results will be called back via the listener */
        coreRouter.calculateRoute(routePlan,
                new Router.Listener<List<RouteResult>, RoutingError>() {

                    @Override
                    public void onProgress(int i) {
                        Log.d(TAG, i + "");
                    }

                    @Override
                    public void onCalculateRouteFinished(List<RouteResult> routeResults,
                                                         RoutingError routingError) {
                        Log.d(TAG, "Begin onCalculateFinished");
                        /* Calculation is done.Let's handle the result */
                        if (routingError == RoutingError.NONE) {
                            if (routeResults.get(0).getRoute() != null) {

                                m_route = routeResults.get(0).getRoute();
                                /* Create a MapRoute so that it can be placed on the map */
                                MapRoute mapRoute = new MapRoute(routeResults.get(0).getRoute());

                                /* Show the maneuver number on top of the route */
                                mapRoute.setManeuverNumberVisible(true);

                                /* Add the MapRoute to the map */
                                m_map.addMapObject(mapRoute);

                                /*
                                 * We may also want to make sure the map view is orientated properly
                                 * so the entire route can be easily seen.
                                 */
                                m_geoBoundingBox = routeResults.get(0).getRoute().getBoundingBox();
                                m_map.zoomTo(m_geoBoundingBox, Map.Animation.NONE,
                                        Map.MOVE_PRESERVE_ORIENTATION);


                                Log.d("MapFragment", "Calculated route. Starting navigation");
                                startNavigation();
                            } else {
                                Toast.makeText(m_activity,
                                        "Error:route results returned is not valid",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(m_activity,
                                    "Error:route calculation returned error code: " + routingError,
                                    Toast.LENGTH_LONG).show();

                        }
                    }
                });
    }

    /*
     * Android 8.0 (API level 26) limits how frequently background apps can retrieve the user's
     * current location. Apps can receive location updates only a few times each hour.
     * See href="https://developer.android.com/about/versions/oreo/background-location-limits.html
     * In order to retrieve location updates more frequently start a foreground service.
     * See https://developer.android.com/guide/components/services.html#Foreground
     */
    private void startForegroundService() {
        floatingSearchView.setVisibility(View.GONE);
        if (!m_foregroundServiceStarted) {
            m_foregroundServiceStarted = true;
            Intent startIntent = new Intent(m_activity, ForegroundService.class);
            startIntent.setAction(ForegroundService.START_ACTION);
            m_activity.getApplicationContext().startService(startIntent);
            Log.d("MapFragment", "Started Foreground Service");
        }
    }

    private void stopForegroundService() {
        if (m_foregroundServiceStarted) {
            m_foregroundServiceStarted = false;
            Intent stopIntent = new Intent(m_activity, ForegroundService.class);
            stopIntent.setAction(ForegroundService.STOP_ACTION);
            m_activity.getApplicationContext().startService(stopIntent);
        }
    }

    private void startNavigation() {
//        m_naviControlButton.setText(R.string.stop_navi);
        /* Configure Navigation manager to launch navigation on current map */
        m_navigationManager.setMap(m_map);

        /*
         * Start the turn-by-turn navigation.Please note if the transport mode of the passed-in
         * route is pedestrian, the NavigationManager automatically triggers the guidance which is
         * suitable for walking. Simulation and tracking modes can also be launched at this moment
         * by calling either simulate() or startTracking()
         */

        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder;
        /* Choose navigation modes between real time navigation and simulation */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(m_activity, AlertDialog.THEME_TRADITIONAL);
        } else {
            alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(m_activity);
        }

        alertDialogBuilder.setTitle("Navigation");
        alertDialogBuilder.setMessage("Choose Mode");
        alertDialogBuilder.setNegativeButton("Navigation",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                m_navigationManager.startNavigation(m_route);
                m_map.setTilt(60);
                startForegroundService();
            };
        });
        alertDialogBuilder.setPositiveButton("Simulation",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                m_navigationManager.simulate(m_route,60);//Simualtion speed is set to 60 m/s
                m_map.setTilt(60);
                Log.d("MapFragment", "Simulating");
                startForegroundService();
            };
        });


        androidx.appcompat.app.AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();


        /*
         * Set the map update mode to ROADVIEW.This will enable the automatic map movement based on
         * the current location.If user gestures are expected during the navigation, it's
         * recommended to set the map update mode to NONE first. Other supported update mode can be
         * found in HERE Android SDK API doc
         */
        m_navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);

        /*
         * NavigationManager contains a number of listeners which we can use to monitor the
         * navigation status and getting relevant instructions.In this example, we will add 2
         * listeners for demo purpose,please refer to HERE Android SDK API documentation for details
         */
        addNavigationListeners();
    }

    private void addNavigationListeners() {

        /*
         * Register a NavigationManagerEventListener to monitor the status change on
         * NavigationManager
         */
        m_navigationManager.addNavigationManagerEventListener(
                new WeakReference<NavigationManager.NavigationManagerEventListener>(
                        m_navigationManagerEventListener));

        /* Register a PositionListener to monitor the position updates */
        m_navigationManager.addPositionListener(
                new WeakReference<NavigationManager.PositionListener>(m_positionListener));

        m_navigationManager.addNewInstructionEventListener(
                new WeakReference<NavigationManager.NewInstructionEventListener>(m_newInstructionEventListener));

    }

    private NavigationManager.NewInstructionEventListener m_newInstructionEventListener = new NavigationManager.NewInstructionEventListener() {
        @Override
        public void onNewInstructionEvent() {
            maneuver = m_navigationManager.getNextManeuver();
            if (maneuver != null) {
                JSONObject json = new JSONObject();

                lastDistance = toMiles(maneuver.getDistanceFromPreviousManeuver());

                double distance = toMiles(maneuver.getDistanceFromPreviousManeuver());


                try {
                    json.put("turn", maneuver.getTurn().toString());

                    if (distance > .19) { // Send in miles if more than 1000 ft
                        json.put("distance", String.format(Locale.US, "%.1f mi", distance));
                    }
                    else {
                        // Convert miles to feet
                        distance = distance * 5280;

                        // Round down numbers
                        if (distance >= 1000) distance = 1000;
                        else if (distance >= 100) {
                            // Takes the first digit of the number and multiplies it by 100
                            // Ex) 945 becomes 900
                            distance = 100 * (("" + distance).charAt(0) - '0');
                        }
                        else {
                            distance = 10 * (("" + distance).charAt(0) - '0');
                        }

                        json.put("distance", String.format(Locale.US, "%.0f ft", distance));
                    }

                    json.put("road", maneuver.getNextRoadName());
                    json.put("end", false);
                    json.put("newManeuver", true);

//                    Log.d("onNewInstructionEvent", maneuver.getAction().toString());

                    if (connectedThreadHolder.getConnectedThread() != null) {
                        Log.d(TAG, "WRITING");
                        Log.d("onNewInstructionEvent", json.toString());
                        connectedThreadHolder.getConnectedThread().write(json.toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    };

    private NavigationManager.PositionListener m_positionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(GeoPosition geoPosition) {
            if (maneuver != null) {

                double distance = toMiles(geoPosition.getCoordinate()
                                .distanceTo(maneuver.getCoordinate()));

                // .19 miles is roughly 1000 ft
                if (distance < .19) {

                    // Don't send if there hasn't been a change of about 100 ft (0.0189394 mi)
                    if (lastDistance - distance < 0.0189394) {
                        return;
                    }

                    lastDistance = distance;

                    // Convert miles to feet
                    distance = distance * 5280;

                    // Rounds numbers
                    if (distance >= 1000) distance = 1000;
                    else if (distance >= 100) {
                        // Takes the first digit of the number and multiplies it by 100
                        // Ex) 945 becomes 900
                        distance = 100 * (("" + distance).charAt(0) - '0');
                    }
                    else {
                        // Don't bother sending anything smaller than 100 ft
                        return;
                    }


                    JSONObject json = new JSONObject();

                    try {
                        json.put("distance", String.format(Locale.US, "%.0f ft", distance));
                        json.put("newManeuver", false);
                        json.put("end", false);


                        if (connectedThreadHolder.getConnectedThread() != null) {
                            Log.d("onPositionUpdate", json.toString());
                            connectedThreadHolder.getConnectedThread().write(json.toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else if (lastDistance - distance > .1) {
                    lastDistance = distance;

                    JSONObject json = new JSONObject();

                    try {
                        json.put("distance", String.format(Locale.US, "%.1f mi", distance));
                        json.put("newManeuver", false);
                        json.put("end", false);

                        Log.d("onPositionUpdate", json.toString());
                        if (connectedThreadHolder.getConnectedThread() != null) {
                            connectedThreadHolder.getConnectedThread().write(json.toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    public Map getMap() {
        return m_map;
    }

    private NavigationManager.NavigationManagerEventListener m_navigationManagerEventListener = new NavigationManager.NavigationManagerEventListener() {
        @Override
        public void onRunningStateChanged() {
            Toast.makeText(m_activity, "Running state changed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNavigationModeChanged() {
            Toast.makeText(m_activity, "Navigation mode changed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEnded(NavigationManager.NavigationMode navigationMode) {
            Toast.makeText(m_activity, navigationMode + " was ended", Toast.LENGTH_SHORT).show();
            stopForegroundService();

            floatingSearchView.setVisibility(View.VISIBLE);

            JSONObject json = new JSONObject();

            try {
                json.put("end", true);

                if (connectedThreadHolder.getConnectedThread() != null) {
                    connectedThreadHolder.getConnectedThread().write(json.toString());
                }
                Log.d("onEnded", json.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        @Override
        public void onMapUpdateModeChanged(NavigationManager.MapUpdateMode mapUpdateMode) {
            Toast.makeText(m_activity, "Map update mode is changed to " + mapUpdateMode,
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRouteUpdated(Route route) {
            Toast.makeText(m_activity, "Route updated", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCountryInfo(String s, String s1) {
            Toast.makeText(m_activity, "Country info updated from " + s + " to " + s1,
                    Toast.LENGTH_SHORT).show();
        }

    };

    public GeoCoordinate getCoordinate() {
        return m_geoCoordinate;
    }

    /**
     * Method called by other classes to start navigation given GeoCoordinate
     * @param coordinate
     */
    public void startNavigation(GeoCoordinate coordinate) {
        createRoute(coordinate);
    }

    public GeoCoordinate getCurrentPos() {
        updatePos();
        return m_geoCoordinate;
    }

    public void onDestroy() {
        Log.d(TAG, "Destroying MapFragmentView");

        /* Stop the navigation when app is destroyed */
        if (m_navigationManager != null) {
            stopForegroundService();
            m_navigationManager.stop();
        }

        if (m_positioningManager != null) {
            m_positioningManager.stop();
        }
    }
}
