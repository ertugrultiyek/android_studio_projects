package com.ertugrultyk.tdp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_FINE_LOCATION = 99;

    // WiFi Direct
    WifiManager wifiManager;
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;

    BroadcastReceiver receiver;
    IntentFilter filter;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;


    // References to UI elements
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_adress;
    Switch sw_locationupdates, sw_gps, sw_wifi;
    Button btn_search;
    ListView lv_devices;

    // API for google location provider
    FusedLocationProviderClient fusedLocationProviderClient;

    // location request
    LocationRequest LocationRequest;
    LocationCallback locationCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // find ui elements
        // Text View Elements
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_speed = findViewById(R.id.tv_speed);
        tv_adress = findViewById(R.id.tv_address);
        tv_updates = findViewById(R.id.tv_updates);
        // Switches
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);
        sw_gps = findViewById(R.id.sw_gps);
        sw_wifi = findViewById(R.id.sw_wifi);
        // Buttons
        btn_search = findViewById(R.id.btn_search);
        // List View
        lv_devices = findViewById(R.id.lv_devices);

        // initiation of location request
        LocationRequest = new LocationRequest();
        LocationRequest.setInterval(3000);
        LocationRequest.setFastestInterval(500);
        LocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // WiFi Direct initialization
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        sw_wifi.setChecked(wifiManager.isWifiEnabled());

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        filter = new IntentFilter();
        // Add actions to Intend Filter
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // event that is triggered whenever the update interval is met.
        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                // Save the locations
                updateUI(locationResult.getLastLocation());
            }
        };


        sw_wifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiManager.setWifiEnabled(sw_wifi.isChecked());
        });

        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Search Started!", Toast.LENGTH_SHORT).show();
//                        manager.requestPeers(channel, peerListListener);
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(getApplicationContext(),"Search Start Failed!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sw_gps.isChecked()){
                    // Accurate data
                    LocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS");
                }else{
                    //power saving
                    LocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers & WiFi");
                }
            }
        });

        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sw_locationupdates.isChecked()){
                    // Turn on tracking
                    tv_updates.setText("Location is being updated.");
                    fusedLocationProviderClient.requestLocationUpdates(LocationRequest,locationCallback, null);
                    updateGPS();                }
                else{
                    // Turn off tracking
                    tv_updates.setText("Update stopped!");
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                }
            }
        });

        updateGPS();
    }// end onCreate Method

    WifiP2pManager.PeerListListener myPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray = new String[peers.size()];
                deviceArray = new WifiP2pDevice[peers.size()];
                int index = 0;

                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                lv_devices.setAdapter(adapter);

                if (peers.size() == 0){
                    Toast.makeText(getApplicationContext(),"No devices found!", Toast.LENGTH_SHORT).show();
                    return;
                }
                else
                    Toast.makeText(getApplicationContext(),peers.size(),Toast.LENGTH_SHORT).show();
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "register receiver", Toast.LENGTH_SHORT).show();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "unregister receiver", Toast.LENGTH_SHORT).show();
        unregisterReceiver(receiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){

            case PERMISSIONS_FINE_LOCATION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    updateGPS();
                }else{
                    Toast.makeText(this,"this app requares permission!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void updateGPS(){
        // Get permissions from user
        // Get current location from the fused client
        // Update UI

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            // Permission Granted
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // TODO update UI
                    updateUI(location);
                }
            });
        }
        else{
            // Permission not granted yet
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    private void updateUI(Location location) {
        // update ui values

        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        if(location.hasAltitude())
             tv_altitude.setText(String.valueOf(location.getAltitude()));
        else
            tv_altitude.setText("Not Available!");

        if(location.hasSpeed())
            tv_speed.setText(String.valueOf(location.getSpeed()));
        else
            tv_speed.setText("Not Available!");

    }

}