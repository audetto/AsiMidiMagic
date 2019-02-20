package inc.andsoft.asimidimagic.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import inc.andsoft.asimidimagic.R;
import inc.andsoft.asimidimagic.tools.NoteSequence;

public class SequenceChart extends View {
    private static final String TAG = "SequenceChart";

    public static class Bar {
        public final int periods;
        public final @ColorInt int color;
        public final int width;

        public Bar(int periods, @ColorInt int color, int width) {
            this.periods = periods;
            this.color = color;
            this.width = width;
        }
    }

    private NoteSequence mySequence;
    private Paint myPaintNotes;

    private List<Bar> myBars;
    private List<Paint> myPaintBars = new ArrayList<>();

    private RectF myRect;

    public SequenceChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        int noteColor = getResources().getColor(android.R.color.holo_blue_dark);

        init(noteColor);
    }

    public void setNotes(NoteSequence sequence) {
        mySequence = sequence;
        invalidate();
    }

    public void setBars(@NonNull List<Bar> bars) {
        myBars = bars;

        myPaintBars.clear();
        for (Bar bar : myBars) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(bar.color);
            paint.setStrokeWidth(bar.width);
            myPaintBars.add(paint);
        }

        invalidate();
    }

    private void init(int noteColor) {
        myPaintNotes = new Paint(Paint.ANTI_ALIAS_FLAG);
        myPaintNotes.setColor(noteColor);
        myPaintNotes.setStrokeWidth(8);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        float extra = 8;
        myRect = new RectF(getPaddingLeft() + extra, getPaddingTop() + extra,
                width - getPaddingLeft() - getPaddingLeft() - 2 * extra,
                height - getPaddingTop() - getPaddingBottom() - 2 * extra);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int numberOfBars = myPaintBars.size();
        for (int j = 0; j < numberOfBars; ++j) {
            float startY = myRect.centerY() - 0.5f * myRect.height();
            float endY = myRect.centerY() + 0.5f * myRect.height();

            int numberOfPeriods = myBars.get(j).periods;
            Paint paintBar = myPaintBars.get(j);

            float dx = myRect.width() / numberOfPeriods;
            for (int i = 0; i <= numberOfPeriods; ++i) {
                float x = myRect.left + i * dx;
                canvas.drawLine(x, startY, x, endY, paintBar);
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

                    canvas.drawCircle(x, y, 8, myPaintNotes);
                }
            }
        }
    }

}
