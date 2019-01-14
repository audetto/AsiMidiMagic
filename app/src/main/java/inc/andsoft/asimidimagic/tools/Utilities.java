package inc.andsoft.asimidimagic.tools;

import android.media.midi.MidiReceiver;
import android.util.Log;

import com.mobileer.miditools.MidiConstants;

import java.io.Closeable;
import java.io.IOException;

public class Utilities {
    private final static String TAG = "Utilities";

    private static final byte ALL_SOUND_OFF = (byte) 120;

    public static void allNotesOff(MidiReceiver input) throws IOException {
        byte[] buffer = new byte[3];
        buffer[0] = MidiConstants.STATUS_CONTROL_CHANGE;
        buffer[1] = ALL_SOUND_OFF;
        buffer[2] = 0;
        input.send(buffer, 0, buffer.length);
    }

    public static void doClose(Closeable object) {
        try {
            Log.d(TAG, "Closing " + object);
            object.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }
}
