package inc.andsoft.asimidimagic.activities;

import android.content.Intent;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.MidiPortWrapper;

import androidx.annotation.LayoutRes;
import inc.andsoft.asimidimagic.R;
import inc.andsoft.support.tools.MidiDeviceOpener;


public abstract class CommonMidiSinkActivity<S extends ReceiverState> extends CommonMidiActivity {

    protected MidiOutputPort myOutputPort;
    protected MidiFramer myFramer;
    protected S myReceiverState;

    protected abstract S getReceiverState();
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
                myReceiverState = getReceiverState();
                myFramer = new MidiFramer(myReceiverState.getReceiver());
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

        myReceiverState = null;
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
