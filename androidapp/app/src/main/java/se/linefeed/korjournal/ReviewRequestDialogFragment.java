package se.linefeed.korjournal;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ReviewRequestDialogFragment extends DialogFragment {
    final int REVIEW_REQUEST_NAG_RETRY_COUNTER = 6;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final String message = "Betygssätt Kilometerkoll på Play Store! Ett betyg hjälper andra användare att hitta och välja rätt app.";
        final String storeUrlHttps = "https://play.google.com/store/apps/details?id=se.linefeed.korjournal";
        final String storeUrl = "market://details?id=se.linefeed.korjournal";
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setPositiveButton("Okej!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ReviewRequestHelper.reviewRequestDone(getContext());
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(storeUrl));
                        try {
                            startActivity(intent);
                        }
                        catch (ActivityNotFoundException e) {
                            // Google play isn't installed
                            intent.setData(Uri.parse(storeUrlHttps));
                            startActivity(intent);
                        }
                    }
                })
                .setNegativeButton("Nej, aldrig!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ReviewRequestHelper.reviewRequestNever(getContext());
                    }
                })
                .setNeutralButton("Kanske senare", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ReviewRequestHelper.setRetryCounter(getContext(), REVIEW_REQUEST_NAG_RETRY_COUNTER);
                    }
                });
        return builder.create();
    }
    // If the network call returns after screen has timed out, we may get errors trying to save state
    @Override
    public void show(FragmentManager manager, String tag) {

        try {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag);
            ft.commit();
        } catch (IllegalStateException e) {
            //
        }
    }
}
