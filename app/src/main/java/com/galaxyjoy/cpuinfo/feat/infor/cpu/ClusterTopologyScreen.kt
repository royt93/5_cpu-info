package com.galaxyjoy.cpuinfo.feat.infor.cpu

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.Utils

@Composable
internal fun ClusterTopologyScreen(clusters: List<ClusterTopologyBuilder.Cluster>) {
    if (clusters.isEmpty()) return

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        clusters.forEach { cluster ->
            ClusterCard(cluster)
        }
    }
}

@Composable
private fun ClusterCard(cluster: ClusterTopologyBuilder.Cluster) {
    val accentColor = tierColor(cluster.tier)
    Surface(
        modifier = Modifier.widthIn(min = 160.dp),
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.1f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = tierLabel(cluster.tier),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
            Text(
                text = cluster.uarchName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.cluster_core_range,
                    cluster.coreCount,
                    cluster.coreIndexRange.first,
                    cluster.coreIndexRange.last,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (cluster.maxFreqMhz > 0) {
                Text(
                    text = stringResource(R.string.cluster_max_freq, cluster.maxFreqMhz),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            cluster.caches.forEach { cache ->
                Text(
                    text = cacheRowText(cache),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun cacheRowText(cache: ClusterTopologyBuilder.CacheEntry): String {
    val sizeReadable = Utils.humanReadableByteCount(cache.sizeBytes.toLong())
    return if (cache.sharedCoreCount <= 1) {
        stringResource(R.string.cluster_cache_private, cache.level.label, sizeReadable)
    } else {
        stringResource(R.string.cluster_cache_shared, cache.level.label, sizeReadable, cache.sharedCoreCount)
    }
}

@Composable
private fun tierLabel(tier: ClusterTopologyBuilder.Tier): String = when (tier) {
    ClusterTopologyBuilder.Tier.PRIME -> stringResource(R.string.cluster_tier_prime)
    ClusterTopologyBuilder.Tier.PERFORMANCE -> stringResource(R.string.cluster_tier_performance)
    ClusterTopologyBuilder.Tier.EFFICIENCY -> stringResource(R.string.cluster_tier_efficiency)
    ClusterTopologyBuilder.Tier.ALL_CORES -> stringResource(R.string.cluster_tier_all_cores)
    ClusterTopologyBuilder.Tier.UNLABELED -> stringResource(R.string.cluster_tier_unlabeled)
}

private fun tierColor(tier: ClusterTopologyBuilder.Tier): Color = when (tier) {
    ClusterTopologyBuilder.Tier.PRIME -> Color(0xFFE53935)
    ClusterTopologyBuilder.Tier.PERFORMANCE -> Color(0xFF1E88E5)
    ClusterTopologyBuilder.Tier.EFFICIENCY -> Color(0xFF43A047)
    ClusterTopologyBuilder.Tier.ALL_CORES -> Color(0xFF546E7A)
    ClusterTopologyBuilder.Tier.UNLABELED -> Color(0xFF546E7A)
}
