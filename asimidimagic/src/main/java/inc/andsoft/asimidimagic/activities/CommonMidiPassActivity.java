package inc.andsoft.asimidimagic.activities;

import android.content.Intent;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.MidiPortWrapper;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import inc.andsoft.asimidimagic.MidiToolFragment;
import inc.andsoft.asimidimagic.R;
import inc.andsoft.support.tools.MidiDeviceOpener;
import inc.andsoft.support.tools.Utilities;


public abstract class CommonMidiPassActivity<S extends ReceiverState> extends CommonMidiActivity {

    private MidiOutputPort myOutputPort;
    private MidiInputPort myInputPort;
    private MidiFramer myFramer;
    protected S myReceiverState;

    protected Switch mySticky;
    protected RadioButton myAmberButton;
    protected RadioButton myGreenButton;

    private MidiToolFragment myMidiToolFragment;

    protected abstract S getReceiverState(MidiInputPort inputPort);
    protected abstract @LayoutRes int getLayoutID();
    protected abstract void setRunning(boolean value);
    protected abstract boolean isLocalControlNeeded();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutID());
        Toolbar toolbar = setActionBar();

        Intent intent = getIntent();
        MidiPortWrapper output = intent.getParcelableExtra("output");
        MidiPortWrapper input = intent.getParcelableExtra("input");

        TextView outputText = findViewById(R.id.output_name);
        outputText.setText(output.toString());

        TextView inputText = findViewById(R.id.input_name);
        inputText.setText(input.toString());

        myMidiDeviceOpener.queueDevice(output);
        myMidiDeviceOpener.queueDevice(input);

        MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        myMidiToolFragment = (MidiToolFragment)getSupportFragmentManager().
                findFragmentById(R.id.fragment_midi);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        myMidiDeviceOpener.execute(midiManager, (MidiDeviceOpener opener) -> {
            myOutputPort = opener.openOutputPort(output);
            myInputPort = opener.openInputPort(input);

            if (myInputPort != null && myOutputPort != null) {
                myReceiverState = getReceiverState(myInputPort);
                myFramer = new MidiFramer(myReceiverState.getReceiver());

                myMidiToolFragment.setReceiver(myInputPort);
                connect();

                // the chain is
                // myOutputPort -> MidiFramer -> MidiFilter -> MidiDelay -> TimeScheduler ->
                // MidiCountedOnOff -> myInputPort
            } else {
                Toast.makeText(CommonMidiPassActivity.this, R.string.missing_ports, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

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
            if (isChecked) {
                setRunning(false);
            }
        });

        myGreenButton = findViewById(R.id.radio_green);
        myGreenButton.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                setRunning(true);
            }
        });

        mySticky = findViewById(R.id.switch_sticky);

        // it should really be red until the callback above is called
        // but we can avoid a red -> amber transition is we set to amber now
        // anyway, if the port opening fails, the activity goes altogether
        amberButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();

        if (myReceiverState != null) {
            Utilities.doClose(myReceiverState);
        }

        myReceiverState = null;
        myFramer = null;
        myOutputPort = null;
        myInputPort = null;
    }

    protected void connect() {
        if (myOutputPort != null && myFramer != null) {
            myMidiToolFragment.setLocalControl(isLocalControlNeeded());
            myOutputPort.connect(myFramer);
        }
    }

    protected void disconnect() {
        if (myOutputPort != null && myFramer != null) {
            // first detach the input port (wrapped in the framer)
            myMidiToolFragment.setLocalControl(true);
            myOutputPort.disconnect(myFramer);
        }
    }

    protected void amberButton() {
        RadioButton amber = findViewById(R.id.radio_amber);
        amber.toggle();
    }

    protected void greenButton() {
        RadioButton green = findViewById(R.id.radio_green);
        green.toggle();
    }

    protected void onPedalChange(boolean value) {
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
    }

}
