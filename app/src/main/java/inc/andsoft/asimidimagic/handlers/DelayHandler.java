package inc.andsoft.asimidimagic.handlers;

import android.media.midi.MidiInputPort;
import androidx.annotation.NonNull;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;

/**
 * Created by andrea on 28/01/18.
 */

public abstract class DelayHandler extends StartStopReceiver {
    static private long MILLIS_PER_NANO = 1000000;

    private MidiInputPort myInputPort;

    private boolean myRunning = false;

    // atomic as it is changed by the UI thread
    private volatile long myOnDelay = 0;
    private volatile long myOffDelay = 0;

    public DelayHandler(@NonNull MidiInputPort inputPort) {
        myInputPort = inputPort;
    }

    public void setOffDelay(long delayInMS) {
        myOffDelay = delayInMS * MILLIS_PER_NANO;
    }

    public void setOnDelay(long delayInMS) {
        myOnDelay = delayInMS * MILLIS_PER_NANO;
    }

    public void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {
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
                super.onSend(data, offset, count, timestamp);
            }
        }
    }

    private void noteOn(byte[] data, int offset, int count, long timestamp) throws IOException {
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

            myInputPort.send(data, offset, count, newTimestamp);
        }
    }

    private void noteOff(byte[] data, int offset, int count, long timestamp) throws IOException {
        long newTimestamp;
        if (myRunning) {
            newTimestamp = timestamp + myOnDelay + myOffDelay;
        } else {
            newTimestamp = timestamp;
        }

        myInputPort.send(data, offset, count, newTimestamp);
    }

    public void setRunning(boolean running) {
        myRunning = running;
    }

}
