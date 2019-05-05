package inc.andsoft.asimidimagic;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import inc.andsoft.asimidimagic.activities.CommonMidiSinkActivity;
import inc.andsoft.asimidimagic.activities.ReceiverStateAdapter;
import inc.andsoft.asimidimagic.dialogs.BeatDialog;
import inc.andsoft.asimidimagic.midi.MidiRecorder;
import inc.andsoft.asimidimagic.tools.NoteSequence;
import inc.andsoft.asimidimagic.views.SequenceChart;


public class SequenceActivity extends CommonMidiSinkActivity<ReceiverStateAdapter<MidiRecorder>>
        implements BeatDialog.BeatDialogListener {
    private SequenceChart myChart;

    private static final String SEQUENCE_KEY = "sequence";

    private ArrayList<SequenceChart.Beat> myBeats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myChart = findViewById(R.id.chart);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(SEQUENCE_KEY, myBeats);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        myBeats.clear();
        ArrayList<SequenceChart.Beat> beats = savedInstanceState.getParcelableArrayList(SEQUENCE_KEY);
        if (beats != null) {
            myBeats = beats;
            myChart.setBeats(myBeats);
        }
    }

    @Override
    protected ReceiverStateAdapter<MidiRecorder> getReceiverState() {
        MidiRecorder midiScales = new MidiRecorder() {
            @Override
            public void onSequence(NoteSequence sequence) {
                runOnUiThread(() -> myChart.setNotes(sequence));
            }
            @Override
            public void onChangeState(@NonNull State state, @NonNull String message) {
                runOnUiThread(() -> {
                    if (state == State.RECORDING) {
                        myChart.setNotes(null);
                    }
                    TextView statusView = findViewById(R.id.text_status);
                    statusView.setText(message);
                });
            }
        };
        return new ReceiverStateAdapter<>(midiScales);
    }

    @Override
    protected @LayoutRes int getLayoutID() {
        return R.layout.activity_sequence;
    }

    @Override
    public void onDialogPositiveClick(int count, int color, int width) {
        if (count > 0 && width > 0) {
            myBeats.add(new SequenceChart.Beat(count, color, width));
            myChart.setBeats(myBeats);
        } else {
            Toast.makeText(this, R.string.invalid_beat, Toast.LENGTH_SHORT).show();
        }
    }

    public void clearBars(View v) {
        myBeats.clear();
        myChart.setBeats(myBeats);
    }

    public void addBeat(View v) {
        String[] colors = getResources().getStringArray(R.array.beat_colors);
        int index = Math.min(myBeats.size(), colors.length - 1);
        String suggested_color = colors[index];

        BeatDialog bd = new BeatDialog();

        Bundle bundle = new Bundle();
        bundle.putString("color", suggested_color);
        bundle.putInt("width", 8);
        bd.setArguments(bundle);

        bd.show(getSupportFragmentManager(), "Beat");
    }
}
