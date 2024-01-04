package com.example.project_ble;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.os.VibrationEffect;
import android.os.Vibrator;

import android.os.Handler;
import android.os.Looper;

import android.media.AudioAttributes;
import android.media.MediaPlayer;

import android.os.Bundle;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserActivity extends AppCompatActivity {

    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback bluetoothLeScanCallback;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private AdvertiseCallback bluetoothLeAdvertiseCallback;

    private boolean roomLocked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        EditText ed_user_room = findViewById(R.id.ed_user_room);
        EditText ed_user_name = findViewById(R.id.ed_user_name);
        Button btn_buzz = findViewById(R.id.btn_buzz);
        Button btn_lock = findViewById(R.id.btn_lock);
        ListView listView = findViewById(R.id.lv_ble_dbg);
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(listAdapter);

        //set MediaPlayer
//        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.error);
//        mediaPlayer.setAudioAttributes(
//                new AudioAttributes.Builder()
//                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                        .setUsage(AudioAttributes.USAGE_MEDIA)
//                        .build()
//        );

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        VibrationEffect effect = VibrationEffect.createOneShot(50, 255);

        bluetoothLeScanCallback = new ScanCallback() {

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                ScanRecord scanRecord = result.getScanRecord();
                //Log.d("BLE", "Device found: " + result.getDevice().getAddress());

                if (scanRecord != null) {
                    Map<ParcelUuid, byte[]> serviceData = scanRecord.getServiceData();
                    ParcelUuid chacRoomUUID = new ParcelUuid(UUID.fromString("00002001-0000-1000-8000-00805f9b34fb"));

                    if (serviceData.containsKey(chacRoomUUID)) {
                        byte[] data = serviceData.get(chacRoomUUID);
                        String room = new String(data, StandardCharsets.UTF_8);

                        if(ed_user_room.getText().toString().equals(room)){
                            btn_buzz.setEnabled(true);
                            vibrator.vibrate(effect);
                            try{
                                bluetoothLeScanner.stopScan(bluetoothLeScanCallback);
                            }catch (SecurityException err){
                                err.printStackTrace();
                            }
                        }

                        // Use for debug(fixed by wei :) )
                        //String deviceInfo = device.getAddress() + " - Data: " + room;
                        //runOnUiThread(listAdapter::clear);
                        //runOnUiThread(() -> listAdapter.add(deviceInfo));

                    }
                }
            }
        };

        btn_lock.setOnClickListener(v -> {
            if(ed_user_room.getText().toString().isEmpty()){
                //mediaPlayer.start();
                Toast.makeText(this, "請輸入房間編號！", Toast.LENGTH_SHORT).show();
            }else if(Integer.parseInt(ed_user_room.getText().toString()) > 1000 || Integer.parseInt(ed_user_room.getText().toString()) < 1){
                //mediaPlayer.start();
                Toast.makeText(this, "請輸入1~1000內的數字！", Toast.LENGTH_SHORT).show();
            }else if(ed_user_name.getText().toString().isEmpty()){
                //mediaPlayer.start();
                Toast.makeText(this, "請輸入名稱", Toast.LENGTH_SHORT).show();
            }else{
                if(!roomLocked){
                    ed_user_room.setEnabled(false);
                    ed_user_name.setEnabled(false);
                    btn_lock.setText("解除鎖定");
                    startScanning();
                    roomLocked = true;
                }else{
                    ed_user_room.setEnabled(true);
                    ed_user_name.setEnabled(true);
                    btn_lock.setText("鎖定房間");
                    try {
                        bluetoothLeScanner.stopScan(bluetoothLeScanCallback);
                    }catch(SecurityException err){
                        err.printStackTrace();
                    }
                    roomLocked = false;
                }
            }
        });

        btn_buzz.setOnClickListener(v -> {
            btn_buzz.setEnabled(false);
            startAdvertising();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                try {
                    bluetoothLeAdvertiser.stopAdvertising(bluetoothLeAdvertiseCallback);
                }catch(SecurityException err){
                    err.printStackTrace();
                }
            }, 500);
        });
    }

    private void startScanning() {
        UUID uuid = UUID.fromString("00002000-0000-1000-8000-00805f9b34fb");

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(uuid))
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        try{
            bluetoothLeScanner.startScan(filters, settings, bluetoothLeScanCallback);
        }catch(SecurityException err){
            err.printStackTrace();
        }
    }

    private void startAdvertising() {

        EditText ed_user_room = findViewById(R.id.ed_user_room);
        EditText ed_user_name = findViewById(R.id.ed_user_name);

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();

        ParcelUuid serviceUUID = new ParcelUuid(UUID.fromString("00003000-0000-1000-8000-00805f9b34fb"));
        ParcelUuid chacRoomUUID = new ParcelUuid(UUID.fromString("00003001-0000-1000-8000-00805f9b34fb"));
        ParcelUuid chacUserUUID = new ParcelUuid(UUID.fromString("00003002-0000-1000-8000-00805f9b34fb"));

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(serviceUUID)
                .addServiceData(chacRoomUUID, ed_user_room.getText().toString().getBytes(StandardCharsets.UTF_8))
                .addServiceData(chacUserUUID, ed_user_name.getText().toString().getBytes(StandardCharsets.UTF_8))
                .build();

        bluetoothLeAdvertiseCallback = new AdvertiseCallback() {
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
            bluetoothLeAdvertiser.startAdvertising(settings, data, bluetoothLeAdvertiseCallback);
        } catch (SecurityException err) {
            err.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            bluetoothLeScanner.stopScan(bluetoothLeScanCallback);
            bluetoothLeAdvertiser.stopAdvertising(bluetoothLeAdvertiseCallback);
        }catch(SecurityException err){
            err.printStackTrace();
        }
    }
}