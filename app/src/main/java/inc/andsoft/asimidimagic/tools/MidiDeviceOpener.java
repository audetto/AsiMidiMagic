package inc.andsoft.asimidimagic.tools;

import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.mobileer.miditools.MidiPortWrapper;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Created by andrea on 28/01/18.
 */

public class MidiDeviceOpener implements Closeable {
    public final static String TAG = "MidiDeviceOpener";

    private Set<MidiDeviceInfo> myDeviceInfos = new HashSet<>();
    private Map<MidiDeviceInfo, MidiDevice> myDevices = new HashMap<>();

    private Stack<Closeable> myMidiToClose = new Stack<>();

    public void queueDevice(@NonNull MidiPortWrapper wrapper) {
        if (wrapper.getDeviceInfo() != null) {
            myDeviceInfos.add(wrapper.getDeviceInfo());
        }
    }

    public interface Completed {
        void action(MidiDeviceOpener opener);
    }

    public void execute(@NonNull MidiManager midiManager, final Completed callBack) {
        if (myDeviceInfos.isEmpty()) {
            // nothing to do, call immediately
            callBack.action(MidiDeviceOpener.this);
        } else {
            Handler handler = new Handler();

            int[] counter = new int [] {myDeviceInfos.size()};

            for (MidiDeviceInfo deviceInfo : myDeviceInfos) {
                midiManager.openDevice(deviceInfo, (MidiDevice device) -> {
                    if (device == null) {
                        Log.e(TAG, "Cannot open device " + deviceInfo);
                    }
                    myMidiToClose.push(device);
                    myDevices.put(deviceInfo, device);  // store for later
                    counter[0]--;
                    if (counter[0] == 0) {
                        // we have processed them all
                        callBack.action(MidiDeviceOpener.this);
                    }
                }, handler);
            }
        }
    }

    public MidiInputPort openInputPort(MidiPortWrapper wrapper) {
        if (wrapper.getDeviceInfo() != null) {
            MidiDevice device = myDevices.get(wrapper.getDeviceInfo());
            MidiInputPort inputPort = device.openInputPort(wrapper.getPortIndex());
            myMidiToClose.push(inputPort);
            return inputPort;
        } else {
            return null;
        }
    }

    public MidiOutputPort openOutputPort(MidiPortWrapper wrapper) {
        if (wrapper.getDeviceInfo() != null) {
            MidiDevice device = myDevices.get(wrapper.getDeviceInfo());
            MidiOutputPort outputPort = device.openOutputPort(wrapper.getPortIndex());
            myMidiToClose.push(outputPort);
            return outputPort;
        } else {
            return null;
        }
    }

    public void close() {
        myDeviceInfos.clear();
        myDevices.clear();

        for (Closeable device : myMidiToClose) {
            try {
                if (device != null) {
                    device.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
        myMidiToClose.clear();
    }

}
