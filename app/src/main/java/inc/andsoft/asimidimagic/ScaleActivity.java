package inc.andsoft.asimidimagic;

import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.mobileer.miditools.MidiFramer;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import inc.andsoft.asimidimagic.midi.MidiScales;
import inc.andsoft.asimidimagic.models.ScaleModel;
import inc.andsoft.asimidimagic.tools.Scale;

public class ScaleActivity extends CommonMidiSinkActivity<MidiScales> {
    private ScaleModel myScaleModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myScaleModel = ViewModelProviders.of(this).get(ScaleModel.class);

        PagerAdapter adapter = new SimpleFragmentPagerAdapter(getSupportFragmentManager());
        ViewPager pager = findViewById(R.id.viewpager);
        pager.setAdapter(adapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(pager);
    }

    @Override
    protected MidiScales getReceiver() {
        MidiScales midiScales = new MidiScales() {
            @Override
            public void onChangeState(@NonNull MidiScales.State state, @NonNull String message) {
                runOnUiThread(() -> {
                    TextView statusView = findViewById(R.id.text_status);
                    statusView.setText(message);

                    if (state == State.FIRST_NOTE) {
                        myScaleModel.setScales(null, null);
                    }
                });
            }

            @Override
            public void complete(Scale leftScale, Scale rightScale) {
                runOnUiThread(() -> myScaleModel.setScales(leftScale, rightScale));
            }
        };
        return midiScales;
    }

    @Override
    protected @LayoutRes
    int getLayoutID() {
        return R.layout.activity_scale;
    }

}

class SimpleFragmentPagerAdapter extends FragmentStatePagerAdapter {

    SimpleFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return ScaleFragment.newInstance(0, 1);
            case 1:
                return ScaleFragment.newInstance(1, 0);
            case 2:
                return PolyRhythmFragment.newInstance(0, 1);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Left";
            case 1:
                return "Right";
            case 2:
                return "Polyrhythm";
            default:
                return null;
        }
    }

}
