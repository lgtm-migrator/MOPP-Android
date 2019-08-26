package ee.ria.DigiDoc.configuration.loader;

import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class CentralConfigurationClient {

    private static final int DEFAULT_TIMEOUT = 10;
    private final OkHttpClient httpClient;
    private final String centralConfigurationServiceUrl;
    private final X509Certificate explicitSSLCert;

    CentralConfigurationClient(String centralConfigurationServiceUrl, X509Certificate explicitSSLCert) {
        this.centralConfigurationServiceUrl = centralConfigurationServiceUrl;
        this.explicitSSLCert = explicitSSLCert;
        httpClient = constructHttpClient();
    }

    String getConfiguration() {
        return requestData(centralConfigurationServiceUrl + "/config.json");
    }

    String getConfigurationSignature() {
        return requestData(centralConfigurationServiceUrl + "/config.rsa");
    }

    String getConfigurationSignaturePublicKey() {
        return requestData(centralConfigurationServiceUrl + "/config.pub");
    }

    private String requestData(String url) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .build();

        Call call = httpClient.newCall(request);
        try {
            Response response = call.execute();
            if (response.code() != 200) {
                throw new CentralConfigurationException("Service responded with not OK status code " + response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new CentralConfigurationException("Service responded with empty body");
            }
            return responseBody.string();
        } catch (IOException e) {
            throw new CentralConfigurationException("Something went wrong during fetching configuration", e);
        }
    }

    private OkHttpClient constructHttpClient() {
        try {
            OkHttpClient.Builder builder = constructClientBuilder();
            if (explicitSSLCert != null) {
                builder.sslSocketFactory(constructTrustingSSLSocketFactory(explicitSSLCert));
            }
            return builder.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct HTTP client",e);
        }
    }

    private OkHttpClient.Builder constructClientBuilder() {
        return new OkHttpClient.Builder()
                .hostnameVerifier((hostname, session) -> true)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .callTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
    }

    private SSLSocketFactory constructTrustingSSLSocketFactory(X509Certificate sslCertificate) {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            keyStore.setCertificateEntry("caCert", sslCertificate);
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct SSLSocketFactory", e);
        }
    }

    class CentralConfigurationException extends RuntimeException {
        CentralConfigurationException(String message) {
            super(message);
        }

        CentralConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}