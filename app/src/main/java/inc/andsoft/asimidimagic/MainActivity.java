package inc.andsoft.asimidimagic;

import android.content.Intent;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.mobileer.miditools.MidiPortSelector;

import inc.andsoft.asimidimagic.tools.DataWithLabel;

public class MainActivity extends BaseActivity {

    private MidiManager myMidiManager;
    private MidiPortSelector myInputPortSelector;
    private MidiPortSelector myOutputPortSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setActionBar();

        myMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        Spinner inputSpinner = findViewById(R.id.spinner_input);
        myInputPortSelector = new MidiPortSelector(myMidiManager, inputSpinner, MidiDeviceInfo.PortInfo.TYPE_INPUT);

        Spinner outputSpinner = findViewById(R.id.spinner_output);
        myOutputPortSelector = new MidiPortSelector(myMidiManager, outputSpinner, MidiDeviceInfo.PortInfo.TYPE_OUTPUT);

        Spinner handlerSpinner = findViewById(R.id.spinner_handler);
        ArrayAdapter<DataWithLabel<Class>> arrayAdapter = new ArrayAdapter<>(
                handlerSpinner.getContext(), android.R.layout.simple_spinner_dropdown_item);

        arrayAdapter.add(new DataWithLabel<>("ON-OFF Delay", DelayActivity.class));
        handlerSpinner.setAdapter(arrayAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        myMidiManager = null;
        myInputPortSelector.onDestroy();
        myOutputPortSelector.onDestroy();
    }

    public void clickButton(View view) {
        Spinner handlerSpinner = findViewById(R.id.spinner_handler);
        Object selected = handlerSpinner.getSelectedItem();

        if (selected != null) {
            DataWithLabel<Class> data = (DataWithLabel<Class>)selected;
            Class activity = data.getData();
            Intent secondActivity = new Intent(MainActivity.this, activity);
            secondActivity.putExtra("input", myInputPortSelector.getPortWrapper());
            secondActivity.putExtra("output", myOutputPortSelector.getPortWrapper());

            startActivity(secondActivity);
        }
    }

    public void bleButton(MenuItem item) {
        Intent bleActivity = new Intent(MainActivity.this, ScanActivity.class);
        startActivity(bleActivity);
    }


}
