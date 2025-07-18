package com.example.medialert.ui.refill;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.medialert.R;

public class RefillRemindersFragment extends Fragment {

    private RefillRemindersViewModel refillRemindersViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        refillRemindersViewModel =
                new ViewModelProvider(this).get(RefillRemindersViewModel.class);
        View root = inflater.inflate(R.layout.fragment_refill_reminders, container, false);
        final TextView textView = root.findViewById(R.id.text_refill_reminders);
        refillRemindersViewModel.getText().observe(getViewLifecycleOwner(), s -> {
            textView.setText(s);
        });
        return root;
    }
}