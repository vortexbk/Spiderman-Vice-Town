package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.SpiderWorldCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify world catalog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Spider City", appName)
        assertEquals(5, SpiderWorldCatalog.districts.size)
        assertEquals(8, SpiderWorldCatalog.initialMissions.size)
        assertTrue(SpiderWorldCatalog.buildings.size >= 30)
    }
}
