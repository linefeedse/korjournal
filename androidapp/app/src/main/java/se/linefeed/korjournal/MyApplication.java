package se.linefeed.korjournal;

import android.app.Application;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Toast;

import se.linefeed.korjournal.api.KorjournalAPI;
import se.linefeed.korjournal.api.RequestDoneInterface;
import se.linefeed.korjournal.api.TeslaAPI;
import se.linefeed.korjournal.models.OdometerSnap;
import se.linefeed.korjournal.models.OdometerSnapArray;
import se.linefeed.korjournal.models.Position;
import se.linefeed.korjournal.models.TeslaVehicle;

public class MyApplication extends Application {

    public static final int FSM_CAMERA = 1;
    public static final int FSM_SELECT = 2;
    public static final int FSM_REASON = 3;
    public static final int FSM_DONE = 4;

    private int fsmState = 0;
    private Boolean pictureTaken = false;

    private OdometerSnap lastOdometerSnap = null;
    private KorjournalAPI api;
    private OdometerSnapArray odoSnaps;
    private Position position;

    public int getNextFsmState() {
        return fsmState;
    }

    public void setNextFsmState(int fsmState) {
        this.fsmState = fsmState;
    }

    public Boolean getPictureTaken() {
        return pictureTaken;
    }

    public void setPictureTaken(Boolean pictureTaken) {
        this.pictureTaken = pictureTaken;
    }

    public OdometerSnap getLastOdometerSnap() {
        return lastOdometerSnap;
    }

    public void setLastOdometerSnap(OdometerSnap lastOdometerSnap) {
        this.lastOdometerSnap = lastOdometerSnap;
    }

    public KorjournalAPI getApi() {
        if (api == null) {
            api = new KorjournalAPI(this);
        }
        return api;
    }

    public void requestOdosnaps(RequestDoneInterface requestDone) {

        odoSnaps = new OdometerSnapArray(getApi(),this, requestDone);
    }

    public OdometerSnapArray getOdoSnaps() {
        return odoSnaps;
    }

    public void setLocation(double lat, double lon) {
        position = new Position(lat, lon);
    }

    public Position getPosition() {
        return position;
    }

    void showToast(String msg, int row) {
        Resources r = getResources();
        int toastOffsetPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, r.getDisplayMetrics());
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0,row*toastOffsetPx);
        toast.show();
    }
}
