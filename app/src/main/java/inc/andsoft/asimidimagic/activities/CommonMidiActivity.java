package inc.andsoft.asimidimagic.activities;

import android.view.Menu;

import inc.andsoft.asimidimagic.R;
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;

public abstract class CommonMidiActivity extends BaseActivity {

    protected MidiDeviceOpener myMidiDeviceOpener = new MidiDeviceOpener();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myMidiDeviceOpener.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_midi, menu);
        return true;
    }

}
