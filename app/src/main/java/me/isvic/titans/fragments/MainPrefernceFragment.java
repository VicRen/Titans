package me.isvic.titans.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import me.isvic.titans.QuickContactActivity;
import me.isvic.titans.R;
import me.isvic.titans.SlideUpActivity;

public class MainPrefernceFragment extends PreferenceFragment {

    private static final String KEY_MULTI_SKRINK_SCROLLER = "key_multi_skrink_scroller";
    private static final String KEY_MULTI_SKRINK_LAYOUT = "key_multi_skrink_layout";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_preference);
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
            if (key.equals(KEY_MULTI_SKRINK_SCROLLER)) {
                startActivity(new Intent(getActivity(), SlideUpActivity.class));
                ret = true;
            } else if (key.equals(KEY_MULTI_SKRINK_LAYOUT)) {
                startActivity(new Intent(getActivity(), QuickContactActivity.class));
                ret = true;
            }
            return ret;
        }
    };

}
