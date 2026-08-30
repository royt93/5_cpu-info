package com.galaxyjoy.cpuinfo.feat.infor.storage

import androidx.annotation.Keep
import com.galaxyjoy.cpuinfo.domain.model.StorageData

@Keep
data class StorageInfoViewState(val storageData: StorageData)
