package com.jivesoftware.v3.generator;

import com.jivesoftware.v3.generator.commands.GetObjectMetadata;
import com.jivesoftware.v3.generator.commands.GetObjectTypes;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by gato on 2/28/14.
 */
public class CodeGenerator {

    private static List<String> primitives;

    static {
        primitives = new ArrayList<String>();
        primitives.add("string");
        primitives.add("integer");
        primitives.add("date");
        primitives.add("boolean");
        primitives.add("jsonobject");
        primitives.add("uri");
    }

    public void generateCode() {
        try {
            createMavenProject();
            GetObjectTypes command = new GetObjectTypes();
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

    private void createMavenProject() throws IOException {
        String outputFolder = getOutputFolder();
        File folder = new File(outputFolder);
        if (folder.exists()) {
            // Clean up folder
            FileUtils.deleteDirectory(folder);
        }

        File pom = new File(outputFolder + "/pom.xml");
        FileUtils.writeStringToFile(pom, getPOMFileContent());
    }

    private String getOutputFolder() {
        return System.getProperty("output") == null ? "target/generated" : System.getProperty("output");
    }

    private String getPOMFileContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.jivesoftware.v3client</groupId>\n" +
                "    <artifactId>library</artifactId>\n" +
                "    <version>0.1-SNAPSHOT</version>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>com.jivesoftware.v3client</groupId>\n" +
                "            <artifactId>framework</artifactId>\n" +
                "            <version>0.1-SNAPSHOT</version>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "            <groupId>org.json</groupId>\n" +
                "            <artifactId>json</artifactId>\n" +
                "            <version>20140107</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>\n";
    }

    private void generateCodeFor(String typeURL) throws IOException, IllegalAccessException {
        if (shouldIgnoreClass(typeURL)) {
            return;
        }
        GetObjectMetadata command = new GetObjectMetadata(typeURL);
        JSONObject typeSpec = command.execute();

        StringBuilder sb = new StringBuilder(100000);
        addPackageAndImports(sb);
        addClassDocs(typeSpec, sb);
        addClassDefinition(typeSpec, sb);
        addConstructor(typeSpec, sb);
        addInstanceVariables(typeSpec, sb);
        addGettersAndSetters(typeSpec, sb);
        addVerbs(typeSpec, sb);
        addAbstractMethods(typeSpec, sb);
        sb.append("}");
        writeToFile(sb, getOutputFolder() + "/src/main/java/com/jivesoftware/v3client/framework/entities/" + getClassName(typeSpec) + ".java");
//        System.out.println(sb.toString());
    }

    private void writeToFile(StringBuilder sb, String filename) throws IOException {
        File file = new File(filename);
        FileUtils.writeStringToFile(file, sb.toString());
    }

    private void addPackageAndImports(StringBuilder sb) {
        sb.append("package com.jivesoftware.v3client.framework.entities;\n\n");

        sb.append("import com.jivesoftware.v3client.framework.AbstractJiveClient;\n");
        sb.append("import com.jivesoftware.v3client.framework.entity.*;\n\n");
        sb.append("import com.jivesoftware.v3client.framework.type.EntityType;\n\n");
        sb.append("import java.net.URI;\n");
        sb.append("import java.util.Collection;\n");
        sb.append("import java.util.Date;\n");
        sb.append("import org.json.JSONObject;\n");
        // TODO Add more imports
    }

    private void addClassDocs(JSONObject typeSpec, StringBuilder sb) {
        sb.append("\n");
        addJavadocs(typeSpec.optString("description"), "", sb);
    }

    private void addJavadocs(String description, String stringIndent, StringBuilder sb) {
        if (description != null && description.length() > 0) {
            sb.append(stringIndent).append("/**\n");
            sb.append(description.replaceAll("(?m)^", stringIndent + " * ")).append("\n");
            sb.append(stringIndent).append(" */\n");
        }
    }

    private void addClassDefinition(JSONObject typeSpec, StringBuilder sb) {
        String className = getClassName(typeSpec);
        sb.append("public class ").append(className).append(" extends AbstractEntity {\n\n");
        // TODO Add extends once we have it
    }

    private void addConstructor(JSONObject typeSpec, StringBuilder sb) {
        sb.append("\tpublic ").append(getClassName(typeSpec)).append("(AbstractJiveClient jiveClient) {\n");
        sb.append("\t\tsuper(jiveClient);\n");
        sb.append("\t}\n\n");
    }

    private String getClassName(JSONObject typeSpec) {
        String name = typeSpec.getString("name");
        String suffix = "Entity";
        // Weird HACK. Too tired to think. Do not append Entity if already has it
        if (name.endsWith("Entity")) {
            suffix = "";
        }

        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + suffix;
    }

    private void addInstanceVariables(JSONObject typeSpec, StringBuilder sb) {
        JSONArray fields = typeSpec.getJSONArray("fields");
        for (int i=0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (shouldIgnoreField(field)) {
                // Ignore unpublished fields or resources field
                continue;
            }

            sb.append("\tprivate ");
            addJavaFieldType(getTypeFromJSON(field), sb);
            sb.append(" _").append(getInstanceVariableName(field)).append(";\n");
        }
        sb.append("\n");
    }

    private String getInstanceVariableName(JSONObject field) {
        String name = field.getString("name");
        // Make sure word has no spaces
        name = name.replaceAll(" ", "_");
        return name;
    }

    private String getTypeFromJSON(JSONObject field) {
        if (field.has("entityType")) {
            return field.getString("entityType");
        }
        return field.getString("type");
    }

    private void addGettersAndSetters(JSONObject typeSpec, StringBuilder sb) {
        JSONArray fields = typeSpec.getJSONArray("fields");
        for (int i=0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (shouldIgnoreField(field)) {
                // Ignore unpublished fields or resources field
                continue;
            }

            String name = getInstanceVariableName(field);
            String methodPartName = Character.toUpperCase(name.charAt(0)) + name.substring(1);

            addJavadocs(field.optString("description"), "\t", sb);
            sb.append("\tpublic ");
            addJavaFieldType(getTypeFromJSON(field), sb);
            sb.append(" get").append(methodPartName).append("() {\n");
            sb.append("\t\treturn this._").append(name).append(";\n");
            sb.append("\t}\n\n");

            if (field.getBoolean("editable")) {
                addJavadocs(field.optString("description"), "\t", sb);
                sb.append("\tpublic void set").append(methodPartName).append("(");
                addJavaFieldType(getTypeFromJSON(field), sb);
                sb.append(" _").append(name).append(") {\n");
                sb.append("\t\tthis._").append(name).append(" = _").append(name).append(";\n");
                sb.append("\t}\n\n");
            }

        }
        sb.append("\n");
    }

    private void addAbstractMethods(JSONObject typeSpec, StringBuilder sb) {
        sb.append("\t@Override\n");
        sb.append("\tprotected EntityType<?> lookupResourceType(String s) {\n");
        sb.append("\t\treturn null;\n");
        sb.append("\t}\n\n");
    }

    private void addVerbs(JSONObject typeSpec, StringBuilder sb) {
        if (!typeSpec.has("resourceLinks")) {
            return;
        }
        JSONArray resourceLinks = typeSpec.getJSONArray("resourceLinks");
        for (int i=0; i < resourceLinks.length(); i++) {
            JSONObject resourceLink = resourceLinks.getJSONObject(i);
            if (shouldIgnoreResourceLink(resourceLink)) {
                // Ignore unpublished resources
                continue;
            }

            addJavadocs(resourceLink.optString("description"), "\t", sb);
            sb.append("\tpublic ");
            // Add return type
            String responseType = resourceLink.getString("responseType");
            boolean hasResponse = !"void".equals(responseType);
            addJavaFieldType(responseType, sb);
            // Add method name
            String methodName = resourceLink.getString("jsMethod");
            methodName = "get".equals(methodName) ? "refresh" : methodName;
            sb.append(" ").append(methodName).append("(");

            String requestType = resourceLink.getString("requestType");
            if (!"void".equals(requestType)) {
                addJavaFieldType(requestType, sb);
                sb.append(" input");
            }
            sb.append(") {\n");
            if (hasResponse) {
                sb.append("\t\treturn null; // TODO This\n");
            }
            sb.append("\t}\n\n");
        }
    }

    private boolean shouldIgnoreClass(String typeURL) {
        // HACK to ignore some types that are having problems with generated code
        return typeURL.endsWith("/url");
    }

    /**
     *
     * @param field
     * @return
     */
    private boolean shouldIgnoreField(JSONObject field) {
        String name = field.getString("name");
        if (field.getBoolean("unpublished") || "resources".equals(name) || "id".equals(name) || "type".equals(name)) {
            return true;
        } else {
            // HACK to ignore special fields that should exist but don't
            if (field.has("type")) {
                String fieldType = field.getString("type");
                return "email".equalsIgnoreCase(fieldType) || "phone".equalsIgnoreCase(fieldType);
            }
        }
        return false;
    }

    private boolean shouldIgnoreResourceLink(JSONObject resourceLink) {
        if (resourceLink.getBoolean("unpublished") || !resourceLink.has("jsMethod") || !resourceLink.has("responseType") || !resourceLink.has("requestType")
                || "?".equals(resourceLink.optString("responseType")) || "?".equals(resourceLink.optString("requestType"))) {
            return true;
        } else {
            // Hack time. To avoid duplicated methods lets ignore these things. Or just because they do not compile for some reason
            String jsMethod = resourceLink.getString("jsMethod");
            return "getOutcomeTypes".equals(jsMethod) || "getContentImages".equals(jsMethod) || "getAttachments".equals(jsMethod) || "getContentExternalURLs".equals(jsMethod) || "addParticipant".equals(jsMethod);
        }
    }

    private void addJavaFieldType(String fieldType, StringBuilder sb) {
        // Special case
        if ("Entity".equals(fieldType)) {
            sb.append("ContentEntity");
            return;
        } else if ("void".equals(fieldType)) {
            sb.append("void");
            return;
        } else if ("Entity?".equals(fieldType)) {
            sb.append("AbstractEntity");
            return;
        }
        if (fieldType.endsWith("[]")) {
            // Handle collections
            sb.append("Collection<");
            addJavaFieldType(fieldType.substring(0, fieldType.length() - 2), sb);
            sb.append(">");
//            .append(fieldType.substring(0, fieldType.length() - 2)).append("Entity>");
        } else {
            // Check if we are dealing with a primitive type or not
            if (primitives.contains(fieldType.toLowerCase())) {
                // Make sure word starts with uppercase
                fieldType = Character.toUpperCase(fieldType.charAt(0)) + fieldType.substring(1);
                sb.append(fieldType);
            } else {
                // Make sure word has no spaces and starts with uppercase
                fieldType = fieldType.replaceAll(" ", "_");
                fieldType = Character.toUpperCase(fieldType.charAt(0)) + fieldType.substring(1);
                // Special case for some weird fields
                if ("text".equalsIgnoreCase(fieldType) || "Largetext".equalsIgnoreCase(fieldType)) {
                    sb.append("String");
                } else if ("any".equalsIgnoreCase(fieldType)) {
                    sb.append("AbstractEntity");
                } else if ("number".equalsIgnoreCase(fieldType)) {
                    sb.append("Integer");
                } else {
                    sb.append(fieldType);
                    // Weird HACK. Too tired to think. Do not append Entity if already has it
                    if (!fieldType.endsWith("Entity")) {
                        sb.append("Entity");
                    }
                }
            }
        }
    }
}
