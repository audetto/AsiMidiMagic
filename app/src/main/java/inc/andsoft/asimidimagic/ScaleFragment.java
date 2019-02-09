package inc.andsoft.asimidimagic;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import inc.andsoft.asimidimagic.tools.RecyclerArrayAdapter;
import inc.andsoft.asimidimagic.tools.Scale;
import inc.andsoft.asimidimagic.tools.Utilities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;


public class ScaleFragment extends Fragment {

    private RecyclerArrayAdapter<Scale.Stats> myAdapterStats;
    private ArrayAdapter<Integer> myAdapterPeriods;
    private Spinner mySpinnerPeriods;
    private TextView myTextStatus;
    private Scale myScale;

    public ScaleFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ScaleFragment.
     */
    static ScaleFragment newInstance() {
        ScaleFragment fragment = new ScaleFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scale, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        myAdapterStats = new RecyclerArrayAdapter<Scale.Stats>(R.layout.listitem_scale_stats) {
            @Override
            public void populateView(@NonNull View itemView, int position, @NonNull Scale.Stats data) {
                TextView grade = itemView.findViewById(R.id.grade);
                grade.setText(String.valueOf(1 + position));

                TextView mean = itemView.findViewById(R.id.mean);
                mean.setText(Utilities.getPercentageFormat(data.mean));

                TextView std = itemView.findViewById(R.id.std);
                std.setText(Utilities.getPercentageFormat(data.std));

                TextView vol = itemView.findViewById(R.id.vol);
                vol.setText(Utilities.getPercentageFormat(data.vol));

                TextView target = itemView.findViewById(R.id.target);
                target.setText(Utilities.getPercentageFormat(data.target));
            }
        };
        RecyclerView recyclerViewStats = view.findViewById(R.id.recycler_stats);
        recyclerViewStats.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewStats.setAdapter(myAdapterStats);

        myAdapterPeriods = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item);
        myAdapterPeriods.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);

        mySpinnerPeriods = view.findViewById(R.id.spinner_periods);
        mySpinnerPeriods.setAdapter(myAdapterPeriods);

        mySpinnerPeriods.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int pos, long id) {
                        Integer period = myAdapterPeriods.getItem(pos);
                        setPeriod(period);
                    }

                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

        myTextStatus = view.findViewById(R.id.text_status);
    }

    private void setPeriod(int period) {
        if (period > 1) {
            List<Scale.Stats> stats = myScale.getStatistics(period, true);
            myAdapterStats.setItems(stats);
            myAdapterStats.notifyDataSetChanged();
        }
    }

    void clear() {
        myScale = null;
        myAdapterPeriods.clear();
        myAdapterPeriods.notifyDataSetChanged();

        myAdapterStats.clear();
        myAdapterStats.notifyDataSetChanged();

        myTextStatus.setText(null);
    }

    void setScale(Scale scale) {
        myScale = scale;

        List<Integer> validPeriods = myScale.getValidPeriods();
        myAdapterPeriods.clear();
        myAdapterPeriods.addAll(validPeriods);
        myAdapterPeriods.notifyDataSetChanged();

        int scaleLength = myScale.getScaleLength();
        int pos = myAdapterPeriods.getPosition(scaleLength);
        if (pos >= 0) {
            mySpinnerPeriods.setSelection(pos);
        }

        String status = String.format(Locale.getDefault(), "%d scale = %d notes",
                myScale.getNotes().get(0), myScale.getNotes().size());

        myTextStatus.setText(status);
    }

}
