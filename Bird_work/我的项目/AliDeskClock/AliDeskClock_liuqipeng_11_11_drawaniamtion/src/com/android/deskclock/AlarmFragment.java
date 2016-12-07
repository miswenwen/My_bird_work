package com.android.deskclock;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import yunos.support.v4.app.Fragment;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link yunos.support.v4.app.Fragment} subclass. Activities that
 * contain this fragment must implement the
 * {@link AlarmFragment.OnFragmentInteractionListener} interface to handle
 * interaction events. Use the {@link AlarmFragment#newInstance} factory method
 * to create an instance of this fragment.
 * 
 */
public class AlarmFragment extends Fragment {
    AliAlarmScreen mScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = getActivity();
        AliAlarmScreen screen = mScreen = new AliAlarmScreen(context);
        return screen;
    }

    @Override
    public void onResume() {
        super.onResume();
        mScreen.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScreen.onPause();
    }
}
