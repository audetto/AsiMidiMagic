package inc.andsoft.asimidimagic;

import android.content.Intent;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.MidiPortWrapper;

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
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;

public class ScaleActivity extends CommonActivity {
    private MidiOutputPort myOutputPort;
    private MidiFramer myFramer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scale);
        setActionBar();

        Intent intent = getIntent();
        MidiPortWrapper output = intent.getParcelableExtra("output");

        TextView outputText = findViewById(R.id.output_name);
        outputText.setText(output.toString());

        ScaleModel scaleModel = ViewModelProviders.of(this).get(ScaleModel.class);

        myMidiDeviceOpener.queueDevice(output);

        MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        myMidiDeviceOpener.execute(midiManager, (MidiDeviceOpener opener) -> {
            myOutputPort = opener.openOutputPort(output);

            if (myOutputPort != null) {
                MidiReceiver midiScales = new MidiScales() {
                    public void onChangeState(@NonNull MidiScales.State state, @NonNull String message) {
                        runOnUiThread(() -> {
                            TextView statusView = findViewById(R.id.text_status);
                            statusView.setText(message);

                            if (state == State.FIRST_NOTE) {
                                scaleModel.setScales(null, null);
                            }
                        });
                    }
                    public void complete(Scale leftScale, Scale rightScale) {
                        runOnUiThread(() -> scaleModel.setScales(leftScale, rightScale));
                    }
                };
                myFramer = new MidiFramer(midiScales);
                connect();
            } else {
                Toast.makeText(ScaleActivity.this, "Missing MIDI port", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        PagerAdapter adapter = new SimpleFragmentPagerAdapter(getSupportFragmentManager());
        ViewPager pager = findViewById(R.id.viewpager);
        pager.setAdapter(adapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(pager);
    }

    void connect() {
        if (myOutputPort != null && myFramer != null) {
            myOutputPort.connect(myFramer);
        }
    }

    void disconnect() {
        if (myOutputPort != null && myFramer != null) {
            // first detach the input port (wrapped in the framer)
            myOutputPort.disconnect(myFramer);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();

        myFramer = null;
        myOutputPort = null;
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
                return PolyRhythmFragment.newInstance();
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
