package inc.andsoft.asimidimagic.midi;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import inc.andsoft.asimidimagic.tools.NoteSequence;


abstract public class MidiRecorder extends StartStopReceiver {

    public enum State {
        RECORDING,
        COMPLETE
    }

    private Integer myChannel;
    private boolean myRecording = true;

    private List<Long> myTimes = new ArrayList<>();
    private List<NoteSequence.Note> myNotes = new ArrayList<>();

    private static final String MSG_FIRST_NOTE = "Waiting for first note";
    private static final String RECORDING = "Recording: %d notes";
    private static final String SEQUENCE = "Sequence: %d notes";

    abstract public void onSequence(NoteSequence sequence);

    abstract public void onChangeState(@NonNull State state, @NonNull String message);

    protected MidiRecorder() {
        initialise();
    }

    private void initialise() {
        onChangeState(State.RECORDING, MSG_FIRST_NOTE);
        myRecording = true;
        myTimes.clear();
        myNotes.clear();
    }

    @Override
    public void onPedalChange(boolean value)
    {
        if (value) {
            if (myRecording) {
                myRecording = false;

                if (!myTimes.isEmpty()) {
                    long startTime = myTimes.get(0);
                    long NANOS_PER_SECOND = 1000000000L;

                    List<Double> times = myTimes.stream()
                            .map(x -> (double) (x - startTime) / NANOS_PER_SECOND)
                            .collect(Collectors.toList());

                    String message = String.format(Locale.getDefault(), SEQUENCE, myNotes.size());
                    onChangeState(State.COMPLETE, message);

                    NoteSequence notes = new NoteSequence(myNotes, times);
                    onSequence(notes);
                }
            } else {
                initialise();
            }
        }
    }

    private void noteOn(@NonNull byte[] data, int offset, int count, long timestamp) throws IOException {
        if (myRecording) {
            int note = data[offset + 1];
            int velocity = data[offset + 2];

            myTimes.add(timestamp);
            myNotes.add(new NoteSequence.Note(note, velocity));
            String message = String.format(Locale.getDefault(), RECORDING, myNotes.size());
            onChangeState(State.RECORDING, message);
        }
    }

    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp) throws IOException {
        super.onSend(data, offset, count, timestamp);

        int command = (data[offset] & MidiConstants.STATUS_COMMAND_MASK);
        int channel = (data[offset] & MidiConstants.STATUS_CHANNEL_MASK);

        switch (command) {
            case MidiConstants.STATUS_NOTE_ON: {
                byte velocity = data[offset + 2];
                if (velocity > 0) {
                    if (myChannel == null) {
                        myChannel = channel;
                    }
                    if (myChannel == channel) {
                        noteOn(data, offset, count, timestamp);
                    }
                }
                break;
            }
        }
    }

}
