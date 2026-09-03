package com.galaxyjoy.cpuinfo.feat.p2pcompare

import android.content.Context
import android.content.Intent
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Same `ACTION_SEND` text-share shape as
 * [com.galaxyjoy.cpuinfo.util.SystemInfoExporter.exportSystemInfo] — takes the Fragment's own
 * `Context` per call rather than an injected `@ApplicationContext`, since this is always invoked
 * from a Fragment that already has one. */
class P2PCompareExporter @Inject constructor(
    private val dispatchersProvider: DispatchersProvider,
) {

    fun exportDeviceCode(context: Context, scope: CoroutineScope, payload: DeviceComparePayload) {
        scope.launch {
            val code = withContext(dispatchersProvider.io) { DeviceComparePayload.encode(payload) }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.p2p_compare_share_subject))
                putExtra(Intent.EXTRA_TEXT, code)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.p2p_compare_share_chooser_title)),
            )
        }
    }
}
