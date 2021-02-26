package se.linefeed.korjournal;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import org.json.JSONObject;

import java.util.ArrayList;

import se.linefeed.korjournal.api.KorjournalAPI;
import se.linefeed.korjournal.api.JsonAPIResponseInterface;
import se.linefeed.korjournal.models.OdometerSnap;
import se.linefeed.korjournal.models.Position;

public class ReasonActivity extends AppCompatActivity {

    private OdometerSnap mOdometerSnap = null;
    private MyApplication app = null;
    private ArrayList<String> reasons;
    private ArrayAdapter<String> reasonSuggestionAdapter;
    private AutoCompleteTextView reasonEditText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reason);
        reasons = new ArrayList<>();
        reasonSuggestionAdapter = new ArrayAdapter<>(getApplicationContext(),
                R.layout.my_dropdown_item_1line,
                reasons);
        reasonEditText = findViewById(R.id.reasonText);
        reasonEditText.setAdapter(reasonSuggestionAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        app = (MyApplication) getApplicationContext();
        mOdometerSnap = app.getLastOdometerSnap();
        if (mOdometerSnap == null) {
            Log.e("ReasonActivity", "Error no odometerSnap for which to apply Reason");
            goBackCamera();
        }
        updateReasons();
    }

    private void goBackCamera() {
        app.setNextFsmState(MyApplication.FSM_CAMERA);
        finish();
    }

    public void saveReason(View v) {
        final ReasonActivity activity = this;
        final MyApplication app = (MyApplication) getApplicationContext();
        OdometerSnap odometerSnap = app.getLastOdometerSnap();
        if (odometerSnap == null) {
            goBackCamera();
        }
        EditText reasonEditText = findViewById(R.id.reasonText);
        String reason = reasonEditText.getText().toString();
        odometerSnap.setReason(reason);
        KorjournalAPI api = app.getApi();
        api.patch_odometersnap_reason(odometerSnap, new JsonAPIResponseInterface() {
            @Override
            public void done(JSONObject response) {
                app.setNextFsmState(MyApplication.FSM_DONE);
                app.showToast("Sparar...",1);
                activity.finish();
            }

            @Override
            public void error(String error) {
                app.showToast("Ett nätverksfel inträffade (försök igen)",1);
            }
        });
    }

    public void skipSaveReason(View v) {
        MyApplication app = (MyApplication) getApplicationContext();
        app.setNextFsmState(MyApplication.FSM_DONE);
        finish();
    }

    void updateReasons() {
        Position currentPos = app.getPosition();
        if (currentPos == null) {
            return;
        }
        if (reasons == null) {
            reasons = new ArrayList<>();
        } else {
            reasons.clear();
        }
        for (OdometerSnap o: app.getOdoSnaps().asArrayList()) {
            if (o.getReason() == null || o.getReason().equals("")) {
                continue;
            }
            if (reasons.contains(o.getReason())) {
                continue;
            }
            if (currentPos.distanceFrom(o.getPosition()) > 0.5) {
                continue;
            }
            reasons.add(o.getReason());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return OptionsMenu.onCreateOptionsMenu(this, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (OptionsMenu.handleOptionsItemSelected(this,item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
