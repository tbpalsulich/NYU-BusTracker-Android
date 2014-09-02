package com.nyubustracker.test;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ViewAsserts;
import android.view.View;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.nyubustracker.R;
import com.nyubustracker.activities.MainActivity;
import com.nyubustracker.helpers.BusManager;
import com.nyubustracker.helpers.MultipleOrientationSlidingDrawer;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    Activity mActivity;

    TextView startText;
    String startTextCorrect;

    TextView endText;
    String endTextCorrect;

    TextView callSafeRideButton;
    String safeRideTextCorrect;

    TextView startButton;
    TextView endButton;

    MultipleOrientationSlidingDrawer drawer;
    TextSwitcher nextTime;
    TextView nextRoute;
    TextView nextBus;

    BusManager busManager;

    View decorView;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = this.getActivity();

        startText = (TextView) mActivity.findViewById(R.id.start);

        endText = (TextView) mActivity.findViewById(R.id.end);

        nextTime = (TextSwitcher) mActivity.findViewById(R.id.next_time);
        nextRoute = (TextView) mActivity.findViewById(R.id.next_route);
        nextBus = (TextView) mActivity.findViewById(R.id.next_bus);

        callSafeRideButton = (TextView) mActivity.findViewById(R.id.safe_ride_button);

        startButton = (TextView) mActivity.findViewById(R.id.start_stop);
        endButton = (TextView) mActivity.findViewById(R.id.end_stop);
        drawer = (MultipleOrientationSlidingDrawer) mActivity.findViewById(R.id.sliding_drawer);

        startTextCorrect = mActivity.getString(R.string.start);
        safeRideTextCorrect = mActivity.getString(R.string.call_safe_ride);
        endTextCorrect = mActivity.getString(R.string.end);

        busManager = BusManager.getBusManager();

        decorView = mActivity.getWindow().getDecorView();
    }

    public void testPreconditions() {
        assertNotNull(mActivity);
        assertNotNull(startText);
        assertNotNull(endText);
        assertNotNull(callSafeRideButton);
        assertNotNull(nextTime);
        assertNotNull(nextBus);
        assertNotNull(nextRoute);
        assertNotNull(drawer);
        assertNotNull(decorView);
    }

    public void testStartText() {
        assertEquals(startTextCorrect, (String) startText.getText());
    }

    public void testStartText_layout() {
        ViewAsserts.assertOnScreen(decorView, startText);
        assertTrue(View.VISIBLE == startText.getVisibility());
    }

    public void testEndText() {
        assertEquals(endTextCorrect, (String) endText.getText());
    }

    public void testEndText_layout() {
        ViewAsserts.assertOnScreen(decorView, endText);
        assertTrue(View.VISIBLE == endText.getVisibility());
    }

    public void testSafeRideButton_text() {
        assertEquals(safeRideTextCorrect, (String) callSafeRideButton.getText());
    }
}