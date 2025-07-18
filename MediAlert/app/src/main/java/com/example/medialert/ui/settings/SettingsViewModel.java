package com.example.medialert.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SettingsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public SettingsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Settings Fragment (General App Settings)");
    }

    public LiveData<String> getText() {
        return mText;
    }
}