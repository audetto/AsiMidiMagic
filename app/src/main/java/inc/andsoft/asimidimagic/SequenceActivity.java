package inc.andsoft.asimidimagic;

import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileer.miditools.MidiFramer;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import inc.andsoft.asimidimagic.dialogs.BeatDialog;
import inc.andsoft.asimidimagic.midi.MidiRecorder;
import inc.andsoft.asimidimagic.tools.NoteSequence;
import inc.andsoft.asimidimagic.views.SequenceChart;


public class SequenceActivity extends CommonMidiSinkActivity<MidiRecorder> implements BeatDialog.BeatDialogListener {
    private SequenceChart myChart;

    private List<SequenceChart.Beat> myBeats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myChart = findViewById(R.id.chart);
    }

    @Override
    protected MidiRecorder getReceiver() {
        MidiRecorder midiScales = new MidiRecorder() {
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
        return midiScales;
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
        BeatDialog bd = new BeatDialog();
        bd.show(getSupportFragmentManager(), "Beat");
    }
}
