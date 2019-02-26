package inc.andsoft.asimidimagic;

import android.content.Intent;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.MidiPortWrapper;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import inc.andsoft.asimidimagic.midi.MidiDelay;
import inc.andsoft.asimidimagic.midi.MidiCountedOnOff;
import inc.andsoft.asimidimagic.midi.MidiFilter;
import inc.andsoft.asimidimagic.midi.MidiTimeScheduler;
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;

public class DelayActivity extends CommonMidiActivity {
    private MidiInputPort myInputPort;
    private MidiOutputPort myOutputPort;

    private MidiTimeScheduler myTimeScheduler;
    private MidiDelay myMidiDelay;
    private MidiFramer myFramer;
    private MidiFilter myFilter;
    private MidiCountedOnOff myCounted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delay);
        Toolbar toolbar = setActionBar();

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
                Toast.makeText(DelayActivity.this,
                        getString(R.string.delay_on, delay), Toast.LENGTH_SHORT).show();
                if (myMidiDelay != null) {
                    myMidiDelay.setOnDelay(delay);
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
                Toast.makeText(DelayActivity.this,
                        getString(R.string.delay_off, delay), Toast.LENGTH_SHORT).show();
                if (myMidiDelay != null) {
                    myMidiDelay.setOffDelay(delay);
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
            if (isChecked && myMidiDelay != null) {
                myMidiDelay.setRunning(false);
            }
        });

        RadioButton green = findViewById(R.id.radio_green);
        green.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked && myMidiDelay != null) {
                myMidiDelay.setRunning(true);
            }
        });

        myMidiDeviceOpener.queueDevice(output);
        myMidiDeviceOpener.queueDevice(input);

        MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        Switch sticky = findViewById(R.id.switch_sticky);

        MidiToolFragment midiToolFragment = (MidiToolFragment)getSupportFragmentManager().
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
                myCounted = new MidiCountedOnOff(myInputPort);
                myTimeScheduler = new MidiTimeScheduler(myCounted);
                myMidiDelay = new MidiDelay(myTimeScheduler) {
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

                myFilter = new MidiFilter(myMidiDelay);
                myFramer = new MidiFramer(myFilter);

                midiToolFragment.setReceiver(myInputPort);

                connect();

                // the chain is
                // myOutputPort -> MidiFramer -> MidiFilter -> MidiDelay -> TimeScheduler ->
                // MidiCountedOnOff -> myInputPort
            } else {
                Toast.makeText(DelayActivity.this, R.string.missing_ports, Toast.LENGTH_SHORT).show();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();

        if (myTimeScheduler != null) {
            myTimeScheduler.stop();
        }

        myCounted = null;
        myFilter = null;
        myFramer = null;
        myInputPort = null;
        myOutputPort = null;
        myMidiDelay = null;
        myTimeScheduler = null;
    }

    private void amberButton() {
        RadioButton amber = findViewById(R.id.radio_amber);
        amber.toggle();
    }

    private void greenButton() {
        RadioButton green = findViewById(R.id.radio_green);
        green.toggle();
    }

    public void counters(View v) {
        StringBuilder builder = new StringBuilder();
        builder.append("In ON = ");
        builder.append(myFilter.myOnCounter);
        builder.append(", In OFF = ");
        builder.append(myFilter.myOffCounter);
        builder.append(", Out ON = ");
        builder.append(myCounted.myOnCounter);
        builder.append(", Out OFF = ");
        builder.append(myCounted.myOffCounter);

        TextView text = findViewById(R.id.text_counters);
        text.setText(builder.toString());
    }
}
