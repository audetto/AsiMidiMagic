package inc.andsoft.asimidimagic.services;

import android.media.midi.MidiDeviceService;
import android.media.midi.MidiReceiver;
import android.util.Log;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by andrea on 13/01/18.
 */

public class SynthMIDIServer extends MidiDeviceService {
    private static final String TAG = "SynthMIDIServer";

    private MidiReceiver myInputPort = new LogReceiver();
    private byte[] buffer = new byte[32];
    private List<Thread> myThreads;

    private final long NANOS_PER_SECOND = 1000000000L;
    private final long NANOS_PER_MS = 1000000L;
    private long myReferenceTime;
    private SimpleDateFormat myDateFormat = new SimpleDateFormat("hh:mm:ss.SSS",
            Locale.getDefault());

    private class LogReceiver extends MidiReceiver {
        public void onSend(byte[] data, int offset, int count, long timestamp) throws IOException
        {
            long now = System.nanoTime();
            Log.d(TAG, "Received = " + getTimeStr(timestamp) + ", note = " + data[offset + 1]
                    + ", command = " + data[offset] + " @ " + getTimeStr(now));
        }
    }

    private String getTimeStr(long timestamp) {
        long delta = timestamp - myReferenceTime;
        long ms = delta / NANOS_PER_MS;
        Date date = new Date(ms);
        return myDateFormat.format(date);
    }

    private void noteOff(MidiReceiver output, byte channel, byte note, byte velocity, long timestamp) throws IOException {
        int numBytes = 0;
        buffer[numBytes++] = (byte) (MidiConstants.STATUS_NOTE_OFF + (channel - 1)); // note on
        buffer[numBytes++] = note;
        buffer[numBytes++] = velocity;
        output.send(buffer, 0, numBytes, timestamp);
    }

    private void noteOn(MidiReceiver output, byte channel, byte note, byte velocity, long timestamp) throws IOException {
        int numBytes = 0;
        buffer[numBytes++] = (byte) (MidiConstants.STATUS_NOTE_ON + (channel - 1)); // note on
        buffer[numBytes++] = note;
        buffer[numBytes++] = velocity;
        output.send(buffer, 0, numBytes, timestamp);
    }

    @Override
    public void onClose() {
        for (Thread thread : myThreads) {
            thread.interrupt();
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // someone interrupted this thread
                    // we have to join again
                }
            }
        }
        myThreads.clear();
    }

    class ScaleRunnable implements Runnable {
        private MidiReceiver myReceiver;
        private int myStartNote;
        private long myPeriod;
        private int myOctaves;

        ScaleRunnable(MidiReceiver receiver, int startNote, long period, int octaves) {
            this.myReceiver = receiver;
            this.myStartNote = startNote;
            this.myPeriod = period;
            this.myOctaves = octaves;
        }

        public void run() {
            Integer up[] = {2, 2, 1, 2, 2, 2, 1};
            Integer down[] = {-1, -2, -2, -2, -1, -2, -2};

            List<Integer> listUp = new ArrayList<>(Arrays.asList(up));
            List<Integer> listDown = new ArrayList<>(Arrays.asList(down));

            List<Integer> increments = new ArrayList<>();

            for (int i = 0; i < myOctaves; ++i) {
                increments.addAll(0, listUp);
                increments.addAll(listDown);
            }
            increments.add(0, 0);

            ThreadLocalRandom random = ThreadLocalRandom.current();

            try {
                Thread.sleep(1000);
                int note = myStartNote;
                for (byte i = 0; i < increments.size(); ++i) {
                    int increment = increments.get(i);
                    note += increment;

                    int rnd = random.nextInt(-40, 40);
                    Thread.sleep(myPeriod + rnd);

                    byte velocity = (byte) 100;
                    byte channel = (byte) 3;

                    long now = System.nanoTime();
                    noteOn(myReceiver, channel, (byte)note, velocity, now);

                    long future = now + 2 * NANOS_PER_SECOND;

                    noteOff(myReceiver, channel, (byte)note, velocity, future);

                    Log.d(TAG, "Sent =     " + getTimeStr(now) + ", note = " + note + ", id = " + i);
                }
            } catch (IOException | InterruptedException e) {
                Log.d(TAG, e.toString());
            }
        }
    }

    class NotesRunnable implements Runnable {
        private MidiReceiver myReceiver;

        NotesRunnable(MidiReceiver receiver) {
            this.myReceiver = receiver;
        }

        public void run() {
            try {
                int i = 0;
                while (true) {
                    Thread.sleep(200);

                    byte note = (byte) (60 + (i % 12));
                    byte velocity = (byte) 100;
                    byte channel = (byte) 3;

                    long now = System.nanoTime();
                    noteOn(myReceiver, channel, note, velocity, now);

                    long future = now + 2 * NANOS_PER_SECOND;

                    noteOff(myReceiver, channel, note, velocity, future);

                    Log.d(TAG, "Sent =     " + getTimeStr(now) + ", note = " + note + ", id = " + i);
                    ++i;
                }
            } catch (IOException | InterruptedException e) {
                Log.d(TAG, e.toString());
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MidiReceiver[] outputs = getOutputPortReceivers();
        myReferenceTime = System.nanoTime();

        myThreads = new ArrayList<>();

        if (outputs.length >= 2) {
            myThreads.add(new Thread(new ScaleRunnable(outputs[0], 44, 300, 2)));
            myThreads.add(new Thread(new ScaleRunnable(outputs[0], 68, 200, 3)));

            myThreads.add(new Thread(new NotesRunnable(outputs[1])));
        }

        for (Thread thread : myThreads) {
            thread.start();
        }
    }

    @Override
    public MidiReceiver[] onGetInputPortReceivers() {
        return new MidiReceiver[] {myInputPort};
    }

}
