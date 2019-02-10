package inc.andsoft.asimidimagic.models;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import inc.andsoft.asimidimagic.tools.Scale;

public class ScaleModel extends ViewModel {

    private MutableLiveData<List<Scale>> myLiveScales = new MutableLiveData<>();

    public void setScales(Scale leftScale, Scale rightScale) {
        List<Scale> scales = new ArrayList<>();
        scales.add(leftScale);
        scales.add(rightScale);

        myLiveScales.setValue(scales);
    }

    public LiveData<List<Scale>> getScales() { return myLiveScales; }

}
