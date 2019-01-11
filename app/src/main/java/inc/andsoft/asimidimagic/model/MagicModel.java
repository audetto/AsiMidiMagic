package inc.andsoft.asimidimagic.model;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothDevice;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MIDI_SERVICE;

public class MagicModel extends AndroidViewModel {
    private final static String TAG = "MagicModel";

    private MidiManager myMidiManager;

    private Map<BluetoothDevice, MidiDevice> myDevices = new HashMap<>();
    private MutableLiveData<Map<BluetoothDevice, MidiDevice>> myLiveData = new MutableLiveData<>();
    private MidiManager.DeviceCallback myCallback;

    public MagicModel(Application application) {
        super(application);
        myMidiManager = (MidiManager) application.getSystemService(MIDI_SERVICE);
        myCallback = new MidiManager.DeviceCallback() {
            public void onDeviceRemoved(MidiDeviceInfo device) {
                myDevices.entrySet().removeIf((value) -> value.getValue().getInfo() == device);
                // this happens in the main event thread (new Handler())
                myLiveData.setValue(myDevices);
            }
        };

        Handler handler = new Handler();
        myMidiManager.registerDeviceCallback(myCallback, handler);
        myLiveData.setValue(myDevices);
    }

    public void addBLEDevice(BluetoothDevice ble, MidiDevice midi) {
        if (!myDevices.containsKey(ble) || myDevices.get(ble) != midi) {
            myDevices.put(ble, midi);
            myLiveData.setValue(myDevices);
        }
    }

    public LiveData<Map<BluetoothDevice, MidiDevice>> getDevices() {
        return myLiveData;
    }

    protected void onCleared() {
        myMidiManager.unregisterDeviceCallback(myCallback);
        for (Map.Entry<BluetoothDevice, MidiDevice> device : myDevices.entrySet()) {
            try {
                device.getValue().close();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
        myDevices.clear();
    }

}
