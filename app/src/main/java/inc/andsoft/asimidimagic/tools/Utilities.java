package inc.andsoft.asimidimagic.tools;

import android.media.midi.MidiReceiver;
import android.util.Log;

import com.mobileer.miditools.MidiConstants;

import java.io.Closeable;
import java.io.IOException;

import androidx.annotation.NonNull;

public class Utilities {
    private final static String TAG = "Utilities";

    private static final String[] NOTE_NAMES = {
            "C", "C\u266f", "D", "E\u266d", "E", "F", "F\u266d", "G", "G\u266f", "A", "B\u266d", "B"};

    /**
     *
     * @param note MIDI note number
     * @return String representation
     */
    public static String getNoteName(int note) {
        int octave = note / 12 - 1;
        int grade = note % 12;
        return NOTE_NAMES[grade] + String.valueOf(octave);
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


}
