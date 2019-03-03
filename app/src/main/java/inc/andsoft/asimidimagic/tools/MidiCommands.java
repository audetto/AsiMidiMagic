package inc.andsoft.asimidimagic.tools;

import android.media.midi.MidiReceiver;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;

public class MidiCommands {
    private static final byte ALL_NOTES_OFF = (byte) 123;
    private static final byte LOCAL_CONTROL = (byte) 122;

    public static void allNotesOff(@NonNull MidiReceiver receiver, List<Integer> channels) throws IOException {
        for (int channel : channels) {
            allNotesOff(receiver, channel);
        }
    }

    public static void allNotesOff(@NonNull MidiReceiver receiver, int channel) throws IOException {
        byte[] buffer = new byte[3];
        buffer[0] = (byte) (MidiConstants.STATUS_CONTROL_CHANGE + channel);
        buffer[1] = ALL_NOTES_OFF;
        buffer[2] = 0;
        receiver.send(buffer, 0, buffer.length);
    }

    public static void localControl(@NonNull MidiReceiver receiver, List<Integer> channels, boolean value) throws IOException {
        for (int channel : channels) {
            localControl(receiver, channel, value);
        }
    }

    public static void localControl(@NonNull MidiReceiver receiver, int channel, boolean value) throws IOException {
        byte[] buffer = new byte[3];
        buffer[0] = (byte) (MidiConstants.STATUS_CONTROL_CHANGE + channel);
        buffer[1] = LOCAL_CONTROL;
        buffer[2] = value ? (byte)127 : 0;
        receiver.send(buffer, 0, buffer.length);
    }
}
