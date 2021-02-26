package se.linefeed.korjournal;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import se.linefeed.korjournal.R;

public class OptionsMenu {
    public static boolean onCreateOptionsMenu(AppCompatActivity activity, Menu menu) {
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.actionbar_main, menu);
        return true;
    }

    static void showSettings(AppCompatActivity activity) {
        Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }

    public static boolean handleOptionsItemSelected(AppCompatActivity activity, MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_settings:
                showSettings(activity);
                return true;
           case R.id.action_selectlog:
                intent = new Intent(activity, SelectLogActivity.class);
                activity.startActivity(intent);
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return false;

        }
    }
}
