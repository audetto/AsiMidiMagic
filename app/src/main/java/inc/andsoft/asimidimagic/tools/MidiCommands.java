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

    public enum MultiTimbre {
        OFF, ON1, ON2
    };

    private static byte[] KawayCA7898SysEx(byte b4, byte d1, byte d2, byte d3) {
        byte[] buffer = {(byte)0xF0, (byte)0x40, 0, b4, (byte)0x04, (byte)0x02, d1, d2, d3, (byte)0xF7};
        return buffer;
    }

    public static void multiTimbre(@NonNull MidiReceiver receiver, MultiTimbre value) throws IOException {
        byte d1 = 0;
        byte d2 = 0;
        byte d3 = 0;

        switch (value) {
            case ON1:
                d1 = 1;
                d2 = 0;
                d3 = 0;
                break;
            case ON2:
                d1 = 2;
                d2 = 0;
                d3 = 0;
                break;
        }
        byte[] data = KawayCA7898SysEx((byte)0x30, d1, d2, d3);
        receiver.send(data, 0, data.length);
    }
}
