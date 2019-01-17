package inc.andsoft.asimidimagic.model;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothDevice;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import inc.andsoft.asimidimagic.tools.Utilities;

import static android.content.Context.MIDI_SERVICE;

public class MagicModel extends AndroidViewModel {
    private final static String TAG = "MagicModel";

    private MidiManager myMidiManager;

    private Map<BluetoothDevice, MidiDevice> myDevices = new HashMap<>();
    private MutableLiveData<Map<BluetoothDevice, MidiDevice>> myLiveData = new MutableLiveData<>();
    private MidiManager.DeviceCallback myCallback;
    private Handler myHandler;

    public MagicModel(Application application) {
        super(application);
        myMidiManager = (MidiManager) application.getSystemService(MIDI_SERVICE);
        myCallback = new MidiManager.DeviceCallback() {
            public void onDeviceRemoved(MidiDeviceInfo device) {
                myDevices.entrySet().removeIf((value) -> value.getValue().getInfo().equals(device));
                // this happens in the main event thread (new Handler())
                myLiveData.setValue(myDevices);
            }
        };

        myHandler = new Handler();  // same thread that created the class (i.e. UI)
        myMidiManager.registerDeviceCallback(myCallback, myHandler);
        myLiveData.setValue(myDevices);
    }

    public void addBLEDevices(Set<BluetoothDevice> devices) {
        for (BluetoothDevice device : devices) {
            if (!myDevices.containsKey(device)) {
                myMidiManager.openBluetoothDevice(device, (midi) -> {
                    if (midi != null) {
                        myDevices.put(device, midi);
                        myLiveData.setValue(myDevices);
                    }
                }, myHandler);
            }
        }
    }

    public LiveData<Map<BluetoothDevice, MidiDevice>> getDevices() {
        return myLiveData;
    }

    protected void onCleared() {
        myMidiManager.unregisterDeviceCallback(myCallback);
        myDevices.forEach((ble, midi) -> Utilities.doClose(midi));
        myDevices.clear();
    }

}
