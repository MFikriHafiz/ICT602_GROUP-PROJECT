package com.example.medialert.ui.refill;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RefillRemindersViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public RefillRemindersViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Refill Reminders Fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}