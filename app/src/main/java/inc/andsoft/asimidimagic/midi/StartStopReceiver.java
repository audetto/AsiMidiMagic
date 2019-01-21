package inc.andsoft.asimidimagic.midi;

import android.media.midi.MidiReceiver;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;

import androidx.annotation.NonNull;

/**
 * Created by andrea on 28/01/18.
 */

abstract public class StartStopReceiver extends MidiReceiver {

    MidiReceiver myReceiver;
    private boolean myPedalPressed = false;

    private static final byte CC_SOSTENUTO = (byte) 66;

    StartStopReceiver(@NonNull MidiReceiver receiver) {
        myReceiver = receiver;
    }

    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {
        byte command = (byte) (data[offset] & MidiConstants.STATUS_COMMAND_MASK);
        switch (command) {
            case MidiConstants.STATUS_CONTROL_CHANGE:
                byte control = data[offset + 1];
                if (control == CC_SOSTENUTO) {
                    byte value = data[offset + 2];
                    sostenutoPedal(value);
                    return;
                } // otherwise pass it
            default: {
                myReceiver.send(data, offset, count, timestamp);
            }
        }
    }

    abstract public void onPedalChange(boolean value);

    private void fireRunningChange() {
        onPedalChange(myPedalPressed);
    }

    private void sostenutoPedal(byte value) {
        boolean newPressed = value >= 64;
        if (newPressed != myPedalPressed) {
            myPedalPressed = newPressed;
            fireRunningChange();
        }
    }

}
