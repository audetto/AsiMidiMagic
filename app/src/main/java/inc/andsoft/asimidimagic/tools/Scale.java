package inc.andsoft.asimidimagic.tools;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Scale extends NoteSequence {

    public Scale(List<Note> notes, List<Double> times) {
        super(notes, times);
    }

    /**
     *
     * @return 0, means not a standard scale
     */
    public int getScaleLength() {
        int firstNote = myNotes.get(0).code;

        int[] tonicIndices = IntStream.range(0, myNotes.size())
                .filter(i -> (myNotes.get(i).code - firstNote) % 12 == 0) // Only keep those indices
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

    public List<Integer> getValidPeriods(int upperBound) {
        int numberOfIntervals = myNotes.size() - 1;
        int maximumPeriod = upperBound > 0 ? upperBound : 12;
        List<Integer> periods = IntStream.rangeClosed(1, maximumPeriod)
                .filter(i -> (numberOfIntervals % i) == 0) // Only keep those indices
                .boxed().collect(Collectors.toList());
        return periods;
    }

    public static class Stats {
        public final double time;           // time of the beat
        public final double vol;            // volatility of this beat
        public final double target;         // target end of the beat
        public final double velocity;       // velocity of this beat

        public Stats(double time, double vol, double target, double velocity) {
            this.time = time;
            this.vol = vol;
            this.target = target;
            this.velocity = velocity;
        }
    }

    public List<Stats> getStatistics(int period) {
        int numberOfNotes = myNotes.size();

        if ((numberOfNotes - 1 ) % period != 0) {
            throw new IllegalArgumentException(String.format(Locale.getDefault(),
                    "Invalid period %d for size %d", period, numberOfNotes));
        }
        ArrayList<SummaryStatistics> beats = new ArrayList<>(period);
        ArrayList<SummaryStatistics> velocities = new ArrayList<>(period);
        for (int i = 0; i < period; ++i) {
            beats.add(new SummaryStatistics());
            velocities.add(new SummaryStatistics());
        }

        for (int i = 0; i < numberOfNotes - 1; ++i) {
            int positionBegin = (i / period) * period;
            double timeBegin = myTimes.get(positionBegin);

            int positionEnd = positionBegin + period;
            double timeEnd = myTimes.get(positionEnd);

            double timeBeat = myTimes.get(i + 1); // time at the end of the beat

            double time = (timeBeat - timeBegin) / (timeEnd - timeBegin);
            int velocity = myNotes.get(i).velocity; // velocity of the beat

            int positionBeat = i % period;
            beats.get(positionBeat).addValue(time);
            velocities.get(positionBeat).addValue(velocity);
        }

        List<Stats> statistics = new ArrayList<>(period);
        for (int i = 0; i < period; ++i) {
            SummaryStatistics statBeat = beats.get(i);

            double mean = statBeat.getMean();
            double stddev = statBeat.getStandardDeviation();
            double vol = stddev / mean;

            double target = (double)(1 + i) / (double)period;

            SummaryStatistics statVelocities = velocities.get(i);
            double velocity = statVelocities.getMean();

            statistics.add(new Stats(mean, vol, target, velocity));
        }

        return statistics;
    }
}
