package com.galaxyjoy.cpuinfo.feat.infor.base

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Pure-logic contract for [ComposeHeaderAdapter]: always exactly one header row, regardless of
 * what the hosted Composable does. The View/Compose side (real `ComposeView` creation, content
 * actually rendering, scrolling away with the rest of the RecyclerView) needs a real Android
 * environment and is covered by the instrumented tests in `ActHostSmokeTest`
 * (`cpuTabHeaderScrollsAwayInsteadOfStayingSticky` / `gpuTabHeaderScrollsAwayInsteadOfStayingSticky`),
 * not here — this project doesn't use Robolectric, so constructing a real `ComposeView` isn't
 * safe from a plain JVM unit test.
 */
class ComposeHeaderAdapterTest {

    @Test
    fun itemCountIsAlwaysExactlyOne() {
        val adapter = ComposeHeaderAdapter { }

        assertEquals(1, adapter.itemCount)
    }
}
