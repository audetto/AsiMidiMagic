package inc.andsoft.asimidimagic;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import inc.andsoft.asimidimagic.models.ScaleModel;
import inc.andsoft.asimidimagic.tools.RecyclerArrayAdapter;
import inc.andsoft.asimidimagic.tools.Scale;
import inc.andsoft.asimidimagic.tools.Utilities;
import inc.andsoft.asimidimagic.views.RhythmChart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


public class ScaleFragment extends Fragment implements Observer<List<Scale>> {

    private RecyclerArrayAdapter<Scale.Stats> myAdapterStats;
    private ArrayAdapter<Integer> myAdapterPeriods;
    private Spinner mySpinnerPeriods;
    private TextView myTextStatus;
    private RhythmChart myChart;
    private Scale myScale;
    private int myIndex;

    public ScaleFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ScaleFragment.
     */
    static ScaleFragment newInstance(int index) {
        ScaleFragment fragment = new ScaleFragment();

        Bundle args = new Bundle();
        args.putInt("index", index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScaleModel scaleModel = ViewModelProviders.of(getActivity()).get(ScaleModel.class);
        LiveData<List<Scale>> liveScales = scaleModel.getScales();
        liveScales.observe(this, this);

        Bundle args = getArguments();
        myIndex = args.getInt("index");
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

                TextView velocity = itemView.findViewById(R.id.velocity);
                velocity.setText(Utilities.getReasonableFormal(data.velocity));

                TextView mean = itemView.findViewById(R.id.mean);
                mean.setText(Utilities.getReasonableFormal(data.mean * 100));

                TextView vol = itemView.findViewById(R.id.vol);
                vol.setText(Utilities.getReasonableFormal(data.vol * 100));

                TextView std = itemView.findViewById(R.id.cumulative);
                std.setText(Utilities.getReasonableFormal(data.cumulative * 100));

                TextView beat = itemView.findViewById(R.id.beat);
                beat.setText(Utilities.getReasonableFormal(data.target * 100));
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

        myChart = view.findViewById(R.id.chart);
    }

    @Override
    public void onChanged(List<Scale> scales) {
        Scale scale = scales.get(myIndex);
        if (scale == null) {
            clear();
        } else {
            setScale(scale);
        }
    }

    private void setPeriod(int period) {
        if (period >= 1) {
            List<Scale.Stats> stats = myScale.getStatistics(period, true);
            myAdapterStats.setItems(stats);
            myAdapterStats.notifyDataSetChanged();

            List<Double> times = stats.stream().map(x -> x.cumulative).collect(Collectors.toList());
            myChart.setNotes(times, period);
        }
    }

    private void clear() {
        myScale = null;
        myAdapterPeriods.clear();
        myAdapterPeriods.notifyDataSetChanged();

        myAdapterStats.clear();
        myAdapterStats.notifyDataSetChanged();

        myTextStatus.setText(null);

        myChart.setNotes(null, 0);
    }

    private void setScale(Scale scale) {
        myScale = scale;

        int scaleLength = myScale.getScaleLength();

        List<Integer> validPeriods = myScale.getValidPeriods(scaleLength);
        myAdapterPeriods.clear();
        myAdapterPeriods.addAll(validPeriods);
        myAdapterPeriods.notifyDataSetChanged();

        int pos = myAdapterPeriods.getPosition(scaleLength);
        if (pos >= 0) {
            mySpinnerPeriods.setSelection(pos);
        }

        String noteName = Utilities.getNoteName(myScale.getNotes().get(0).code);
        String status = String.format(Locale.getDefault(), "%s scale = %d notes", noteName,
                myScale.getNotes().size());

        myTextStatus.setText(status);
    }

}
