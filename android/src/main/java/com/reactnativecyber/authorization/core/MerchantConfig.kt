package com.reactnativecyber.authorization.core

class MerchantConfig {

    var merchantKeyId: String? = null
    var merchantSecretKey: String? = null
    var merchantID: String? = null
    var url: String? = null
    var requestTarget: String? = null
    var authenticationType: String? = null
    var requestHost: String? = null
    var responseMessage: String? = null
    var responseCode: String? = null
    var requestType: String? = null
    var runEnvironment: String? = null
    var requestJsonPath: String? = null
    var requestData: String? = null
    var useMetaKey: Boolean = false

    fun isUseMetaKeyEnabled(): Boolean {
        //TODO
        return false
    }
}