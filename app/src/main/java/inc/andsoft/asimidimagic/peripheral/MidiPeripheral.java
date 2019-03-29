package inc.andsoft.asimidimagic.peripheral;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.media.midi.MidiReceiver;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class MidiPeripheral extends GattPeripheral {
    private static final String TAG = "MidiPeripheral";

    private static final UUID MIDI_SERVICE_UUID = UUID
            .fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700");

    private static final UUID MIDI_IO_UUID = UUID
            .fromString("7772E5DB-3868-4112-A1A9-F2669D106BF3");

    // MIDI
    // PacketReceiver for receiving formatted packets from our BluetoothPacketEncoder
    private final PacketEncoder.PacketReceiver myPacketReceiver = new PacketReceiver();

    private final BluetoothPacketEncoder myPacketEncoder
            = new BluetoothPacketEncoder(myPacketReceiver, MAX_PACKET_SIZE);

    private final BluetoothPacketDecoder myPacketDecoder
            = new BluetoothPacketDecoder(MAX_PACKET_SIZE);

    private static final int MAX_PACKET_SIZE = 20;

    private MidiReceiver myReceiver;
    private ServiceDelegate myDelegate;

    private boolean myNotificationEnabled = false;

    // GATT
    private BluetoothGattService myMIDIService;
    private BluetoothGattCharacteristic myMIDIIOCharacteristic;

    public MidiPeripheral(MidiReceiver receiver, ServiceDelegate delegate) {
        myMIDIIOCharacteristic =
                new BluetoothGattCharacteristic(MIDI_IO_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ |
                                BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                        BluetoothGattCharacteristic.PERMISSION_READ |
                                BluetoothGattCharacteristic.PERMISSION_WRITE);

        myMIDIIOCharacteristic.addDescriptor(
                getClientCharacteristicConfigurationDescriptor());

        myMIDIService = new BluetoothGattService(MIDI_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        myMIDIService.addCharacteristic(myMIDIIOCharacteristic);

        myReceiver = receiver;
        myDelegate = delegate;
    }

    public BluetoothGattService getBluetoothGattService() {
        return myMIDIService;
    }

    @Override
    public ParcelUuid getServiceUUID() {
        return new ParcelUuid(MIDI_SERVICE_UUID);
    }

    @Override
    public void write(byte[] msg, int offset, int count, long timestamp) throws IOException {
        if (myNotificationEnabled) {
            Log.d("XYZ", "onSend 2: " + count);
            myPacketEncoder.send(msg, offset, count, timestamp);
            Log.d("XYZ", "onSent 2: " + count);
        }
    }

    @Override
    public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
        final byte[] buffer = Arrays.copyOfRange(value, offset, value.length);
        myPacketDecoder.decodePacket(buffer, myReceiver);
        return BluetoothGatt.GATT_SUCCESS;
    }

    @Override
    public void notificationSent() {
        myPacketEncoder.writeComplete();
    }

    @Override
    public void notificationsEnabled(BluetoothGattCharacteristic characteristic, boolean indicate) {
        Log.d("XYZ", "notifications enabled: " + indicate);
        myNotificationEnabled = true;
    }

    @Override
    public void notificationsDisabled(BluetoothGattCharacteristic characteristic) {
        Log.d("XYZ", "notifications disabled");
        myNotificationEnabled = false;
    }

    private class PacketReceiver implements PacketEncoder.PacketReceiver {
        // buffers of every possible packet size
        private final byte[][] mWriteBuffers;

        public PacketReceiver() {
            // Create buffers of every possible packet size
            mWriteBuffers = new byte[MAX_PACKET_SIZE + 1][];
            for (int i = 0; i <= MAX_PACKET_SIZE; i++) {
                mWriteBuffers[i] = new byte[i];
            }
        }

        @Override
        public void writePacket(byte[] buffer, int count) {
            if (myMIDIIOCharacteristic == null) {
                Log.w(TAG, "not ready to send packet yet");
                return;
            }
            byte[] writeBuffer = mWriteBuffers[count];
            System.arraycopy(buffer, 0, writeBuffer, 0, count);
            Log.d("XYZ", "write Packet: " + count);
            myMIDIIOCharacteristic.setValue(writeBuffer);
            myDelegate.sendNotificationToDevices(myMIDIIOCharacteristic);
            Log.d("XYZ", "written Packet: " + count);
        }
    }

}
