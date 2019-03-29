package inc.andsoft.asimidimagic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import androidx.annotation.LayoutRes;
import inc.andsoft.asimidimagic.activities.CommonMidiPassActivity;
import inc.andsoft.asimidimagic.activities.ReceiverState;
import inc.andsoft.asimidimagic.peripheral.GattPeripheral;
import inc.andsoft.asimidimagic.peripheral.MidiPeripheral;

class BlueAnalyserReceiverState implements ReceiverState {
    MidiReceiver myReceiver;

    @Override
    public MidiReceiver getReceiver() {
        return myReceiver;
    }

    @Override
    public void close() throws IOException {
    }
}

public class BluetoothAnalyserActivity extends CommonMidiPassActivity<BlueAnalyserReceiverState> implements GattPeripheral.ServiceDelegate {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "BluetoothAnalyserActivity";

    private TextView myAdvStatus;
    private TextView myConnectionStatus;
    private TextView myTextLog;

    private GattPeripheral myGattPeripheral;
    private BluetoothGattService myBluetoothGattService;
    private HashSet<BluetoothDevice> myBluetoothDevices;
    private BluetoothManager myBluetoothManager;
    private BluetoothAdapter myBluetoothAdapter;
    private AdvertiseData myAdvData;
    private AdvertiseData myAdvScanResponse;
    private AdvertiseSettings myAdvSettings;
    private BluetoothLeAdvertiser myAdvertiser;

    private boolean myRunning = false;
    private boolean myOpen = false;

    private final AdvertiseCallback myAdvCallback = new AdvertiseCallback() {
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "Not broadcasting: " + errorCode);
            int statusText;
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    statusText = R.string.status_advertising;
                    Log.w(TAG, "App was already advertising");
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    statusText = R.string.status_advDataTooLarge;
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    statusText = R.string.status_advFeatureUnsupported;
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    statusText = R.string.status_advInternalError;
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    statusText = R.string.status_advTooManyAdvertisers;
                    break;
                default:
                    statusText = R.string.status_notAdvertising;
                    Log.wtf(TAG, "Unhandled error: " + errorCode);
            }
            myAdvStatus.setText(statusText);
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.v(TAG, "Broadcasting");
            myAdvStatus.setText(R.string.status_advertising);
        }
    };

    private BluetoothGattServer myGattServer;
    private final BluetoothGattServerCallback myGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    myBluetoothDevices.add(device);
                    updateConnectedDevicesStatus();
                    Log.v(TAG, "Connected to device: " + device.getAddress());
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    myBluetoothDevices.remove(device);
                    updateConnectedDevicesStatus();
                    Log.v(TAG, "Disconnected from device");
                }
            } else {
                myBluetoothDevices.remove(device);
                updateConnectedDevicesStatus();
                // There are too many gatt errors (some of them not even in the documentation) so we just
                // show the error to the user.
                final String errorMessage = getString(R.string.status_errorWhenConnecting) + ": " + status;
                runOnUiThread(() -> Toast.makeText(BluetoothAnalyserActivity.this, errorMessage, Toast.LENGTH_LONG).show());
                Log.e(TAG, "Error when connecting: " + status);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d("XYZ", "onCharacteristicReadRequest");
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            if (offset != 0) {
                myGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
                        /* value (optional) */ null);
                return;
            }
            myGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.d("XYZ", "onNotificationSent: " + status);
            Log.v(TAG, "Notification sent. Status: " + status);
            myGattPeripheral.notificationSent();
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value);
            Log.d("XYZ", "onCharacteristicWriteRequest");
            Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));
            int status = myGattPeripheral.writeCharacteristic(characteristic, offset, value);
            if (responseNeeded) {
                myGattServer.sendResponse(device, requestId, status,
                        /* No need to respond with an offset */ 0,
                        /* No need to respond with a value */ null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                            int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.d("XYZ", "onDescriptorReadRequest");
            Log.d(TAG, "Device tried to read descriptor: " + descriptor.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(descriptor.getValue()));
            if (offset != 0) {
                myGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
                        /* value (optional) */ null);
                return;
            }
            myGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
                    offset, value);
            Log.d("XYZ", "onDescriptorWriteRequest");
            Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
            int status;
            if (descriptor.getUuid() == GattPeripheral.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                boolean supportsNotifications = (characteristic.getProperties() &
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
                boolean supportsIndications = (characteristic.getProperties() &
                        BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;

                if (!(supportsNotifications || supportsIndications)) {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                } else if (value.length != 2) {
                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
                } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    myGattPeripheral.notificationsDisabled(characteristic);
                    descriptor.setValue(value);
                } else if (supportsNotifications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    myGattPeripheral.notificationsEnabled(characteristic, false /* indicate */);
                    descriptor.setValue(value);
                } else if (supportsIndications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    myGattPeripheral.notificationsEnabled(characteristic, true /* indicate */);
                    descriptor.setValue(value);
                } else {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                }
            } else {
                status = BluetoothGatt.GATT_SUCCESS;
                descriptor.setValue(value);
            }
            if (responseNeeded) {
                myGattServer.sendResponse(device, requestId, status,
                        /* No need to respond with offset */ 0,
                        /* No need to respond with a value */ null);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myAdvStatus = findViewById(R.id.textView_advertisingStatus);
        myConnectionStatus = findViewById(R.id.textView_connectionStatus);

        myTextLog = findViewById(R.id.text_logging);
        myTextLog.setMovementMethod(new ScrollingMovementMethod());

        Button clear = findViewById(R.id.button_clear);
        clear.setOnClickListener(v -> myTextLog.setText(""));

        EditText data = findViewById(R.id.text_data);
        Button send = findViewById(R.id.button_send);
        send.setOnClickListener(v -> sendData(data.getText().toString()));

        myBluetoothDevices = new HashSet<>();
        myBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        myBluetoothAdapter = myBluetoothManager.getAdapter();

        myGattPeripheral = new MidiPeripheral(new MidiReceiver() {
            @Override
            public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
                if (myInputPort != null) {
                    myInputPort.send(msg, offset, count, timestamp);
                    log("B2M", msg, offset, count);
                }
            }
        }, this);

        myBluetoothGattService = myGattPeripheral.getBluetoothGattService();

        myAdvSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build();
        myAdvData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(myGattPeripheral.getServiceUUID())
                .build();
        myAdvScanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();
    }

    public void sendData(String data) {
        String rows[] = data.split("\n");
        for (String row : rows) {
            String[] commands = row.split(" ");

            int n = commands.length;

            byte[] buffer = new byte[n];

            for (int i = 0; i < n; ++i) {
                int v = Integer.parseInt(commands[i], 16);
                buffer[i] = (byte) v;
            }

            try {
                myReceiverState.myReceiver.send(buffer, 0, buffer.length);
                log("CMD", buffer, 0, buffer.length);
            } catch (IOException io) {

            }
        }
    }

    @Override
    protected BlueAnalyserReceiverState getReceiverState() {
        BlueAnalyserReceiverState state = new BlueAnalyserReceiverState();

        MidiReceiver receiver = new MidiReceiver() {
            @Override
            public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
                if (myRunning) {
                    myGattPeripheral.write(msg, offset, count, timestamp);
                    log("M2B", msg, offset, count);
                }
            }
        };

        state.myReceiver = receiver;

        return state;
    }

    private void log(String prefix, byte[] data, int offset, int count) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < count; i++) {
            byte b = data[offset + i];
            if (b == (byte) 0xF0) {
                myOpen = true;
            }
            sb.append(String.format(" %02X", b));
            if (b == (byte) 0xF7) {
                myOpen = false;
            }
        }
        if (!myOpen) {
            sb.append(" ");
            sb.append(prefix);
            sb.append("\n");
        }

        runOnUiThread(() -> myTextLog.append(sb.toString()));
    }


    @Override
    protected @LayoutRes int getLayoutID() {
        return R.layout.activity_analyser;
    }

    @Override
    protected void setRunning(boolean value) {
        myRunning = value;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                if (!myBluetoothAdapter.isMultipleAdvertisementSupported()) {
                    Toast.makeText(this, R.string.error_bluetooth_advertising_not_supported, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Advertising not supported");
                }
                onStart();
            } else {
                Toast.makeText(this, R.string.error_bluetooth_not_enabled, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Bluetooth not enabled");
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        resetStatusViews();
        // If the user disabled Bluetooth when the app was in the background,
        // openGattServer() will return null.
        myGattServer = myBluetoothManager.openGattServer(this, myGattServerCallback);
        if (myGattServer == null) {
            ensureBleFeaturesAvailable();
            return;
        }
        // Add a service for a total of three services (Generic Attribute and Generic Access
        // are present by default).
        myGattServer.addService(myBluetoothGattService);

        if (myBluetoothAdapter.isMultipleAdvertisementSupported()) {
            myAdvertiser = myBluetoothAdapter.getBluetoothLeAdvertiser();
            myAdvertiser.startAdvertising(myAdvSettings, myAdvData, myAdvScanResponse, myAdvCallback);
        } else {
            myAdvStatus.setText(R.string.status_no_le_adv);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (myGattServer != null) {
            myGattServer.close();
        }
        if (myBluetoothAdapter.isEnabled() && myAdvertiser != null) {
            // If stopAdvertising() gets called before close() a null
            // pointer exception is raised.
            myAdvertiser.stopAdvertising(myAdvCallback);
        }
        resetStatusViews();
    }

    public void sendNotificationToDevices(BluetoothGattCharacteristic characteristic) {
        Log.d("XYZ", "sendNotificationToDevices");
        if (myGattServer != null) {
            boolean indicate = (characteristic.getProperties()
                    & BluetoothGattCharacteristic.PROPERTY_INDICATE)
                    == BluetoothGattCharacteristic.PROPERTY_INDICATE;
            for (BluetoothDevice device : myBluetoothDevices) {
                // true for indication (acknowledge) and false for notification (un-acknowledge).
                myGattServer.notifyCharacteristicChanged(device, characteristic, indicate);
            }
        } else {
            // otherwise the peripheral waits for confirmation forever
            // should this be done even if there are no devices?
            myGattPeripheral.notificationSent();
        }

    }

    private void resetStatusViews() {
        myAdvStatus.setText(R.string.status_notAdvertising);
        updateConnectedDevicesStatus();
    }

    private void updateConnectedDevicesStatus() {
        final String message = getString(R.string.status_devicesConnected) + " "
                + myBluetoothManager.getConnectedDevices(BluetoothGattServer.GATT).size();
        runOnUiThread(() -> myConnectionStatus.setText(message));
    }

    private void ensureBleFeaturesAvailable() {
        if (myBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Bluetooth not supported");
            finish();
        } else if (!myBluetoothAdapter.isEnabled()) {
            // Make sure bluetooth is enabled.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    private void disconnectFromDevices() {
        Log.d(TAG, "Disconnecting devices...");
        for (BluetoothDevice device : myBluetoothManager.getConnectedDevices(
                BluetoothGattServer.GATT)) {
            Log.d(TAG, "Devices: " + device.getAddress() + " " + device.getName());
            myGattServer.cancelConnection(device);
        }
    }

}
