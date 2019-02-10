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
    private RectF myRect;

    public RhythmChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.RhythmChart, 0, 0);

        int beatColor = getResources().getColor(android.R.color.holo_red_dark);
        int noteColor = getResources().getColor(android.R.color.holo_blue_dark);

        try {
            beatColor = a.getColor(R.styleable.RhythmChart_beatColor, beatColor);
            noteColor = a.getColor(R.styleable.RhythmChart_noteColor, noteColor);
        } finally {
            a.recycle();
        }

        init(beatColor, noteColor);
    }

    public void setNotes(List<Double> times, int beats) {
        myTimes = times;
        myBeats = beats;
        invalidate();
    }

    private void init(int beatColor, int noteColor) {
        myPaintTarget = new Paint(Paint.ANTI_ALIAS_FLAG);
        myPaintTarget.setColor(beatColor);
        myPaintTarget.setStrokeWidth(3);

        myPaintNotes = new Paint(Paint.ANTI_ALIAS_FLAG);
        myPaintNotes.setColor(noteColor);
        myPaintNotes.setStrokeWidth(3);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        float extra = 2;
        myRect = new RectF(getPaddingLeft() + extra, getPaddingTop() + extra,
                width - getPaddingLeft() - getPaddingLeft() - 2 * extra,
                height - getPaddingTop() - getPaddingBottom() - 2 * extra);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (myBeats > 0) {
            float dx = myRect.width() / myBeats;
            float centerY = myRect.centerY();
            for (int i = 0; i < myBeats; ++i) {
                float x = myRect.left + (i + 1) * dx;
                canvas.drawLine(x, myRect.top, x, centerY, myPaintTarget);
            }
        }

        if (myTimes != null) {
            int numberOfNotes = myTimes.size();
            if (numberOfNotes > 0) {
                float centerY = myRect.centerY();
                float coefficient = myRect.width() / myTimes.get(numberOfNotes - 1).floatValue();
                for (int i = 0; i < numberOfNotes; ++i) {
                    float x = myRect.left + myTimes.get(i).floatValue() * coefficient;
                    canvas.drawLine(x, centerY, x, myRect.bottom, myPaintNotes);
                }
            }
        }
    }

}
