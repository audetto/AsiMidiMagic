package inc.andsoft.asimidimagic.tools;

import android.media.midi.MidiReceiver;
import android.util.Log;

import com.mobileer.miditools.MidiConstants;

import java.io.Closeable;
import java.io.IOException;
import java.util.Locale;

import androidx.annotation.NonNull;

public class Utilities {
    private final static String TAG = "Utilities";

    private static final byte ALL_NOTES_OFF = (byte) 123;

    /**
     * Send ALL_NOTES_OFF to all 15 MIDI channels
     * @param receiver input device to send data to
     * @throws IOException
     */
    public static void allNotesOff(@NonNull MidiReceiver receiver) throws IOException {
        for (int i = 0; i < MidiConstants.MAX_CHANNELS; ++i) {
            byte[] buffer = new byte[3];
            buffer[0] = (byte) (MidiConstants.STATUS_CONTROL_CHANGE + i);
            buffer[1] = ALL_NOTES_OFF;
            buffer[2] = 0;
            receiver.send(buffer, 0, buffer.length);
        }
    }

    /**
     * Close and log any exception
     * @param object object to close
     */
    public static void doClose(@NonNull Closeable object) {
        try {
            Log.d(TAG, "Closing " + object);
            object.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public static String getPercentageFormat(double number) {
        return String.format(Locale.getDefault(), "%5.2f", number * 100);
    }
}
