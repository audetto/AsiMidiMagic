package inc.andsoft.asimidimagic.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

import inc.andsoft.asimidimagic.R;

public class RhythmChart extends View {

    private List<Double> myTimes;
    private int myBeats;

    private Paint myPaintTarget;
    private Paint myPaintNotes;
    private Paint myPaintBeats;

    private RectF myRect;

    private int myPosition;

    public RhythmChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.RhythmChart, 0, 0);

        int targetColor = getResources().getColor(android.R.color.holo_red_dark);
        int noteColor = getResources().getColor(android.R.color.holo_blue_dark);
        int beatColor = getResources().getColor(android.R.color.holo_green_dark);
        myPosition = 0;

        try {
            targetColor = a.getColor(R.styleable.RhythmChart_targetColor, targetColor);
            noteColor = a.getColor(R.styleable.RhythmChart_noteColor, noteColor);
            beatColor = a.getColor(R.styleable.RhythmChart_beatColor, beatColor);
            myPosition = a.getInt(R.styleable.RhythmChart_position, myPosition);
        } finally {
            a.recycle();
        }

        init(targetColor, noteColor, beatColor);
    }

    public void setNotes(List<Double> times) {
        myTimes = times;
        invalidate();
    }

    public void setBeats(int beats) {
        myBeats = beats;
        invalidate();
    }

    private void init(int tragetColor, int noteColor, int beatColor) {
        myPaintTarget = new Paint(Paint.ANTI_ALIAS_FLAG);
        myPaintTarget.setColor(tragetColor);
        myPaintTarget.setStrokeWidth(8);

        myPaintNotes = new Paint(Paint.ANTI_ALIAS_FLAG);
        myPaintNotes.setColor(noteColor);
        myPaintNotes.setStrokeWidth(8);

        myPaintBeats = new Paint(Paint.ANTI_ALIAS_FLAG);
        myPaintBeats.setColor(beatColor);
        myPaintBeats.setStrokeWidth(4);
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
        if (myBeats > 0) {
            float startY = myRect.centerY() - 0.25f * myRect.height();
            float endY = myRect.centerY() + 0.25f * myRect.height();

            float dx = myRect.width() / myBeats;
            for (int i = 0; i <= myBeats; ++i) {
                float x = myRect.left + i * dx;
                canvas.drawLine(x, startY, x, endY, myPaintBeats);
            }
        }

        if (myTimes != null) {
            int numberOfNotes = myTimes.size();
            if (numberOfNotes > 1) {
                float centerY = myRect.centerY();
                float endNotesY = myPosition == 0 ? myRect.bottom : myRect.top;
                float endBeatsY = myPosition == 0 ? myRect.top : myRect.bottom;

                // the first note is at 0
                float dx = myRect.width() / (numberOfNotes - 1);
                float coefficient = myRect.width() / myTimes.get(numberOfNotes - 1).floatValue();

                for (int i = 0; i < numberOfNotes; ++i) {
                    float note = myRect.left + myTimes.get(i).floatValue() * coefficient;
                    canvas.drawLine(note, centerY, note, endNotesY, myPaintNotes);

                    float beat = myRect.left + i * dx;
                    canvas.drawLine(beat, centerY, beat, endBeatsY, myPaintTarget);
                }
            }
        }
    }

}
