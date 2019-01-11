package inc.andsoft.asimidimagic;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import inc.andsoft.asimidimagic.model.MagicModel;

public class ScanActivity extends AppCompatActivity implements Observer<Map<BluetoothDevice, MidiDevice>> {
    public final static String TAG = "ScanActivity";

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning = false;
    private Handler mHandler = new Handler();
    private MagicModel myMagicModel;
    private MidiManager myMidiManager;

    private static final ParcelUuid MIDI_UUID =
            ParcelUuid.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700");

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 9500; // arbitrary

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myMagicModel = ViewModelProviders.of(ScanActivity.this).get(MagicModel.class);
        myMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        ListView listView = findViewById(R.id.list_ble);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mLeDeviceListAdapter);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        myMagicModel.getDevices().observe(this, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScanning) {
            stopScanningLeDevices();
        }
        mLeDeviceListAdapter.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ble, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_scan:
                startScanningIfPermitted();
                break;
            case R.id.menu_stop:
                stopScanningLeDevices();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startScanningIfPermitted() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(this, R.string.why_btle_location, Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        } else {
            startScanningLeDevices();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanningLeDevices();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void startScanningLeDevices() {
        BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (leScanner != null) {
            // user might have disable Bluetooth by now
            mScanning = true;
            ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
            filterBuilder.setServiceUuid(MIDI_UUID);
            Vector<ScanFilter> filters = new Vector<>();
            filters.add(filterBuilder.build());

            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
            ScanSettings settings = settingsBuilder.build();

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(this::stopScanningLeDevices, SCAN_PERIOD);

            leScanner.startScan(filters, settings, mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private void stopScanningLeDevices() {
        mScanning = false;
        BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (leScanner != null) {
            // user might have disable Bluetooth by now
            leScanner.stopScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    @Override
    public void onChanged(@Nullable Map<BluetoothDevice, MidiDevice> devices) {
        mLeDeviceListAdapter.set(devices);
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private List<Map.Entry<BluetoothDevice, MidiDevice>> myLeDevices;
        private LayoutInflater myInflator;

        LeDeviceListAdapter() {
            super();
            myLeDevices = new ArrayList<>();
            myInflator = ScanActivity.this.getLayoutInflater();
            clear();
        }

        void clear() {
            myLeDevices.clear();
            updateKeys();
        }

        void set(Map<BluetoothDevice, MidiDevice> devices) {
            myLeDevices.clear();
            myLeDevices.addAll(devices.entrySet());
            updateKeys();
        }

        private void updateKeys() {
            notifyDataSetChanged();
        }

        private Map.Entry<BluetoothDevice, MidiDevice> getDevices(int position) {
            Map.Entry<BluetoothDevice, MidiDevice> value = myLeDevices.get(position);
            return value;
        }

        @Override
        public int getCount() {
            return myLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return getDevices(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = myInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                viewHolder.deviceMidi = view.findViewById(R.id.device_midi);
                viewHolder.button = view.findViewById(R.id.button_action);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            Map.Entry<BluetoothDevice, MidiDevice> devices = getDevices(i);
            BluetoothDevice bleDevice = devices.getKey();
            String deviceName = bleDevice.getName();

            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }

            viewHolder.deviceAddress.setText(devices.getKey().getAddress());

            Handler handler = new Handler();

            MidiDevice midiDevice = devices.getValue();
            if (midiDevice != null) {
                viewHolder.deviceMidi.setText(midiDevice.toString());
                viewHolder.button.setText(R.string.button_disconnect);
                viewHolder.button.setOnClickListener((v) ->
                    myMidiManager.openBluetoothDevice(bleDevice, (MidiDevice md) -> {
                        try {
                            midiDevice.close();
                            myMagicModel.addBLEDevice(bleDevice, null);
                        } catch (IOException e) {
                            Log.e(TAG, e.toString());
                        }
                    }, handler)
                );
            } else {
                viewHolder.deviceMidi.setText(R.string.no_midi_device);
                viewHolder.button.setText(R.string.button_connect);
                viewHolder.button.setOnClickListener((v) ->
                    myMidiManager.openBluetoothDevice(bleDevice,
                            (MidiDevice md) -> myMagicModel.addBLEDevice(bleDevice, md),
                            handler)
                );
            }

            return view;
        }
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            runOnUiThread(() -> {
                BluetoothDevice device = result.getDevice();
                myMagicModel.addBLEDevice(device, null);
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            runOnUiThread(() -> {
                for (ScanResult result : results) {
                    BluetoothDevice device = result.getDevice();
                    myMagicModel.addBLEDevice(device, null);
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
        }

    };

    private static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceMidi;
        Button button;
    }

}
