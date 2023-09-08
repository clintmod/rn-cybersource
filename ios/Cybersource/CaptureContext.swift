//
//  CaptureContext.swift
//  flex-api-ios-sample-app
//
//  Created by Rakesh Ramamurthy on 04/08/21.
//

import Foundation

/*
    WARNING: Cybersource API credentials are included for demonstration purposes only!
    The API credentials have been included in this sample mobile application for demonstration purposes only,
    and to provide a encapsulated demo.  The Credentials must be removed for the final application build.
 */

let US_MERCHANT_ID_UAT = "reg100levi"
let US_MERCHANT_KEY_UAT = "7aa9d362-4794-49df-be7b-3fde513809d8"
let US_MERCHANT_SECRET_UAT = "azmNU6cYeaXG5wpb+U/xHJozNai/dxflE2cm9u7TVow="

let US_MERCHANT_ID_PROD = "prod000levi"
let US_MERCHANT_KEY_PROD = "bf7a4abc-6123-45aa-83e0-861a4e12d44b"
let US_MERCHANT_SECRET_PROD = "VcFnyucPAIGPfuWVtRCoZy/nCPbwQzjetr9uiBFIxv8="

let CA_MERCHANT_ID_UAT = "regca100levi"
let CA_MERCHANT_KEY_UAT = "cc35be4a-a6c7-4d59-8428-4b308f6c21f4"
let CA_MERCHANT_SECRET_UAT = "xevsGNPlCKa2RMffwrwyTXMNIaby6tbJ0JmVi7p3oUo="

let CA_MERCHANT_ID_PROD = "prodca000levi"
let CA_MERCHANT_KEY_PROD = "b7d9ebc8-7db8-4bef-9484-33a7af8d0ad9"
let CA_MERCHANT_SECRET_PROD = "x7GLncY1PLcBdcr22AWntNLylw45bdmaawx9q4pPP7U="


/*
    WARNING: this code is Included for demonstration purposes only!
    The sample code found here was included for demonstration purposes and to make the sample application self-contained. Your merchant credentials and the sessions API call should be invoked from a secure backend service. The responding capture context JWT data element can them be sent to the application.
 
    The sessions API is to be invoked to establish the authentication of the merchants and to set the context of the information that is to be captured. The response to the sessions rest call is a JWT data object that contains the one-time keys used for point to point encryption.
    Samples of how to generate the capture context server side can be found on the Cybersource git-hub repository
*/

class CaptureContext {
    
    //Environment to test
    private var environment = Environment.sandbox

    typealias Result = FlexCaptureContextResult

    func createCaptureContext(isProd: Bool, isUSA: Bool, completion: @escaping (Result) -> Void) {
        if (isProd) {
            environment = Environment.production
        }
        let cardData = FlexCardData()
        cardData.number = FlexFieldData(isRequired: true)
        cardData.securityCode = FlexFieldData(isRequired: true)
        cardData.expirationMonth = FlexFieldData(isRequired: true)
        cardData.expirationYear = FlexFieldData(isRequired: true)
        cardData.type = FlexFieldData(isRequired: false)
        
        let paymentInfo = FlexPaymentInfo(data: cardData)
        let sessionFields = FlexSessionFields(info: paymentInfo)
        let requestObj = FlexSessionRequest(fields: sessionFields)
        
        let httpClient = URLSessionHTTPClient()

        var payloadData: Data?
        do {
            payloadData = try JSONEncoder().encode(requestObj)
        } catch let error {
            completion(.failure(error))
        }

        guard let payload = payloadData else {
            completion(.failure(NSError(domain:"Invaid payload", code:400, userInfo:nil)))
            return
        }
        
        let merchantConfig = createMerchantConfig(isProd: isProd, isUSA: isUSA)
        merchantConfig.requestData = String(decoding: payload, as: UTF8.self)
        
        let apiUrl = URL(string: self.environment.scheme + self.environment.host + self.environment.path)!
        let api = FlexSessionCreator(url: apiUrl, client: httpClient, payload: payload, headers: createHeaders(merchantConfig: merchantConfig))
        
        api.createCaptureContext { (result) in
            DispatchQueue.main.async {
                switch(result) {
                case let .success(response):
                    completion(.success(response))
                    return
                case let .failure(error):
                    completion(.failure(error))
                }
            }
        }
    }

    private func createMerchantConfig(isProd: Bool, isUSA: Bool) -> ApiConfig {
        var merchantId = ""
        var merchantKey = ""
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
        return ApiConfig(id: merchantId, key: merchantKey, secret: merchantSecret, env: self.environment)
    }

    private func createHeaders(merchantConfig: ApiConfig) -> [String: String] {
        var headers = [String: String]()
        headers[Constants.V_C_MERCHANTID] = merchantConfig.merchantID
        headers[Constants.ACCEPT] = "application/jwt"
        headers[Constants.CONTENTTYPE] = "application/json; charset=utf-8"
        headers[Constants.DATE] = PayloadUtility().iso8601().full
        //headers[Constants.HOST] = Constants.HOSTCAS
        headers[Constants.CONNECTION] = "keep-alive"
        headers[Constants.USERAGENT] = "iOS"

        let value = HTTPSignature(merchantConfig: merchantConfig).getHTTPSignature()
        headers[Constants.SIGNATURE] = value

        let payloadDigest = PayloadDigest(merchantConfig: merchantConfig)
        if let digest = payloadDigest.getDigest() {
            headers[Constants.DIGEST] = digest
        }

        return headers
    }
}
