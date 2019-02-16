package inc.andsoft.asimidimagic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.math3.util.ArithmeticUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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


public class PolyRhythmFragment extends Fragment implements Observer<List<Scale>> {

    private static final String FORMAT_SCALE = "%s: %s (%d) in %d";

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
    static PolyRhythmFragment newInstance() {
        PolyRhythmFragment fragment = new PolyRhythmFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScaleModel scaleModel = ViewModelProviders.of(getActivity()).get(ScaleModel.class);
        LiveData<List<Scale>> liveScales = scaleModel.getScales();
        liveScales.observe(this, this);
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

    static private void processScale(Scale scale, String name, int gcd, RhythmChart chart, TextView text) {
        int numberOfNotes = scale.getTimes().size();

        int period = (numberOfNotes - 1) / gcd;
        List<Scale.Stats> stats = scale.getStatistics(period, true);
        List<Double> times = stats.stream().map(x -> x.cumulative).collect(Collectors.toList());
        chart.setNotes(times, period);

        String noteName = Utilities.getNoteName(scale.getNotes().get(0).code);
        String message = String.format(Locale.getDefault(), FORMAT_SCALE, name, noteName,
                scale.getTimes().size(), period);
        text.setText(message);
    }

    @Override
    public void onChanged(List<Scale> scales) {
        Scale leftScale = scales.get(0);
        Scale rightScale = scales.get(1);

        if (leftScale != null && rightScale != null) {
            int numberOfPeriodsLeft = leftScale.getTimes().size() - 1;
            int numberOfPeriodsRight = rightScale.getTimes().size() - 1;

            int gcd = ArithmeticUtils.gcd(numberOfPeriodsLeft, numberOfPeriodsRight);

            processScale(leftScale, "Left", gcd, myChartLeft, myTextLeft);
            processScale(rightScale, "Right", gcd, myChartRight, myTextRight);
        } else {
            // otherwise clean
            myChartLeft.setNotes(null, 0);
            myChartRight.setNotes(null, 0);

            myTextLeft.setText("Waiting for left scale");
            myTextRight.setText("Waiting for right scale");
        }
    }

}
