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

        SeekBar onDelaySeek = findViewById(R.id.on_delay_seek);
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

        SeekBar offDelaySeek = findViewById(R.id.off_delay_seek);
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

        RadioButton red = findViewById(R.id.red_radio);
        red.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    close();
                    redButton();
                }
            }
        });

        RadioButton amber = findViewById(R.id.amber_radio);
        amber.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && myDelayHandler != null) {
                    myDelayHandler.setRunning(false);
                }
            }
        });

        RadioButton green = findViewById(R.id.green_radio);
        green.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && myDelayHandler != null) {
                    myDelayHandler.setRunning(true);
                }
            }
        });

        redButton();

        myMidiDeviceOpener.queueDevice(input);
        myMidiDeviceOpener.queueDevice(output);

        MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        myMidiDeviceOpener.execute(midiManager, new MidiDeviceOpener.Completed() {
            @Override
            public void action(MidiDeviceOpener opener) {
                myInputPort = opener.openInputPort(input);
                myOutputPort = opener.openOutputPort(output);

                if (myInputPort != null && myOutputPort != null) {
                    myDelayHandler = new DelayHandler(myInputPort) {
                        @Override
                        public void onRunningChange(final boolean value) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
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
                // and send a final ALL_NOTES_OFF
                StartStopReceiver.allNotesOff(myInputPort);
                // then clean all remaining messages
                myInputPort.flush();
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

    void redButton() {
        RadioButton red = findViewById(R.id.red_radio);
        RadioButton amber = findViewById(R.id.amber_radio);
        RadioButton green = findViewById(R.id.green_radio);
        SeekBar onDelaySeek = findViewById(R.id.on_delay_seek);
        SeekBar offDelaySeek = findViewById(R.id.off_delay_seek);

        onDelaySeek.setProgress(0);
        offDelaySeek.setProgress(0);
        red.toggle();

        onDelaySeek.setEnabled(false);
        offDelaySeek.setEnabled(false);
        green.setEnabled(false);
        red.setEnabled(false);
        amber.setEnabled(false);
    }

    void amberButton() {
        RadioButton amber = findViewById(R.id.amber_radio);
        amber.toggle();
    }

    void greenButton() {
        RadioButton red = findViewById(R.id.red_radio);
        RadioButton amber = findViewById(R.id.amber_radio);
        RadioButton green = findViewById(R.id.green_radio);
        SeekBar onDelaySeek = findViewById(R.id.on_delay_seek);
        SeekBar offDelaySeek = findViewById(R.id.off_delay_seek);

        onDelaySeek.setEnabled(true);
        offDelaySeek.setEnabled(true);
        green.setEnabled(true);
        red.setEnabled(true);
        amber.setEnabled(true);

        green.toggle();
    }

}
