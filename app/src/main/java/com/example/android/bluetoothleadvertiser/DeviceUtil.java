package com.example.android.bluetoothleadvertiser;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;

public class DeviceUtil {

    /* Full Bluetooth UUID that defines the Health Thermometer Service */
    public static final ParcelUuid THERM_SERVICE =
            ParcelUuid.fromString("00001809-0000-1000-8000-00805f9b34fb");

    //Construct a new advertisement packet and being advertising
    public static void startAdvertising(BluetoothLeAdvertiser advertiser,
                                        AdvertiseCallback callback,
                                        byte[] payload) {
        if (advertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(THERM_SERVICE)
                .addServiceData(THERM_SERVICE, payload)
                .build();

        advertiser.startAdvertising(settings, data, callback);
    }

    //Cancel the current advertisement
    public static void stopAdvertising(BluetoothLeAdvertiser advertiser,
                                       AdvertiseCallback callback) {
        if (advertiser == null) return;

        advertiser.stopAdvertising(callback);
    }

    //Restart after a value change
    public static void restartAdvertising(BluetoothLeAdvertiser advertiser,
                                          AdvertiseCallback callback,
                                          byte[] payload) {
        stopAdvertising(advertiser, callback);
        startAdvertising(advertiser, callback, payload);
    }
}
