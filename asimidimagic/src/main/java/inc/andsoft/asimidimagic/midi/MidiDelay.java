package inc.andsoft.asimidimagic.midi;

import android.media.midi.MidiReceiver;

import androidx.annotation.NonNull;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;

/**
 * Created by andrea on 28/01/18.
 */

public abstract class MidiDelay extends StartStopReceiver {
    static private long MILLIS_PER_NANO = 1000000;

    private MidiReceiver myReceiver;
    private volatile boolean myRunning = false;

    // atomic as it is changed by the UI thread
    private volatile long myOnDelay = 0;
    private volatile long myOffDelay = 0;

    protected MidiDelay(@NonNull MidiReceiver receiver) {
        myReceiver = receiver;
    }

    public void setOffDelay(long delayInMS) {
        myOffDelay = delayInMS * MILLIS_PER_NANO;
    }

    public void setOnDelay(long delayInMS) {
        myOnDelay = delayInMS * MILLIS_PER_NANO;
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
            long newTimestamp;
            if (myRunning) {
                newTimestamp = timestamp + myOnDelay;
            } else {
                newTimestamp = timestamp;
            }

            myReceiver.send(data, offset, count, newTimestamp);
        }
    }

    private void noteOff(byte[] data, int offset, int count, long timestamp) throws IOException {
        long newTimestamp;
        if (myRunning) {
            newTimestamp = timestamp + myOnDelay + myOffDelay;
        } else {
            newTimestamp = timestamp;
        }

        myReceiver.send(data, offset, count, newTimestamp);
    }

    public void setRunning(boolean running) {
        myRunning = running;
    }

}
