package com.example.arifm2.bleperipheralexample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button btn;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    private BluetoothGattServer mGattServer;
    private BluetoothGatt mGattClient;
    private BluetoothDevice clientDevice;
    private BluetoothGattService mBluetoothGattService;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;

    private static final int REQUEST_ENABLE_BT = 10;
    private static final UUID SERVICE_UUID = UUID
            .fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private static final UUID Characteristic_UUID = UUID
            .fromString("00002A19-0000-1000-8000-00805f9b34fb");

    final String TAG = getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkerForPermission();
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
/////////////////////////////////creating service and characteristic///////////////////////////////
        mBluetoothGattService = new BluetoothGattService(SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mBluetoothGattCharacteristic =
                new BluetoothGattCharacteristic(Characteristic_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ);
        mBluetoothGattService.addCharacteristic(mBluetoothGattCharacteristic);


///////////////////////////////////////////////////////////////////////////////////////////////////
        btn=(Button)findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Advetise();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
////////////////////////////////////////////////////////adding service to gatt server///////////////
            mGattServer = btManager.openGattServer(this,  mGattServerCallback);
        if (mGattServer == null) {
            return;
        }
        mGattServer.addService(mBluetoothGattService);
//////////////////////////////////////////////////////////////////////////
    }

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback(){

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.d(getClass().getName() ,device.getAddress()+" "+device.getName());
            clientDevice=device;

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(getClass().getName() ,characteristic.getUuid().toString());
            mBluetoothGattCharacteristic.setValue(50,
                    BluetoothGattCharacteristic.FORMAT_UINT8, /* offset */ 0);
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());

        }
    };
    private BluetoothGattCallback mGattClientCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            Log.d(TAG, "Connection State Change: " + status + " -> " + connectionState(newState));
                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {

                    gatt.discoverServices();
                    Log.d(TAG, "Discovering Services...");

                } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {

                    Log.e(TAG, "Discovering Services error...");

                } else if (status != BluetoothGatt.GATT_SUCCESS) {

                    gatt.disconnect();
                }
        }
    };
    public void Advetise(){
        ParcelUuid pUuid = new ParcelUuid( UUID.fromString( "0000180F-0000-1000-8000-00805f9b34fb" ) );

        BluetoothLeAdvertiser advertiser = btAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_BALANCED )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( true )
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName( true )
                .addServiceUuid( pUuid )
                .build();


        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d( "BLE", "Advertising onStartSuccess: " );
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
                super.onStartFailure(errorCode);
            }
        };

        AdvertiseData mAdvScanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        advertiser.startAdvertising( settings, data, mAdvScanResponse, advertisingCallback );


    }

    public void getDeviceinfo(View view){
        ///////////////////////////////////////////////let connect as client///////////////////////////////////////////////
        mGattClient = clientDevice.connectGatt(this,false,mGattClientCallback);

        //////////////////////////////////////////////////////////////////////////////////////////////
    }

    private void checkerForPermission() {

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    10);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    10);
        }
    }

    ;
}
