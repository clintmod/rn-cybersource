package com.reactnativecyber.sessions.model

import com.reactnativecyber.sessions.model.FlexFieldData
data class FlexCardData(
        var number: FlexFieldData? = null,
        var securityCode: FlexFieldData? = null,
        var expirationMonth: FlexFieldData? = null,
        var expirationYear: FlexFieldData? = null,
        var type: FlexFieldData? = null
)
