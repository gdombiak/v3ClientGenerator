package com.jivesoftware.v3.generator.commands;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;

/**
 * Created by gato on 2/28/14.
 */
public class GetObjectMetadata extends JiveCommand {

    private String service;

    public GetObjectMetadata(String service) {
        this.service = service;
    }

    @Override
    public JSONObject execute() throws JSONException, IOException, IllegalAccessException {
        System.out.println(new Date() + " - Getting Object Metadata: " + service);
        CloseableHttpResponse response = get(service);
        int code = response.getStatusLine().getStatusCode();
        if (code == HttpURLConnection.HTTP_OK) {
            String s = EntityUtils.toString(response.getEntity());
            s = s.replaceFirst("^throw [^;]*;", "");
            return new JSONObject(s);
        }
        throw new IllegalAccessException("Failed to execute command. Http error code: " + code);
    }
}
