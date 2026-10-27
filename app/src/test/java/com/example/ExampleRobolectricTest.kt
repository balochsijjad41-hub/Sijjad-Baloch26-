package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.JarvisMode
import com.example.data.model.JarvisTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("JARVIS", appName)
  }

  @Test
  fun `verify jarvis default themes and modes`() {
    assertNotNull(JarvisTheme.CYAN)
    assertNotNull(JarvisTheme.CRIMSON)
    assertEquals(4, JarvisMode.entries.size)
  }
}
