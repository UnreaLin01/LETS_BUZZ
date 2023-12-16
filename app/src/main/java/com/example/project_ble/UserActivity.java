package com.example.project_ble;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanRecord;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserActivity extends AppCompatActivity {

    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;
    private ScanCallback leScanCallback;
    private ArrayAdapter<String> listAdapter;
    private ListView listView;
    private Button btn_lockRoom, btn_go;
    private EditText num_user_room;
    private int lockRoom = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        listView = findViewById(R.id.listview_ble_devices);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(listAdapter);

        // 初始化蓝牙适配器和扫描回调
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        num_user_room = findViewById(R.id.num_user_room);
        btn_go = findViewById(R.id.btn_go);

        leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                ScanRecord scanRecord = result.getScanRecord();
                Log.d("BLE", "Device found: " + result.getDevice().getAddress());

                if (scanRecord != null) {
                    Map<ParcelUuid, byte[]> serviceData = scanRecord.getServiceData();
                    ParcelUuid chacRoomUUID = new ParcelUuid(UUID.fromString("00002001-0000-1000-8000-00805f9b34fb"));
                    ParcelUuid chacKeyUUID = new ParcelUuid(UUID.fromString("00002002-0000-1000-8000-00805f9b34fb"));

                    if (serviceData.containsKey(chacRoomUUID)) {
                        byte[] data = serviceData.get(chacRoomUUID);
                        String room = new String(data, StandardCharsets.US_ASCII);

                        if(room.equals(num_user_room.getText().toString())){
                            btn_go.setEnabled(true);
                            bluetoothLeScanner.stopScan(leScanCallback);
                        }

                        String deviceInfo = device.getAddress() + " - Data: " + room;
                        // 更新UI列表
                        runOnUiThread(() -> listAdapter.clear());
                        runOnUiThread(() -> listAdapter.add(deviceInfo));

                    }
                }
            }
        };

        btn_lockRoom = findViewById(R.id.btn_lockRoom);
        num_user_room = findViewById(R.id.num_user_room);


        btn_lockRoom.setOnClickListener(v -> {
            if(num_user_room.getText().toString().isEmpty()){
                Toast.makeText(this, "請輸入房間編號！", Toast.LENGTH_SHORT).show();
            }else{
                if(lockRoom == 0){
                    lockRoom = 1;
                    num_user_room.setEnabled(false);
                    btn_lockRoom.setText("解除鎖定");
                    Log.d("BLE","Start Scan");
                    startScanning();
                }else if(lockRoom == 1){
                    lockRoom = 0;
                    num_user_room.setEnabled(true);
                    btn_lockRoom.setText("鎖定房間");
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }
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

        bluetoothLeScanner.startScan(filters, settings, leScanCallback);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothLeScanner.stopScan(leScanCallback);
        advertiser.stopAdvertising(advertiseCallback);
    }
}