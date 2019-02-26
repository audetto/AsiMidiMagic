package inc.andsoft.asimidimagic;

import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import androidx.annotation.LayoutRes;
import inc.andsoft.asimidimagic.activities.CommonMidiPassActivity;
import inc.andsoft.asimidimagic.activities.ReceiverState;
import inc.andsoft.asimidimagic.midi.MidiDelay;
import inc.andsoft.asimidimagic.midi.MidiCountedOnOff;
import inc.andsoft.asimidimagic.midi.MidiFilter;
import inc.andsoft.asimidimagic.midi.MidiTimeScheduler;


class DelayReceiverState implements ReceiverState {
    MidiCountedOnOff myCounted;
    MidiDelay myMidiDelay;
    MidiTimeScheduler myTimeScheduler;
    MidiFilter myFilter;

    @Override
    public MidiReceiver getReceiver() {
        return myFilter;
    }

    @Override
    public void close() throws IOException {
        myTimeScheduler.stop();
    }
}


public class DelayActivity extends CommonMidiPassActivity<DelayReceiverState> {
    private Switch mySticky;
    private RadioButton myAmberButton;
    private RadioButton myGreenButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                if (myReceiverState.myMidiDelay != null) {
                    myReceiverState.myMidiDelay.setOnDelay(delay);
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
                if (myReceiverState.myMidiDelay != null) {
                    myReceiverState.myMidiDelay.setOffDelay(delay);
                }
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
            if (isChecked && myReceiverState.myMidiDelay != null) {
                myReceiverState.myMidiDelay.setRunning(false);
            }
        });

        myGreenButton = findViewById(R.id.radio_green);
        myGreenButton.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked && myReceiverState.myMidiDelay != null) {
                myReceiverState.myMidiDelay.setRunning(true);
            }
        });

        mySticky = findViewById(R.id.switch_sticky);

        // it should really be red until the callback above is called
        // but we can avoid a red -> amber transition is we set to amber now
        // anyway, if the port opening fails, the activity goes altogether
        amberButton();
    }

    @Override
    protected DelayReceiverState getReceiverState() {
        DelayReceiverState state = new DelayReceiverState();

        state.myCounted = new MidiCountedOnOff(myInputPort);
        state.myTimeScheduler = new MidiTimeScheduler(state.myCounted);
        state.myMidiDelay = new MidiDelay(state.myTimeScheduler) {
            @Override
            public void onPedalChange(boolean value) {
                runOnUiThread(() ->  {
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
                });
            }
        };

        state.myFilter = new MidiFilter(state.myMidiDelay);

        return state;
    }

    @Override
    protected @LayoutRes int getLayoutID() {
        return R.layout.activity_delay;
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
        builder.append(myReceiverState.myFilter.myOnCounter);
        builder.append(", In OFF = ");
        builder.append(myReceiverState.myFilter.myOffCounter);
        builder.append(", Out ON = ");
        builder.append(myReceiverState.myCounted.myOnCounter);
        builder.append(", Out OFF = ");
        builder.append(myReceiverState.myCounted.myOffCounter);

        TextView text = findViewById(R.id.text_counters);
        text.setText(builder.toString());
    }
}
