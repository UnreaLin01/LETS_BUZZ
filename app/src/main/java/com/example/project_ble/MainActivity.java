package com.example.project_ble;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
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

        Button btn_host = findViewById(R.id.btn_host);
        Button btn_user = findViewById(R.id.btn_user);

        btn_host.setOnClickListener(v -> {
            Intent intent = new Intent(this, HostActivity.class);
            startActivity(intent);
        });

        btn_user.setOnClickListener(v->{
            Intent intent = new Intent(this, UserActivity.class);
            startActivity(intent);
        });
    }

    private void checkAndAskPermission(){
        ArrayList<String> requestPermissionArray = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissionArray.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissionArray.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            requestPermissionArray.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED){
            requestPermissionArray.add(android.Manifest.permission.BLUETOOTH_SCAN);
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
                        //onDestroy();
                    }
                    break;
                case android.Manifest.permission.BLUETOOTH_CONNECT:
                    if(grantResults[x] == PackageManager.PERMISSION_GRANTED){
                        Log.d("Permission", "Permission BLUETOOTH_CONNECT get");
                    }else{
                        Toast.makeText(this, "需要藍芽權限", Toast.LENGTH_SHORT).show();
                        //onDestroy();
                    }
                    break;
            }
        }
        checkAndEnableBluetooth();
    }
}