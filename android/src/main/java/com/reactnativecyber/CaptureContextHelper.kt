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
        val MERCHANT_ID = "reg100levi"
        val MERCHANT_KEY = "7aa9d362-4794-49df-be7b-3fde513809d8"
        val MERCHANT_SECRET = "azmNU6cYeaXG5wpb+U/xHJozNai/dxflE2cm9u7TVow="
    }

    fun createCaptureContext(callback: CaptureContextEvent) {
        val service = FlexSessionServiceGenerator().getRetrofirApiService(Environment.SANDBOX)

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
        merchantConfig.merchantID = MERCHANT_ID
        merchantConfig.merchantKeyId = MERCHANT_KEY
        merchantConfig.merchantSecretKey = MERCHANT_SECRET

        merchantConfig.requestHost = Constants.HOSTCAS

        val jsonObjectString = gson.toJson(requestObj)
        merchantConfig.requestData = jsonObjectString

        val body = jsonObjectString.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        var keyGenerationResponse: Response<String>?
        CoroutineScope(Dispatchers.IO).launch {
            try {
                keyGenerationResponse = service.createCaptureContext(
                    body,
                    getHeaderMapForCaptureContext(merchantConfig)
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

    private fun getHeaderMapForCaptureContext(merchantConfig: MerchantConfig): Map<String, String> {
        val headerMap = mutableMapOf<String, String>()
        headerMap[Constants.V_C_MERCHANTID] = MERCHANT_ID
        headerMap[Constants.ACCEPT] = "application/jwt"
        headerMap["Content-Type"] = "application/json;charset=utf-8"
        headerMap[Constants.DATE] = PayloadUtility().getNewDate()
        headerMap[Constants.HOST] = Constants.HOSTCAS
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