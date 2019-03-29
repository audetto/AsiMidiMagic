package inc.andsoft.asimidimagic.peripheral;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

import java.io.IOException;
import java.util.UUID;

public abstract class GattPeripheral {
    private static final UUID CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
            .fromString("00002901-0000-1000-8000-00805f9b34fb");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    public abstract BluetoothGattService getBluetoothGattService();
    public abstract ParcelUuid getServiceUUID();

    public void write(byte[] msg, int offset, int count, long timestamp)  throws IOException {
        throw new UnsupportedOperationException("Method write not overridden");
    }

    public void notificationSent() {
        throw new UnsupportedOperationException("Method notificationSent not overridden");
    }

    /**
     * Function to communicate to the ServiceFragment that a device wants to write to a
     * characteristic.
     *
     * The ServiceFragment should check that the value being written is valid and
     * return a code appropriately. The ServiceFragment should update the UI to reflect the change.
     * @param characteristic Characteristic to write to
     * @param value Value to write to the characteristic
     * @return {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} if the write operation
     * was completed successfully. See {@link android.bluetooth.BluetoothGatt} for GATT return codes.
     */
    public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
        throw new UnsupportedOperationException("Method writeCharacteristic not overridden");
    }

    /**
     * Function to notify to the ServiceFragment that a device has disabled notifications on a
     * CCC descriptor.
     *
     * The ServiceFragment should update the UI to reflect the change.
     * @param characteristic Characteristic written to
     */
    public void notificationsDisabled(BluetoothGattCharacteristic characteristic) {
        throw new UnsupportedOperationException("Method notificationsDisabled not overridden");
    }

    /**
     * Function to notify to the ServiceFragment that a device has enabled notifications on a
     * CCC descriptor.
     *
     * The ServiceFragment should update the UI to reflect the change.
     * @param characteristic Characteristic written to
     * @param indicate Boolean that says if it's indicate or notify.
     */
    public void notificationsEnabled(BluetoothGattCharacteristic characteristic, boolean indicate) {
        throw new UnsupportedOperationException("Method notificationsEnabled not overridden");
    }

    /**
     * This interface must be implemented by activities that contain a ServiceFragment to allow an
     * interaction in the fragment to be communicated to the activity.
     */
    public interface ServiceDelegate {
        void sendNotificationToDevices(BluetoothGattCharacteristic characteristic);
    }

    public static BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor() {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        descriptor.setValue(new byte[]{0, 0});
        return descriptor;
    }

    public static BluetoothGattDescriptor getCharacteristicUserDescriptionDescriptor(String defaultValue) {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CHARACTERISTIC_USER_DESCRIPTION_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        try {
            descriptor.setValue(defaultValue.getBytes("UTF-8"));
        } finally {
            return descriptor;
        }
    }

}
