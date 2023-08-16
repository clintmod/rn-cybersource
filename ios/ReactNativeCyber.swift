//
//  RCTCybersourceModule.swift
//  LeviStraussAmericasApp
//
//  Created by Prem Kumar Chaudhary on 17/07/23.
//

import Foundation
import UIKit
import flex_api_ios_sdk
import React

@objc(ReactNativeCyber)
class ReactNativeCyber: NSObject {
    // ... (existing code remains the same)

    // Define a PaymentInfo struct to hold the payment details
    struct PaymentInfo {
        var cardNumber: String
        var cardVerificationCode: String
        var cardExpirationMonth: String
        var cardExpirationYear: String
    }

    // Add a property to store the cardInfo
    private var cardInfo: PaymentInfo?
    fileprivate var captureContextResponseString: String = ""


    // Add a callback property to store the JavaScript callback function
    private var createTransientTokenCallback: RCTResponseSenderBlock?

      
  private func getPayload() -> [String: String] {
      var payload = [String: String]()
      if let cardNumber = self.cardInfo?.cardNumber {
        payload["paymentInformation.card.number"] = cardNumber
      }
      
      if let cvv = self.cardInfo?.cardVerificationCode {
        payload["paymentInformation.card.securityCode"] = cvv
      }
      
      if let expMonth = self.cardInfo?.cardExpirationMonth {
        payload["paymentInformation.card.expirationMonth"] = expMonth
      }
      
      if let expYear = self.cardInfo?.cardExpirationYear {
        payload["paymentInformation.card.expirationYear"] = expYear
      }

      return payload
  }
  
    private func createTransientToken() {
        let service = FlexService()
      service.createTransientToken(from: self.captureContextResponseString, data: getPayload()) { [weak self] (result) in
            DispatchQueue.main.async {
                switch result {
                case .success(let tokenResponse):
                    self?.showResponseController()

                  let tokenDict: [String: Any] = ["token": tokenResponse.token]

                    // Check if the callback is set and invoke it with the success data
                    if let callback = self?.createTransientTokenCallback {
                      let response = ["status": "success", "message": "Transient token created successfully", "token": tokenDict]
                        callback([NSNull(), response])
                    }
                case let .failure(error):
                    self?.showTokenCreationError(error: error)

                    // Check if the callback is set and invoke it with the error data
                    if let callback = self?.createTransientTokenCallback {
                        let response = ["status": "error", "message": error.localizedDescription]
                        callback([NSNull(), response])
                    }
                }
            }
        }
    }

    // Expose makeReservation method to JavaScript using RCT_EXTERN_METHOD
    @objc
    static func requiresMainQueueSetup() -> Bool {
        return true
    }
  
    private func showSessionCreationError(error: Error) {

    }
  
    private func showResponseController() {

    }
  
    private func showTokenCreationError(error: FlexErrorResponse) {
    
    }
  
    private func requestCaptureContext() {
      CaptureContext().createCaptureContext() { [weak self] (result) in
                  DispatchQueue.main.async {
                      switch(result) {
                      case let .success(response):
                          if let sessionToken = response.keyId {
                              self?.captureContextResponseString = sessionToken
                              self?.createTransientToken()
                          }
                          break
                      case let .failure(error):
                          self?.showSessionCreationError(error: error)
                      }
                  }
              }
    }

    @objc
      func makeReservation(_ cardNumber: String, cardVerificationCode: String, cardExpirationMonth: String, cardExpirationYear: String, callback: @escaping RCTResponseSenderBlock) {
        // Store the JavaScript callback function
        createTransientTokenCallback = callback

      
        // Store the cardInfo in the local property
        self.cardInfo = PaymentInfo(
            cardNumber: cardNumber,
            cardVerificationCode: cardVerificationCode,
            cardExpirationMonth: cardExpirationMonth,
            cardExpirationYear: cardExpirationYear
        )

        // Call createTransientToken with the stored cardInfo
       self.requestCaptureContext()
    }
}
