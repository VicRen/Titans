package me.isvic.titans.fragments;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import me.isvic.titans.R;
import me.isvic.titans.utils.TitansLog;


public class ViewsPreferenceFragment extends PreferenceFragment {

    private static final boolean DEBUG = true;
    private static final String TAG = ViewsPreferenceFragment.class.getSimpleName();

    private static final String KEY_DRAWER_LAYOUT = "drawerlayout";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");
        addPreferencesFromResource(R.xml.views_preference);
        initPreference();
    }

    private void initPreference() {
        PreferenceScreen ps = getPreferenceScreen();
        final int screenCount = ps.getPreferenceCount();
        for (int i = 0; i < screenCount; ++i) {
            ps.getPreference(i).setOnPreferenceClickListener(mClickListener);
        }
    }

    private final OnPreferenceClickListener mClickListener = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            boolean ret = false;
            String key = preference.getKey();
            if (key.equals(KEY_DRAWER_LAYOUT)) {
//                startActivity(new Intent(getActivity(), DrawerLayoutActivity.class));
                ret = true;
            } else if (key.equals(KEY_DRAWER_LAYOUT)) {

            }
            return ret;
        }
    };

    private void log(String msg) {
        if (DEBUG) {
            TitansLog.log(TAG, msg);
        }
    }

}
