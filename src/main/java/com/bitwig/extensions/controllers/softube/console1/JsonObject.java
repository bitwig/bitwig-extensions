package com.bitwig.extensions.controllers.softube.console1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class JsonObject {
    public static final Pattern DIGIT = Pattern.compile("^-?\\d+$");
    public static final Pattern FLOAT = Pattern.compile("^[-+]?[0-9]*\\.?[0-9]*$");
    private final Map<String, Object> data = new HashMap<>();
    
    public Object getValue(final String key) {
        return data.get(key);
    }
    
    public Optional<String> getStringValue(final String key) {
        if (data.get(key) instanceof final String stringValue) {
            return Optional.of(stringValue);
        }
        return Optional.empty();
    }
    
    public String getString(final String key) {
        if (data.get(key) instanceof final String stringValue) {
            return stringValue;
        }
        return null;
    }
    
    public boolean getBool(final String key) {
        if (data.get(key) instanceof final Boolean boolValue) {
            return boolValue;
        }
        return false;
    }
    
    public List<String> getStringList(final String key) {
        if (data.get(key) instanceof final List list) {
            return list;
        }
        return List.of();
    }
    
    public void set(final String key, final Object value) {
        if (value instanceof final String strValue) {
            if (strValue.startsWith("\"") && strValue.endsWith("\"")) {
                data.put(key, strValue.substring(1, strValue.length() - 1));
            } else if ("true".equals(strValue)) {
                data.put(key, Boolean.TRUE);
            } else if ("false".equals(strValue)) {
                data.put(key, Boolean.FALSE);
            } else if (DIGIT.matcher(strValue).matches()) { // strValue.matches("-?\\d+")
                data.put(key, Integer.parseInt(strValue));
            } else if (FLOAT.matcher(strValue).matches()) {
                data.put(key, Double.parseDouble(strValue));
            } else {
                data.put(key, strValue);
            }
        } else {
            data.put(key, value);
        }
    }
    
    public JsonObject getJsonObject(final String key) {
        final Object value = data.get(key);
        if (value instanceof final JsonObject jsonObject) {
            return jsonObject;
        }
        return null;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final var entry : data.entrySet()) {
            sb.append(entry.getKey() + " = <" + entry.getValue() + ">").append("\n");
        }
        return sb.toString();
    }
    
    public boolean contains(final String key) {
        return data.containsKey(key);
    }
    
    public Stream<Map.Entry<String, Object>> stream() {
        return data.entrySet().stream();
    }
}

