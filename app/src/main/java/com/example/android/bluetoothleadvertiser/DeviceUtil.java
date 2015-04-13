package com.example.android.bluetoothleadvertiser;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DeviceUtil {

    /*
     * Adding manufacturer-specific custom data to the ad packet.
     * We will use Google's Bluetooth SIG identifier as the example.
     */
    public static final int MANUFACTURER_GOOGLE = 0x00E0;

    //Check if a given record has the custom data we want
    public static boolean hasManufacturerData(ScanRecord record) {
        SparseArray<byte[]> data =
                record.getManufacturerSpecificData();

        return (data != null
                && data.get(MANUFACTURER_GOOGLE) != null);
    }

    //Construct a new advertisement packet and being advertising
    public static void startAdvertising(BluetoothLeAdvertiser advertiser,
                                        AdvertiseCallback callback,
                                        float tempValue) {
        if (advertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                //Necessary to see friendly name in a scanning app
                .setIncludeDeviceName(true)
                //Helpful for proximity calculations
                .setIncludeTxPowerLevel(true)
                //Our custom temp data
                .addManufacturerData(MANUFACTURER_GOOGLE,
                        buildPayload(tempValue))
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
                                          float newTempValue) {
        stopAdvertising(advertiser, callback);
        startAdvertising(advertiser, callback, newTempValue);
    }

    //Start a new scan
    public static void startScanning(BluetoothLeScanner scanner,
                                     ScanCallback callback) {
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();
        scanner.startScan(null, settings, callback);
    }

    //Cancel the current scan
    public static void stopScanning(BluetoothLeScanner scanner,
                                    ScanCallback callback) {
        scanner.stopScan(callback);
    }

    /*
     * While not used as a characteristic, this payload matches the
     * format for the adopted Temperature Measurement Characteristic.
     */
    private static byte[] buildPayload(float value) {
        //Set the MSB to indicate fahrenheit
        byte flags = (byte)0x80;
        return ByteBuffer.allocate(5)
                //GATT APIs expect LE order
                .order(ByteOrder.LITTLE_ENDIAN)
                //Add the flags byte
                .put(flags)
                //Add the temperature value
                .putFloat(value)
                .array();
    }

    /*
     * Extract the temperature float back from the characteristic
     * payload value.
     */
    public static float unpackPayload(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data)
                .order(ByteOrder.LITTLE_ENDIAN);
        //Flags are first
        buffer.get();
        //Temp value is next, as a float
        float temp = buffer.getFloat();

        return temp;
    }

    //Utility to display raw bytes in the UI
    public static String bytesToString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte chunk : data) {
            sb.append(String.format("%02X ", chunk));
        }

        return sb.toString();
    }
}
