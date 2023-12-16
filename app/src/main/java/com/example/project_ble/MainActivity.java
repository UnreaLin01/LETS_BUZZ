package com.example.project_ble;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;

    private ActivityResultLauncher<Intent> enableBluetoothLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Log.d("Permission", "BLUETOOTH enabled");
                    }else{
                        Toast.makeText(this, "請開啟藍芽後重試", Toast.LENGTH_SHORT).show();
                        onDestroy();
                    }
                }
        );

        checkAndAskPermission();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        Button btnAdvertise = findViewById(R.id.btnAdvertise);
        btnAdvertise.setOnClickListener(v -> startAdvertising());
    }

    private void checkAndAskPermission(){
        ArrayList<String> requestPermissionArray = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            requestPermissionArray.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        }
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED){
            requestPermissionArray.add(android.Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        if(!requestPermissionArray.isEmpty()){
            String[] requestPermissions = new String[requestPermissionArray.size()];
            requestPermissionArray.toArray(requestPermissions);
            ActivityCompat.requestPermissions(this, requestPermissions, 1);
        }else{
            checkAndEnableBluetooth();
        }
    }

    private void checkAndEnableBluetooth(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "此裝置不支持藍芽，關閉應用程式", Toast.LENGTH_SHORT).show();
            onDestroy();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int x = 0; x < permissions.length; x++){
            switch (permissions[x]){
                case android.Manifest.permission.BLUETOOTH_ADVERTISE:
                    if(grantResults[x] == PackageManager.PERMISSION_GRANTED){
                        Log.d("Permission", "Permission BLUETOOTH_ADVERTISE get");
                    }else{
                        Toast.makeText(this, "需要鄰近分享權限", Toast.LENGTH_SHORT).show();
                        onDestroy();
                    }
                    break;
                case android.Manifest.permission.BLUETOOTH_CONNECT:
                    if(grantResults[x] == PackageManager.PERMISSION_GRANTED){
                        Log.d("Permission", "Permission BLUETOOTH_CONNECT get");
                    }else{
                        Toast.makeText(this, "需要藍芽權限", Toast.LENGTH_SHORT).show();
                        onDestroy();
                    }
                    break;
            }
        }
        checkAndEnableBluetooth();
    }

    @SuppressLint("MissingPermission")
    private void startAdvertising() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();

        ParcelUuid pUuid = new ParcelUuid(UUID.fromString("00002000-0000-1000-8000-00805f9b34fb"));
        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(pUuid)
                .addServiceData(pUuid, "HELLOWORLD".getBytes(Charset.forName("ASCII")))
                .build();

        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                // 廣播開始成功
                Log.d("BLE", "廣播開始成功");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                // 廣播開始失敗
                Log.e("BLE", "廣播開始失敗: " + errorCode);
            }
        };

        advertiser.startAdvertising(settings, data, advertiseCallback);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (advertiser != null) {
                advertiser.stopAdvertising(advertiseCallback);
        }
        finish();
    }
}