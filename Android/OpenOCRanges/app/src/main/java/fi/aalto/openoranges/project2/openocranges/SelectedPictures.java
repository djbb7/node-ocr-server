package fi.aalto.openoranges.project2.openocranges;

import android.app.Application;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;


public class SelectedPictures extends Application {

    private List<Uri> mSelectedPictures = new ArrayList<>();

    public List<Uri> getSelectedPictures() {
        return mSelectedPictures;
    }

    public void setSelectedPictures(Uri mUri) {
        mSelectedPictures.add(mUri);
    }
}
