package inc.andsoft.asimidimagic.activities;

import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;

public abstract class CommonMidiActivity extends BaseActivity {

    protected MidiDeviceOpener myMidiDeviceOpener = new MidiDeviceOpener();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myMidiDeviceOpener.close();
    }

}
