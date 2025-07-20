package com.example.pondmatev1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PondSharedViewModel extends ViewModel {

    private final MutableLiveData<PondModel> selectedPond = new MutableLiveData<>();

    public void setSelectedPond(PondModel pond) {
        selectedPond.setValue(pond);
    }

    public LiveData<PondModel> getSelectedPond() {
        return selectedPond;
    }
}
