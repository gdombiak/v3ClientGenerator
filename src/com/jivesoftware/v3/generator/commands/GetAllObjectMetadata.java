package com.jivesoftware.v3.generator.commands;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;

/**
 * Created by gato on 2/28/14.
 */
public class GetAllObjectMetadata extends JiveCommand {

    private static final String service = api + "/metadata/objects/@all";

    @Override
    public void execute() throws IOException {
        System.out.println(new Date() + " - Getting All Objects Metadata");
        CloseableHttpResponse response = get(service);
        int code = response.getStatusLine().getStatusCode();
        if (code == HttpURLConnection.HTTP_OK) {
            String s = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = null;
            try {
                s = s.replaceFirst("^throw [^;]*;", "");
                jsonObject = new JSONObject(s);
                JSONArray list = jsonObject.getJSONArray("list");
                System.out.print(list);

            } catch (JSONException e) {
                // TODO Handle this exception
                e.printStackTrace();
            }
        } else {
            System.out.println(new Date() + " - Failed to execute command. Http error code: " + code);
        }
    }
}
