package me.isvic.titans;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import me.isvic.titans.fragments.ViewsPreferenceFragment;
import me.isvic.titans.utils.TitansLog;

public class SecondActivity extends Activity {

    private static final boolean DEBUG = true;
    private static String TAG = SecondActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");
        setContentView(R.layout.main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new ViewsPreferenceFragment()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static void log(String msg) {
        if (DEBUG) {
            TitansLog.log(TAG, msg);
        }
    }

}
