package com.makeability.protosound.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.makeability.protosound.R;
import com.makeability.protosound.ui.home.service.ForegroundService;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
	/**
	 * Recording
	 */

	public static boolean IS_RECORDING = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }


	/**
	 * Run this function to start streaming audio and send prediction back to phone
	 */
	private void onRecordClick() {
    	if (!IS_RECORDING) {
			startRecording(requireActivity());
		} else {
    		stopRecording(requireActivity());
		}
    	// Flip the flag so we can turn off/on next time
    	IS_RECORDING = !IS_RECORDING;
	}

	private void startRecording(final Context main) {
		Intent serviceIntent = new Intent(main, ForegroundService.class);
		ContextCompat.startForegroundService(main, serviceIntent);
	}

	public void stopRecording(final Context main) {
		Intent serviceIntent = new Intent(main, ForegroundService.class);
		main.stopService(serviceIntent);
	}

}
