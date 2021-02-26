package se.linefeed.korjournal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public final class ReviewRequestHelper {
    private static final String PREF_KEY_SNAP_TIMES = "stats_usage_snap_times";
    public static final int MIN_USAGE_COUNT_FOR_REVIEW = 10;
    public static final String PREF_KEY_REVIEW_NEVER = "review_request_declined";
    public static final String PREF_KEY_REVIEW_DONE = "review_request_done";
    private static final String PREF_KEY_REVIEW_NAG_COUNTER = "review_request_nag_counter";

    private ReviewRequestHelper() {
    }
    static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
    static Editor getPreferencesEditor(Context context) {
        return getPreferences(context).edit();
    }
    public static void setUsagesLastMonth(Context context, int usages) {
        SharedPreferences.Editor editor = getPreferencesEditor(context);
        editor.putInt(PREF_KEY_SNAP_TIMES, usages);
        editor.apply();
    }
    static boolean reviewConditionsMet(Context context) {
        if (getUsagesLastMonth(context) < MIN_USAGE_COUNT_FOR_REVIEW)
            return false;
        if (reviewRequestDoneOrDeclined(context))
            return false;
        if (getAndDecrementRetryCounter(context) > 0)
            return false;
        return true;
    }
    static void setRetryCounter(Context context, int counter) {
        SharedPreferences.Editor editor = getPreferencesEditor(context);
        editor.putInt(PREF_KEY_REVIEW_NAG_COUNTER, counter);
        editor.apply();
    }
    static int getAndDecrementRetryCounter(Context context) {
        int counter = getPreferences(context).getInt(PREF_KEY_REVIEW_NAG_COUNTER, 0);
        if (counter > 0) {
            counter--;
            SharedPreferences.Editor editor = getPreferencesEditor(context);
            editor.putInt(PREF_KEY_REVIEW_NAG_COUNTER, counter);
            editor.apply();
        }
        return counter;
    }

    static int getUsagesLastMonth(Context context) {
        return getPreferences(context).getInt(PREF_KEY_SNAP_TIMES, 0);
    }
    static void reviewRequestNever(Context context) {
        SharedPreferences.Editor editor = getPreferencesEditor(context);
        editor.putBoolean(PREF_KEY_REVIEW_NEVER, true);
        editor.apply();
    }
    static void reviewRequestDone(Context context) {
        SharedPreferences.Editor editor = getPreferencesEditor(context);
        editor.putBoolean(PREF_KEY_REVIEW_DONE, true);
        editor.apply();
    }
    static boolean reviewRequestDoneOrDeclined(Context context) {
        return getPreferences(context).getBoolean(PREF_KEY_REVIEW_NEVER,false)
                || getPreferences(context).getBoolean(PREF_KEY_REVIEW_DONE,false);
    }
}
