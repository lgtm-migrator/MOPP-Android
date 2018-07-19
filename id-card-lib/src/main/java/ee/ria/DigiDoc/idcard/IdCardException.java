package ee.ria.DigiDoc.idcard;

import ee.ria.DigiDoc.smartcardreader.SmartCardReaderException;

public class IdCardException extends SmartCardReaderException {

    IdCardException(String message) {
        super(message);
    }

    IdCardException(String message, Throwable cause) {
        super(message, cause);
    }
}
