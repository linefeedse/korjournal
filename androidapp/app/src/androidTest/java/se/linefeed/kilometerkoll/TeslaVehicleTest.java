package se.linefeed.kilometerkoll;

import android.content.Context;
import android.content.pm.PackageManager;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CountDownLatch;

import se.linefeed.korjournal.api.JsonAPIResponseInterface;
import se.linefeed.korjournal.api.TeslaAPI;
import se.linefeed.korjournal.models.TeslaVehicle;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TeslaVehicleTest {
    private Context context;
    private String packageName;
    private PackageManager packageManager;
    private static String accessToken;
    @Before
    public void setUp() {
        context = mock(Context.class);
        packageName = getInstrumentation().getContext().getPackageName();
        when(context.getPackageName()).thenReturn(packageName);
        packageManager = getInstrumentation().getContext().getPackageManager();
        when(context.getPackageManager()).thenReturn(packageManager);
    }

    @Test
    public void get01TeslaAccessToken() {
        final CountDownLatch signal = new CountDownLatch(1);
        final String[] theToken = {"",""};
        try {
            assertTrue(TeslaAPI.getNewAccessToken(context, "email@gmail.com", "Passw0rd", new JsonAPIResponseInterface() {
                @Override
                public void done(JSONObject response) {
                    try {
                        theToken[0] = response.getString("access_token");
                        theToken[1] = String.valueOf(response.getInt("expires_in") + response.getInt("created_at"));
                    } catch (JSONException e) {
                        theToken[0] = "Missing fields in JSON";
                    }
                    signal.countDown();
                }

                @Override
                public void error(String error) {
                    theToken[0] = error;
                    signal.countDown();
                }
            }));
            try {
                signal.await();
            } catch (InterruptedException i) {
                // We were interrupted
            }
        } catch (Exception e) {

        }
        assertEquals(68, theToken[0].length());
        assertFalse(System.currentTimeMillis() < Integer.decode(theToken[1]) * 1000);
        accessToken = theToken[0];
    }

    @Test
    public void get02VehicleOdometer() {

        class Odometer {
            private long km;
            private Odometer() {
                km = 0L;
            }
        }
        final CountDownLatch signal = new CountDownLatch(1);
        final Odometer odometer = new Odometer();
        TeslaAPI teslaAPI = new TeslaAPI(context, accessToken);
        final TeslaVehicle testVehicle = new TeslaVehicle("any","");
        testVehicle.loadAnyFromAPI(teslaAPI, new JsonAPIResponseInterface() {
            @Override
            public void done(JSONObject response) {
                odometer.km = testVehicle.getOdometerKm();
                signal.countDown();
            }

            @Override
            public void error(String error) {
                // Note that Tesla must be awake for this test
                odometer.km = -1;
                signal.countDown();
            }
        });
        try {
            signal.await();
        } catch (InterruptedException i) {
            // We were interrupted
        }
        assertEquals (true,odometer.km > 0);
    }
}
