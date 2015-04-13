package com.example.android.bluetoothleadvertiser;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class ScannerActivity extends Activity {
    private static final String TAG = ScannerActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    /* UI Control References */
    private TextView mAddressView, mRecordView;
    private TextView mNameView, mPowerView, mDataView;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        mAddressView = (TextView) findViewById(R.id.text_address);
        mRecordView = (TextView) findViewById(R.id.text_record);
        mNameView = (TextView) findViewById(R.id.text_name);
        mPowerView = (TextView) findViewById(R.id.text_transmit);
        mDataView = (TextView) findViewById(R.id.text_manufacturer);

        /*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager,
         * rather than the old static BluetoothAdapter.getInstance()
         */
        BluetoothManager manager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "No Bluetooth Support.", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (!mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry
         * will keep this from installing on these devices, but this will
         * allow test devices or other sideloads to report whether or not
         * the feature exists.
         */
        if (!getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        DeviceUtil.startScanning(mBluetoothLeScanner, mScanCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DeviceUtil.stopScanning(mBluetoothLeScanner, mScanCallback);
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Update the UI with the latest info
            final BluetoothDevice remoteDevice = result.getDevice();
            final ScanRecord record = result.getScanRecord();
            if (!DeviceUtil.hasManufacturerData(record)) {
                Log.w(TAG, "This is not the device we're looking for...");
                return;
            }

            //Post scan update to the UI thread
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateScanResult(remoteDevice.getAddress(), record);
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "Error scanning devices: "+errorCode);
        }
    };

    private void updateScanResult(String address, ScanRecord record) {
        mAddressView.setText(address);
        //Print the raw scan record
        mRecordView.setText(DeviceUtil.bytesToString(record.getBytes()));

        mNameView.setText(record.getDeviceName());
        mPowerView.setText(record.getTxPowerLevel()+"dBm");
        //Extract our custom temp value
        byte[] customData = record.getManufacturerSpecificData(
                DeviceUtil.MANUFACTURER_GOOGLE);
        float tempValue = DeviceUtil.unpackPayload(customData);
        mDataView.setText(tempValue + "\u00B0F");
    }
}
