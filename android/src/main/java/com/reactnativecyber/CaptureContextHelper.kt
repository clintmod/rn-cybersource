package com.reactnativecyber;

import com.reactnativecyber.sessions.model.FlexCardData
import com.reactnativecyber.sessions.model.FlexFieldData
import com.reactnativecyber.sessions.model.FlexPaymentInfo
import com.reactnativecyber.sessions.model.FlexSessionFields
import com.reactnativecyber.authorization.core.Authorization
import com.reactnativecyber.authorization.core.MerchantConfig
import com.reactnativecyber.authorization.payloaddigest.PayloadDigest
import com.reactnativecyber.authorization.util.Constants
import com.reactnativecyber.authorization.util.PayloadUtility
import com.reactnativecyber.sessions.api.Environment
import com.reactnativecyber.sessions.api.FlexSessionServiceGenerator


import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

interface CaptureContextEvent {
    fun onCaptureContextError(e: Exception)
    fun onCaptureContextResponse(cc: String)
}

/*
    WARNING: this code is Included for demonstration purposes only!
    The sample code found here was included for demonstration purposes and to make the sample application self-contained.  Your merchant credentials and the sessions API call should be invoked from a secure backend service.  The responding capture context JWT data element can them be sent to the application.
    The sessions API is to be invoked to establish the authentication of the merchants and to set the context of the information that is to be captured.  The response to the sessions rest call is a JWT data object that contains the one-time keys used for point to point encryption.
    Samples of how to generate the capture context server side can be found on the Cybersource git-hub repository
*/
class CaptureContextHelper {

    /*
        WARNING: Cybersource API credentials are included for demonstration purposes only!
        The API credentials have been included in this sample mobile application for demonstration purposes only,
        and to provide a encapsulated demo.  The Credentials must be removed for the final application build.
     */
    companion object {
        val US_MERCHANT_ID_UAT = "reg100levi"
        val US_MERCHANT_KEY_UAT = "7aa9d362-4794-49df-be7b-3fde513809d8"
        val US_MERCHANT_SECRET_UAT = "azmNU6cYeaXG5wpb+U/xHJozNai/dxflE2cm9u7TVow="

        val US_MERCHANT_ID_PROD = "prod000levi"
        val US_MERCHANT_KEY_PROD = "bf7a4abc-6123-45aa-83e0-861a4e12d44b"
        val US_MERCHANT_SECRET_PROD = "VcFnyucPAIGPfuWVtRCoZy/nCPbwQzjetr9uiBFIxv8="

        val CA_MERCHANT_ID_UAT = "regca100levi"
        val CA_MERCHANT_KEY_UAT = "cc35be4a-a6c7-4d59-8428-4b308f6c21f4"
        val CA_MERCHANT_SECRET_UAT = "xevsGNPlCKa2RMffwrwyTXMNIaby6tbJ0JmVi7p3oUo="

        val CA_MERCHANT_ID_PROD = "prodca000levi"
        val CA_MERCHANT_KEY_PROD = "b7d9ebc8-7db8-4bef-9484-33a7af8d0ad9"
        val CA_MERCHANT_SECRET_PROD = "x7GLncY1PLcBdcr22AWntNLylw45bdmaawx9q4pPP7U="
    }

    fun createCaptureContext(isProd: Boolean, isUSA: Boolean, callback: CaptureContextEvent) {
        val service = if (isProd) {
            FlexSessionServiceGenerator().getRetrofirApiService(Environment.PRODUCTION)
        } else {
            FlexSessionServiceGenerator().getRetrofirApiService(Environment.SANDBOX)
        }

        var cardData = FlexCardData(
            FlexFieldData(true), //card number
            FlexFieldData(true), //security code
            FlexFieldData(true), //expiration Month
            FlexFieldData(true), //expiration year
            FlexFieldData(false) //type
        )

        var paymentInfo = FlexPaymentInfo(cardData)
        val sessionFields = FlexSessionFields(paymentInfo)
        var requestObj = FlexSessionRequest(sessionFields)

        val gson = Gson()

        var merchantConfig = MerchantConfig()

        var merchantId = ""
        var merchantKey =  ""
        var merchantSecret = ""

        if (isProd) {
            if (isUSA) {
                merchantId = US_MERCHANT_ID_PROD
                merchantKey = US_MERCHANT_KEY_PROD
                merchantSecret = US_MERCHANT_SECRET_PROD
            } else {
                merchantId = CA_MERCHANT_ID_PROD
                merchantKey = CA_MERCHANT_KEY_PROD
                merchantSecret = CA_MERCHANT_SECRET_PROD
            }
        } else {
            if (isUSA) {
                merchantId = US_MERCHANT_ID_UAT
                merchantKey = US_MERCHANT_KEY_UAT
                merchantSecret = US_MERCHANT_SECRET_UAT
            } else {
                merchantId = CA_MERCHANT_ID_UAT
                merchantKey = CA_MERCHANT_KEY_UAT
                merchantSecret = CA_MERCHANT_SECRET_UAT
            }
        }

        merchantConfig.merchantID = merchantId
        merchantConfig.merchantKeyId = merchantKey
        merchantConfig.merchantSecretKey = merchantSecret

        merchantConfig.requestHost = isProd ? Constants.HOSTPROD : Constants.HOSTCAS

        val jsonObjectString = gson.toJson(requestObj)
        merchantConfig.requestData = jsonObjectString

        val body = jsonObjectString.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        var keyGenerationResponse: Response<String>?
        CoroutineScope(Dispatchers.IO).launch {
            try {
                keyGenerationResponse = service.createCaptureContext(
                    body,
                    getHeaderMapForCaptureContext(merchantId, merchantConfig, isProd)
                )
                withContext(Dispatchers.Main) {
                    if (keyGenerationResponse!!.isSuccessful) {
                        keyGenerationResponse!!.body()?.let {
                            callback.onCaptureContextResponse(it)
                        }
                    } else {
                        callback.onCaptureContextError(Exception("Failed to create capture context"))
                    }
                }
            } catch (e: Exception) {
                callback.onCaptureContextError(e)
            }
        }
    }

    private fun getHeaderMapForCaptureContext(merchantId: String, merchantConfig: MerchantConfig, isProd: Boolean): Map<String, String> {
        val headerMap = mutableMapOf<String, String>()
        headerMap[Constants.V_C_MERCHANTID] = merchantId
        headerMap[Constants.ACCEPT] = "application/jwt"
        headerMap["Content-Type"] = "application/json;charset=utf-8"
        headerMap[Constants.DATE] = PayloadUtility().getNewDate()
        headerMap[Constants.HOST] = isProd ? Constants.HOSTPROD : Constants.HOSTCAS
        headerMap["Connection"] = "keep-alive"
        headerMap["User-Agent"] = "Android"

        val value = Authorization().getToken(merchantConfig)
        headerMap[Constants.SIGNATURE] = value

        val payloadDigest = PayloadDigest(merchantConfig)
        val digest = payloadDigest.getDigest()
        headerMap[Constants.DIGEST] = digest!!

        return headerMap
    }

}