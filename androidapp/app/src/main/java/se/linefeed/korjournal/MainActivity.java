package se.linefeed.korjournal;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

import se.linefeed.korjournal.R;

public class MainActivity extends AppCompatActivity {
    Bitmap image;
    private ImageView odoImage;
    private MyApplication application = null;
    private Handler delayedActionHandler = null;
    private Runnable reviewDialogRunnable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        delayedActionHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        application = (MyApplication) getApplicationContext();
        switch (application.getNextFsmState()) {
            case MyApplication.FSM_REASON:
                cancelAskForReview();
                startReasonActivity(null);
                return;
            case MyApplication.FSM_DONE:
                askForReview();
                return;
            case MyApplication.FSM_SELECT:
                cancelAskForReview();
                startSelectkmActivity(null);
                return;
            default:
                cancelAskForReview();
                application.setNextFsmState(MyApplication.FSM_CAMERA);
                startCameraActivity(null);
                return;
        }
    }

    public void startCameraActivity(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    public void selectStartCameraActivity(View view) {
        File dir = new File(getExternalFilesDir(null), "kilometerkoll");
        File[] photoFiles = dir.listFiles();

        if (photoFiles != null) {
            for (File deleteIt: photoFiles) {
                deleteIt.delete();
            }
        }
        startCameraActivity(view);
    }
    public void startSelectkmActivity(View view) {
        Intent intent = new Intent(this, SelectkmActivity.class);
        startActivity(intent);
    }

    public void startReasonActivity(View view) {
        Intent intent = new Intent(this, ReasonActivity.class);
        startActivity(intent);
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

    protected void askForReview() {
        if (!ReviewRequestHelper.reviewConditionsMet(this)) {
            return;
        }
        reviewDialogRunnable = new Runnable() {
            @Override
            public void run() {
                ReviewRequestDialogFragment reviewRequest = new ReviewRequestDialogFragment();
                reviewRequest.show(getFragmentManager(),"ask_review");
            }
        };
        delayedActionHandler.postDelayed(reviewDialogRunnable, 3000);
    }
    protected void cancelAskForReview() {
        if (delayedActionHandler == null)
            return;
        if (reviewDialogRunnable == null)
            return;
        delayedActionHandler.removeCallbacks(reviewDialogRunnable);
        reviewDialogRunnable = null;
    }
}
