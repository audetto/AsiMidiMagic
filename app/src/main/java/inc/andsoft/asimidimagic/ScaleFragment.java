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
import inc.andsoft.support.tools.Utilities;
import inc.andsoft.asimidimagic.views.RhythmChart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.math3.util.ArithmeticUtils;

import java.util.List;
import java.util.stream.Collectors;


public class ScaleFragment extends Fragment implements Observer<List<Scale>> {

    private RecyclerArrayAdapter<Scale.Stats> myAdapterStats;
    private ArrayAdapter<Integer> myAdapterPeriods;
    private Spinner mySpinnerPeriods;
    private TextView myTextStatus;
    private RhythmChart myChart;
    private Scale myScale;

    private int myScaleIndex;
    private int myOtherIndex;

    public ScaleFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ScaleFragment.
     */
    static ScaleFragment newInstance(int index, int other) {
        ScaleFragment fragment = new ScaleFragment();

        Bundle args = new Bundle();
        args.putInt("index", index);
        args.putInt("other", other);
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
        myScaleIndex = args.getInt("index");
        myOtherIndex = args.getInt("other");
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
                velocity.setText(String.valueOf(Math.round(data.velocity)));

                TextView vol = itemView.findViewById(R.id.vol);
                vol.setText(String.valueOf(Math.round(data.vol * 1000)));

                TextView time = itemView.findViewById(R.id.time);
                time.setText(String.valueOf(Math.round(data.time * 1000)));

                TextView target = itemView.findViewById(R.id.target);
                target.setText(String.valueOf(Math.round(data.target * 1000)));
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
        Scale scale = scales.get(myScaleIndex);
        if (scale == null) {
            clear();
        } else {
            Scale other = scales.get(myOtherIndex);
            setScale(scale, other);
        }
    }

    private void setPeriod(int period) {
        if (period >= 1) {
            List<Scale.Stats> stats = myScale.getStatistics(period);
            myAdapterStats.setItems(stats);
            myAdapterStats.notifyDataSetChanged();

            List<Double> times = stats.stream().map(x -> x.time).collect(Collectors.toList());
            times.add(0, 0.0);
            myChart.setNotes(times);
        }
    }

    private void clear() {
        myScale = null;
        myAdapterPeriods.clear();
        myAdapterPeriods.notifyDataSetChanged();

        myAdapterStats.clear();
        myAdapterStats.notifyDataSetChanged();

        myTextStatus.setText(null);

        myChart.setNotes(null);
    }

    private void setScale(Scale scale, Scale other) {
        myScale = scale;

        int scaleLength = myScale.getScaleLength();

        List<Integer> validPeriods = myScale.getValidPeriods(scaleLength);
        myAdapterPeriods.clear();
        myAdapterPeriods.addAll(validPeriods);
        myAdapterPeriods.notifyDataSetChanged();

        // we try to guess what is a good period for the display

        int bestPos = myAdapterPeriods.getPosition(scaleLength);
        int numberOfPeriodsScale = myScale.getTimes().size() - 1;
        int numberOfPeriodsOther = other.getTimes().size() - 1;

        if (numberOfPeriodsOther != numberOfPeriodsScale) {
            int gcd = ArithmeticUtils.gcd(numberOfPeriodsScale, numberOfPeriodsOther);
            int periodLength = numberOfPeriodsScale / gcd;
            int periodPos = myAdapterPeriods.getPosition(periodLength);
            if (periodPos >= 0) {
                bestPos = periodPos;
            }
        }

        if (bestPos >= 0) {
            mySpinnerPeriods.setSelection(bestPos);
        }

        String noteName = Utilities.getNoteName(myScale.getNotes().get(0).code);
        String status = getString(R.string.scale_notes, noteName, myScale.getNotes().size());

        myTextStatus.setText(status);
    }

}
