package inc.andsoft.asimidimagic;

import android.content.Intent;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
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

import inc.andsoft.asimidimagic.handlers.DelayHandler;
import inc.andsoft.asimidimagic.handlers.StartStopReceiver;
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;

public class DelayActivity extends CommonActivity {

    public final static String TAG = "DelayActivity";

    private MidiInputPort myInputPort;
    private MidiOutputPort myOutputPort;

    private DelayHandler myDelayHandler;
    private MidiFramer myFramer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delay);
        setActionBar();

        Intent intent = getIntent();
        final MidiPortWrapper input = intent.getParcelableExtra("input");
        final MidiPortWrapper output = intent.getParcelableExtra("output");

        TextView inputText = findViewById(R.id.input_name);
        inputText.setText(input.toString());

        TextView outputText = findViewById(R.id.output_name);
        outputText.setText(output.toString());

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
                close();
                redButton();
            }
        });

        final RadioButton amber = findViewById(R.id.radio_amber);
        amber.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked && myDelayHandler != null) {
                myDelayHandler.setRunning(false);
            }
        });

        final RadioButton green = findViewById(R.id.radio_green);
        green.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked && myDelayHandler != null) {
                myDelayHandler.setRunning(true);
            }
        });

        amberButton();

        myMidiDeviceOpener.queueDevice(input);
        myMidiDeviceOpener.queueDevice(output);

        MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        final Switch sticky = findViewById(R.id.switch_sticky);

        myMidiDeviceOpener.execute(midiManager, (MidiDeviceOpener opener) -> {
            myInputPort = opener.openInputPort(input);
            myOutputPort = opener.openOutputPort(output);

            if (myInputPort != null && myOutputPort != null) {
                myDelayHandler = new DelayHandler(myInputPort) {
                    @Override
                    public void onRunningChange(final boolean value) {
                        runOnUiThread(() ->  {
                            if (sticky.isChecked()) {
                                // change amber <-> green each time the pedal is pressed
                                // do nothing when it is released
                                if (value) {
                                    if (green.isChecked()) {
                                        amberButton();
                                    }
                                    if (amber.isChecked()) {
                                        greenButton();
                                    }
                                }
                            } else {
                                // pressed => green
                                // released => amber
                                if (value) {
                                    greenButton();
                                } else {
                                    amberButton();
                                }
                            }
                        });
                    }
                };

                myFramer = new MidiFramer(myDelayHandler);
                myOutputPort.connect(myFramer);
                myDelayHandler.fireRunningChange();
            } else {
                Toast.makeText(DelayActivity.this, "Missing MIDI ports", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void close() {
        if (myOutputPort != null && myFramer != null) {
            // first detach the input port (wrapped in the framer)
            myOutputPort.disconnect(myFramer);
        }

        if (myInputPort != null) {
            try {
                // clean all remaining messages
                myInputPort.flush();
                // and send a final ALL_NOTES_OFF
                StartStopReceiver.allNotesOff(myInputPort);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        myFramer = null;
        myInputPort = null;
        myOutputPort = null;
        myDelayHandler = null;

        super.close();
    }

    private void redButton() {
        RadioButton red = findViewById(R.id.radio_red);
        RadioButton amber = findViewById(R.id.radio_amber);
        RadioButton green = findViewById(R.id.radio_green);
        SeekBar onDelaySeek = findViewById(R.id.seek_on_delay);
        SeekBar offDelaySeek = findViewById(R.id.seek_off_delay);
        Switch sticky = findViewById(R.id.switch_sticky);

        onDelaySeek.setProgress(0);
        offDelaySeek.setProgress(0);
        red.toggle();

        onDelaySeek.setEnabled(false);
        offDelaySeek.setEnabled(false);
        green.setEnabled(false);
        red.setEnabled(false);
        amber.setEnabled(false);
        sticky.setEnabled(false);
    }

    private void amberButton() {
        RadioButton amber = findViewById(R.id.radio_amber);
        amber.toggle();
    }

    private void greenButton() {
        RadioButton red = findViewById(R.id.radio_red);
        RadioButton amber = findViewById(R.id.radio_amber);
        RadioButton green = findViewById(R.id.radio_green);
        SeekBar onDelaySeek = findViewById(R.id.seek_on_delay);
        SeekBar offDelaySeek = findViewById(R.id.seek_off_delay);

        onDelaySeek.setEnabled(true);
        offDelaySeek.setEnabled(true);
        green.setEnabled(true);
        red.setEnabled(true);
        amber.setEnabled(true);

        green.toggle();
    }

}
