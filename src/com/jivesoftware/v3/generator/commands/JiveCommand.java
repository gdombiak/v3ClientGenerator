package com.jivesoftware.v3.generator.commands;

/**
 * Created by gato on 2/28/14.
 */

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

/**
 * System properties:
 * <ul>
 *     <li>jiveHome - <b>URL of the Jive instance to talk to</b> or null means http://localhost:8080</li>
 *     <li>username - null means admin</li>
 *     <li>password - null means admin</li>
 * </ul>
 */
public abstract class JiveCommand {

    protected static final String jiveHome = System.getProperty("jiveHome") == null ? "http://localhost:8080" : System.getProperty("jiveHome");
    protected static final String api = jiveHome + "/api/core/v3";
    protected static final String clientUsername = System.getProperty("username") == null ? "admin" : System.getProperty("username");
    protected static final String clientPassword = System.getProperty("password") == null ? "admin" : System.getProperty("password");

    protected CloseableHttpResponse post(String json, String service) throws IOException {
        HttpPost httpPost = new HttpPost(service);
        StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        return execute(httpPost);

    }

    protected CloseableHttpResponse get(String service) throws IOException {
        return execute(new HttpGet(service));
    }

    private CloseableHttpResponse execute(HttpRequestBase request) throws IOException {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(clientUsername, clientPassword));
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build();
        // Add pre-emptive BASIC auth
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicScheme = new BasicScheme();

        URL jiveURL = new URL(jiveHome);
        String schema = jiveURL.getProtocol();
        String hostname = jiveURL.getHost();
        int port = jiveURL.getPort();
        HttpHost httpHost = new HttpHost(hostname, port, schema);
        authCache.put(httpHost, basicScheme);
        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(ClientContext.AUTH_CACHE, authCache);

        return httpclient.execute(request, httpContext);
    }

    public abstract JSONObject execute() throws JSONException, IOException, IllegalAccessException;
}