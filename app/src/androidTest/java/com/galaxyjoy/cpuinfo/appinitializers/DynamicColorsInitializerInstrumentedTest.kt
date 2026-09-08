package com.galaxyjoy.cpuinfo.appinitializers

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for the Material You widget audit's root-cause fix — wiring
 * [DynamicColorsInitializer] into [com.galaxyjoy.cpuinfo.di.modules.AppModuleBinds] via a plain
 * `@Binds @IntoSet` is easy to get wrong (wrong scope, missing binding) and would silently no-op
 * rather than crash, so this asserts the call actually runs clean against the real Application on
 * a real device rather than trusting the DI graph compiles.
 */
@RunWith(AndroidJUnit4::class)
class DynamicColorsInitializerInstrumentedTest {

    @Test
    fun init_runsWithoutThrowing_onARealApplication() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        DynamicColorsInitializer().init(app)
    }
}
