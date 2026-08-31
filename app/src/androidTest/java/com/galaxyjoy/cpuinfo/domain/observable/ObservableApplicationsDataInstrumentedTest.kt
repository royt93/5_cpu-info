package com.galaxyjoy.cpuinfo.domain.observable

import android.content.ContentResolver
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T2.30 regression coverage: [buildAppIconUri] can't be unit-tested on the JVM because
 * `android.net.Uri.Builder`'s fluent setters all return null under the project's
 * `isReturnDefaultValues = true` stub — chaining `.scheme().authority()` NPEs on the
 * second call. Runs against the real Android framework instead.
 */
@RunWith(AndroidJUnit4::class)
class ObservableApplicationsDataInstrumentedTest {

    @Test
    fun buildAppIconUri_buildsAndroidResourceUriFromPackageAndIconId() {
        val uri = buildAppIconUri("com.example.app", 12345)

        assertEquals(ContentResolver.SCHEME_ANDROID_RESOURCE, uri.scheme)
        assertEquals("com.example.app", uri.authority)
        assertEquals("/12345", uri.path)
        assertEquals("android.resource://com.example.app/12345", uri.toString())
    }

    @Test
    fun buildAppIconUri_handlesIconId0_noIconDeclared() {
        val uri = buildAppIconUri("com.example.noicon", 0)

        assertEquals("com.example.noicon", uri.authority)
        assertEquals("/0", uri.path)
    }
}
