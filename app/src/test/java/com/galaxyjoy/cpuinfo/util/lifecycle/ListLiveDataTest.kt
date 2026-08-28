package com.galaxyjoy.cpuinfo.util.lifecycle

import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for B29: [ListLiveData] used to call [androidx.lifecycle.MutableLiveData.setValue]
 * unconditionally, which throws IllegalStateException when invoked off the main thread (e.g. a
 * sensor callback registered without a Handler). It must fall back to postValue() there instead.
 */
class ListLiveDataTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mainLooper: Looper = mockk()

    @Before
    fun setUp() {
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mainLooper
    }

    @After
    fun tearDown() {
        unmockkStatic(Looper::class)
    }

    @Test
    fun `add on main thread updates value synchronously via setValue`() {
        every { Looper.myLooper() } returns mainLooper // simulate: caller IS on main thread

        val list = ListLiveData<String>()
        list.add("hello")

        // setValue() is synchronous — value is visible immediately without needing to drain a
        // message queue, unlike postValue().
        assertEquals(ListLiveDataState.ITEM_RANGE_INSERTED, list.listStatusChangeNotificator.value?.listLiveDataState)
        assertEquals(1, list.size)
    }

    @Test
    fun `add off main thread does not throw and falls back to postValue`() {
        val backgroundLooper: Looper = mockk()
        every { Looper.myLooper() } returns backgroundLooper // simulate: caller NOT on main thread

        val list = ListLiveData<String>()
        // Prior to the fix this called setValue() unconditionally and would throw
        // IllegalStateException here. The important assertion is that it does not throw.
        list.add("hello")

        assertEquals(1, list.size)
    }

    @Test
    fun `set notifies ITEM_RANGE_CHANGED`() {
        every { Looper.myLooper() } returns mainLooper

        val list = ListLiveData<String>()
        list.add("a")
        list[0] = "b"

        assertEquals(ListLiveDataState.ITEM_RANGE_CHANGED, list.listStatusChangeNotificator.value?.listLiveDataState)
        assertEquals("b", list[0])
    }

    @Test
    fun `clear on non-empty list notifies ITEM_RANGE_REMOVED`() {
        every { Looper.myLooper() } returns mainLooper

        val list = ListLiveData<String>()
        list.addAll(listOf("a", "b"))
        list.clear()

        assertEquals(ListLiveDataState.ITEM_RANGE_REMOVED, list.listStatusChangeNotificator.value?.listLiveDataState)
        assertTrue(list.isEmpty())
    }
}
