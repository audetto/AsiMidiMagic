package inc.andsoft.asimidimagic.midi;

import android.media.midi.MidiReceiver;
import android.util.SparseArray;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;

public class MidiCountedOnOff extends MidiReceiver {
    private MidiReceiver myReceiver;

    private SparseArray<ChannelData> myChannels = new SparseArray<>();

    public MidiCountedOnOff(MidiReceiver receiver) {
        myReceiver = receiver;
    }

    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp) throws IOException {
        int command = (data[offset] & MidiConstants.STATUS_COMMAND_MASK);
        int channel = (data[offset] & MidiConstants.STATUS_CHANNEL_MASK);
        if (myChannels.get(channel) == null) {
            myChannels.put(channel, new ChannelData());
        }

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
            int channel = (data[offset] & MidiConstants.STATUS_CHANNEL_MASK);
            ChannelData channelData = myChannels.get(channel);

            byte note = data[offset + 1];
            channelData.onCounters[note]++;
            myReceiver.onSend(data, offset, count, timestamp);
        }
    }

    private void noteOff(byte[] data, int offset, int count, long timestamp) throws IOException {
        byte note = data[offset + 1];
        int channel = (data[offset] & MidiConstants.STATUS_CHANNEL_MASK);
        ChannelData channelData = myChannels.get(channel);

        if (channelData.onCounters[note] == 0) {
            // must have lost some
            // or started after the Ons
            myReceiver.onSend(data, offset, count, timestamp);
        } else {
            channelData.onCounters[note]--;
            channelData.offToSend[note]++;

            if (channelData.onCounters[note] == 0) {
                while (channelData.offToSend[note] > 0) {
                    myReceiver.onSend(data, offset, count, timestamp);
                    channelData.offToSend[note]--;
                }
            }
        }
    }

    static class ChannelData {
        int[] onCounters = new int[128];
        int[] offToSend = new int[128];
    }
}
