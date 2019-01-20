package inc.andsoft.asimidimagic.midi;

import android.media.midi.MidiReceiver;
import android.util.SparseArray;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

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
                forward(data, offset, count, timestamp);
            }
        }
    }

    private void forward(byte[] data, int offset, int count, long timestamp) throws IOException {
        myReceiver.send(data, offset, count, timestamp);
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
            forward(data, offset, count, timestamp);
        }
    }

    private void noteOff(byte[] data, int offset, int count, long timestamp) throws IOException {
        byte note = data[offset + 1];
        int channel = (data[offset] & MidiConstants.STATUS_CHANNEL_MASK);
        ChannelData channelData = myChannels.get(channel);

        if (channelData.onCounters[note] == 0) {
            // must have lost some
            // or started after the Ons
            forward(data, offset, count, timestamp);
        } else {
            channelData.onCounters[note]--;
            channelData.offToSend.add(new Message(data, offset, count, timestamp));

            if (channelData.onCounters[note] == 0) {
                // this was the last OFF, send them all
                while (!channelData.offToSend.isEmpty()) {
                    Message msg = channelData.offToSend.remove();
                    forward(msg.data, msg.offset, msg.count, msg.timestamp);
                }
            }
        }
    }

    static class Message {
        byte[] data;
        int offset;
        int count;
        long timestamp;

        Message(byte[] data, int offset, int count, long timestamp) {
            this.data = data;
            this.offset = offset;
            this.count = count;
            this.timestamp = timestamp;
        }
    }

    static class ChannelData {
        int[] onCounters = new int[128];
        Queue<Message> offToSend = new LinkedList<>();
    }
}
