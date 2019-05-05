package inc.andsoft.asimidimagic.midi;

import android.media.midi.MidiReceiver;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;

public class MidiFilter extends MidiReceiver {
    private MidiReceiver myReceiver;

    public int myOnCounter;
    public int myOffCounter;

    public MidiFilter(MidiReceiver receiver) {
        myReceiver = receiver;
    }

    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp) throws IOException {
        int command = (data[offset] & MidiConstants.STATUS_COMMAND_MASK);

        switch (command) {
            case MidiConstants.STATUS_NOTE_ON: {
                myOnCounter++;
                break;
            }
            case MidiConstants.STATUS_NOTE_OFF: {
                myOffCounter++;
                break;
            }
        }

        switch (command) {
            case MidiConstants.STATUS_NOTE_ON:
            case MidiConstants.STATUS_NOTE_OFF:
            case MidiConstants.STATUS_POLYPHONIC_AFTERTOUCH:
            case MidiConstants.STATUS_CONTROL_CHANGE:
            case MidiConstants.STATUS_PROGRAM_CHANGE:
            case MidiConstants.STATUS_CHANNEL_PRESSURE:
            case MidiConstants.STATUS_PITCH_BEND: {
                myReceiver.send(data, offset, count, timestamp);
                break;
            }
            default: {
                // we drop everything else
            }
        }
    }

}
