package inc.andsoft.asimidimagic.handlers;

import android.media.midi.MidiReceiver;

import com.mobileer.miditools.EventScheduler;

import java.io.IOException;

public class MidiTimeScheduler extends MidiReceiver {
    private MidiReceiver myReceiver;
    private EventScheduler myEventScheduler;
    private volatile boolean myThreadEnabled;
    private Thread myThread;

    public static class MidiEvent extends EventScheduler.SchedulableEvent {
        public int count;
        public byte[] data;

        private MidiEvent(byte[] msg, int offset, int count, long timestamp) {
            super(timestamp);
            data = new byte[count];
            System.arraycopy(msg, offset, data, 0, count);
            this.count = count;
        }

        @Override
        public String toString() {
            String text = "Event: ";
            for (int i = 0; i < count; i++) {
                text += data[i] + ", ";
            }
            return text;
        }
    }

    class EventThread implements Runnable {
        @Override
        public void run() {
            while (myThreadEnabled) {
                try {
                    MidiEvent event = (MidiEvent) myEventScheduler.waitNextEvent();
                    try {
                        myReceiver.send(event.data, 0, event.count, event.getTimestamp());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    // OK, this is how we stop the thread.
                }
            }
        }
    }


    public MidiTimeScheduler(MidiReceiver receiver) {
        myReceiver = receiver;
    }

    @Override
    public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
        EventScheduler.SchedulableEvent event = new MidiEvent(msg, offset, count, timestamp);
        myEventScheduler.add(event);
    }

    public void start() {
        myEventScheduler = new EventScheduler();
        myThread = new Thread(new EventThread());
        myThread.start();
        myThreadEnabled = true;
    }

    public void stop() {
        myThreadEnabled = false;
        if (myThread != null) {
            try {
                myThread.interrupt();
                myThread.join(500);
            } catch (InterruptedException e) {
                // OK, just stopping safely.
            }
            myThread = null;
            myEventScheduler = null;
        }
    }

}
