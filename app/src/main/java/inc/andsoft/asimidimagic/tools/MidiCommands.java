package inc.andsoft.asimidimagic.tools;

import android.media.midi.MidiReceiver;
import android.util.Log;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;

public class MidiCommands {
    private final static String TAG = "Utilities";

    public static void doSend(@NonNull MidiReceiver receiver, byte[] msg, int offset, int count) {
        try {
            receiver.send(msg, offset, count);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public static void allNotesOff(@NonNull MidiReceiver receiver, List<Integer> channels) {
        for (int channel : channels) {
            allNotesOff(receiver, channel);
        }
    }

    public static void allNotesOff(@NonNull MidiReceiver receiver, int channel) {
        byte[] buffer = new byte[3];
        buffer[0] = (byte) (MidiConstants.STATUS_CONTROL_CHANGE + channel);
        buffer[1] = MidiConstants.ALL_NOTES_OFF;
        buffer[2] = 0;
        doSend(receiver, buffer, 0, buffer.length);
    }

    public static void localControl(@NonNull MidiReceiver receiver, List<Integer> channels, boolean value) {
        for (int channel : channels) {
            localControl(receiver, channel, value);
        }
    }

    public static void localControl(@NonNull MidiReceiver receiver, int channel, boolean value) {
        byte[] buffer = new byte[3];
        buffer[0] = (byte) (MidiConstants.STATUS_CONTROL_CHANGE + channel);
        buffer[1] = MidiConstants.LOCAL_CONTROL;
        buffer[2] = value ? (byte)127 : 0;
        doSend(receiver, buffer, 0, buffer.length);
    }

    public static void sustainPedal(@NonNull MidiReceiver receiver, List<Integer> channels, byte value) {
        for (int channel : channels) {
            sustainPedal(receiver, channel, value);
        }
    }

    public static void sustainPedal(@NonNull MidiReceiver receiver, int channel, byte value) {
        byte[] buffer = new byte[3];
        buffer[0] = (byte) (MidiConstants.STATUS_CONTROL_CHANGE + channel);
        buffer[1] = MidiConstants.CC_SUSTAIN;
        buffer[2] = value;
        doSend(receiver, buffer, 0, buffer.length);
    }

    public enum MultiTimbre {
        OFF, ON1, ON2
    }

    private static byte[] kawaiCA7898SysEx(byte b4, byte d1, byte d2, byte d3) {
        byte[] buffer = {(byte)0xF0, (byte)0x40, 0, b4, (byte)0x04, (byte)0x02, d1, d2, d3, (byte)0xF7};
        return buffer;
    }

    public static void multiTimbre(@NonNull MidiReceiver receiver, MultiTimbre value) {
        byte b4 = (byte)0x30;
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
        byte[] data = kawaiCA7898SysEx(b4, d1, d2, d3);
        doSend(receiver, data, 0, data.length);
    }
}
