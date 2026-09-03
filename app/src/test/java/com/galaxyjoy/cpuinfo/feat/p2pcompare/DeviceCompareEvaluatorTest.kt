package com.galaxyjoy.cpuinfo.feat.p2pcompare

import kotlin.test.assertEquals
import org.junit.Test

class DeviceCompareEvaluatorTest {

    private fun payload(model: String, ramBytes: Long, storageBytes: Long) =
        DeviceComparePayload(1, model, ramBytes, storageBytes)

    @Test
    fun `local wins when local has more RAM and more storage`() {
        val local = payload("Local", ramBytes = 16_000_000_000L, storageBytes = 512_000_000_000L)
        val remote = payload("Remote", ramBytes = 8_000_000_000L, storageBytes = 128_000_000_000L)

        val result = DeviceCompareEvaluator.compare(local, remote)

        assertEquals(DeviceCompareEvaluator.Winner.LOCAL, result.ram.winner)
        assertEquals(DeviceCompareEvaluator.Winner.LOCAL, result.storage.winner)
    }

    @Test
    fun `remote wins when remote has more RAM and more storage`() {
        val local = payload("Local", ramBytes = 8_000_000_000L, storageBytes = 128_000_000_000L)
        val remote = payload("Remote", ramBytes = 16_000_000_000L, storageBytes = 512_000_000_000L)

        val result = DeviceCompareEvaluator.compare(local, remote)

        assertEquals(DeviceCompareEvaluator.Winner.REMOTE, result.ram.winner)
        assertEquals(DeviceCompareEvaluator.Winner.REMOTE, result.storage.winner)
    }

    @Test
    fun `each field is compared independently - local wins RAM, remote wins storage`() {
        val local = payload("Local", ramBytes = 16_000_000_000L, storageBytes = 128_000_000_000L)
        val remote = payload("Remote", ramBytes = 8_000_000_000L, storageBytes = 512_000_000_000L)

        val result = DeviceCompareEvaluator.compare(local, remote)

        assertEquals(DeviceCompareEvaluator.Winner.LOCAL, result.ram.winner)
        assertEquals(DeviceCompareEvaluator.Winner.REMOTE, result.storage.winner)
    }

    @Test
    fun `exactly equal values produce a tie, not a crash or arbitrary winner`() {
        val local = payload("Local", ramBytes = 8_000_000_000L, storageBytes = 128_000_000_000L)
        val remote = payload("Remote", ramBytes = 8_000_000_000L, storageBytes = 128_000_000_000L)

        val result = DeviceCompareEvaluator.compare(local, remote)

        assertEquals(DeviceCompareEvaluator.Winner.TIE, result.ram.winner)
        assertEquals(DeviceCompareEvaluator.Winner.TIE, result.storage.winner)
    }

    @Test
    fun `result carries through the original local and remote payloads unchanged`() {
        val local = payload("Local", ramBytes = 8_000_000_000L, storageBytes = 128_000_000_000L)
        val remote = payload("Remote", ramBytes = 16_000_000_000L, storageBytes = 256_000_000_000L)

        val result = DeviceCompareEvaluator.compare(local, remote)

        assertEquals(local, result.local)
        assertEquals(remote, result.remote)
        assertEquals(8_000_000_000L, result.ram.localBytes)
        assertEquals(16_000_000_000L, result.ram.remoteBytes)
    }
}
