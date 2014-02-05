package com.palsulich.nyubustracker.test;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

import com.palsulich.nyubustracker.R;
import com.palsulich.nyubustracker.activities.MainActivity;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    Activity mActivity;
    TextView startText;
    String startTextCorrect;
    TextView endText;
    String endTextCorrect;

    public MainActivityTest(){
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = this.getActivity();
        startText = (TextView) mActivity.findViewById(R.id.start);
        startTextCorrect = mActivity.getString(R.string.start);
        endText = (TextView) mActivity.findViewById(R.id.end);
        endTextCorrect = mActivity.getString(R.string.end);
    }

    public void testPreconditions() {
        assertNotNull(startText);
        assertNotNull(endText);
    }

    public void testStartText() {
        assertEquals(startTextCorrect, (String)startText.getText());
    }

    public void testEndText() {
        assertEquals(endTextCorrect, (String) endText.getText());
    }
}
