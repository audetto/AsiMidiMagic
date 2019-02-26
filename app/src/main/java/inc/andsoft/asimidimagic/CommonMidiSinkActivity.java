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

import androidx.annotation.LayoutRes;
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;

public abstract class CommonMidiSinkActivity<M extends MidiReceiver> extends CommonMidiActivity {

    protected MidiOutputPort myOutputPort;
    protected MidiFramer myFramer;
    protected M myReceiver;

    protected abstract M getReceiver();
    protected abstract @LayoutRes int getLayoutID();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutID());
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
                myReceiver = getReceiver();
                myFramer = new MidiFramer(myReceiver);
                connect();
            } else {
                Toast.makeText(CommonMidiSinkActivity.this, R.string.missing_port,
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();

        myFramer = null;
        myOutputPort = null;
    }

    protected void connect() {
        if (myOutputPort != null && myFramer != null) {
            myOutputPort.connect(myFramer);
        }
    }

    protected void disconnect() {
        if (myOutputPort != null && myFramer != null) {
            // first detach the input port (wrapped in the framer)
            myOutputPort.disconnect(myFramer);
        }
    }

}
