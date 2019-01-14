package inc.andsoft.asimidimagic;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.mobileer.miditools.MidiPortSelector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import inc.andsoft.asimidimagic.model.MagicModel;
import inc.andsoft.asimidimagic.tools.DataWithLabel;

public class MainActivity extends BaseActivity implements Observer<Map<BluetoothDevice, MidiDevice>> {
    private final static String TAG = "MainActivity";
    private static final int REQUEST_BLUETOOTH_SCAN = 1;

    private MidiManager myMidiManager;
    private MidiPortSelector myInputPortSelector;
    private MidiPortSelector myOutputPortSelector;
    private MagicModel myMagicModel;
    private MidiDeviceListAdapter myMidiDeviceListAdapter;

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

        RecyclerView recyclerView = findViewById(R.id.list_midi);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        myMidiDeviceListAdapter = new MidiDeviceListAdapter();
        recyclerView.setAdapter(myMidiDeviceListAdapter);

        myMagicModel = ViewModelProviders.of(this).get(MagicModel.class);
        myMagicModel.getDevices().observe(this, this);
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

        myMidiManager = null;
        myInputPortSelector.onDestroy();
        myOutputPortSelector.onDestroy();
    }

    @Override
    public void onChanged(Map<BluetoothDevice, MidiDevice> data) {
        myMidiDeviceListAdapter.setDevices(data);
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
            secondActivity.putExtra("input", myInputPortSelector.getPortWrapper());
            secondActivity.putExtra("output", myOutputPortSelector.getPortWrapper());

            startActivity(secondActivity);
        }
    }

}

// Adapter for holding devices found through scanning.
class MidiDeviceListAdapter extends RecyclerView.Adapter<MidiDeviceListAdapter.MidiViewHolder> {
    private final static String TAG = "MidiDeviceListAdapter";
    private List<Map.Entry<BluetoothDevice, MidiDevice>> myDevices;

    static class MidiViewHolder extends RecyclerView.ViewHolder {

        MidiViewHolder(View view) {
            super(view);
        }

        void setValues(BluetoothDevice ble, MidiDevice midi,
                       CompoundButton.OnClickListener listener) {
            TextView nameView = itemView.findViewById(R.id.device_name);
            TextView midiView = itemView.findViewById(R.id.device_midi);
            Button disconnectButton = itemView.findViewById(R.id.button_disconnect);

            String deviceName = ble.getName();
            if (deviceName != null && deviceName.length() > 0)
                nameView.setText(deviceName);
            else
                nameView.setText(R.string.unknown_device);
            midiView.setText(midi.getInfo().toString());

            disconnectButton.setOnClickListener(listener);
        }
    }

    MidiDeviceListAdapter() {
        myDevices = new ArrayList<>();
    }

    void setDevices(Map<BluetoothDevice, MidiDevice> data) {
        myDevices = new ArrayList<>(data.entrySet());
        notifyDataSetChanged();
    }

    void clear() {
        myDevices.clear();
    }

    @NonNull
    @Override
    public MidiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_ble_device, parent, false);

        MidiViewHolder viewHolder = new MidiViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MidiViewHolder holder, int position) {
        Map.Entry<BluetoothDevice, MidiDevice> data = myDevices.get(position);
        BluetoothDevice bleDevice = data.getKey();
        MidiDevice midiDevice = data.getValue();

        holder.setValues(bleDevice, midiDevice,
                (View v) -> {
                    try {
                        midiDevice.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                });
    }

    @Override
    public int getItemCount() {
        return myDevices.size();
    }

}
