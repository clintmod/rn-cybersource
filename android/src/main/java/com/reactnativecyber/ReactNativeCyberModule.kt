/*package com.reactnativecyber

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class ReactNativeCyberModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  fun multiply(a: Double, b: Double, promise: Promise) {
    promise.resolve(a * b)
  }

  companion object {
    const val NAME = "ReactNativeCyber"
  }
}*/

package com.reactnativecyber;

import android.util.Log

import com.cybersource.flex.android.CaptureContext
import com.cybersource.flex.android.FlexException
import com.cybersource.flex.android.FlexService
import com.cybersource.flex.android.TransientToken
import com.cybersource.flex.android.TransientTokenCreationCallback
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class CybersourceModule(reactContext: ReactApplicationContext?) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "ReactNativeCyber"
    }

    private fun createToken(keyId: String, sad: MutableMap<String, Any>, callback: Callback) {
        val cc = CaptureContext.fromJwt(keyId)
        val flexService = FlexService.getInstance()

        try {
            flexService.createTokenAsyncTask(cc, sad, object : TransientTokenCreationCallback {
                override fun onSuccess(tokenResponse: TransientToken?) {
                    if (tokenResponse != null) {
                        val token = tokenResponse.encoded;
                        callback.invoke(null, token) // Success callback with token as the argument
                    } else {
                        callback.invoke("Error: Transient token is null")
                    }
                }

                override fun onFailure(error: FlexException?) {
                    callback.invoke("Error: ${error?.message}")
                }
            })
        } catch (e: FlexException) {
            callback.invoke("Error: ${e.message}")
        }
    }

//     class PaymentInformation(
//            val cardNumber: String ,
//            val cardVerificationCode: String ,
//            val cardExpirationMonth: String,
//            val cardExpirationYear: String ,
//    )

    @ReactMethod
    fun makeReservation(isProd: Boolean, isUSA: Boolean, cardNumber: String, cardVerificationCode: String,
                        cardExpirationMonth: String,
                        cardExpirationYear: String,
                        callback: Callback) {
        val sad: MutableMap<String, Any> = HashMap()
        sad["paymentInformation.card.number"] = cardNumber
        sad["paymentInformation.card.securityCode"] = cardVerificationCode
        sad["paymentInformation.card.expirationMonth"] = cardExpirationMonth
        sad["paymentInformation.card.expirationYear"] = cardExpirationYear

        requestCaptureContext(isProd, isUSA, sad, callback)
    }

    private fun requestCaptureContext(isProd: Boolean, isUSA: Boolean, sad: MutableMap<String, Any>, callback: Callback) {
        CaptureContextHelper().createCaptureContext(isProd, isUSA, object : CaptureContextEvent {
            override fun onCaptureContextError(e: Exception) {
                callback.invoke("Error: ${e.message}")
            }

            override fun onCaptureContextResponse(cc: String) {
                createToken(cc, sad, callback)
                Log.d("CybersourceModule", cc)
            }
        })
    }
}

