package inc.andsoft.asimidimagic;

import android.graphics.Point;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import inc.andsoft.asimidimagic.activities.CommonMidiPassActivity;
import inc.andsoft.asimidimagic.activities.ReceiverState;
import inc.andsoft.asimidimagic.midi.MidiFilter;
import inc.andsoft.asimidimagic.midi.MidiRemap;
import inc.andsoft.asimidimagic.tools.RecyclerPointArrayAdapter;


class VelocityRemapReceiverState implements ReceiverState {
    MidiRemap myMidiRemap;
    MidiFilter myFilter;

    @Override
    public MidiReceiver getReceiver() {
        return myFilter;
    }

    @Override
    public void close() throws IOException {
    }
}


public class VelocityRemapActivity extends CommonMidiPassActivity<VelocityRemapReceiverState> {

    private RecyclerPointArrayAdapter myAdapterPoints;
    private ArrayList<Point> myPoints = new ArrayList<>();

    private static final String POINTS_KEY = "points";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myAdapterPoints = new RecyclerPointArrayAdapter(R.layout.listitem_point);

        RecyclerView recyclerPoints = findViewById(R.id.recycler_remap);
        recyclerPoints.setLayoutManager(new LinearLayoutManager(this));
        recyclerPoints.setAdapter(myAdapterPoints);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(POINTS_KEY, myPoints);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        myPoints.clear();
        ArrayList<Point> points = savedInstanceState.getParcelableArrayList(POINTS_KEY);
        if (points != null) {
            myPoints = points;
            myAdapterPoints.setItems(myPoints);
            myAdapterPoints.notifyDataSetChanged();
        }
    }

    @Override
    protected VelocityRemapReceiverState getReceiverState() {
        VelocityRemapReceiverState state = new VelocityRemapReceiverState();

        state.myMidiRemap = new MidiRemap(myInputPort) {
            @Override
            public void onPedalChange(boolean value) {
                runOnUiThread(() ->  VelocityRemapActivity.this.onPedalChange(value));
            }
        };

        state.myFilter = new MidiFilter(state.myMidiRemap);
        return state;
    }

    @Override
    protected @LayoutRes int getLayoutID() {
        return R.layout.activity_remap;
    }

    @Override
    protected void setRunning(boolean value) {
        if (myReceiverState != null && myReceiverState.myMidiRemap != null) {
            myReceiverState.myMidiRemap.setRunning(value);
        }
    }

    public void clearPoints(View v) {
        myPoints.clear();
        myAdapterPoints.setItems(myPoints);
        myAdapterPoints.notifyDataSetChanged();
    }

    public void addPoint(View v) {
        myPoints.add(new Point());
        myAdapterPoints.setItems(myPoints);
        myAdapterPoints.notifyDataSetChanged();
    }

    public void process(View v) {
        Collections.sort(myPoints, (lhs, rhs) -> (lhs.x == rhs.x) ? (lhs.y - rhs.y) : (lhs.x - rhs.x));
        myAdapterPoints.setItems(myPoints);
        myAdapterPoints.notifyDataSetChanged();

        double [] xs = myPoints.stream().mapToDouble(p -> p.x).toArray();
        double [] ys = myPoints.stream().mapToDouble(p -> p.y).toArray();

        try {
            UnivariateFunction f = new LinearInterpolator().interpolate(xs, ys);

            MidiRemap.VelocityRemap remapper = (int note, int channel, int velocity) -> {
                long y = Math.round(f.value(velocity));
                return (int) y;
            };

            myReceiverState.myMidiRemap.setRemapper(remapper);
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

}
