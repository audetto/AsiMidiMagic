package inc.andsoft.asimidimagic.midi;

import android.media.midi.MidiReceiver;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;

/**
 * Created by andrea on 28/01/18.
 */

abstract public class StartStopReceiver extends MidiReceiver {

    private boolean myPedalPressed = false;

    private static final byte CC_SOSTENUTO = (byte) 66;

    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {
        byte command = (byte) (data[offset] & MidiConstants.STATUS_COMMAND_MASK);

        switch (command) {
            case MidiConstants.STATUS_CONTROL_CHANGE: {
                byte control = data[offset + 1];
                if (control == CC_SOSTENUTO) {
                    byte value = data[offset + 2];
                    sostenutoPedal(value);
                }
                break;
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
