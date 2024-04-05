package com.xxx.zzz.aall.permasd.utilsssss

import androidx.annotation.Keep

@Keep
data class QuickPermissionsOptions(
    var handleRationale: Boolean = true,
    var rationaleMessage: String = "",
    var handlePermanentlyDenied: Boolean = true,
    var permanentlyDeniedMessage: String = "",
    var rationaleMethod: ((QuickPermissionsRequest) -> Unit)? = null,
    var permanentDeniedMethod: ((QuickPermissionsRequest) -> Unit)? = null,
    var permissionsDeniedMethod: ((QuickPermissionsRequest) -> Unit)? = null
)