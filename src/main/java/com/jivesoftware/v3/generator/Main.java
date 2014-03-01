package com.jivesoftware.v3.generator;

import org.apache.maven.cli.MavenCli;

/**
 * Created by gato on 2/28/14.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CodeGenerator codeGenerator = new CodeGenerator();
        codeGenerator.generateCode();
        // Run maven project and install it
        MavenCli cli = new MavenCli();
        cli.doMain(new String[]{"clean", "install"}, getOutputFolder(), System.out, System.out);
    }

    private static String getOutputFolder() {
        return System.getProperty("output") == null ? "target/generated" : System.getProperty("output");
    }


}
