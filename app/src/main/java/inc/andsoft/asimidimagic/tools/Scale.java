package inc.andsoft.asimidimagic.tools;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Scale {

    private List<Integer> myNotes;
    private List<Long> myTimes;

    public Scale(List<Integer> notes, List<Long> times) {
        myNotes = notes;
        myTimes = times;
    }

    public List<Integer> getNotes() {
        return myNotes;
    }

    public List<Long> getTimes() {
        return myTimes;
    }

    public int getLowestNote() {
        return Collections.min(myNotes);
    }

    public int getHighestNote() {
        return Collections.max(myNotes);
    }

    /**
     *
     * @return 0, means not a standard scale
     */
    public int getScaleLength() {
        int firstNote = myNotes.get(0);

        int[] tonicIndices = IntStream.range(0, myNotes.size())
                .filter(i -> (myNotes.get(i) - firstNote) % 12 == 0) // Only keep those indices
                .toArray();

        Set<Integer> differences = new HashSet<>();

        for (int i = 1; i < tonicIndices.length; ++i) {
            differences.add(tonicIndices[i] - tonicIndices[i - 1]);
        }

        Integer [] uniques = differences.toArray(new Integer[0]);
        if (uniques.length == 1) {
            return uniques[0];
        } else {
            return 0;
        }
    }

    public List<Integer> getValidPeriods() {
        int numberOfIntervals = myNotes.size() - 1;
        List<Integer> periods = IntStream.range(2, numberOfIntervals)
                .filter(i -> (numberOfIntervals % i) == 0) // Only keep those indices
                .boxed().collect(Collectors.toList());
        return periods;
    }

    public static class Stats {
        public final double mean;
        public final double std;
        public final double ratio;
        public final double target;

        public Stats(double mean, double std, double ratio, double target) {
            this.mean = mean;
            this.std = std;
            this.ratio = ratio;
            this.target = target;
        }
    }

    public List<Stats> getStatistics(int period, boolean normalise) {
        int numberOfNotes = myNotes.size();

        if (numberOfNotes % period != 1) {
            throw new IllegalArgumentException(String.format(Locale.getDefault(),
                    "Invalid period %d for size %d", period, numberOfNotes));
        }

        double coefficient = 1.0;
        if (normalise) {
            long totalTime = myTimes.get(numberOfNotes - 1) - myTimes.get(0);
            long numberOfGroups = (numberOfNotes - 1) / period;
            coefficient = (double)numberOfGroups / (double)totalTime;
        }

        ArrayList<SummaryStatistics> deltas = new ArrayList<>(period);
        for (int i = 0; i < period; ++i) {
            deltas.add(new SummaryStatistics());
        }

        long previous = myTimes.get(0);
        int position = 0;
        for (int i = 1; i < numberOfNotes; ++i) {
            long current = myTimes.get(i);
            double delta = (current - previous) * coefficient;
            deltas.get(position).addValue(delta);
            previous = current;
            position = (position + 1) % period;
        }

        List<Stats> statistics = new ArrayList<>(period);
        double target = 1.0 / period;
        for (int i = 0; i < period; ++i) {
            SummaryStatistics stats = deltas.get(i);
            double mean = stats.getMean();
            double stddev = stats.getStandardDeviation();
            double ratio = stddev / mean;

            statistics.add(new Stats(mean, stddev, ratio, target));
        }

        return statistics;
    }
}
