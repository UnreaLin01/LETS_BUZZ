package com.example.project_ble;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.UUID;

public class HostActivity extends AppCompatActivity {

    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;
    private EditText num_room;

    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_host);

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

            Button btn_start = findViewById(R.id.btn_start);
            Button btn_stop = findViewById(R.id.btn_stop);
            EditText num_room = findViewById(R.id.num_room);

            btn_start.setOnClickListener(v -> {
                if(num_room.getText().toString().isEmpty()){
                    Toast.makeText(this, "請輸入房間編號！", Toast.LENGTH_SHORT).show();
                }else{
                    btn_stop.setEnabled(true);
                    btn_start.setEnabled(false);
                    startAdvertising();
                }
            });

            btn_stop.setOnClickListener(v -> {
                btn_stop.setEnabled(false);
                btn_start.setEnabled(true);
                try{
                    advertiser.stopAdvertising(advertiseCallback);
                }catch(SecurityException err){
                    err.printStackTrace();
                }
            });
        }

    private void startAdvertising() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();

        ParcelUuid serviceUUID = new ParcelUuid(UUID.fromString("00002000-0000-1000-8000-00805f9b34fb"));
        ParcelUuid chacRoomUUID = new ParcelUuid(UUID.fromString("00002001-0000-1000-8000-00805f9b34fb"));
        ParcelUuid chacKeyUUID = new ParcelUuid(UUID.fromString("00002002-0000-1000-8000-00805f9b34fb"));
        EditText num_room = findViewById(R.id.num_room);
        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(serviceUUID)
                .addServiceData(chacRoomUUID, num_room.getText().toString().getBytes(Charset.forName("ASCII")))
                .addServiceData(chacKeyUUID, new String("KEY").getBytes(Charset.forName("ASCII")))
                .build();

        advertiseCallback = new AdvertiseCallback(){
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d("BLE", "BLE advertise successful");
            }
            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e("BLE", "BLE advertise fail" + errorCode);
            }
        };

        try {
            advertiser.startAdvertising(settings, data, advertiseCallback);
        }catch (SecurityException err){
            err.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (advertiser != null) {
            try{
                advertiser.stopAdvertising(advertiseCallback);
            }catch(SecurityException err){
                err.printStackTrace();
            }
        }
    }
}