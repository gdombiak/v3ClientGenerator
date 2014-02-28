package com.jivesoftware.v3.generator;

import com.jivesoftware.v3.generator.commands.GetAllObjectMetadata;

/**
 * Created by gato on 2/28/14.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        GetAllObjectMetadata command = new GetAllObjectMetadata();
        command.execute();
    }
}
