package com.apollo.apollo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.SearchEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.search.DiscoveryResult;
import com.here.android.mpa.search.DiscoveryResultPage;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.PlaceLink;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.SearchRequest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener, DistanceCalculator {

    protected Handler mHandler; // handler that gets info from Bluetooth service

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 0;
    private final static int REQUEST_ENABLE_BT = 1;
    static final int PICK_CONTACT = 2;

    protected final static String TAG = "MainActivity";


    private ConnectedThreadHolder connectedThreadHolder = new ConnectedThreadHolder();
    private ConnectThread connectThread;

    BluetoothAdapter mBtAdapter;
    BluetoothDevice mBtDevice;

    TextView status;
    ProgressBar progressBar;
    EditText dest;
    String origin;

    DatabaseHelper mDatabaseHelper;

    Button btnViewData; // create a button variable to go to a second activity
    Button btnAdd;
    EditText contact_name,contact_number;

    private MapFragmentView m_mapFragmentView;
    private BottomNavigationView bottomNavigationView;
    private FloatingSearchView floatingSearchView;
    private LinearLayout helmetSearchAlert;
    private View container;
    private View mapContainer;

    private List<DiscoveryResult> discoveryResultList;
    private ResultListener<DiscoveryResultPage> discoveryResultPageListener;

    private boolean loadingSuggestions = false;

    private EmergencyFragment emergencyFragment;
    private HelmetFragment helmetFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container);
        mapContainer = findViewById(R.id.mapcontainer);
        floatingSearchView = findViewById(R.id.floatingSearchView);
        helmetSearchAlert = findViewById(R.id.helmetSearch);

        mDatabaseHelper = new DatabaseHelper(this);
//        mDatabaseHelper.delete();

        emergencyFragment = new EmergencyFragment();
        emergencyFragment.setDatabaseHelper(mDatabaseHelper);

        helmetFragment = new HelmetFragment();


        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // If phone has no Bluetooth capabilities, notify user and shut down app
        if (mBtAdapter == null) {
            handleBluetoothNotFound();
        }

        // If bluetooth is off, ask user to turn it on
        if (!mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        bottomNavigationView.setSelectedItemId(R.id.navigation_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        initSearchBar();

        requestPermissions();

        scanDevices();
    }

    // Creates a BroadcastReceiver that handles when a BluetoothDevice is found
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String deviceName = device.getName();

                if (deviceName != null) {
                    // The Bluetooth name of the device we are looking for (Should be Apollo)
                    String targetDevice = context.getResources().getString(R.string.bt_device);

                    // If system scans a device with the name we are looking for, save the device
                    // as a BluetoothDevice object and connect socket between phone and that device
                    if (deviceName.equals(targetDevice)) {
                        mBtDevice = device;
                        String found = "Found " + targetDevice;
                        Log.d(TAG, found);
//                        status.setText(found);
                        connectSocket(mBtDevice);
                        toastMessage("Connected to Apollo helmet");
                        helmetSearchAlert.setVisibility(View.GONE);
                    }

                    Log.d(TAG, "Added " + deviceName);
                }
                // String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "In onActivityResult");
        // After user has picked contact from their contact manager, add the result to the database
        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
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
        else {
            super.onActivityResult(requestCode, resultCode, data);
            emergencyFragment.setList(mDatabaseHelper.getData());
            getSupportFragmentManager()
                    .beginTransaction()
                    .detach(emergencyFragment)
                    .attach(emergencyFragment)
                    .commit();
        }
    }


    /**
     * Adds name and phone number into SQL database
     * @param newNameEntry Name of contact
     * @param newNumberEntry Phone number of contact
     */
    public void addData(String newNameEntry, String newNumberEntry) {
        boolean insertData = mDatabaseHelper.addData(newNameEntry, newNumberEntry);
        if (insertData) {
            toastMessage("Data Successfully Inserted!");
        } else {
            toastMessage("Something went wrong");
        }
    }

    /**
     * Convenience method to print a toast
     * @param message
     */
    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }


    protected void onResume() {
        super.onResume();

        Log.d(TAG, "Resuming...");
    }

    /**
     * Performs a bluetooth scan and the logic
     * is handled by the BroadcastReceiver mReceiver defined above.
     */
    public void scanDevices() {
        helmetSearchAlert.setVisibility(View.VISIBLE);

        Log.d(TAG, "Scanning...");

        // If already discovering, cancel it so that a new scan can be performed.
        if (mBtAdapter.isDiscovering()) {
            Log.d(TAG, "Already discovering. Cancelling...");
            mBtAdapter.cancelDiscovery();
        }

        // startDiscovery returns true if the scan starts successfully
        if (mBtAdapter.startDiscovery()) {
//            status.setText("Scanning");
        }
    }

    /**
     * Connects the phone to the server socket on the Pi. Creates a new thread to do so.
     * @param mBtDevice The BluetoothDevice object that represents the server being connected to
     */
    public void connectSocket(BluetoothDevice mBtDevice) {
        Log.d("Socket", "In the connectSocket method");
        mBtAdapter.cancelDiscovery();

        connectThread = new ConnectThread(mBtDevice, mDatabaseHelper, connectedThreadHolder);
        Thread thread = new Thread(connectThread);
        thread.start();
    }


    /**
     * Checks to see if the user has given access to location permission. Needed for GPS and
     * Bluetooth scan. If the user has not given access, request permission.
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

        }
    }
    
    private void checkSMSPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;  
            ActivityCompat.requestPermissions(this,
                  new String[]{Manifest.permission.SEND_SMS},
                  MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }

    private void checkReadPhoneStatePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
        }
    }

    /**
     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
     * needs when the app is running.
     */
    private void requestPermissions() {

        final List<String> requiredSDKPermissions = new ArrayList<String>();
        requiredSDKPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        requiredSDKPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requiredSDKPermissions.add(Manifest.permission.INTERNET);
        requiredSDKPermissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        requiredSDKPermissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        requiredSDKPermissions.add(Manifest.permission.READ_PHONE_STATE);
        requiredSDKPermissions.add(Manifest.permission.SEND_SMS);

        ActivityCompat.requestPermissions(this,
                requiredSDKPermissions.toArray(new String[requiredSDKPermissions.size()]),
                REQUEST_CODE_ASK_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d("Test", "onRequestPermissionsResult");
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /**
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                permissions[index])) {
                            Toast.makeText(this,
                                    "Required permission " + permissions[index] + " not granted. "
                                            + "Please go to settings and turn on for sample app",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    "Required permission " + permissions[index] + " not granted",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }

                Log.d(TAG, "Getting MapFragmentView");
                /**
                 * All permission requests are being handled.Create map fragment view.Please note
                 * the HERE SDK requires all permissions defined above to operate properly.
                 */
                m_mapFragmentView = new MapFragmentView(this,
                        floatingSearchView,
                        connectedThreadHolder);
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void handleBluetoothNotFound() {
       DialogFragment bluetoothNotFoundFragment = new BluetoothNotFoundFragment();
       bluetoothNotFoundFragment.show(getSupportFragmentManager(), "not_found");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");

        // Don't forget to unregister the ACTION_FOUND receiver.
        if (m_mapFragmentView != null) {
            m_mapFragmentView.onDestroy();
        }
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.navigation_navigation:
                container.setVisibility(View.GONE);
                mapContainer.setVisibility(View.VISIBLE);
                return true;
            case R.id.navigation_contacts:
                container.setVisibility(View.VISIBLE);
                mapContainer.setVisibility(View.GONE);
                emergencyFragment.setList(mDatabaseHelper.getData());
                getSupportFragmentManager().beginTransaction().replace(R.id.container, emergencyFragment).commit();
                return true;
            case R.id.navigation_helmet:
                container.setVisibility(View.VISIBLE);
                mapContainer.setVisibility(View.GONE);
                getSupportFragmentManager().beginTransaction().replace(R.id.container, helmetFragment).commit();
                return true;
        }

        return false;
    }

    protected interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
    }

    /**
     * Initializes the search bar to be able to provide search suggestions
     */
    private void initSearchBar() {

        // Implement callback method for when results are found
        discoveryResultPageListener = new ResultListener<DiscoveryResultPage>() {
            @Override
            public void onCompleted(DiscoveryResultPage discoveryResultPage, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NONE) {
                    discoveryResultList = discoveryResultPage.getItems();

                    List<DestinationSuggestion> suggestList = new ArrayList<>();

                    // For each DiscoveryResult, make a corresponding DestinationSuggestion object
                    for (DiscoveryResult result: discoveryResultList) {
                        if (result.getResultType() == DiscoveryResult.ResultType.PLACE) {
                            suggestList.add(new DestinationSuggestion(
                                    result.getTitle(),
                                    result.getVicinity().replace("<br/>", " "),
                                    ((PlaceLink) result).getPosition()
                            ));
                        }
                    }

                    // This is what causes the list to be shown. onBindSuggestion is called right after
                    floatingSearchView.swapSuggestions(suggestList);
                    floatingSearchView.hideProgress(); // Hide loading animation
                    loadingSuggestions = false;
                }
                else {
                    Log.e(TAG, errorCode.toString());
                }
            }
        };

        // Implements callback method to bind the Views that show the suggestions to user
        floatingSearchView.setOnBindSuggestionCallback(new SearchSuggestionsAdapter.OnBindSuggestionCallback() {
            @Override
            public void onBindSuggestion(View suggestionView, ImageView leftIcon, TextView textView, SearchSuggestion item, int itemPosition) {

                // Cast to DestinationSuggestion to use custom methods
                DestinationSuggestion destinationSuggestion = (DestinationSuggestion) item;

                // Creates drawable image with text being the distance to the location
                TextDrawable tempDrawable =
                        new TextDrawable(getDistanceMiles(
                                m_mapFragmentView.getCoordinate(),
                                destinationSuggestion.getCoordinate()));

                // Set the left icon to the drawable created
                leftIcon.setImageDrawable(tempDrawable);

                // Format in HTML the text shown
                String text =
                        "<font color=\"" + "#000000" + "\">" + destinationSuggestion.getName() + "</font>"
                                +"<br>" + "<font color=\"" + "#727272" + "\">" + destinationSuggestion.getAddress() + "</font>";

                textView.setText(Html.fromHtml(text));

                floatingSearchView.hideProgress(); // Hide loading animation
                loadingSuggestions = false;
            }
        });

        // Implements callback method for when text is entered into search bar
        floatingSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, String newQuery) {

                if (!loadingSuggestions) {
                    loadingSuggestions = true;
                    floatingSearchView.showProgress(); // Show loading animation
                }


                SearchRequest searchRequest = new SearchRequest(newQuery);
                searchRequest.setCollectionSize(10); // Max of 10 results per page
                searchRequest.setSearchCenter(m_mapFragmentView.getMap().getCenter());
                searchRequest.execute(discoveryResultPageListener);

            }
        });

        floatingSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                DestinationSuggestion destinationSuggestion
                        = (DestinationSuggestion) searchSuggestion;

                m_mapFragmentView.startNavigation(destinationSuggestion.getCoordinate());

                floatingSearchView.hideProgress(); // Hide loading animation
                loadingSuggestions = false;
            }

            @Override
            public void onSearchAction(String currentQuery) {}
        });
    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}


