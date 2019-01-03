package inc.andsoft.asimidimagic;

import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;

public class CommonActivity extends BaseActivity {

    protected MidiDeviceOpener myMidiDeviceOpener = new MidiDeviceOpener();

    @Override
    protected void onDestroy() {
        close();
        super.onDestroy();
    }

    protected void close() {
        myMidiDeviceOpener.close();
    }

}
