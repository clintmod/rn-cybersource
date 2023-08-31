#import <React/RCTBridgeModule.h>

@interface
RCT_EXTERN_MODULE(ReactNativeCyber, NSObject)

RCT_EXTERN_METHOD(makeReservation: (BOOL)isProd isUSA: (BOOL *)isUSA cardNumber: (NSString *)cardNumber  cardVerificationCode: (NSString *)cardVerificationCode cardExpirationMonth: (NSString *)cardExpirationMonth cardExpirationYear: (NSString *)cardExpirationYear callback: (RCTResponseSenderBlock)callback)

@end

