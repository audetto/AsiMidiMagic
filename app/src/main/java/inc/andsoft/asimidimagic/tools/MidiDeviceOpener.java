package inc.andsoft.asimidimagic.tools;

import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.util.Log;

import com.mobileer.miditools.MidiPortWrapper;

import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by andrea on 28/01/18.
 */

public class MidiDeviceOpener implements Closeable {
    private final static String TAG = "MidiDeviceOpener";

    private Set<MidiDeviceInfo> myDeviceInfos = new HashSet<>();
    private Map<MidiDeviceInfo, MidiDevice> myDevices = new HashMap<>();

    private Stack<Closeable> myMidiToClose = new Stack<>();

    public void queueDevice(@NonNull MidiPortWrapper wrapper) {
        MidiDeviceInfo info = wrapper.getDeviceInfo();
        if (info != null && !myDevices.containsKey(info)) {
            myDeviceInfos.add(wrapper.getDeviceInfo());
        }
    }

    public void execute(@NonNull MidiManager midiManager, Consumer<MidiDeviceOpener> callBack) {
        if (myDeviceInfos.isEmpty()) {
            // nothing to do, call immediately
            callBack.accept(MidiDeviceOpener.this);
        } else {
            Handler handler = new Handler(); // to run in this thread

            AtomicInteger counter = new AtomicInteger(myDeviceInfos.size());

            for (MidiDeviceInfo deviceInfo : myDeviceInfos) {
                MidiManager.OnDeviceOpenedListener listener = (MidiDevice device) -> {
                    if (device == null) {
                        Log.e(TAG, "Cannot open device " + deviceInfo);
                    } else {
                        myMidiToClose.push(device);
                    }
                    myDevices.put(deviceInfo, device);  // store for later

                    int remaining = counter.decrementAndGet();
                    if (remaining == 0) {
                        // we have processed them all
                        callBack.accept(MidiDeviceOpener.this);
                    }
                };

                try {
                    midiManager.openDevice(deviceInfo, listener, handler);
                } catch (Exception e) {
                    Log.e(TAG, "Exception " + e + " while opening device " + deviceInfo);
                    // call it manually here
                    listener.onDeviceOpened(null);
                }
            }
            myDeviceInfos.clear();
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

    @Override
    public void close() {
        myDeviceInfos.clear();
        myDevices.clear();

        myMidiToClose.forEach(Utilities::doClose);
        myMidiToClose.clear();
    }

}
