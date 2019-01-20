package inc.andsoft.asimidimagic;

import android.content.Intent;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.MidiPortWrapper;

import java.io.IOException;

import inc.andsoft.asimidimagic.midi.DelayHandler;
import inc.andsoft.asimidimagic.midi.MidiCountedOnOff;
import inc.andsoft.asimidimagic.midi.MidiFilter;
import inc.andsoft.asimidimagic.midi.MidiTimeScheduler;
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;
import inc.andsoft.asimidimagic.tools.Utilities;

public class DelayActivity extends CommonActivity {

    private final static String TAG = "DelayActivity";

    private MidiInputPort myInputPort;
    private MidiOutputPort myOutputPort;

    private MidiTimeScheduler myTimeScheduler;
    private DelayHandler myDelayHandler;
    private MidiFramer myFramer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delay);
        setActionBar();

        Intent intent = getIntent();
        MidiPortWrapper output = intent.getParcelableExtra("output");
        MidiPortWrapper input = intent.getParcelableExtra("input");

        TextView outputText = findViewById(R.id.output_name);
        outputText.setText(output.toString());

        TextView inputText = findViewById(R.id.input_name);
        inputText.setText(input.toString());

        SeekBar onDelaySeek = findViewById(R.id.seek_on_delay);
        onDelaySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int delay = seekBar.getProgress();
                Toast.makeText(DelayActivity.this, "ON Delay is " + delay + "ms", Toast.LENGTH_SHORT).show();
                if (myDelayHandler != null) {
                    myDelayHandler.setOnDelay(delay);
                }
            }
        });

        SeekBar offDelaySeek = findViewById(R.id.seek_off_delay);
        offDelaySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int delay = seekBar.getProgress();
                Toast.makeText(DelayActivity.this, "OFF Delay is " + delay + "ms", Toast.LENGTH_SHORT).show();
                if (myDelayHandler != null) {
                    myDelayHandler.setOffDelay(delay);
                }
            }
        });

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
            if (isChecked && myDelayHandler != null) {
                myDelayHandler.setRunning(false);
            }
        });

        RadioButton green = findViewById(R.id.radio_green);
        green.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked && myDelayHandler != null) {
                myDelayHandler.setRunning(true);
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
                MidiReceiver counted = new MidiCountedOnOff(myInputPort);
                myTimeScheduler = new MidiTimeScheduler(counted);
                myDelayHandler = new DelayHandler(myTimeScheduler) {
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

                MidiReceiver filter = new MidiFilter(myDelayHandler);
                myFramer = new MidiFramer(filter);
                connect();

                // the chain is
                // myOutputPort -> MidiFramer -> MidiFilter -> DelayHandler -> TimeScheduler ->
                // MidiCountedOnOff -> myInputPort
            } else {
                Toast.makeText(DelayActivity.this, "Missing MIDI ports", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

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
                // clean all remaining messages
                myInputPort.flush();
                // and send a final ALL_NOTES_OFF
                Utilities.allNotesOff(myInputPort);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    protected void close() {
        disconnect();

        if (myTimeScheduler != null) {
            myTimeScheduler.stop();
        }

        myFramer = null;
        myInputPort = null;
        myOutputPort = null;
        myDelayHandler = null;
        myTimeScheduler = null;

        super.close();
    }

    private void amberButton() {
        RadioButton amber = findViewById(R.id.radio_amber);
        amber.toggle();
    }

    private void greenButton() {
        RadioButton green = findViewById(R.id.radio_green);
        green.toggle();
    }

}
