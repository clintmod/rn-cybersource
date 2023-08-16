package com.reactnativecyber.authorization.core

import com.reactnativecyber.authorization.http.HttpSignatureToken

class Authorization {
    private var jwtRequestBody: String? = null

    fun setJWTRequestBody(jwtrequest: String) {
        jwtRequestBody = jwtrequest
    }

    fun getToken(merchantConfig: MerchantConfig): String {
        return HttpSignatureToken(merchantConfig).getToken()!!
    }
}