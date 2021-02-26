package se.linefeed.korjournal;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import se.linefeed.korjournal.ReviewRequestHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ReviewRequestResetNeverDone {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("se.linefeed.korjournal", appContext.getPackageName());
        SharedPreferences.Editor editor = ReviewRequestHelper.getPreferencesEditor(appContext);
        editor.remove(ReviewRequestHelper.PREF_KEY_REVIEW_DONE);
        editor.remove(ReviewRequestHelper.PREF_KEY_REVIEW_NEVER);
        editor.apply();
        ReviewRequestHelper.setUsagesLastMonth(appContext, ReviewRequestHelper.MIN_USAGE_COUNT_FOR_REVIEW);
        ReviewRequestHelper.setRetryCounter(appContext, 0);
        assertTrue(ReviewRequestHelper.reviewConditionsMet(appContext));
    }
}
