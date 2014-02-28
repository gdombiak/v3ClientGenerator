package com.jivesoftware.v3.generator;

import com.jivesoftware.v3.generator.commands.GetObjectMetadata;
import com.jivesoftware.v3.generator.commands.GetObjectTypes;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by gato on 2/28/14.
 */
public class CodeGenerator {

    public void generateCode() {
        GetObjectTypes command = new GetObjectTypes();
        try {
            JSONObject objectTypes = command.execute();
            Iterator keys = objectTypes.keys();
            while (keys.hasNext()) {
                String type = (String) keys.next();
                String typeURL = objectTypes.getString(type);
                generateCodeFor(typeURL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateCodeFor(String typeURL) throws IOException, IllegalAccessException {
        GetObjectMetadata command = new GetObjectMetadata(typeURL);
        JSONObject typeSpec = command.execute();
        System.out.println(typeSpec);
    }
}
