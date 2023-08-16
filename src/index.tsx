import { NativeModules } from 'react-native';

const ReactNativeCyber = NativeModules.ReactNativeCyber;

    export const makeReservation = (
      ccKey: string, 
      cardNumber: string, 
      cardVerificationCode: string, 
      cardExpirationMonth: string, 
      cardExpirationYear: string, ): Promise<number> => {
      return ReactNativeCyber.makeReservation(ccKey, cardNumber, cardVerificationCode, cardExpirationMonth, cardExpirationYear);
}
