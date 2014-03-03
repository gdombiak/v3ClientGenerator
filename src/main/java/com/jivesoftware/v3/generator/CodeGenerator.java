package com.jivesoftware.v3.generator;

import com.jivesoftware.v3.generator.commands.GetObjectMetadata;
import com.jivesoftware.v3.generator.commands.GetObjectTypes;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by gato on 2/28/14.
 */
public class CodeGenerator {

    private static final Pattern MATCH_STATIC_METHOD_NAME = Pattern.compile("^osapi\\.jive\\.corev3(?:\\.(\\w+))?(?:\\.(\\w+))$");
    private static final Pattern MATCH_PATH_PARAM = Pattern.compile("\\{(\\w+)}");

    private static final Comparator<Map.Entry<String,?>> MAGIC_QUERY_PARAM_ORDER = new Comparator<Map.Entry<String,?>>() {

        private final MagicQueryParamOrder magicOrder = new MagicQueryParamOrder();

        @Override
        public int compare(Map.Entry<String,?> o1, Map.Entry<String,?> o2) {
            return magicOrder.compare(o1.getKey(), o2.getKey());
        }
    };

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
            GetObjectTypes getObjectTypes = new GetObjectTypes();
            JSONObject objectTypes = getObjectTypes.execute();
            StringBuilder jiveClient = new StringBuilder(100000);
            addJiveClientPackageAndImports(jiveClient);
            addJiveClientClassDocs(jiveClient);
            addJiveClientClassDefinition(jiveClient);
            addJiveClientConstructor(jiveClient);
            addJiveClientStaticBegin(jiveClient);
            Map<String,StaticInnerClass> staticClasses = new TreeMap<>();
            addStaticClasses(getContentsEntity(), staticClasses);
            addStaticClasses(getPlacesEntity(), staticClasses);
            Iterator keys = objectTypes.keys();
            while (keys.hasNext()) {
                String type = (String) keys.next();
                String typeURL = objectTypes.getString(type);
                generateCodeFor(typeURL, staticClasses, jiveClient);
            }
            addJiveClientStaticEnd(jiveClient);
            for (StaticInnerClass staticClass : staticClasses.values()) {
                addJiveClientStaticClass(staticClass, jiveClient);
            }
            addJiveClientClassEnd(jiveClient);
            writeToFile(jiveClient, getOutputFolder() + "/src/main/java/com/jivesoftware/v3client/framework/JiveClient.java");
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
                "    <artifactId>corev3-client</artifactId>\n" +
                "    <version>8c2-SNAPSHOT</version>\n" +
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
                "    <build>\n" +
                "        <plugins>\n" +
                "            <plugin>\n" +
                "                <groupId>org.apache.maven.plugins</groupId>\n" +
                "                <artifactId>maven-compiler-plugin</artifactId>\n" +
                "                <version>3.1</version>\n" +
                "                <configuration>\n" +
                "                    <source>1.7</source>\n" +
                "                    <target>1.7</target>\n" +
                "                </configuration>\n" +
                "            </plugin>\n" +
                "            <plugin>\n" +
                "                <groupId>org.apache.maven.plugins</groupId>\n" +
                "                <artifactId>maven-source-plugin</artifactId>\n" +
                "                <executions>\n" +
                "                    <execution>\n" +
                "                        <id>attach-sources</id>\n" +
                "                        <goals>\n" +
                "                            <goal>jar</goal>\n" +
                "                        </goals>\n" +
                "                    </execution>\n" +
                "                </executions>\n" +
                "            </plugin>\n" +
                "        </plugins>\n" +
                "    </build>\n" +
                "</project>\n";
    }

    private JSONObject getContentsEntity() {
        return new JSONObject("" +
                "{\n" +
                "  \"staticMethods\" : [{\n" +
                "    \"name\" : \"osapi.jive.corev3.contents.find\",\n" +
                "    \"description\" : \"<p>Return a paginated list of contents that match the specified criteria.</p>\",\n" +
                "    \"responseType\" : \"content[]\",\n" +
                "    \"unpublished\" : false,\n" +
                "    \"verb\" : \"GET\",\n" +
                "    \"paramPath\" : \"/contents\",\n" +
                "    \"queryParams\" : {\n" +
                "      \"count\" : \"Integer\",\n" +
                "      \"fields\" : \"String\",\n" +
                "      \"filter\" : \"String[]\",\n" +
                "      \"sort\" : \"String\",\n" +
                "      \"startIndex\" : \"Integer\"\n" +
                "    },\n" +
                "    \"requestType\" : \"void\",\n" +
                "    \"hasBodyParam\" : false\n" +
                "  }]\n" +
                "}");
    }

    private JSONObject getPlacesEntity() {
        return new JSONObject("" +
                "{\n" +
                "  \"staticMethods\" : [{\n" +
                "    \"name\" : \"osapi.jive.corev3.places.find\",\n" +
                "    \"description\" : \"<p>Return a paginated list of places that match the specified criteria.</p>\",\n" +
                "    \"responseType\" : \"place[]\",\n" +
                "    \"unpublished\" : false,\n" +
                "    \"verb\" : \"GET\",\n" +
                "    \"paramPath\" : \"/places\",\n" +
                "    \"queryParams\" : {\n" +
                "      \"count\" : \"Integer\",\n" +
                "      \"fields\" : \"String\",\n" +
                "      \"filter\" : \"String[]\",\n" +
                "      \"sort\" : \"String\",\n" +
                "      \"startIndex\" : \"Integer\"\n" +
                "    },\n" +
                "    \"requestType\" : \"void\",\n" +
                "    \"hasBodyParam\" : false\n" +
                "  }]\n" +
                "}");
    }

    private void generateCodeFor(String typeURL, Map<String,StaticInnerClass> staticClasses, StringBuilder jiveClient) throws IOException, IllegalAccessException {
        if (shouldIgnoreClass(typeURL)) {
            return;
        }
        GetObjectMetadata command = new GetObjectMetadata(typeURL);
        JSONObject typeSpec = command.execute();
        addStaticClasses(typeSpec, staticClasses);

        StringBuilder sb = new StringBuilder(100000);
        addPackageAndImports(sb);
        addClassDocs(typeSpec, sb);
        addClassDefinition(typeSpec, sb);
        addEntityToLibrary(typeSpec, jiveClient);
        addConstructor(typeSpec, sb);
        addInstanceVariables(typeSpec, sb);
        addGettersAndSetters(typeSpec, sb);
        addVerbs(typeSpec, sb);
        sb.append("}");
        writeToFile(sb, getOutputFolder() + "/src/main/java/com/jivesoftware/v3client/framework/entities/" + getClassName(typeSpec) + ".java");
//        System.out.println(sb.toString());
    }

    private void writeToFile(StringBuilder sb, String filename) throws IOException {
        File file = new File(filename);
        FileUtils.writeStringToFile(file, sb.toString());
    }

    private void addStaticClasses(JSONObject typeSpec, Map<String,StaticInnerClass> staticClasses) {
        JSONArray methods = typeSpec.optJSONArray("staticMethods");
        if (methods != null) {
            for (int i = 0, l = methods.length(); i < l; i++) {
                JSONObject jsonObject = methods.optJSONObject(i);
                Matcher nameMatcher = MATCH_STATIC_METHOD_NAME.matcher(jsonObject.getString("name"));
                if (nameMatcher.find()) {
                    String name = nameMatcher.group(1);
                    StaticInnerClass staticClass = staticClasses.get(name);
                    if (staticClass == null) {
                        staticClass = new StaticInnerClass(name);
                        staticClasses.put(name, staticClass);
                    }
                    staticClass.getStaticMethodsByName().put(nameMatcher.group(2), jsonObject);
                }
            }
        }
    }

    private void addPackageAndImports(StringBuilder sb) {
        sb.append("package com.jivesoftware.v3client.framework.entities;\n\n");

        sb.append("import com.jivesoftware.v3client.framework.AbstractJiveClient;\n");
        sb.append("import com.jivesoftware.v3client.framework.entity.*;\n");
        sb.append("import com.jivesoftware.v3client.framework.http.EndpointDef;\n");
        sb.append("import com.jivesoftware.v3client.framework.http.HttpTransport;\n");
        sb.append("import com.jivesoftware.v3client.framework.NameValuePair;\n");
        sb.append("import com.jivesoftware.v3client.framework.type.EntityType;\n");
        sb.append("import com.jivesoftware.v3client.framework.type.EntityTypeLibrary;\n");
        sb.append("\n");
        sb.append("import java.net.URI;\n");
        sb.append("import java.util.Collection;\n");
        sb.append("import java.util.Date;\n");
        sb.append("import org.json.JSONObject;\n");
        sb.append("\n");
        sb.append("import static com.jivesoftware.v3client.framework.http.HttpTransport.Method.*;\n");
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
        String superClass = "AbstractEntity";
        if (typeSpec.optBoolean("content")) {
            superClass = "ContentEntity";
        } else if (typeSpec.optBoolean("place")) {
            superClass = "PlaceEntity";
        }
        sb.append("public class ").append(className).append(" extends ").append(superClass).append(" {\n\n");
    }

    private void addEntityToLibrary(JSONObject typeSpec, StringBuilder sb) {
        if ("resource".equals(typeSpec.getString("name"))) return;
        sb.append("\t\tEntityTypeLibrary.ROOT.add(new EntityType<");
        sb.append(getClassName(typeSpec)).append(">(");
        sb.append(getClassName(typeSpec)).append(".class, ");
        sb.append("\"").append(typeSpec.getString("name")).append("\", ");
        sb.append("\"").append(typeSpec.optString("plural", typeSpec.getString("name"))).append("\"");
        sb.append("));\n");
    }

    private void addConstructor(JSONObject typeSpec, StringBuilder sb) {
        sb.append("\tpublic ").append(getClassName(typeSpec)).append("(AbstractJiveClient jiveClient) {\n");
        sb.append("\t\tsuper(jiveClient, \"").append(typeSpec.getString("name")).append("\");\n");
        sb.append("\t}\n\n");
        sb.append("\tpublic ").append(getClassName(typeSpec)).append("() {\n");
        sb.append("\t\tsuper(AbstractJiveClient.JIVE_CLIENT.get(), \"").append(typeSpec.getString("name")).append("\");\n");
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
            addJavaFieldType(getTypeFromJSON(field), true, sb);
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
            addJavaFieldType(getTypeFromJSON(field), true, sb);
            sb.append(" get").append(methodPartName).append("() {\n");
            sb.append("\t\treturn this._").append(name).append(";\n");
            sb.append("\t}\n\n");
            sb.append("\tpublic void set").append(methodPartName).append("(");
            addJavaFieldType(getTypeFromJSON(field), true, sb);
            sb.append(" _").append(name).append(") {\n");
            sb.append("\t\tthis._").append(name).append(" = _").append(name).append(";\n");
            sb.append("\t}\n\n");

        }
        sb.append("\n");
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

            String methodName = resourceLink.getString("jsMethod");
            methodName = "get".equals(methodName) ? "refresh" : methodName;
            addEnpointDef(methodName, resourceLink, "", true, sb);
            addMethod(typeSpec.getString("name"), methodName, resourceLink, "", sb);
        }
    }

    private void addEnpointDef(String methodName, JSONObject methodDef, String indent, boolean isStatic, StringBuilder sb) {
        sb.append(indent).append("\tprivate ").append(isStatic ? "static " : "").append("final ");
        sb.append("EndpointDef ").append(methodName).append("EndpointDef =\n");
        sb.append(indent).append("\t\t\tnew EndpointDef(").append(methodDef.optString("verb")).append(",\n");
        sb.append(indent).append("\t\t\t                \"").append(methodDef.optString("paramPath")).append("\",\n");
        sb.append(indent).append("\t\t\t                queryParams(");
        addFormattedJoin(magicSortQueryParams(extractQueryParams(methodDef)), "\"%s\"", ", ", sb);
        sb.append("),\n");
        String requestType = methodDef.getString("requestType");
        if (!"void".equals(requestType)) {
            sb.append(indent).append("\t\t\t                \"").append(requestType).append("\",\n");
        } else {
            sb.append(indent).append("\t\t\t                null,\n");
        }
        addParameterOverrides(methodDef, indent, sb);
        sb.append(");\n\n");
    }

    private void addMethod(String containerType, String methodName, JSONObject methodDef, String indent, StringBuilder sb) {
        addJavadocs(methodDef.optString("description"), indent + "\t", sb);
        sb.append(indent).append("\tpublic ");
        // Add return type
        String responseType = methodDef.getString("responseType");
        boolean hasResponse = !"void".equals(responseType);
        addJavaFieldType(responseType, true, sb);
        // Add method name
        sb.append(" ").append(methodName).append("(");

        boolean first = true;
        boolean hasBody = false;
        String requestType = methodDef.getString("requestType");
        boolean selfUpdate = containerType != null && containerType.equals(requestType) && "update".equals(methodName);
        if (!"void".equals(requestType) && !selfUpdate) {
            addJavaFieldType(requestType, true, sb);
            sb.append(" input");
            first = false;
            hasBody = true;
        }
        Set<String> pathParams = containerType != null ? Collections.<String>emptySet() : extractPathParams(methodDef);
        if (!pathParams.isEmpty()) {
            if (first) first = false;
            else sb.append(", ");
            addFormattedJoin(pathParams, "String %s", ", ", sb);
        }
        Map<String,String> queryParams = extractQueryParams(methodDef);
        boolean filtered = queryParams.containsKey("filter");
        if (filtered) queryParams.remove("filter");
        if (!queryParams.isEmpty()) {
            if (first) first = false;
            else sb.append(", ");
            addFormattedJoin(magicSortQueryParams(queryParams), "%2$s %1$s", ", ", sb);
        }
        if (filtered) {
            if (!first) sb.append(", ");
            sb.append("Iterable<NameValuePair> filters");
        }
        sb.append(") {\n");
        if (containerType != null) {
            sb.append(indent).append("\t\tString ref = resourceRef(");
            sb.append(methodDef.optString("verb")).append(", ");
            sb.append("\"").append(methodDef.optString("name")).append("\");\n");
        }
        boolean hasParams = false;
        if (!queryParams.isEmpty() || !pathParams.isEmpty()) {
            sb.append(indent).append("\t\tNameValuePair.Builder parameters = NameValuePair.many();\n");
            if (!pathParams.isEmpty()) {
                sb.append(indent);
                addFormattedJoin(pathParams, "\t\toptionalParam(parameters, \"%1$s\", %1$s);\n", indent, sb);
            }
            sb.append(indent);
            addFormattedJoin(magicSortQueryParams(queryParams), "\t\toptionalParam(parameters, \"%1$s\", %1$s);\n", indent, sb);
            if (filtered) {
                sb.append(indent).append("\t\toptionalParam(parameters, filters);\n");
            }
            hasParams = true;
        } else if (filtered) {
            sb.append(indent).append("\t\tIterable<NameValuePair> parameters = (filters == null) ? NameValuePair.EMPTY : filters;\n");
            hasParams = true;
        }
        sb.append(indent).append("\t\tHttpTransport.Request request = ");
        sb.append("buildRequest(").append(methodName).append("EndpointDef, ");
        if (containerType != null) {
            sb.append("ref, ");
        }
        sb.append(hasParams ? "parameters" : "null").append(", ");
        sb.append(hasBody ? "input" : selfUpdate ? "this" : "null").append(");\n");
        if (hasResponse) {
            sb.append(indent).append("\t\tHttpTransport.Response response = executeImpl(request);\n");
            if (responseType.endsWith("[]")) {
                sb.append(indent).append("\t\treturn response.getEntities(EntityTypeLibrary.ROOT.subLibrary(");
                addJavaFieldType(responseType.substring(0, responseType.length() - 2), false, sb);
            } else {
                sb.append(indent).append("\t\treturn response.getBody(EntityTypeLibrary.ROOT.subLibrary(");
                addJavaFieldType(responseType, false, sb);
            }
            sb.append(".class));\n");
        } else {
            sb.append(indent).append("\t\texecuteImpl(request);\n");
        }
        sb.append(indent).append("\t}\n\n");
    }

    private Collection<Map.Entry<String,String>> magicSortQueryParams(Map<String,String> queryParams) {
        List<Map.Entry<String,String>> result = new ArrayList<>(queryParams.entrySet());
        Collections.sort(result, MAGIC_QUERY_PARAM_ORDER);
        return result;
    }

    private Set<String> extractPathParams(JSONObject methodDef) {
        Set<String> result = new LinkedHashSet<>(4);
        Matcher pathParamMatcher = MATCH_PATH_PARAM.matcher(methodDef.optString("paramPath"));
        while (pathParamMatcher.find()) {
            result.add(pathParamMatcher.group(1));
        }
        return result;
    }

    private Map<String,String> extractQueryParams(JSONObject methodDef) {
        Map<String,String> result = new LinkedHashMap<>();
        JSONObject queryParams = methodDef.optJSONObject("queryParams");
        if (queryParams != null) {
            //noinspection unchecked
            for (String key : (Iterable<String>)queryParams.keySet()) {
                result.put(key, queryParams.optString(key));
            }
        }
        return result;
    }

    private void addFormattedJoin(Collection<?> items, String format, String delim, StringBuilder sb) {
        if (items == null) return;
        boolean first = true;
        for (Object item : items) {
            if (first) first = false;
            else sb.append(delim);
            if (item instanceof Map.Entry) {
                Map.Entry<?,?> entry = (Map.Entry) item;
                sb.append(String.format(format, entry.getKey(), entry.getValue()));
            } else {
                sb.append(String.format(format, item));
            }
        }
    }

    private void addParameterOverrides(JSONObject resourceLink, String indent, StringBuilder sb) {
        JSONObject parameterOverrides = resourceLink.optJSONObject("parameterOverrides");
        if (parameterOverrides == null || parameterOverrides.length() == 0) {
            sb.append(indent).append("\t\t\t                NameValuePair.EMPTY");
        } else {
            sb.append(indent).append("\t\t\t                NameValuePair.many()");
            boolean first = true;
            //noinspection unchecked
            for (String name : (Iterable<String>)parameterOverrides.keySet()) {
                if (first) first = false;
                else sb.append("\n").append(indent).append("\t\t\t                                    ");
                sb.append(".add(\"").append(name).append("\",\"").append(parameterOverrides.optString(name)).append("\")");
            }
        }
    }

    private void addJiveClientPackageAndImports(StringBuilder sb) {
        sb.append("package com.jivesoftware.v3client.framework;\n\n");

        sb.append("import com.jivesoftware.v3client.framework.credentials.Credentials;\n");
        sb.append("import com.jivesoftware.v3client.framework.entity.AbstractEntity;\n");
        sb.append("import com.jivesoftware.v3client.framework.entity.ContentEntity;\n");
        sb.append("import com.jivesoftware.v3client.framework.entity.PlaceEntity;\n");
        sb.append("import com.jivesoftware.v3client.framework.entity.Entities;\n");
        sb.append("import com.jivesoftware.v3client.framework.entities.*;\n");
        sb.append("import com.jivesoftware.v3client.framework.http.EndpointDef;\n");
        sb.append("import com.jivesoftware.v3client.framework.http.HttpTransport;\n");
        sb.append("import com.jivesoftware.v3client.framework.type.EntityType;\n");
        sb.append("import com.jivesoftware.v3client.framework.type.EntityTypeLibrary;\n");
        sb.append("\n");
        sb.append("import java.net.URI;\n");
        sb.append("import java.util.Collection;\n");
        sb.append("import java.util.Date;\n");
        sb.append("import org.json.JSONObject;\n");
        sb.append("\n");
        sb.append("import static com.jivesoftware.v3client.framework.http.HttpTransport.Method.*;\n\n");
    }

    private void addJiveClientClassDocs(StringBuilder sb) {
        sb.append("/**\n");
        sb.append(" */\n");
    }

    private void addJiveClientClassDefinition(StringBuilder sb) {
        sb.append("public class JiveClient extends AbstractJiveClient {\n\n");
    }

    private void addJiveClientConstructor(StringBuilder sb) {
        sb.append("\tpublic JiveClient (String jiveURL, Credentials creds, HttpTransport transport) {\n");
        sb.append("\t\tsuper(jiveURL, creds, transport);\n");
        sb.append("\t}\n\n");
    }

    private void addJiveClientStaticBegin(StringBuilder sb) {
        sb.append("\tstatic {\n\n");
        sb.append("\t\t// Populate the type library\n\n");
    }

    private void addJiveClientStaticEnd(StringBuilder sb) {
        sb.append("\t}\n\n");
    }

    private void addJiveClientStaticClass(StaticInnerClass staticClass, StringBuilder sb) {
        sb.append("\tpublic final ").append(staticClass.getClassName()).append(" ").append(staticClass.getFieldName());
        sb.append(" = new ").append(staticClass.getClassName()).append("();\n\n");
        sb.append("\tpublic class ").append(staticClass.getClassName()).append(" {\n\n");
        for (Map.Entry<String,JSONObject> entry : staticClass.getStaticMethodsByName().entrySet()) {
            if (shouldIgnoreStaticMethod(entry.getValue())) continue;
            addEnpointDef(entry.getKey(), entry.getValue(), "\t", false, sb);
            addMethod(null, entry.getKey(), entry.getValue(), "\t", sb);
        }
        sb.append("\t}\n");
    }

    private void addJiveClientClassEnd(StringBuilder sb) {
        sb.append("}\n");
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

    private boolean shouldIgnoreStaticMethod(JSONObject staticMethod) {
        if (staticMethod.getBoolean("unpublished") || !staticMethod.has("requestType") || !staticMethod.has("responseType")
                || "?".equals(staticMethod.optString("responseType")) || "?".equals(staticMethod.optString("requestType"))) {
            return true;
        } else {
            // Hack time. To avoid duplicated methods lets ignore these things. Or just because they do not compile for some reason
            String methodName = staticMethod.getString("name");
            return "osapi.jive.corev3.collaborations.participants".equals(methodName) || "osapi.jive.corev3.interactions.participants".equals(methodName);
        }
    }

    private void addJavaFieldType(String fieldType, boolean includeGenericType, StringBuilder sb) {
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
            sb.append("Iterable");
            if (includeGenericType) {
                sb.append("<");
                addJavaFieldType(fieldType.substring(0, fieldType.length() - 2), true, sb);
                sb.append(">");
            }
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
