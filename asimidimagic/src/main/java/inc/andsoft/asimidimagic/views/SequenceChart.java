package inc.andsoft.asimidimagic.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import inc.andsoft.asimidimagic.R;
import inc.andsoft.asimidimagic.tools.NoteSequence;

public class SequenceChart extends View {

    public static class Beat implements Parcelable {
        public final int count;
        public final @ColorInt int color;
        public final int width;

        public Beat(int count, @ColorInt int color, int width) {
            this.count = count;
            this.color = color;
            this.width = width;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(count);
            parcel.writeInt(color);
            parcel.writeInt(width);
        }

        public static final Parcelable.Creator<Beat> CREATOR =
                new Parcelable.Creator<Beat>() {

                    @Override
                    public Beat createFromParcel(Parcel in) {
                        int count = in.readInt();
                        int color = in.readInt();
                        int width = in.readInt();

                        Beat wrapper = new Beat(count, color, width);

                        return wrapper;
                    }

                    @Override
                    public Beat[] newArray(int size) {
                        return new Beat[size];
                    }
                };

    }

    private Paint myPaintNotes;
    private RectF myRect;
    private float myNoteRadius;

    private NoteSequence mySequence;

    private List<Beat> myBeats;
    private List<Paint> myPaintBeats = new ArrayList<>();


    public SequenceChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.SequenceChart, 0, 0);

        int noteColor = getResources().getColor(android.R.color.holo_blue_dark);
        float noteRadius = 10.0f;

        try {
            noteColor = a.getColor(R.styleable.SequenceChart_noteColor, noteColor);
            noteRadius = a.getFloat(R.styleable.SequenceChart_noteRadius, noteRadius);
        } finally {
            a.recycle();
        }

        init(noteColor, noteRadius);
    }

    public void setNotes(NoteSequence sequence) {
        mySequence = sequence;
        invalidate();
    }

    public void setBeats(@NonNull List<Beat> beats) {
        myBeats = beats;

        myPaintBeats.clear();
        for (Beat beat : myBeats) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(beat.color);
            paint.setStrokeWidth(beat.width);
            myPaintBeats.add(paint);
        }

        invalidate();
    }

    private void init(int noteColor, float noteRadius) {
        myPaintNotes = new Paint(Paint.ANTI_ALIAS_FLAG);
        myPaintNotes.setColor(noteColor);

        myNoteRadius = noteRadius;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        float extra = myNoteRadius;
        myRect = new RectF(getPaddingLeft() + extra, getPaddingTop() + extra,
                width - getPaddingLeft() - getPaddingLeft() - 2 * extra,
                height - getPaddingTop() - getPaddingBottom() - 2 * extra);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int numberOfBars = myPaintBeats.size();
        for (int j = 0; j < numberOfBars; ++j) {
            float startY = myRect.centerY() - 0.5f * myRect.height();
            float endY = myRect.centerY() + 0.5f * myRect.height();

            int numberOfPeriods = myBeats.get(j).count;
            Paint paintBeat = myPaintBeats.get(j);

            float dx = myRect.width() / numberOfPeriods;
            for (int i = 0; i <= numberOfPeriods; ++i) {
                float x = myRect.left + i * dx;
                canvas.drawLine(x, startY, x, endY, paintBeat);
            }
        }

        if (mySequence != null) {
            List<NoteSequence.Note> notes = mySequence.getNotes();
            List<Double> times = mySequence.getTimes();
            int numberOfNotes = notes.size();
            if (numberOfNotes > 1) {
                int lowestNote = mySequence.getLowestNote();
                int highestNote = mySequence.getHighestNote();

                // the first note is at 0
                float coefficientX = myRect.width() / times.get(numberOfNotes - 1).floatValue();
                float coefficientY = myRect.height() / (highestNote - lowestNote);

                for (int i = 0; i < numberOfNotes; ++i) {
                    float x = myRect.left + times.get(i).floatValue() * coefficientX;
                    float y = myRect.bottom - (notes.get(i).code - lowestNote) * coefficientY;

                    canvas.drawCircle(x, y, myNoteRadius, myPaintNotes);
                }
            }
        }
    }

}
