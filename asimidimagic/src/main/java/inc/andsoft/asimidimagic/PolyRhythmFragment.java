package inc.andsoft.asimidimagic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.apache.commons.math3.util.ArithmeticUtils;

import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import inc.andsoft.asimidimagic.models.ScaleModel;
import inc.andsoft.asimidimagic.tools.Scale;
import inc.andsoft.support.tools.Utilities;
import inc.andsoft.asimidimagic.views.RhythmChart;


public class PolyRhythmFragment extends Fragment implements Observer<List<Scale>> {

    private int myLeftIndex;
    private int myRightIndex;

    private RhythmChart myChartLeft;
    private RhythmChart myChartRight;

    private TextView myTextLeft;
    private TextView myTextRight;

    public PolyRhythmFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ScaleFragment.
     */
    static PolyRhythmFragment newInstance(int left, int right) {
        PolyRhythmFragment fragment = new PolyRhythmFragment();
        Bundle args = new Bundle();

        args.putInt("left", left);
        args.putInt("right", right);
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
        myLeftIndex = args.getInt("left");
        myRightIndex = args.getInt("right");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_polyrhythm, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        myChartLeft = view.findViewById(R.id.chart_left);
        myChartRight = view.findViewById(R.id.chart_right);

        myTextLeft = view.findViewById(R.id.text_left);
        myTextRight = view.findViewById(R.id.text_right);
    }

    private int processScale(Scale scale, String name, int gcd, RhythmChart chart, TextView text) {
        int numberOfNotes = scale.getTimes().size();

        int period = (numberOfNotes - 1) / gcd;
        List<Scale.Stats> stats = scale.getStatistics(period);
        List<Double> times = stats.stream().map(x -> x.time).collect(Collectors.toList());
        times.add(0, 0.0);
        chart.setNotes(times);

        String noteName = Utilities.getNoteName(scale.getNotes().get(0).code);
        String message = getString(R.string.polyrhythm_scale, name, noteName,
                scale.getTimes().size(), period);
        text.setText(message);

        return period;
    }

    @Override
    public void onChanged(List<Scale> scales) {
        Scale leftScale = scales.get(myLeftIndex);
        Scale rightScale = scales.get(myRightIndex);

        if (leftScale != null && rightScale != null) {
            int numberOfPeriodsLeft = leftScale.getTimes().size() - 1;
            int numberOfPeriodsRight = rightScale.getTimes().size() - 1;

            int gcd = ArithmeticUtils.gcd(numberOfPeriodsLeft, numberOfPeriodsRight);

            int leftPeriod = processScale(leftScale, getString(R.string.left), gcd, myChartLeft, myTextLeft);
            int rightPeriod = processScale(rightScale, getString(R.string.right), gcd, myChartRight, myTextRight);

            int bars = leftPeriod * rightPeriod;
            myChartLeft.setBeats(bars);
            myChartRight.setBeats(bars);
        } else {
            // otherwise clean
            myChartLeft.setNotes(null);
            myChartRight.setNotes(null);

            myTextLeft.setText(R.string.chart_waiting_left);
            myTextRight.setText(R.string.chart_waiting_right);
        }
    }

}
