package inc.andsoft.asimidimagic;

import android.support.v7.app.AppCompatActivity;
import inc.andsoft.asimidimagic.tools.MidiDeviceOpener;

public class CommonActivity extends AppCompatActivity {

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
