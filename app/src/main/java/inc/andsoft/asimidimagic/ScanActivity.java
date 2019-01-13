package inc.andsoft.asimidimagic;

import android.Manifest;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;


public class ScanActivity extends AppCompatActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning = false;
    private Handler mHandler = new Handler();

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
    }

    @Override
    public void onBackPressed() {
        BluetoothDevice[] devices = mLeDeviceListAdapter.getSelectedDevices();
        if (devices.length > 0) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("devices", devices);
            setResult(RESULT_OK, resultIntent);
        }
        super.onBackPressed();
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

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private LayoutInflater mInflator;
        private List<Map.Entry<BluetoothDevice, Boolean>> myDevices;

        LeDeviceListAdapter() {
            myDevices = new ArrayList<>();
            mInflator = ScanActivity.this.getLayoutInflater();
        }

        void addDevice(BluetoothDevice device) {
            boolean present = myDevices.stream().anyMatch(x -> x.getKey().equals(device));
            if (!present) {
                Map.Entry<BluetoothDevice, Boolean> value = new AbstractMap.SimpleEntry<>(device, false);
                myDevices.add(value);
                notifyDataSetChanged();
            }
        }

        void setDevice(BluetoothDevice device, boolean selected) {
            Optional<Map.Entry<BluetoothDevice, Boolean>> value =
                    myDevices.stream().filter(x -> x.getKey().equals(device)).findFirst();
            value.get().setValue(selected);
            // this is called as part of an event listener,
            // so the view is automatically update
        }

        void clear() {
            myDevices.clear();
        }

        BluetoothDevice[] getSelectedDevices() {
            BluetoothDevice[] devices = myDevices.stream().filter(x -> x.getValue())
                    .map(x -> x.getKey()).toArray(BluetoothDevice[]::new);
            return devices;
        }

        @Override
        public int getCount() {
            return myDevices.size();
        }

        @Override
        public Map.Entry<BluetoothDevice, Boolean> getItem(int i) {
            return myDevices.get(i);
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
                view = mInflator.inflate(R.layout.listitem_ble_scan, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            Map.Entry<BluetoothDevice, Boolean> data = myDevices.get(i);
            BluetoothDevice device = data.getKey();
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            viewHolder.deviceName.setChecked(data.getValue());
            viewHolder.deviceName.setOnCheckedChangeListener(
                    (CompoundButton buttonView, boolean isChecked) -> setDevice(device, isChecked));

            return view;
        }

    }

    private static class ViewHolder {
        Switch deviceName;
        TextView deviceAddress;
        Button button;
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            runOnUiThread(() -> {
                BluetoothDevice device = result.getDevice();
                mLeDeviceListAdapter.addDevice(device);
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            runOnUiThread(() -> {
                for (ScanResult result : results) {
                    BluetoothDevice device = result.getDevice();
                    mLeDeviceListAdapter.addDevice(device);
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
        }

    };

}
