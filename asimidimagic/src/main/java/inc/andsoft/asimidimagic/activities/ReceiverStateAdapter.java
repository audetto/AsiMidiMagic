package inc.andsoft.asimidimagic.activities;

import android.media.midi.MidiReceiver;

import java.io.IOException;

public class ReceiverStateAdapter<R extends MidiReceiver> implements ReceiverState {
    public R myReceiver;

    public ReceiverStateAdapter(R receiver) {
        myReceiver = receiver;
    }

    @Override
    public MidiReceiver getReceiver() {
        return myReceiver;
    }

    @Override
    public void close() throws IOException {

    }

}
