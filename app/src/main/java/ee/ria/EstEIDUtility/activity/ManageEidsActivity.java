package ee.ria.EstEIDUtility.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.security.cert.CertificateParsingException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.domain.X509Cert;
import ee.ria.EstEIDUtility.util.DateUtils;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;
import ee.ria.token.tokenservice.callback.UseCounterCallback;
import ee.ria.token.tokenservice.token.Token;

public class ManageEidsActivity extends AppCompatActivity {

    private static final String TAG = ManageEidsActivity.class.getName();

    private TokenService tokenService;
    private TokenServiceConnection tokenServiceConnection;
    private boolean serviceBound;

    private TextView givenNames;
    private TextView surnameView;
    private TextView documentNumberView;
    private TextView cardValidity;
    private TextView cardValidityTime;
    private TextView certValidity;
    private TextView certValidityTime;
    private TextView certUsedView;
    private TextView personIdCode;
    private TextView dateOfBirth;
    private TextView nationalityView;
    private TextView emailView;
    private TextView info;
    private View infoSeparator;
    private BroadcastReceiver cardInsertedReceiver;
    private BroadcastReceiver cardRemovedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eids);

        info = (TextView) findViewById(R.id.info);
        info.setText(Html.fromHtml(getString(R.string.eid_info)));
        info.setMovementMethod(LinkMovementMethod.getInstance());

        infoSeparator = findViewById(R.id.info_separator);

        givenNames = (TextView) findViewById(R.id.givenNames);
        surnameView = (TextView) findViewById(R.id.surname);
        documentNumberView = (TextView) findViewById(R.id.document_number);
        cardValidity = (TextView) findViewById(R.id.card_validity);
        cardValidityTime = (TextView) findViewById(R.id.card_valid_value);

        certValidity = (TextView) findViewById(R.id.cert_validity);
        certValidityTime = (TextView) findViewById(R.id.cert_valid_value);
        certUsedView = (TextView) findViewById(R.id.cert_used);

        personIdCode = (TextView) findViewById(R.id.person_id);
        dateOfBirth = (TextView) findViewById(R.id.date_of_birth);
        nationalityView = (TextView) findViewById(R.id.nationality);
        emailView = (TextView) findViewById(R.id.email);

        tokenServiceConnection = new TokenServiceConnection();
        tokenServiceConnection.connectService();

        cardInsertedReceiver = new CardAbsentReciever();
        registerReceiver(cardInsertedReceiver, new IntentFilter(TokenService.CARD_PRESENT_INTENT));

        cardRemovedReceiver = new CardRemovedReciever();
        registerReceiver(cardRemovedReceiver, new IntentFilter(TokenService.CARD_ABSENT_INTENT));
    }

    class TokenServiceConnection implements ServiceConnection {

        void connectService() {
            Intent intent = new Intent(ManageEidsActivity.this, TokenService.class);
            ManageEidsActivity.this.bindService(intent, this, Context.BIND_AUTO_CREATE);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TokenService.LocalBinder binder = (TokenService.LocalBinder) service;
            tokenService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            tokenService = null;
        }
    }

    class CardAbsentReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            readPersonalData();
            readCertInfo();
            info.setVisibility(View.GONE);
            infoSeparator.setVisibility(View.GONE);
        }

    }

    class CardRemovedReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            info.setVisibility(View.VISIBLE);
            infoSeparator.setVisibility(View.VISIBLE);
            String empty = "";
            givenNames.setText(empty);
            givenNames.setText(empty);
            surnameView.setText(empty);
            documentNumberView.setText(empty);
            certValidityTime.setText(empty);
            cardValidityTime.setText(empty);
            personIdCode.setText(empty);
            dateOfBirth.setText(empty);
            nationalityView.setText(empty);
            cardValidity.setText(empty);
            emailView.setText(empty);
            certValidity.setText(empty);
            certUsedView.setText(empty);
        }
    }

    @Override
    protected void onDestroy() {
        if (cardInsertedReceiver != null) {
            unregisterReceiver(cardInsertedReceiver);
        }
        if (cardRemovedReceiver != null) {
            unregisterReceiver(cardRemovedReceiver);
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(tokenServiceConnection);
            serviceBound = false;
        }
    }

    public void readPersonalData() {
        PersonalFileCallback callback = new PersonalFileCallback() {
            @Override
            public void onPersonalFileResponse(SparseArray<String> result) {
                String expiryDate = result.get(9);

                surnameView.setText(result.get(1));
                givenNames.setText(result.get(2));
                givenNames.append(result.get(3));
                nationalityView.setText(result.get(5));
                dateOfBirth.setText(result.get(6));
                personIdCode.setText(result.get(7));
                documentNumberView.setText(result.get(8));
                certValidityTime.setText(expiryDate);

                try {
                    Date expiry = DateUtils.DATE_FORMAT.parse(expiryDate);
                    if (!expiry.before(new Date()) && expiry.after(new Date())) {
                        cardValidity.setText(getText(R.string.eid_valid));
                        cardValidity.setTextColor(Color.GREEN);
                        cardValidityTime.setTextColor(Color.GREEN);
                    } else {
                        cardValidity.setText(getText(R.string.eid_invalid));
                        cardValidity.setTextColor(Color.RED);
                        cardValidityTime.setTextColor(Color.RED);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "onPersonalFileResponse: ", e);
                }
            }

            @Override
            public void onPersonalFileError(String msg) {
                Log.d(TAG, "onPersonalFileError: " + msg);
            }
        };
        tokenService.readPersonalFile(callback);
    }

    public void readCertInfo() {
        SignCertificateCallback callback = new SignCertificateCallback();
        tokenService.readCert(Token.CertType.CertSign, callback);

        AuthCertificateCallback authCertificateCallback = new AuthCertificateCallback();
        tokenService.readCert(Token.CertType.CertAuth, authCertificateCallback);
    }

    private class UseCounterTaskCallback implements UseCounterCallback {
        @Override
        public void onCounterRead(int counterByte) {
            certUsedView.setText(String.valueOf(counterByte));
        }

    }

    class SignCertificateCallback implements CertCallback {

        @Override
        public void onCertificateResponse(byte[] cert) {
            X509Cert x509Cert = new X509Cert(cert);

            tokenService.readUseCounter(Token.CertType.CertSign, new UseCounterTaskCallback());

            if (x509Cert.isValid()) {
                certValidity.setText(getText(R.string.eid_valid));
                certValidity.setTextColor(Color.GREEN);
                certValidityTime.setTextColor(Color.GREEN);
            } else {
                certValidity.setText(getText(R.string.eid_invalid));
                certValidity.setTextColor(Color.RED);
                certValidityTime.setTextColor(Color.RED);
            }
        }

        @Override
        public void onCertificateError(Exception e) {
            Toast.makeText(ManageEidsActivity.this, getText(R.string.cert_read_failed) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    class AuthCertificateCallback implements CertCallback {

        @Override
        public void onCertificateResponse(byte[] cert) {
            X509Cert x509Cert = new X509Cert(cert);
            try {
                Collection<List<?>> subjectAlternativeNames = x509Cert.getCertificate().getSubjectAlternativeNames();
                if (subjectAlternativeNames == null) {
                    Log.d(TAG, "Couldn't read email");
                    return;
                }
                for (List subjectAlternativeName : subjectAlternativeNames) {
                    if ((Integer) subjectAlternativeName.get(0) == 1) {
                        emailView.setText((CharSequence) subjectAlternativeName.get(1));
                    }
                }
            } catch (CertificateParsingException e) {
                Log.e(TAG, "onCertificateResponse: ", e);
            }

        }

        @Override
        public void onCertificateError(Exception e) {
            Toast.makeText(ManageEidsActivity.this, getText(R.string.cert_read_failed) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

}