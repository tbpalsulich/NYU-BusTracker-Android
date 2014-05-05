package com.palsulich.nyubustracker.test;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
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

import java.util.Calendar;

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

        timeButton          =   (Button) mActivity.findViewById(R.id.times_text);
        startButton         =   (Button) mActivity.findViewById(R.id.start_stop);
        endButton           =   (Button) mActivity.findViewById(R.id.end_stop);

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
                        if (!startStop.getUltimateName().equals(endButton.getText())){
                            startList.performItemClick(startAdapter.getView(j, null, null),
                                                       j,
                                                       startAdapter.getItemId(j));
                            assertEquals(startStop.getName(), startButton.getText());
                            for (int i = 0; i < endAdapter.getCount(); i++) {
                                Stop endStop = (Stop) endAdapter.getItem(i);
                                endButton.performClick();
                                endList.performItemClick(endAdapter.getView(i, null, null),
                                                           i,
                                                           endAdapter.getItemId(i));
                                assertEquals(endStop.getName(), endButton.getText());
                            }
                        }
                    }
                }
            });
        } catch (Throwable t){t.printStackTrace();}
    }

    public void testBusLeavingNow() {
        Calendar cal = Calendar.getInstance();
        cal.set(2014, Calendar.JANUARY, 1, 11, 46);
        BusManager.spoofTime(cal);
        testNextBusTime(R.string.less_one_minute);
    }

    private void testNextBusTime(final int expectedID){
        Context c = mActivity.getApplicationContext();
        assertNotNull(c);
        Resources r = mActivity.getApplicationContext().getResources();
        assertNotNull(r);
        final String expected = r.getString(expectedID);
        final ListView startList = (ListView) mActivity.findViewById(R.id.start_stop_list);
        final ListView endList = (ListView) mActivity.findViewById(R.id.end_stop_list);
        try{
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ListAdapter startAdapter = startList.getAdapter();
                    ListAdapter endAdapter = endList.getAdapter();
                    for (int i = 0; i < startAdapter.getCount(); i++) {
                        Stop start = (Stop) startAdapter.getItem(i);
                        if (start.getUltimateName().equals("715 Broadway @ Washington Square")){
                            startButton.performClick();
                            startList.performItemClick(startAdapter.getView(i, null, null),
                                                       i,
                                                       startAdapter.getItemId(i));
                        }
                    }
                    for (int i = 0; i < endAdapter.getCount(); i++) {
                        Stop end = (Stop) endAdapter.getItem(i);
                        if (end.getUltimateName().equals("3rd Ave @ 14th St")){
                            endButton.performClick();
                            endList.performItemClick(endAdapter.getView(i, null, null),
                                                     i,
                                                     endAdapter.getItemId(i));
                        }
                    }
                    TextView tv = (TextView) nextTime.getCurrentView();
                    assertNotNull(tv);
                    assertEquals(tv.getText(), expected);
                }
            });
        } catch (Throwable t){t.printStackTrace();}
    }
}
