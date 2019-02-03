package inc.andsoft.asimidimagic;

import android.content.Intent;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.MidiPortWrapper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import inc.andsoft.asimidimagic.midi.MidiScales;
import inc.andsoft.asimidimagic.tools.Scale;
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;

public class ScaleActivity extends CommonActivity {
    private MidiOutputPort myOutputPort;
    private MidiFramer myFramer;
    private MidiScales myScales;
    private SimpleFragmentPagerAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scale);
        setActionBar();

        Intent intent = getIntent();
        MidiPortWrapper output = intent.getParcelableExtra("output");

        TextView outputText = findViewById(R.id.output_name);
        outputText.setText(output.toString());

        myMidiDeviceOpener.queueDevice(output);

        MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        myMidiDeviceOpener.execute(midiManager, (MidiDeviceOpener opener) -> {
            myOutputPort = opener.openOutputPort(output);

            if (myOutputPort != null) {
                myScales = new MidiScales() {
                    public void onChangeState(@NonNull MidiScales.State state, @NonNull String message) {
                        runOnUiThread(() -> {
                            TextView statusView = findViewById(R.id.text_status);
                            statusView.setText(message);
                        });
                    }
                    public void complete(Scale leftScale, Scale rightScale) {
                        runOnUiThread(() -> processScales(leftScale, rightScale));
                    }
                };
                myFramer = new MidiFramer(myScales);
                connect();
            } else {
                Toast.makeText(ScaleActivity.this, "Missing MIDI port", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        myAdapter = new SimpleFragmentPagerAdapter(getSupportFragmentManager());
        ViewPager pager = findViewById(R.id.viewpager);
        pager.setAdapter(myAdapter);
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

    void processScales(Scale leftScale, Scale rightScale) {
        ScalesFragment leftFragment = (ScalesFragment)myAdapter.getRegisteredFragment(0);
        leftFragment.setScale(leftScale);
        ScalesFragment rightFragment = (ScalesFragment)myAdapter.getRegisteredFragment(1);
        rightFragment.setScale(rightScale);
    }

    @Override
    protected void close() {
        disconnect();

        myScales = null;
        myFramer = null;
        myOutputPort = null;

        super.close();
    }
}

class SimpleFragmentPagerAdapter extends FragmentPagerAdapter {

    private SparseArray<Fragment> myRegisteredFragments = new SparseArray<>();

    SimpleFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public Fragment getRegisteredFragment(int position) {
        return myRegisteredFragments.get(position);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return ScalesFragment.newInstance();
            case 1:
                return ScalesFragment.newInstance();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Left";
            case 1:
                return "Right";
            default:
                return null;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        myRegisteredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        myRegisteredFragments.remove(position);
        super.destroyItem(container, position, object);
    }
}
