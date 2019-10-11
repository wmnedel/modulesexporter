package org.jahia.modules;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class HttpConnectionHelper {
    Logger logger = LoggerFactory.getLogger(HttpConnectionHelper.class);

    private HttpHost targetHost;
    private CloseableHttpClient client;
    private HttpClientContext context;

    private String hostName;
    private String scheme;
    private String userName;
    private String password;
    private int port;

    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPassword;

    private StringBuffer errorMessage;

    public String getErrorMessage() {
        return errorMessage.toString();
    }

    public void setErrorMessage(String message) {
        errorMessage.append("</br>" + message);
    }

    public HttpConnectionHelper(String hostName, String scheme, int port, String userName, String password) {
        this.hostName = hostName;
        this.scheme = scheme;
        this.userName = userName;
        this.password = password;
        this.port = port;
        this.errorMessage = new StringBuffer();
    }

    public HttpConnectionHelper(String hostName,
                                int port,
                                String userName,
                                String password,
                                String proxyHost,
                                int proxyPort,
                                String proxyUser,
                                String proxyPassword) {
        this.hostName = hostName;

        this.userName = userName;
        this.password = password;
        this.port = port;

        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        this.errorMessage = new StringBuffer();
    }

    private void prepareConnection() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        if (this.scheme.equalsIgnoreCase("https")) {
            this.targetHost = new HttpHost(hostName, port, "https");
        } else if (this.scheme.equalsIgnoreCase("http")) {
            this.targetHost = new HttpHost(hostName, port, "http");
        }

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(userName, password));

        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());

        // Add AuthCache to the execution contexts
        this.context = HttpClientContext.create();
        context.setCredentialsProvider(credsProvider);
        context.setAuthCache(authCache);

        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                NoopHostnameVerifier.INSTANCE);

        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", sslsf)
                        .register("http", new PlainConnectionSocketFactory())
                        .build();

        BasicHttpClientConnectionManager connectionManager =
                new BasicHttpClientConnectionManager(socketFactoryRegistry);

        this.client = HttpClients.custom().setSSLSocketFactory(sslsf)
                .setConnectionManager(connectionManager).build();
    }

    public String executeGetRequest(String URI) {

        try {
            prepareConnection();
            HttpGet httpGet = new HttpGet(URI);
            HttpResponse response = client.execute(targetHost, httpGet, context);

            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    return EntityUtils.toString(response.getEntity());
                case HttpStatus.SC_UNAUTHORIZED:
                    setErrorMessage("Connection to " + targetHost + "/" + URI + " returned unauthorized error. Please check your parameters");
                    return null;
                case HttpStatus.SC_FORBIDDEN:
                    setErrorMessage("Access forbidden to " + targetHost + "/" + URI);
                    return null;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    setErrorMessage("Internal server error: " + targetHost + "/" + URI);
                    return null;
                default:
                    setErrorMessage("Generic error accessing: " + targetHost + "/" + URI + "Please check the instance details");
                    return null;
            }

        } catch (KeyStoreException e) {
            logger.debug(e.getMessage(), e);
            setErrorMessage("GET failed for " + targetHost + "/" + URI + " with KeyStoreException");
        } catch (NoSuchAlgorithmException e) {
            logger.debug(e.getMessage(), e);
            setErrorMessage("GET failed for " + targetHost + "/" + URI + " with NoSuchAlgorithmException");
        } catch (KeyManagementException e) {
            logger.debug(e.getMessage(), e);
            setErrorMessage("GET failed for " + targetHost + "/" + URI + " with KeyManagementException");
        } catch (ClientProtocolException e) {
            logger.debug(e.getMessage(), e);
            setErrorMessage("GET failed for " + targetHost + "/" + URI + " with ClientProtocolException");
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
            setErrorMessage("GET failed for " + targetHost + "/" + URI + " with IOException");
        }

        setErrorMessage("Error accessing: " + targetHost + " Please check the instance details");
        return null;
    }

    public String executePostRequest(String URI, HttpEntity multipart) {
        try {
            prepareConnection();
            HttpPost httpPost = new HttpPost(URI);
            httpPost.setEntity(multipart);
            HttpResponse response = client.execute(targetHost, httpPost, context);

            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    return EntityUtils.toString(response.getEntity());
                case HttpStatus.SC_UNAUTHORIZED:
                    setErrorMessage("Connection to " + targetHost + "/" + URI + " returned unauthorized error. Please check the instance details");
                    return null;
                case HttpStatus.SC_FORBIDDEN:
                    setErrorMessage("Access forbidden to " + targetHost + "/" + URI);
                    return null;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    setErrorMessage("Internal server error: " + targetHost + "/" + URI);
                    return null;
                default:
                    setErrorMessage("Generic error accessing: " + targetHost + "/" + URI);
                    return null;
            }
        } catch (KeyStoreException e) {
            logger.debug(e.getMessage(), e);
            setErrorMessage("POST failed for " + targetHost + "/" + URI + " with KeyStoreException");
        } catch (NoSuchAlgorithmException e) {
            logger.debug(e.getMessage(), e);
            setErrorMessage("POST failed for " + targetHost + "/" + URI + " with NoSuchAlgorithmException");
        } catch (KeyManagementException e) {
            logger.debug(e.getMessage(), e);
            setErrorMessage("POST failed for " + targetHost + "/" + URI + " with KeyManagementException");
        } catch (ClientProtocolException e) {
            logger.debug(e.getMessage(), e);
            setErrorMessage("POST failed for " + targetHost + "/" + URI + " with ClientProtocolException");
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
            setErrorMessage("POST failed for " + targetHost + "/" + URI + " with IOException");
        }

        setErrorMessage("Generic error accessing: " + targetHost + "/" + URI);
        return null;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
}