package inc.andsoft.asimidimagic.midi;

import android.media.midi.MidiReceiver;

import com.mobileer.miditools.EventScheduler;

import java.io.IOException;

public class MidiTimeScheduler extends MidiReceiver {
    private MidiReceiver myReceiver;
    private EventScheduler myEventScheduler;
    private volatile boolean myThreadEnabled;
    private Thread myThread;

    public static class MidiEvent extends EventScheduler.SchedulableEvent {
        int count;
        public byte[] data;

        private MidiEvent(byte[] msg, int offset, int count, long timestamp) {
            super(timestamp);
            data = new byte[count];
            System.arraycopy(msg, offset, data, 0, count);
            this.count = count;
        }
    }

    class EventProcessing implements Runnable {
        @Override
        public void run() {
            while (myThreadEnabled) {
                try {
                    MidiEvent event = (MidiEvent) myEventScheduler.waitNextEvent();
                    try {
                        myReceiver.send(event.data, 0, event.count, 0);
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

        myEventScheduler = new EventScheduler();
        myThread = new Thread(new EventProcessing());
        myThread.start();
        myThreadEnabled = true;
    }

    @Override
    public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
        EventScheduler.SchedulableEvent event = new MidiEvent(msg, offset, count, timestamp);
        myEventScheduler.add(event);  // this is synchronised
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
