package inc.andsoft.asimidimagic;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import inc.andsoft.asimidimagic.activities.BaseActivity;
import inc.andsoft.asimidimagic.tools.summary.MultiSelectSummaryProvider;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setActionBar();

        Fragment preferences = new GeneralPreferenceFragment();

        getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, preferences).commit();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_general, rootKey);

            findPreference("list_piano").setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            findPreference("list_channels").setSummaryProvider(MultiSelectSummaryProvider.getInstance());
        }
    }

}
