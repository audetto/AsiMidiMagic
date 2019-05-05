package inc.andsoft.asimidimagic.tools;

import java.util.Comparator;
import java.util.List;

public class NoteSequence {

    static public class Note {
        public final int code;
        public final int velocity;

        public Note(int code, int velocity) {
            this.code = code;
            this.velocity = velocity;
        }
    }

    protected List<Note> myNotes;
    protected List<Double> myTimes;

    public NoteSequence(List<Note> notes, List<Double> times) {
        myNotes = notes;
        myTimes = times;
    }

    public List<Note> getNotes() {
        return myNotes;
    }

    public List<Double> getTimes() {
        return myTimes;
    }

    public int getLowestNote() {
        int lowest = myNotes.stream().min(Comparator.comparingInt(x -> x.code)).get().code;
        return lowest;
    }

    public int getHighestNote() {
        int highest = myNotes.stream().max(Comparator.comparingInt(x -> x.code)).get().code;
        return highest;
    }

}
