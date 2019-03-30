package inc.andsoft.asimidimagic;

import android.content.SharedPreferences;
import android.media.midi.MidiReceiver;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import inc.andsoft.asimidimagic.tools.MidiCommands;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class MidiToolFragment extends Fragment {

    private MidiReceiver myReceiver;
    private SharedPreferences myPreferences;
    private Switch myLocalControl;

    public MidiToolFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        myPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_midi_tools, container, false);

        Button allNotesOff = view.findViewById(R.id.button_all_notes_off);
        allNotesOff.setOnClickListener(v -> {
            if (myReceiver != null) {
                List<Integer> channels = getSelectedChannels();
                MidiCommands.allNotesOff(myReceiver, channels);
            }
        });

        myLocalControl = view.findViewById(R.id.switch_local_control);
        myLocalControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (myReceiver != null) {
                List<Integer> channels = getSelectedChannels();
                MidiCommands.localControl(myReceiver, channels, isChecked);
            }
        });

        RadioButton multiTimbreOff = view.findViewById(R.id.radio_multi_off);
        multiTimbreOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && (myReceiver != null)) {
                MidiCommands.multiTimbre(myReceiver, MidiCommands.MultiTimbre.OFF);
            }
        });

        RadioButton multiTimbreOn1 = view.findViewById(R.id.radio_multi_on1);
        multiTimbreOn1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && (myReceiver != null)) {
                MidiCommands.multiTimbre(myReceiver, MidiCommands.MultiTimbre.ON1);
            }
        });

        RadioButton multiTimbreOn2 = view.findViewById(R.id.radio_multi_on2);
        multiTimbreOn2.setOnCheckedChangeListener((buttonView, isChecked) -> {
        if (isChecked && (myReceiver != null)) {
                MidiCommands.multiTimbre(myReceiver, MidiCommands.MultiTimbre.ON2);
            }
        });

        SeekBar sustainBar = view.findViewById(R.id.seek_sustain);
        sustainBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                List<Integer> channels = getSelectedChannels();
                MidiCommands.sustainPedal(myReceiver, channels, (byte)progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        return view;
    }

    public void setReceiver(MidiReceiver receiver) {
        myReceiver = receiver;
    }

    public void setLocalControl(boolean enable) {
        myLocalControl.setChecked(enable);
    }

    private List<Integer> getSelectedChannels() {
        Set<String> values = myPreferences.getStringSet("list_channels", null);

        List<Integer> channels;
        if (values != null) {
            channels = values.stream().map(Integer::valueOf).collect(Collectors.toList());
        } else {
            channels = new ArrayList<>();
        }

        return channels;
    }

}
