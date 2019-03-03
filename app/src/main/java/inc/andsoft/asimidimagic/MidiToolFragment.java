package inc.andsoft.asimidimagic;

import android.content.SharedPreferences;
import android.media.midi.MidiReceiver;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import inc.andsoft.asimidimagic.tools.MidiCommands;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class MidiToolFragment extends Fragment {

    private MidiReceiver myReceiver;

    public MidiToolFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_midi_tools, container, false);

        Button allNotesOff = view.findViewById(R.id.button_all_notes_off);
        allNotesOff.setOnClickListener(v -> {
            if (myReceiver != null) {
                List<Integer> channels = getSelectedChannels();
                try {
                    MidiCommands.allNotesOff(myReceiver, channels);
                } catch (Exception e) {

                }
            }
        });

        Switch localControl = view.findViewById(R.id.switch_local_control);
        localControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (myReceiver != null) {
                List<Integer> channels = getSelectedChannels();
                try {
                    MidiCommands.localControl(myReceiver, channels, isChecked);
                } catch (Exception e) {

                }
            }
        });

        return view;
    }

    public void setReceiver(MidiReceiver receiver) {
        myReceiver = receiver;
    }

    private List<Integer> getSelectedChannels() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Set<String> values = preferences.getStringSet("list_channels", null);

        List<Integer> channels;
        if (values != null) {
            channels = values.stream().map(Integer::valueOf).collect(Collectors.toList());
        } else {
            channels = new ArrayList<>();
        }
        return channels;
    }

}
