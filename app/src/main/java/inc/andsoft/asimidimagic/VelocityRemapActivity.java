package inc.andsoft.asimidimagic;

import android.content.Intent;
import android.graphics.Point;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.MidiPortWrapper;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import inc.andsoft.asimidimagic.midi.MidiFilter;
import inc.andsoft.asimidimagic.midi.MidiRemap;
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;
import inc.andsoft.asimidimagic.tools.RecyclerPointArrayAdapter;
import inc.andsoft.asimidimagic.tools.Utilities;

public class VelocityRemapActivity extends CommonActivity {

    private final static String TAG = "VelocityRemapActivity";

    private MidiInputPort myInputPort;
    private MidiOutputPort myOutputPort;

    private MidiRemap myMidiRemap;
    private MidiFramer myFramer;
    private MidiFilter myFilter;

    private RecyclerPointArrayAdapter myAdapterPoints;
    private List<Point> myPoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remap);
        setActionBar();

        Intent intent = getIntent();
        MidiPortWrapper output = intent.getParcelableExtra("output");
        MidiPortWrapper input = intent.getParcelableExtra("input");

        TextView outputText = findViewById(R.id.output_name);
        outputText.setText(output.toString());

        TextView inputText = findViewById(R.id.input_name);
        inputText.setText(input.toString());

        RadioButton red = findViewById(R.id.radio_red);

        red.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                disconnect();
            } else {
                connect();
            }
        });

        RadioButton amber = findViewById(R.id.radio_amber);
        amber.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked && myMidiRemap != null) {
                myMidiRemap.setRunning(false);
            }
        });

        RadioButton green = findViewById(R.id.radio_green);
        green.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked && myMidiRemap != null) {
                myMidiRemap.setRunning(true);
            }
        });

        myMidiDeviceOpener.queueDevice(output);
        myMidiDeviceOpener.queueDevice(input);

        MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        Switch sticky = findViewById(R.id.switch_sticky);

        myMidiDeviceOpener.execute(midiManager, (MidiDeviceOpener opener) -> {
            myOutputPort = opener.openOutputPort(output);
            myInputPort = opener.openInputPort(input);

            if (myInputPort != null && myOutputPort != null) {
                myMidiRemap = new MidiRemap(myInputPort) {
                    @Override
                    public void onPedalChange(boolean value) {
                        runOnUiThread(() ->  {
                            // we only change if it is non sticky or if the pedal goes down
                            if (!sticky.isChecked() || value) {
                                // the 'else' is really important
                                // as otherwise amber becomes true and green is triggered again
                                if (green.isChecked()) {
                                    amberButton();
                                } else {
                                    if (amber.isChecked()) {
                                        greenButton();
                                    }
                                }
                            }
                        });
                    }
                };

                myFilter = new MidiFilter(myMidiRemap);
                myFramer = new MidiFramer(myFilter);
                connect();

                // the chain is
                // myOutputPort -> MidiFramer -> MidiFilter -> MidiDelay -> TimeScheduler ->
                // MidiCountedOnOff -> myInputPort
            } else {
                Toast.makeText(VelocityRemapActivity.this, R.string.missing_ports, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        myAdapterPoints = new RecyclerPointArrayAdapter(R.layout.listitem_point);

        RecyclerView recyclerPoints = findViewById(R.id.recycler_remap);
        recyclerPoints.setLayoutManager(new LinearLayoutManager(this));
        recyclerPoints.setAdapter(myAdapterPoints);

        // it should really be red until the callback above is called
        // but we can avoid a red -> amber transition is we set to amber now
        // anyway, if the port opening fails, the activity goes altogether
        amberButton();
    }

    void connect() {
        if (myOutputPort != null && myFramer != null) {
            myOutputPort.connect(myFramer);
        }
    }

    void disconnect() {
        if (myOutputPort != null && myFramer != null) {
            // first detach the input port (wrapped in the framer)
            myOutputPort.disconnect(myFramer);
        }

        if (myInputPort != null) {
            try {
                // send a final ALL_NOTES_OFF
                Utilities.allNotesOff(myInputPort);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();

        myFilter = null;
        myFramer = null;
        myInputPort = null;
        myOutputPort = null;
        myMidiRemap = null;
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

            myMidiRemap.setRemapper(remapper);
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
