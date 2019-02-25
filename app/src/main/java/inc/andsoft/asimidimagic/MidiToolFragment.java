package inc.andsoft.asimidimagic;

import android.media.midi.MidiReceiver;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import inc.andsoft.asimidimagic.tools.Utilities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;


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
                try {
                    Utilities.allNotesOff(myReceiver);
                } catch (Exception e) {

                }
            }
        });

        Switch localControl = view.findViewById(R.id.switch_local_control);
        localControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (myReceiver != null) {
                try {
                    Utilities.localControl(myReceiver, isChecked);
                } catch (Exception e) {

                }
            }
        });

        return view;
    }

    public void setReceiver(MidiReceiver receiver) {
        myReceiver = receiver;
    }
}
