package inc.andsoft.asimidimagic.midi;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import inc.andsoft.asimidimagic.tools.Scale;


class ScaleStorage {
    int myFirstNote;
    int myLastNote;
    int myValidSign;
    List<Integer> myNotes = new ArrayList<>();
    private List<Long> myTimes = new ArrayList<>();

    void addNote(int note, long timestamp) {
        myValidSign = 0;
        myLastNote = note;
        myNotes.add(note);
        myTimes.add(timestamp);
    }

    void copy(@NonNull ScaleStorage other) {
        myFirstNote = other.myFirstNote;
        myLastNote = other.myLastNote;
        myNotes.clear();
        myTimes.clear();
        myNotes.addAll(other.myNotes);
        myTimes.addAll(other.myTimes);
    }

    boolean isComplete() {
        return (myNotes.size() >= 2) && (myFirstNote == myLastNote);
    }

    boolean isValid(int note) {
        boolean ok = !isComplete();
        if (myValidSign != 0) {
            int noteSign = Integer.signum(note - myFirstNote);
            ok = (myValidSign * noteSign) != -1;
        }
        return ok;
    }

    Scale getScale() {
        long startTime = myTimes.get(0);
        long NANOS_PER_SECOND = 1000000000L;

        List<Double> times = myTimes.stream()
                .map(x -> (double)(x - startTime) / NANOS_PER_SECOND)
                .collect(Collectors.toList());

        return new Scale(myNotes, times);
    }
}

abstract public class MidiScales extends StartStopReceiver {
    public enum State {
        FIRST_NOTE,
        SECOND_NOTE,
        NOTES,
        COMPLETE
    }

    private State myState;

    private ScaleStorage mySecondScale;
    private ScaleStorage myFirstScale;

    private Integer myChannel;

    private static final String MSG_FIRST_NOTE = "Waiting for first note";
    private static final String FORMAT_ROOT_NOTE = "Root note: %d";
    private static final String FORMAT_TWO_SCALES = "Scales: 2nd = %d (%d) - 1st = %d (%d)";
    private static final String FORMAT_COMPLETE = "Complete, notes: 2nd = %d, 1st = %d";

    protected MidiScales() {
        initialise();
    }

    public void onPedalChange(boolean value)
    {
        if (value) {
            initialise();
        }
    }

    private void initialise() {
        changeState(State.FIRST_NOTE, MSG_FIRST_NOTE);
        mySecondScale = new ScaleStorage();
        myFirstScale = new ScaleStorage();
        myChannel = null;
    }

    private void changeState(@NonNull State state, @NonNull String message) {
        myState = state;
        onChangeState(state, message);
    }

    /**
     *
     * @param state New state
     * @param message A message
     */
    abstract public void onChangeState(@NonNull State state, @NonNull String message);

    /**
     *
     * @param leftScale
     * @param rightScale
     */
    abstract public void complete(Scale leftScale, Scale rightScale);

    private void waitingForFirstNote(int note, long timestamp) {
        myFirstScale.myFirstNote = note;
        myFirstScale.addNote(note, timestamp);
        String message = String.format(Locale.getDefault(), FORMAT_ROOT_NOTE, note);
        changeState(State.SECOND_NOTE, message);
    }

    private void waitingForSecondNote(int note, long timestamp) {
        int firstNote = myFirstScale.myFirstNote;

        int distance = firstNote - note;
        if (distance % 12 == 0) {
            // we have 2 scales
            mySecondScale.myFirstNote = note;
            mySecondScale.addNote(note, timestamp);

            // this could still be 2 contrary motion scales
            // which is ok, as long as they do not share the middle note
        } else {
            // it could be a contrary motion scale
            mySecondScale.copy(myFirstScale);
            myFirstScale.addNote(note, timestamp);

            // in which case they need to go to separate directions
            myFirstScale.myValidSign = Integer.signum(note - myFirstScale.myFirstNote);
            mySecondScale.myValidSign = -myFirstScale.myValidSign;

            // or no 2nd scale at all
        }

        String message = String.format(Locale.getDefault(), FORMAT_TWO_SCALES,
                mySecondScale.myFirstNote, mySecondScale.myNotes.size(),
                myFirstScale.myFirstNote, myFirstScale.myNotes.size());
        changeState(State.NOTES, message);
    }

    private void waitingForNotes(int note, long timestamp) {
        /*
        What is not working if 2 scales contrary motion,
        starting far apart and peaking to the same note in the middle
         */

        int distance1st = Math.abs(note - myFirstScale.myLastNote);
        int distance2nd = Math.abs(note - mySecondScale.myLastNote);

        boolean valid1st = myFirstScale.isValid(note);
        boolean valid2nd = mySecondScale.isValid(note);

        if (valid1st && valid2nd) {
            boolean closingCommonScales = (myFirstScale.myFirstNote == mySecondScale.myFirstNote) &&
                    (note == myFirstScale.myFirstNote);

            if (distance1st <= distance2nd) {
                myFirstScale.addNote(note, timestamp);
                if (closingCommonScales) {
                    mySecondScale.addNote(note, timestamp);
                }
            } else {
                mySecondScale.addNote(note, timestamp);
                if (closingCommonScales) {
                    myFirstScale.addNote(note, timestamp);
                }
            }
        } else {
            if (valid1st) {
                myFirstScale.addNote(note, timestamp);
            }
            if (valid2nd) {
                mySecondScale.addNote(note, timestamp);
            }
        }

        if (mySecondScale.isComplete() && myFirstScale.isComplete()) {
            String message = String.format(Locale.getDefault(), FORMAT_COMPLETE,
                    mySecondScale.myNotes.size(), myFirstScale.myNotes.size());
            changeState(State.COMPLETE, message);

            Scale scale1st = myFirstScale.getScale();
            Scale scale2nd = mySecondScale.getScale();

            int lowest1st = scale1st.getLowestNote();
            int highest1st = scale1st.getHighestNote();

            int lowest2nd = scale2nd.getLowestNote();
            int highest2nd = scale2nd.getHighestNote();

            Scale leftScale;
            Scale rightScale;

            if (lowest1st < lowest2nd || highest1st < highest2nd) {
                leftScale = scale1st;
                rightScale = scale2nd;
            } else {
                leftScale = scale2nd;
                rightScale = scale1st;
            }

            complete(leftScale, rightScale);
        } else {
            String message = String.format(Locale.getDefault(), FORMAT_TWO_SCALES,
                    mySecondScale.myFirstNote, mySecondScale.myNotes.size(),
                    myFirstScale.myFirstNote, myFirstScale.myNotes.size());
            changeState(State.NOTES, message);
        }
    }

    private void noteOn(@NonNull byte[] data, int offset, int count, long timestamp) throws IOException {
        int note = data[offset + 1];
        switch (myState) {
            case FIRST_NOTE:
                waitingForFirstNote(note, timestamp);
                break;
            case SECOND_NOTE:
                waitingForSecondNote(note, timestamp);
                break;
            case NOTES:
                waitingForNotes(note, timestamp);
                break;
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
