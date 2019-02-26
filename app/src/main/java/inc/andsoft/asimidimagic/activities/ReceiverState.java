package inc.andsoft.asimidimagic.activities;

import android.media.midi.MidiReceiver;

import java.io.Closeable;

public interface ReceiverState extends Closeable {
    MidiReceiver getReceiver();
}
