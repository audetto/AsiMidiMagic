package inc.andsoft.asimidimagic.services;

import android.media.midi.MidiDeviceService;
import android.media.midi.MidiReceiver;
import android.util.Log;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by andrea on 13/01/18.
 */

public class SynthMIDIServer extends MidiDeviceService {
    private static final String TAG = "SynthMIDIServer";

    private MidiReceiver myInputPort = new LogReceiver();
    private byte[] buffer = new byte[32];
    private Thread myThread;

    private long NANOS_PER_SECOND = 1000000000L;
    private long NANOS_PER_MS = 1000000L;
    private long myReferenceTime;
    private SimpleDateFormat myDateFormat = new SimpleDateFormat("hh:mm:ss.SSS");

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
        if (myThread != null) {
            myThread.interrupt();
            while (myThread.isAlive()) {
                try {
                    myThread.join();
                } catch (InterruptedException e) {
                    // someone interrupted this thread
                    // we have to join again
                }
            }
            myThread = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MidiReceiver[] outputs = getOutputPortReceivers();
        myReferenceTime = System.nanoTime();

        if (outputs.length > 0 && outputs[0] != null) {
            myThread = new Thread(() -> {
                try {
                    for (byte i = 0; i < 120; ++i) {
                        Thread.sleep(1000);

                        byte note = (byte) (60 + (i % 12));
                        byte velocity = (byte) 100;
                        byte channel = (byte) 3;

                        long now = System.nanoTime();
                        noteOn(outputs[0], channel, note, velocity, now);

                        long future = now + 2 * NANOS_PER_SECOND;

                        noteOff(outputs[0], channel, note, velocity, future);

                        Log.d(TAG, "Sent =     " + getTimeStr(now) + ", note = " + note + ", id = " + i);
                    }
                } catch (IOException | InterruptedException e) {
                    Log.d(TAG, e.toString());
                }
            });
            myThread.start();
        }
    }

    @Override
    public MidiReceiver[] onGetInputPortReceivers() {
        return new MidiReceiver[] {myInputPort};
    }

}
