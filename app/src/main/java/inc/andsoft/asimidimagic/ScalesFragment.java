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


public class ScalesFragment extends Fragment {

    private RecyclerArrayAdapter<Scale.Stats> myAdapterStats;
    private ArrayAdapter<Integer> myAdapterPeriods;
    private Spinner mySpinnerPeriods;
    private Scale myScale;

    public ScalesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ScalesFragment.
     */
    static ScalesFragment newInstance() {
        ScalesFragment fragment = new ScalesFragment();
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
        return inflater.inflate(R.layout.fragment_scales, container, false);
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

                TextView ratio = itemView.findViewById(R.id.ratio);
                ratio.setText(Utilities.getPercentageFormat(data.ratio));

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
    }

    private void setPeriod(int period) {
        if (period > 1) {
            List<Scale.Stats> stats = myScale.getStatistics(period, true);
            myAdapterStats.setItems(stats);
            myAdapterStats.notifyDataSetChanged();
        }
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
    }

}