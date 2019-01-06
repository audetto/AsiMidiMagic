package inc.andsoft.asimidimagic.handlers;

import android.media.midi.MidiReceiver;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;

/**
 * Created by andrea on 28/01/18.
 */

abstract public class StartStopReceiver extends MidiReceiver {

    private boolean myPedalPressed = false;

    private static final byte CC_SOSTENUTO = (byte) 66;
    private static final byte ALL_SOUND_OFF = (byte) 120;

    public static void allNotesOff(MidiReceiver input) throws IOException {
        byte[] buffer = new byte[3];
        buffer[0] = MidiConstants.STATUS_CONTROL_CHANGE;
        buffer[1] = ALL_SOUND_OFF;
        buffer[2] = 0;
        input.send(buffer, 0, buffer.length);
    }

    public void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {
        byte command = (byte) (data[offset] & MidiConstants.STATUS_COMMAND_MASK);
//        int channel = (byte) (data[offset] & MidiConstants.STATUS_CHANNEL_MASK);
        switch (command) {
            case MidiConstants.STATUS_CONTROL_CHANGE:
                byte control = data[offset + 1];
                if (control == CC_SOSTENUTO) {
                    byte value = data[offset + 2];
                    sostenutoPedal(value);
                }
                break;
        }
    }

    abstract public void onPedalChange(boolean value);

    public void fireRunningChange() {
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
