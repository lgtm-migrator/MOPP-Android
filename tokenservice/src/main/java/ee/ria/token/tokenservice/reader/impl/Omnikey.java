/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.token.tokenservice.reader.impl;

import android.content.Context;
import android.content.Intent;
import android.smartcardio.Card;
import android.smartcardio.CardException;
import android.smartcardio.CommandAPDU;
import android.smartcardio.TerminalFactory;
import android.smartcardio.ipc.CardService;
import android.smartcardio.ipc.ICardService;
import android.util.Log;

import ee.ria.token.tokenservice.reader.CardReader;
import ee.ria.token.tokenservice.reader.SmartCardCommunicationException;

public class Omnikey extends CardReader {
    private ICardService mService;
    private TerminalFactory mFactory;
    private Card card;

    public Omnikey(Context context) {
        try {
            Intent serviceIntent = new Intent("com.theobroma.cardreadermanager.backendipc.BroadcastRecord");
            serviceIntent.setPackage("com.theobroma.cardreadermanager.backendipc");
            if (context.getPackageManager().queryIntentServices(serviceIntent, 0).isEmpty()) {
                context.startService(serviceIntent);
            }
            mService = CardService.getInstance(context.getApplicationContext());
            mFactory = mService.getTerminalFactory();
        } catch (Exception e) {
            Log.e(TAG, "Omnikey: ", e);
        }
    }

    public boolean hasSupportedReader() {
        try {
            return mFactory.terminals().list().size() > 0;
        } catch (CardException e) {
            Log.e(TAG, "hasSupportedReader: ", e);
            return false;
        }
    }

    @Override
    public boolean isSecureChannel() {
        return true;
    }

    @Override
    public byte[] transmit(byte[] apdu) {
        try {
            return card.getBasicChannel().transmit(new CommandAPDU(apdu)).getBytes();
        } catch (CardException e) {
            throw new SmartCardCommunicationException(e);
        }
    }

    @Override
    public void connect(Connected connected) {
        try {
            card = mFactory.terminals().list().get(0).connect("T=0");
            connected.connected();
        } catch (CardException e) {
            Log.e(TAG, "connect: ", e);
        }
    }

    @Override
    public void close() {
        if (card != null) {
            try {
                card.disconnect(true);
            } catch (CardException e) {
                Log.e(TAG, "close: ", e);
            }
        }
        mService.releaseService();
    }
}
