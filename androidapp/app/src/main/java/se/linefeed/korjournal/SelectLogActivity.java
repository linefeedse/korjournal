package se.linefeed.korjournal;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

import se.linefeed.korjournal.api.JsonAPIResponseInterface;
import se.linefeed.korjournal.api.KorjournalAPI;
import se.linefeed.korjournal.api.RequestDoneInterface;
import se.linefeed.korjournal.models.OdometerSnapArray;
import se.linefeed.korjournal.models.TripListModel;
import se.linefeed.korjournal.models.TripListModelArray;
import se.linefeed.korjournal.models.VehicleList;

public class SelectLogActivity extends AppCompatActivity {
    private final String[] pickerMonths = {
            "Januari",
            "Februari",
            "Mars",
            "April",
            "Maj",
            "Juni",
            "Juli",
            "Augusti",
            "September",
            "Oktober",
            "November",
            "December"
    };
    private NumberPicker monthPicker;
    private VehicleList vehicleList;
    private final int REQUEST_CODE_TRIPLIST = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectlog);
        monthPicker = findViewById(R.id.monthPicker);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(pickerMonths);
        Calendar today = Calendar.getInstance();
        // Assume we want to see this months trips
        monthPicker.setValue(today.get(Calendar.MONTH));

        Spinner vehicleSpinner = findViewById(R.id.vehicleLogSpinner);
        vehicleList = new VehicleList(this,
                R.layout.my_spinner_item,
                vehicleSpinner);
        Button showLogButton = findViewById(R.id.viewLogButton);
        showLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogForMonth();
            }
        });
        Button exportCSVButton = findViewById(R.id.exportCsvButton);
        exportCSVButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportCSVForMonth();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        KorjournalAPI api = ((MyApplication) getApplicationContext()).getApi();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        vehicleList.request(api,
                preferences,
                new JsonAPIResponseInterface() {
                    @Override
                    public void done(JSONObject response) {
                    }

                    @Override
                    public void error(String error) {

                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.getClass() == AuthFailureError.class) {
                            authFailed();
                        }
                    }
                });
    }

    public void authFailed() {
        final MyApplication app = (MyApplication) getApplicationContext();
        app.showToast("Inloggningsfel. Gå till Inställningar",2);
    }

    public void clickLogVehicleSpinner(View v) {
        Spinner vehicleSpinner = findViewById(R.id.vehicleLogSpinner);
        vehicleSpinner.performClick();
    }

    /**
     *
     * @return month 0-11
     */
    int getSelectedMonth() {
        return monthPicker.getValue();
    }

    /**
     *
     * @param month
     * @return previous year if month > this month
     */
    int getSelectedYear(int month) {
        Calendar today = Calendar.getInstance();
        // 0 is January
        int year = today.get(Calendar.YEAR);
        if (today.get(Calendar.MONTH) < month) {
            return year-1;
        }
        return year;
    }

    public void showLogForMonth() {
        final Activity activity = this;
        final MyApplication app = (MyApplication) getApplicationContext();
        final Intent intent = new Intent(activity, TripListActivity.class);
        if (vehicleList.getSelectedVehicle() == null) {
            app.showToast("Du verkar inte ha några fordon",4);
            return;
        }
        String vehicleUrl = vehicleList.getSelectedVehicle().getUrl();
        int selectedMonth = getSelectedMonth();
        int year = getSelectedYear(selectedMonth);
        intent.putExtra("vehicleUrl", vehicleUrl);
        intent.putExtra("year", year);
        intent.putExtra("month", selectedMonth + 1);
        KorjournalAPI api = ((MyApplication) getApplicationContext()).getApi();
        OdometerSnapArray snapsForMonth = new OdometerSnapArray(api,
                this, year, selectedMonth + 1, new RequestDoneInterface() {
            @Override
            public void done(int results) {
                if (results < 1) {
                    app.showToast("Inga resor funna i vald månad",4);
                    return;
                }
                startActivityForResult(intent, REQUEST_CODE_TRIPLIST);
            }

            @Override
            public void error(String error) {
                app.showToast("Kunde inte hämta (nätverksfel)", 4);
            }
        });
    }

    public void exportCSVForMonth() {
        File csvPath = new File(getFilesDir(), "logs");
        final MyApplication app = (MyApplication) getApplicationContext();
        if (vehicleList.getSelectedVehicle() == null) {
            app.showToast("Du verkar inte ha några fordon",4);
            return;
        }
        String vehicleName = vehicleList.getSelectedVehicle().getName();
        int selectedMonth = getSelectedMonth();
        int year = getSelectedYear(selectedMonth);
        String fileName = String.format(Locale.getDefault(),
                "%s_%d-%02d.csv", vehicleName, year, selectedMonth + 1);

        File dummyCsv = new File(csvPath, fileName);
        try {
            csvPath.mkdir();
            dummyCsv.createNewFile();
        }
        catch (IOException io) {
            Log.e("exportCSVForMonth", "Unable to create file " + csvPath + "/" + fileName +": " +  io.getMessage());
            return;
        }
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(dummyCsv);
        }
        catch (FileNotFoundException fnf) {
            Log.e("exportCSVForMonth", "File not found");
            return;
        }
        try {
            writeCSVData(stream);
        } catch (IOException io) {
            Log.e("exportCSVForMonth", io.getMessage());
            return;
        } finally {
            try {
                stream.close();
            }
            catch (IOException notused) {}
        }

        Uri contentUri = FileProvider.getUriForFile(this, "se.linefeed.fileprovider", dummyCsv);
        String mime = getContentResolver().getType(contentUri); // + "; charset=UTF-8";

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(contentUri, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        }
        catch (ActivityNotFoundException notfound) {
            Log.e("exportCSVForMonth", notfound.getLocalizedMessage());
            ((MyApplication) getApplicationContext()).showToast("Hittar ingen applikation som kan visa kalkylark", 2);
            Intent sendFileIntent = new Intent();
            sendFileIntent.setType("text/plain");
            sendFileIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            sendFileIntent.setAction(Intent.ACTION_SEND);
            startActivity(sendFileIntent);
        }
    }

    private void writeCSVData(FileOutputStream stream) throws IOException {
        TripListModelArray array = new TripListModelArray();
        String vehicleUrl = vehicleList.getSelectedVehicle().getUrl();
        int selectedMonth = getSelectedMonth();
        int year = getSelectedYear(selectedMonth);
        DatabaseOpenHelper dboh = new DatabaseOpenHelper(this);
        array.loadFromDb(dboh, vehicleUrl, year, selectedMonth + 1);
        Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_16LE);
        writer.write(String.format(Locale.getDefault(),
                "Körjournal för %s\n", vehicleList.getSelectedVehicle().getName()));
        writer.write(String.format(Locale.getDefault(),
                "%s\t%s\t%s\t%s\t%s\t%s\n",
                "Datum",
                "Start km",
                "Slut km",
                "Från",
                "Till",
                "Syfte"
        ));

        ArrayList<TripListModel> dateDescending = new ArrayList<>(array.asArrayList());
        Collections.reverse(dateDescending);
        for (TripListModel trip: dateDescending) {
            // TODO: handle double quotes in CSV fields
            writer.write(String.format(Locale.getDefault(),
                    "%s\t%s\t%s\t%s\t%s\t%s\n",
                    trip.getWhenDay(),
                    trip.getStartKmString(),
                    trip.getEndKmString(),
                    trip.getFromAddress(),
                    trip.getToAddress(),
                    trip.getReason()));
        }
        writer.flush();
    }
    @Override
    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data) {
        if (resultCode == RESULT_CANCELED || requestCode != REQUEST_CODE_TRIPLIST) {
            return;
        }
        askForReview();
    }
    protected void askForReview() {
        if (!ReviewRequestHelper.reviewConditionsMet(this)) {
            return;
        }
        ReviewRequestDialogFragment reviewRequest = new ReviewRequestDialogFragment();
        reviewRequest.show(getFragmentManager(),"ask_review");
    }
}
