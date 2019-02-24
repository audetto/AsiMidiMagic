package inc.andsoft.asimidimagic.midi;

import android.media.midi.MidiReceiver;
import android.util.Log;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;

import androidx.annotation.NonNull;

/**
 * Created by andrea on 28/01/18.
 */

public abstract class RemapHandler extends StartStopReceiver {
    private final static String TAG = "RemapHandler";

    private MidiReceiver myReceiver;
    private volatile boolean myRunning = false;

    public interface VelocityRemap {
        int map(int note, int channel, int velocity);
    }

    private VelocityRemap myRemapper = null;

    protected RemapHandler(@NonNull MidiReceiver receiver) {
        myReceiver = receiver;
    }

    public void setRemapper(VelocityRemap remapper) {
        myRemapper = remapper;
    }

    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {
        super.onSend(data, offset, count, timestamp);

        byte command = (byte) (data[offset] & MidiConstants.STATUS_COMMAND_MASK);
        switch (command) {
            case MidiConstants.STATUS_NOTE_ON: {
                noteOn(data, offset, count, timestamp);
                break;
            }
            case MidiConstants.STATUS_NOTE_OFF: {
                noteOff(data, offset, count, timestamp);
                break;
            }
            default: {
                myReceiver.send(data, offset, count, timestamp);
            }
        }
    }

    private void noteOn(@NonNull byte[] data, int offset, int count, long timestamp) throws IOException {
        byte velocity = data[offset + 2];
        if (velocity == 0) {
            noteOff(data, offset, count, timestamp);
        } else {
            // only remapping note ON
            if (myRunning && myRemapper != null) {
                int note = data[offset + 1];
                int channel = (data[offset] & MidiConstants.STATUS_CHANNEL_MASK);

                try {
                    int mapped = myRemapper.map(note, channel, velocity);

                    Log.d(TAG, "Remapped " + velocity + " to " + mapped);

                    if (mapped >= 0 && mapped <= 127) {
                        data[offset + 2] = (byte) mapped;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error " + e);
                }
            }
            myReceiver.send(data, offset, count, timestamp);
        }
    }

    private void noteOff(byte[] data, int offset, int count, long timestamp) throws IOException {
        myReceiver.send(data, offset, count, timestamp);
    }

    public void setRunning(boolean running) {
        myRunning = running;
    }

}
