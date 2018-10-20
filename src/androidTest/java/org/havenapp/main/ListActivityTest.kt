package org.havenapp.main

import android.content.Intent
import android.support.test.annotation.UiThreadTest
import android.support.test.espresso.Espresso
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.v4.content.LocalBroadcastManager
import android.test.ActivityInstrumentationTestCase2
import junit.framework.Assert
import org.havenapp.main.database.DB_INIT_END
import org.havenapp.main.database.DB_INIT_START
import org.havenapp.main.database.DB_INIT_STATUS

/**
 * Created by Arka Prava Basu <arka.basu@zomato.com> on 20/10/18.
 */
class ListActivityTest : ActivityInstrumentationTestCase2<ListActivity>(ListActivity::class.java) {
    private var listActivity: ListActivity? = null

    override fun setUp() {
        super.setUp()
        listActivity = activity
    }

    fun testCheckActivityNotNull() {
        Assert.assertNotNull(listActivity)
    }

    /**
     * Test that we show a progress dialog while database init/migration is in process.
     * Test that we remove that on db init/migration success
     */
    @UiThreadTest
    fun testCheckProgressBarShownOnBroadcast() {
        Assert.assertNotNull(listActivity)

        var dbIntent = Intent()
        dbIntent.putExtra(DB_INIT_STATUS, DB_INIT_START)
        dbIntent.action = DB_INIT_STATUS
        LocalBroadcastManager.getInstance(activity).sendBroadcast(dbIntent)

        Espresso.onView(withText(R.string.please_wait)).check(matches(isDisplayed()))
        Espresso.onView(withText(R.string.migrating_data)).check(matches(isDisplayed()))

        Thread.sleep(5000) // keeping a waiting time to check the view

        dbIntent = Intent()
        dbIntent.putExtra(DB_INIT_STATUS, DB_INIT_END)
        dbIntent.action = DB_INIT_STATUS
        LocalBroadcastManager.getInstance(activity).sendBroadcast(dbIntent)

        Espresso.onView(withText(R.string.please_wait)).check(doesNotExist())
        Espresso.onView(withText(R.string.migrating_data)).check(doesNotExist())
    }
}
