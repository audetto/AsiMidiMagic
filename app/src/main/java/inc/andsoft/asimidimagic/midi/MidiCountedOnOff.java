package inc.andsoft.asimidimagic.midi;

import android.media.midi.MidiReceiver;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;

public class MidiCountedOnOff extends MidiReceiver {
    private MidiReceiver myReceiver;

    private int[] myOnCounters = new int[128];
    private int[] myOffToSend = new int[128];

    public MidiCountedOnOff(MidiReceiver receiver) {
        myReceiver = receiver;
    }

    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp) throws IOException {
        byte command = (byte) (data[offset] & MidiConstants.STATUS_COMMAND_MASK);
        switch (command) {
            case MidiConstants.STATUS_NOTE_ON: {
                noteOn(data, offset, count, timestamp);
                break;
            }
            case MidiConstants.STATUS_NOTE_OFF: {
                noteOff(data, offset, count, timestamp);
                break;
            }
            default: {
                myReceiver.onSend(data, offset, count, timestamp);
            }
        }
    }

    private void noteOn(byte[] data, int offset, int count, long timestamp) throws IOException {
        byte velocity = data[offset + 2];
        if (velocity == 0) {
            noteOff(data, offset, count, timestamp);
        } else {
            byte note = data[offset + 1];
            myOnCounters[note]++;
            myReceiver.onSend(data, offset, count, timestamp);
        }
    }

    private void noteOff(byte[] data, int offset, int count, long timestamp) throws IOException {
        byte note = data[offset + 1];

        if (myOnCounters[note] == 0) {
            // must have lost some
            // or started after the Ons
            myReceiver.onSend(data, offset, count, timestamp);
        } else {
            myOnCounters[note]--;
            myOffToSend[note]++;

            if (myOnCounters[note] == 0) {
                while (myOffToSend[note] > 0) {
                    myReceiver.onSend(data, offset, count, 0);
                    myOffToSend[note]--;
                }
            }
        }
    }
}
