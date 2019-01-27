package inc.andsoft.asimidimagic.midi;

import com.mobileer.miditools.MidiConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;


class Scale {
    int myFirstNote;
    int myLastNote;
    int myValidSign;
    List<Integer> myNotes = new ArrayList<>();
    List<Long> myTimes = new ArrayList<>();

    void addNote(int note, long timestamp) {
        myValidSign = 0;
        myLastNote = note;
        myNotes.add(note);
        myTimes.add(timestamp);
    }

    void copy(@NonNull Scale other) {
        myFirstNote = other.myFirstNote;
        myLastNote = other.myLastNote;
        myNotes.clear();
        myTimes.clear();
        myNotes.addAll(other.myNotes);
        myTimes.addAll(other.myTimes);
    }

    boolean complete() {
        return (myNotes.size() >= 2) && (myFirstNote == myLastNote);
    }

    boolean valid(int note) {
        boolean ok = !complete();
        if (myValidSign != 0) {
            int noteSign = Integer.signum(note - myFirstNote);
            ok = (myValidSign * noteSign) != -1;
        }
        return ok;
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

    private Scale mySecondScale;
    private Scale myFirstScale;

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
        mySecondScale = new Scale();
        myFirstScale = new Scale();
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

        boolean valid1st = myFirstScale.valid(note);
        boolean valid2nd = mySecondScale.valid(note);

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

        if (mySecondScale.complete() && myFirstScale.complete()) {
            String message = String.format(Locale.getDefault(), FORMAT_COMPLETE,
                    mySecondScale.myNotes.size(), myFirstScale.myNotes.size());
            changeState(State.COMPLETE, message);
            complete();
        } else {
            String message = String.format(Locale.getDefault(), FORMAT_TWO_SCALES,
                    mySecondScale.myFirstNote, mySecondScale.myNotes.size(),
                    myFirstScale.myFirstNote, myFirstScale.myNotes.size());
            changeState(State.NOTES, message);
        }
    }

    private void complete() {

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
