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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
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
    private LeDeviceListAdapter myLeDeviceListAdapter;
    private BluetoothAdapter myBluetoothAdapter;
    private boolean myScanning = false;
    private Handler myHandler = new Handler();

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

        RecyclerView recyclerView = findViewById(R.id.list_ble);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        myLeDeviceListAdapter = new LeDeviceListAdapter();
        recyclerView.setAdapter(myLeDeviceListAdapter);

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
        myBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (myBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!myBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onBackPressed() {
        BluetoothDevice[] devices = myLeDeviceListAdapter.getSelectedDevices();
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
        if (myScanning) {
            stopScanningLeDevices();
        }
        myLeDeviceListAdapter.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ble, menu);
        if (!myScanning) {
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
        BluetoothLeScanner leScanner = myBluetoothAdapter.getBluetoothLeScanner();
        if (leScanner != null) {
            // user might have disable Bluetooth by now
            myScanning = true;
            ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
            filterBuilder.setServiceUuid(MIDI_UUID);
            Vector<ScanFilter> filters = new Vector<>();
            filters.add(filterBuilder.build());

            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
            ScanSettings settings = settingsBuilder.build();

            // Stops scanning after a pre-defined scan period.
            myHandler.postDelayed(this::stopScanningLeDevices, SCAN_PERIOD);

            leScanner.startScan(filters, settings, mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private void stopScanningLeDevices() {
        myScanning = false;
        BluetoothLeScanner leScanner = myBluetoothAdapter.getBluetoothLeScanner();
        if (leScanner != null) {
            // user might have disable Bluetooth by now
            leScanner.stopScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            runOnUiThread(() -> {
                BluetoothDevice device = result.getDevice();
                myLeDeviceListAdapter.addDevice(device);
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            runOnUiThread(() -> {
                for (ScanResult result : results) {
                    BluetoothDevice device = result.getDevice();
                    myLeDeviceListAdapter.addDevice(device);
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
        }

    };

}

// Adapter for holding devices found through scanning.
class LeDeviceListAdapter extends RecyclerView.Adapter<LeDeviceListAdapter.LeViewHolder> {
    private List<Map.Entry<BluetoothDevice, Boolean>> myDevices;

    static class LeViewHolder extends RecyclerView.ViewHolder {

        LeViewHolder(View view) {
            super(view);
        }

        void setValues(BluetoothDevice device, Boolean selected,
                       CompoundButton.OnCheckedChangeListener listener) {
            Switch name = itemView.findViewById(R.id.device_name);
            TextView address = itemView.findViewById(R.id.device_address);

            String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                name.setText(deviceName);
            else
                name.setText(R.string.unknown_device);
            address.setText(device.getAddress());

            name.setChecked(selected);
            name.setOnCheckedChangeListener(listener);
        }
    }

    LeDeviceListAdapter() {
        myDevices = new ArrayList<>();
    }

    void addDevice(BluetoothDevice device) {
        boolean present = myDevices.stream().anyMatch(x -> x.getKey().equals(device));
        if (!present) {
            Map.Entry<BluetoothDevice, Boolean> value = new AbstractMap.SimpleEntry<>(device, false);
            myDevices.add(value);
            notifyDataSetChanged();
        }
    }

    private void setDevice(BluetoothDevice device, boolean selected) {
        Optional<Map.Entry<BluetoothDevice, Boolean>> value =
                myDevices.stream().filter(x -> x.getKey().equals(device)).findFirst();
        value.ifPresent(x -> x.setValue(selected));
        // this is called as part of an event listener,
        // so the view is automatically update
    }

    void clear() {
        myDevices.clear();
    }

    BluetoothDevice[] getSelectedDevices() {
        BluetoothDevice[] devices = myDevices.stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toArray(BluetoothDevice[]::new);
        return devices;
    }

    @NonNull
    @Override
    public LeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_ble_scan, parent, false);

        LeViewHolder viewHolder = new LeViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull LeViewHolder holder, int position) {
        Map.Entry<BluetoothDevice, Boolean> data = myDevices.get(position);
        BluetoothDevice device = data.getKey();

        holder.setValues(device, data.getValue(),
                (CompoundButton buttonView, boolean isChecked) -> setDevice(device, isChecked));
    }

    @Override
    public int getItemCount() {
        return myDevices.size();
    }

}
