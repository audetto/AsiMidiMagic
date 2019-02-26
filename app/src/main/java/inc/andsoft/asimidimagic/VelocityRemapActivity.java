package inc.andsoft.asimidimagic;

import android.graphics.Point;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
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
    private Switch mySticky;
    private RadioButton myAmberButton;
    private RadioButton myGreenButton;

    private RecyclerPointArrayAdapter myAdapterPoints;
    private List<Point> myPoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RadioButton redButton = findViewById(R.id.radio_red);

        redButton.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                disconnect();
            } else {
                connect();
            }
        });

        myAmberButton = findViewById(R.id.radio_amber);
        myAmberButton.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked && myReceiverState.myMidiRemap != null) {
                myReceiverState.myMidiRemap.setRunning(false);
            }
        });

        myGreenButton = findViewById(R.id.radio_green);
        myGreenButton.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked && myReceiverState.myMidiRemap != null) {
                myReceiverState.myMidiRemap.setRunning(true);
            }
        });

        mySticky = findViewById(R.id.switch_sticky);

        myAdapterPoints = new RecyclerPointArrayAdapter(R.layout.listitem_point);

        RecyclerView recyclerPoints = findViewById(R.id.recycler_remap);
        recyclerPoints.setLayoutManager(new LinearLayoutManager(this));
        recyclerPoints.setAdapter(myAdapterPoints);

        // it should really be red until the callback above is called
        // but we can avoid a red -> amber transition is we set to amber now
        // anyway, if the port opening fails, the activity goes altogether
        amberButton();
    }

    @Override
    protected VelocityRemapReceiverState getReceiverState() {
        VelocityRemapReceiverState state = new VelocityRemapReceiverState();

        state.myMidiRemap = new MidiRemap(myInputPort) {
            @Override
            public void onPedalChange(boolean value) {
                runOnUiThread(() ->  {
                    // we only change if it is non sticky or if the pedal goes down
                    if (!mySticky.isChecked() || value) {
                        // the 'else' is really important
                        // as otherwise amber becomes true and green is triggered again
                        if (myGreenButton.isChecked()) {
                            amberButton();
                        } else {
                            if (myAmberButton.isChecked()) {
                                greenButton();
                            }
                        }
                    }
                });
            }
        };

        state.myFilter = new MidiFilter(state.myMidiRemap);
        return state;
    }

    @Override
    protected @LayoutRes int getLayoutID() {
        return R.layout.activity_remap;
    }

    private void amberButton() {
        RadioButton amber = findViewById(R.id.radio_amber);
        amber.toggle();
    }

    private void greenButton() {
        RadioButton green = findViewById(R.id.radio_green);
        green.toggle();
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
