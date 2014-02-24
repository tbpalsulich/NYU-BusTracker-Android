package com.palsulich.nyubustracker.test;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ViewAsserts;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.palsulich.nyubustracker.R;
import com.palsulich.nyubustracker.activities.MainActivity;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.models.Stop;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    Activity mActivity;

    TextView startText;
    String startTextCorrect;

    TextView endText;
    String endTextCorrect;

    Button callSafeRideButton;
    String safeRideTextCorrect;

    Button startButton;
    Button endButton;

    Button timeButton;
    TextSwitcher nextTime;
    TextView nextRoute;
    TextView nextBus;

    BusManager busManager;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = this.getActivity();

        startText           = (TextView) mActivity.findViewById(R.id.start);

        endText             = (TextView) mActivity.findViewById(R.id.end);

        nextTime            = (TextSwitcher) mActivity.findViewById(R.id.next_time);
        nextRoute           = (TextView) mActivity.findViewById(R.id.next_route);
        nextBus             = (TextView) mActivity.findViewById(R.id.next_bus);

        callSafeRideButton  =   (Button) mActivity.findViewById(R.id.safe_ride_button);

        timeButton          =   (Button) mActivity.findViewById(R.id.times_button);
        startButton         =   (Button) mActivity.findViewById(R.id.start_button);
        endButton           =   (Button) mActivity.findViewById(R.id.end_button);

        startTextCorrect    = mActivity.getString(R.string.start);
        safeRideTextCorrect = mActivity.getString(R.string.call_safe_ride);
        endTextCorrect      = mActivity.getString(R.string.end);

        busManager = BusManager.getBusManager();

    }

    public void testPreconditions() {
        assertNotNull(mActivity);
        assertNotNull(startText);
        assertNotNull(endText);
        assertNotNull(callSafeRideButton);
        assertNotNull(nextTime);
        assertNotNull(nextBus);
        assertNotNull(nextRoute);
        assertNotNull(timeButton);
    }

    public void testStartText() {
        assertEquals(startTextCorrect, (String) startText.getText());
    }

    public void testStartText_layout() {
        final View decorView = mActivity.getWindow().getDecorView();
        ViewAsserts.assertOnScreen(decorView, startText);
        assertTrue(View.VISIBLE == startText.getVisibility());
    }

    public void testEndText() {
        assertEquals(endTextCorrect, (String) endText.getText());
    }

    public void testEndText_layout() {
        final View decorView = mActivity.getWindow().getDecorView();
        ViewAsserts.assertOnScreen(decorView, endText);
        assertTrue(View.VISIBLE == endText.getVisibility());
    }

    public void testSafeRideButton_layout() {
        final View decorView = mActivity.getWindow().getDecorView();
        ViewAsserts.assertOnScreen(decorView, callSafeRideButton);
        assertTrue(View.GONE == callSafeRideButton.getVisibility());
    }

    public void testSafeRideButton_text() {
        assertEquals(safeRideTextCorrect, (String) callSafeRideButton.getText());
    }

    public void testAllStopCombinations() {
        final ListView startList = (ListView) mActivity.findViewById(R.id.start_stop_list);
        final ListView endList = (ListView) mActivity.findViewById(R.id.end_stop_list);
            try{
                runTestOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ListAdapter startAdapter = startList.getAdapter();
                        ListAdapter endAdapter = endList.getAdapter();
                        for (int j = 0; j < startAdapter.getCount(); j++) {
                            startButton.performClick();
                            Stop startStop = (Stop) startAdapter.getItem(j);
                            startList.performItemClick(startAdapter.getView(j, null, null),
                                                       j,
                                                       startAdapter.getItemId(j));
                            for (int i = 0; i < endAdapter.getCount(); i++) {
                                if (((Stop) endAdapter.getItem(i)).getName().equals(startStop.getName())){
                                    endButton.performClick();
                                    endList.performItemClick(endAdapter.getView(i, null, null),
                                                               i,
                                                               endAdapter.getItemId(i));
                                }
                            }
                        }
                    }
                });
            } catch (Throwable t){t.printStackTrace();}
    }
}
