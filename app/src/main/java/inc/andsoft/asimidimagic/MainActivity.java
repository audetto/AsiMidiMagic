package inc.andsoft.asimidimagic;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.mobileer.miditools.MidiPortSelector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import inc.andsoft.asimidimagic.model.MagicModel;
import inc.andsoft.asimidimagic.tools.DataWithLabel;
import inc.andsoft.asimidimagic.tools.RecyclerArrayAdapter;
import inc.andsoft.asimidimagic.tools.Utilities;

public class MainActivity extends BaseActivity implements Observer<Map<BluetoothDevice, MidiDevice>> {
    private static final int REQUEST_BLUETOOTH_SCAN = 1;

    private MidiManager myMidiManager;
    private MidiPortSelector myInputPortSelector;
    private MidiPortSelector myOutputPortSelector;
    private MagicModel myMagicModel;
    private RecyclerArrayAdapter<Map.Entry<BluetoothDevice, MidiDevice>> myMidiDeviceListAdapter;

    private static final String OUTPUT_SELECTOR_KEY = "output selector";
    private static final String INPUT_SELECTOR_KEY = "input selector";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setActionBar();

        myMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        Spinner outputSpinner = findViewById(R.id.spinner_output);
        myOutputPortSelector = new MidiPortSelector(myMidiManager, outputSpinner, MidiDeviceInfo.PortInfo.TYPE_OUTPUT);

        Spinner inputSpinner = findViewById(R.id.spinner_input);
        myInputPortSelector = new MidiPortSelector(myMidiManager, inputSpinner, MidiDeviceInfo.PortInfo.TYPE_INPUT);

        Spinner handlerSpinner = findViewById(R.id.spinner_handler);
        ArrayAdapter<DataWithLabel<Class>> arrayAdapter = new ArrayAdapter<>(
                handlerSpinner.getContext(), android.R.layout.simple_spinner_item);

        arrayAdapter.add(new DataWithLabel<>("ON-OFF Delay", DelayActivity.class));
        arrayAdapter.add(new DataWithLabel<>("Scales", ScaleActivity.class));
        arrayAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        handlerSpinner.setAdapter(arrayAdapter);

        RecyclerView recyclerView = findViewById(R.id.list_midi);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        myMidiDeviceListAdapter = new RecyclerArrayAdapter<Map.Entry<BluetoothDevice, MidiDevice>>(R.layout.listitem_ble_device) {
            @Override
            public void populateView(@NonNull View itemView, @NonNull Map.Entry<BluetoothDevice, MidiDevice> data) {
                TextView nameView = itemView.findViewById(R.id.device_name);
                TextView midiView = itemView.findViewById(R.id.device_midi);
                Button disconnectButton = itemView.findViewById(R.id.button_disconnect);

                BluetoothDevice bleDevice = data.getKey();
                MidiDevice midiDevice = data.getValue();

                String deviceName = bleDevice.getName();
                if (deviceName != null && deviceName.length() > 0)
                    nameView.setText(deviceName);
                else
                    nameView.setText(R.string.unknown_device);
                midiView.setText(midiDevice.getInfo().toString());

                disconnectButton.setOnClickListener((View v) -> Utilities.doClose(midiDevice));
            }
        };
        recyclerView.setAdapter(myMidiDeviceListAdapter);

        myMagicModel = ViewModelProviders.of(this).get(MagicModel.class);
        myMagicModel.getDevices().observe(this, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Parcelable outputSelectorState = myOutputPortSelector.onSaveInstanceState();
        outState.putParcelable(OUTPUT_SELECTOR_KEY, outputSelectorState);

        Parcelable inputSelectorState = myInputPortSelector.onSaveInstanceState();
        outState.putParcelable(INPUT_SELECTOR_KEY, inputSelectorState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Parcelable outputSelectorState = savedInstanceState.getParcelable(OUTPUT_SELECTOR_KEY);
        myOutputPortSelector.onRestoreInstanceState(outputSelectorState);

        Parcelable inputSelectorState = savedInstanceState.getParcelable(INPUT_SELECTOR_KEY);
        myInputPortSelector.onRestoreInstanceState(inputSelectorState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                break;
            case R.id.action_bluetooth:
                startBLEActivity();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BLUETOOTH_SCAN && resultCode == Activity.RESULT_OK) {
            Object[] devices = data.getParcelableArrayExtra("devices");
            Set<BluetoothDevice> bleDevices = Arrays.stream(devices)
                    .map(x -> (BluetoothDevice)x).collect(Collectors.toSet());
            myMagicModel.addBLEDevices(bleDevices);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        myMidiDeviceListAdapter.clear();
        myMidiManager = null;
        myInputPortSelector.onDestroy();
        myOutputPortSelector.onDestroy();
    }

    @Override
    public void onChanged(Map<BluetoothDevice, MidiDevice> data) {
        List<Map.Entry<BluetoothDevice, MidiDevice>> items = new ArrayList<>(data.entrySet());
        myMidiDeviceListAdapter.setItems(items);
    }

    private void startBLEActivity() {
        Intent bleIntent = new Intent(this, ScanActivity.class);
        startActivityForResult(bleIntent, REQUEST_BLUETOOTH_SCAN);
    }

    public void clickButton(View view) {
        Spinner handlerSpinner = findViewById(R.id.spinner_handler);
        Object selected = handlerSpinner.getSelectedItem();

        if (selected != null) {
            DataWithLabel<Class> data = (DataWithLabel<Class>)selected;
            Class activity = data.getData();
            Intent secondActivity = new Intent(MainActivity.this, activity);
            secondActivity.putExtra("output", myOutputPortSelector.getPortWrapper());
            secondActivity.putExtra("input", myInputPortSelector.getPortWrapper());

            startActivity(secondActivity);
        }
    }

}
