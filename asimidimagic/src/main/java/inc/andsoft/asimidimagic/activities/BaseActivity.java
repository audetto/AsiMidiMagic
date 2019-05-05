package inc.andsoft.asimidimagic.activities;

import android.content.Intent;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import inc.andsoft.asimidimagic.R;
import inc.andsoft.asimidimagic.SettingsActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected Toolbar setActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        return toolbar;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startSettingsActivity();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void startSettingsActivity() {
        Intent settingIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingIntent);
    }

}
