package com.example.project_ble;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HostActivity extends AppCompatActivity {
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback bluetoothLeScanCallback;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private AdvertiseCallback bluetoothLeAdvertiseCallback;
    private static final long START_TIME_IN_MILLIS = 5050;
    private TextView mTextViewCountDown;
    private CountDownTimer countDownTimer;
    private boolean mTimerRunning;
    private long mTimeLeftInMillis = START_TIME_IN_MILLIS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        Button btn_start = findViewById(R.id.btn_start);
        Button btn_stop = findViewById(R.id.btn_stop);
        EditText ed_room = findViewById(R.id.ed_room);
        EditText ed_set_time = findViewById(R.id.ed_set_time);
        ListView lv_rank = findViewById(R.id.lv_rank);
        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lv_rank.setAdapter(listAdapter);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        MediaPlayer countdown = MediaPlayer.create(this, R.raw.countdown);
        countdown.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        MediaPlayer timesup = MediaPlayer.create(this, R.raw.timesup);
        timesup.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );



        bluetoothLeScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                ScanRecord scanRecord = result.getScanRecord();
                //Log.d("BLE", "Device found: " + result.getDevice().getAddress());

                if (scanRecord != null) {
                    Map<ParcelUuid, byte[]> serviceData = scanRecord.getServiceData();
                    ParcelUuid chacRoomUUID = new ParcelUuid(UUID.fromString("00003001-0000-1000-8000-00805f9b34fb"));
                    ParcelUuid chacNameUUID = new ParcelUuid(UUID.fromString("00003002-0000-1000-8000-00805f9b34fb"));

                    if (serviceData.containsKey(chacRoomUUID) && serviceData.containsKey(chacNameUUID)) {
                        byte[] data = serviceData.get(chacRoomUUID);
                        String room = new String(data, StandardCharsets.UTF_8);

                        if (ed_room.getText().toString().equals(room)) {
                            data = serviceData.get(chacNameUUID);
                            String userName = new String(data, StandardCharsets.UTF_8);
                            String userInfo = "Name: " + userName + " - Mac: " + device.getAddress();

                            if (listAdapter.getPosition(userInfo) == -1) {
                                runOnUiThread(() -> listAdapter.add(userInfo));
                            }
                        }
                    }
                }
            }
        };

        bluetoothLeAdvertiseCallback = new AdvertiseCallback(){
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d("BLE", "BLE advertise successful!");
            }
            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e("BLE", "BLE advertise fail!" + errorCode);
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable taskEndBuzz = () -> {
            btn_stop.setEnabled(false);
            btn_start.setEnabled(true);
            resetTimer();
            mTextViewCountDown.setText("");
            timesup.start();
            try{
                bluetoothLeScanner.stopScan(bluetoothLeScanCallback);
                bluetoothLeAdvertiser.stopAdvertising(bluetoothLeAdvertiseCallback);
            }catch(SecurityException err){
                err.printStackTrace();
            }
        };

        Handler adhandler = new Handler(Looper.getMainLooper());
        Runnable addelay = () -> {
            startAdvertising();
        };



        btn_start.setOnClickListener(v -> {
            if(ed_room.getText().toString().isEmpty()){
                Toast.makeText(this, "請輸入房間編號！", Toast.LENGTH_SHORT).show();
            }else if(Integer.parseInt(ed_room.getText().toString()) > 1000 || Integer.parseInt(ed_room.getText().toString()) < 1){
                Toast.makeText(this, "請輸入1~1000內的數字！", Toast.LENGTH_SHORT).show();
            }else{
                listAdapter.clear();
                btn_stop.setEnabled(true);
                btn_start.setEnabled(false);
                adhandler.postDelayed(addelay,5000);
                countdown.start();
                startTimer();
                updateCountDownText();
                handler.postDelayed(taskEndBuzz, 5000+(Integer.parseInt(ed_set_time.getText().toString())*1000));
            }
        });

        btn_stop.setOnClickListener(v -> {
            handler.removeCallbacks(taskEndBuzz);
            handler.post(taskEndBuzz);
        });
    }

    private void startScanning() {
        UUID uuid = UUID.fromString("00003000-0000-1000-8000-00805f9b34fb");
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
        EditText ed_room = findViewById(R.id.ed_room);

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();

        ParcelUuid serviceUUID = new ParcelUuid(UUID.fromString("00002000-0000-1000-8000-00805f9b34fb"));
        ParcelUuid chacRoomUUID = new ParcelUuid(UUID.fromString("00002001-0000-1000-8000-00805f9b34fb"));

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(serviceUUID)
                .addServiceData(chacRoomUUID, ed_room.getText().toString().getBytes(StandardCharsets.UTF_8))
                .build();

        try{
            bluetoothLeAdvertiser.startAdvertising(settings, data, bluetoothLeAdvertiseCallback);
        }catch (SecurityException err){
            err.printStackTrace();
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {


            }
        }.start();
    }

    private void resetTimer() {
        mTimeLeftInMillis = START_TIME_IN_MILLIS;
        updateCountDownText();
    }

    private void pauseTimer() {
        countDownTimer.cancel();

    }

    private void updateCountDownText() {
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format("%d",seconds);
        if (seconds>0){
            mTextViewCountDown.setText("搶答倒數:"+timeLeftFormatted);
        }
        else {
            mTextViewCountDown.setText("");
        }

    }

}