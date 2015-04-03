package com.example.android.bluetoothleadvertiser;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


public class AdvertiserActivity extends Activity
        implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = AdvertiserActivity.class.getSimpleName();
    private static final int DEFAULT_VALUE = 20;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    /* UI to control advertise value */
    private TextView mCurrentValue;
    private SeekBar mSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advertiser);

        mCurrentValue = (TextView) findViewById(R.id.current);
        mSlider = (SeekBar) findViewById(R.id.slider);

        mSlider.setMax(100);
        mSlider.setOnSeekBarChangeListener(this);
        mSlider.setProgress(DEFAULT_VALUE);

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
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
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

        /*
         * Check for advertising support. Not all devices are enabled to
         * advertise Bluetooth LE data.
         */
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "No Advertising Support.", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        DeviceUtil.startAdvertising(mBluetoothLeAdvertiser,
                mAdvertiseCallback,
                buildTemperaturePacket());
    }

    @Override
    protected void onPause() {
        super.onPause();
        DeviceUtil.stopAdvertising(mBluetoothLeAdvertiser, mAdvertiseCallback);
    }

    private byte[] buildTemperaturePacket() {
        int value;
        try {
            value = Integer.parseInt(mCurrentValue.getText().toString());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new byte[] {(byte)value, 0x00};
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
        }
    };

    /** Click handler to update advertisement data */

    public void onUpdateClick(View v) {
        DeviceUtil.restartAdvertising(mBluetoothLeAdvertiser,
                mAdvertiseCallback,
                buildTemperaturePacket());
    }

    /** Callbacks to update UI when slider changes */

    @Override
    public void onProgressChanged(SeekBar seekBar,
                                  int progress,
                                  boolean fromUser) {
        mCurrentValue.setText(String.valueOf(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }
}
