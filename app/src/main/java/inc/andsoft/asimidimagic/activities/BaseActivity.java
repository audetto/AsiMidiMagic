package inc.andsoft.asimidimagic.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import inc.andsoft.asimidimagic.R;

public abstract class BaseActivity extends AppCompatActivity {

    protected Toolbar setActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        return toolbar;
    }

}
