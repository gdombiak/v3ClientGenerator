package com.jivesoftware.v3.generator;

import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by ed.venaglia on 2/28/14.
 */
public class StaticInnerClass {

    private final String className;
    private final String fieldName;
    private final Map<String,JSONObject> staticMethodsByName = new TreeMap<>();

    public StaticInnerClass(String name) {
        this.fieldName = name;
        this.className = name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public String getClassName() {
        return className;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Map<String,JSONObject> getStaticMethodsByName() {
        return staticMethodsByName;
    }
}
