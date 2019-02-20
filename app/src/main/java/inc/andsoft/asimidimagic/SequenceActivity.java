package inc.andsoft.asimidimagic;

import android.content.Intent;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.MidiPortWrapper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import inc.andsoft.asimidimagic.midi.MidiRecorder;
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;
import inc.andsoft.asimidimagic.tools.NoteSequence;
import inc.andsoft.asimidimagic.views.SequenceChart;


public class SequenceActivity extends CommonActivity {
    private MidiOutputPort myOutputPort;
    private MidiFramer myFramer;

    private SequenceChart myChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sequence);
        setActionBar();

        Intent intent = getIntent();
        MidiPortWrapper output = intent.getParcelableExtra("output");

        TextView outputText = findViewById(R.id.output_name);
        outputText.setText(output.toString());

        myMidiDeviceOpener.queueDevice(output);

        MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        myChart = findViewById(R.id.chart);

        List<SequenceChart.Bar> bars = new ArrayList<>();
        bars.add(new SequenceChart.Bar(8, getResources().getColor(android.R.color.holo_blue_dark), 4));
        bars.add(new SequenceChart.Bar(3, getResources().getColor(android.R.color.holo_green_dark), 2));
        bars.add(new SequenceChart.Bar(5, getResources().getColor(android.R.color.holo_orange_light), 6));
        myChart.setBars(bars);

        myMidiDeviceOpener.execute(midiManager, (MidiDeviceOpener opener) -> {
            myOutputPort = opener.openOutputPort(output);

            if (myOutputPort != null) {
                MidiReceiver midiScales = new MidiRecorder() {
                    @Override
                    public void onSequence(NoteSequence sequence) {
                        runOnUiThread(() -> myChart.setNotes(sequence));
                    }
                    @Override
                    public void onChangeState(@NonNull State state, @NonNull String message) {
                        runOnUiThread(() -> {
                            TextView statusView = findViewById(R.id.text_status);
                            statusView.setText(message);
                        });
                    }
                };
                myFramer = new MidiFramer(midiScales);
                connect();
            } else {
                Toast.makeText(SequenceActivity.this, getString(R.string.missing_port),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();

        myFramer = null;
        myOutputPort = null;
    }
}
