package inc.andsoft.asimidimagic.tools.summary;

import android.text.TextUtils;

import java.util.Set;

import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

public class MultiSelectSummaryProvider implements Preference.SummaryProvider<MultiSelectListPreference> {

    private static MultiSelectSummaryProvider sSimpleSummaryProvider;

    private MultiSelectSummaryProvider() {}

    public static MultiSelectSummaryProvider getInstance() {
        if (sSimpleSummaryProvider == null) {
            sSimpleSummaryProvider = new MultiSelectSummaryProvider();
        }
        return sSimpleSummaryProvider;
    }

    @Override
    public CharSequence provideSummary(MultiSelectListPreference preference) {
        Set<String> values = preference.getValues();

        CharSequence summary = TextUtils.join(", ", values);
        return summary;
    }

}
