package inc.andsoft.asimidimagic;

import android.content.Intent;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.MidiPortWrapper;

import inc.andsoft.asimidimagic.midi.MidiScales;
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;

public class ScaleActivity extends CommonActivity {
    private MidiOutputPort myOutputPort;
    private MidiFramer myFramer;
    private MidiScales myScales;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scale);
        setActionBar();

        Intent intent = getIntent();
        MidiPortWrapper output = intent.getParcelableExtra("output");

        TextView outputText = findViewById(R.id.output_name);
        outputText.setText(output.toString());

        myMidiDeviceOpener.queueDevice(output);

        MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        myMidiDeviceOpener.execute(midiManager, (MidiDeviceOpener opener) -> {
            myOutputPort = opener.openOutputPort(output);

            if (myOutputPort != null) {
                myScales = new MidiScales() {
                    public void onChangeState(MidiScales.State state, String message) {
                        runOnUiThread(() -> {
                            TextView statusView = findViewById(R.id.text_status);
                            statusView.setText(message);
                        });
                    }
                };
                myFramer = new MidiFramer(myScales);
                connect();
            } else {
                Toast.makeText(ScaleActivity.this, "Missing MIDI port", Toast.LENGTH_SHORT).show();
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
    protected void close() {
        disconnect();

        myScales = null;
        myFramer = null;
        myOutputPort = null;

        super.close();
    }

}
