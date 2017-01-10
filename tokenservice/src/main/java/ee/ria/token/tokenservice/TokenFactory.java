package ee.ria.token.tokenservice;

import android.util.Log;

import ee.ria.token.tokenservice.reader.CardReader;
import ee.ria.token.tokenservice.token.EstEIDv3d4;
import ee.ria.token.tokenservice.token.EstEIDv3d5;
import ee.ria.token.tokenservice.token.Token;
import ee.ria.token.tokenservice.util.TokenVersion;
import ee.ria.token.tokenservice.util.Util;

public class TokenFactory {

    private static final String TAG = "TokenFactory";
    static Token getTokenImpl(CardReader cardReader) {

        String cardVersion = null;
        try {
            byte[] versionBytes = cardReader.transmitExtended(new byte[]{0x00, (byte) 0xCA, 0x01, 0x00, 0x03});
            cardVersion = Util.toHex(versionBytes);
        } catch (Exception e) {
            Log.e(TAG, "getTokenImpl: ", e);
        }

        if (cardVersion == null) {
            return null;
        }
        Token token = null;
        if (cardVersion.startsWith(TokenVersion.V3D5.getVersion())) {
            token = new EstEIDv3d5(cardReader);
        } else if (cardVersion.startsWith(TokenVersion.V3D4.getVersion()) || cardVersion.startsWith(TokenVersion.V3D0.getVersion())) {
            token = new EstEIDv3d4(cardReader);
        }
        return token;
    }

}