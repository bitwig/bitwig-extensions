package com.bitwig.extensions.controllers.softube.console1;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class JsonParser {
    
    private final String input;
    private int pos = 0;
    
    private Token token = Token.NONE;
    private JsonObject current;
    private List<Object> currentArray;
    private StringBuilder key;
    private StringBuilder value;
    private final Stack<JsonObject> stack = new Stack<>();
    private final Stack<String> keyStack = new Stack<>();
    
    private record Extract(List<String> elements, int nextPos) {
    
    }
    
    public static void main(final String[] args) {
        final String test = """
            {"abc":"deff",  "nested":{ "other":"x", "nxcc": true }  "it":  true}
            """;
        
        final String test2 = """
            {"deckId":"A","metadata":{"title":"Contradictions","artist":"Native Instruments","album":"Decoded Forms (Expansion)","cover":"124/2PK3GTD2OXK3KC0M1GHFAUJH22ZA","duration":"03:40","bpm":162,"key":"10A","bitrate":320000},"player":{"isPlaying":false,"isCueing":false,"isLooping":false,"loopSize":"4","loopActive":false,"slip":false,"isSlipping":false,"sync":false,"syncInPhase":false,"syncInRange":true,"tempoRange":"8%","tempo":0,"bpm":162,"keyAdjust":0,"resultingKey":"10A","keySync":false}}
            """;
        
        final String arrayTest = """
                {
                 "activeMeters": [
                  "fea0147c-9173-4593-ba5c-890e3cef778d",
                  "25dbf170-2656-4b7b-9813-d7e5f5cab189",
                  "c791ac5b-22c8-46e5-a96b-b3dc1bb5175c",
                  "8d2d6a88-0b9a-4b03-b39f-a2b16c986958",
                  "30150793-cbc5-4d86-871c-ac0cfdf04ed0",
                  "e0abde81-1345-479c-ab5e-381e29a4dcc9"
                 ],
                 "test": "funnx"
                }
            """;
        
        final JsonParser parser = new JsonParser(arrayTest);
        final JsonObject json = parser.parse();
        
        //System.out.println("DECKID = " + json.getValue("deckId"));
        final JsonObject meta = json.getJsonObject("activeMeters");
        System.out.printf("c=%s  v=%s\n", json.contains("activeMeters"), json.getStringList("activeMeters"));
        System.out.printf("c=%s  v=%s\n", json.contains("test"), json.getString("test"));
        if (meta != null) {
            System.out.println("   KEY = " + meta.getValue("key"));
        }
    }
    
    private enum Token {
        NONE,
        OBJECT,
        START_KEY,
        EXPECT_VALUE,
        VALUE
    }
    
    public JsonParser(final String input) {
        this.input = input;
    }
    
    public JsonObject parse() {
        final JsonObject root = new JsonObject();
        stack.push(root);
        current = root;
        while (pos < input.length()) {
            final char c = input.charAt(pos);
            switch (token) {
                case NONE -> handleNone(c);
                case OBJECT -> handleObject(c);
                case START_KEY -> handleInKey(c);
                case EXPECT_VALUE -> handleForValue(c);
                case VALUE -> readValue(c);
            }
            //System.out.println(" <%c> %s".formatted(c,token));
            pos++;
        }
        return root;
    }
    
    private void readValue(final char c) {
        if (c == ',') {
            current.set(key.toString().trim(), value.toString().trim());
            token = Token.OBJECT;
        } else if (c == '}') {
            current.set(key.toString().trim(), value.toString().trim());
            if (!keyStack.isEmpty()) {
                current.set(key.toString().trim(), value.toString().trim());
                final String keyPrev = keyStack.pop();
                final JsonObject parent = stack.pop();
                parent.set(keyPrev, current);
                current = parent;
            }
            token = Token.OBJECT;
        } else if (c == '{') {
            keyStack.push(key.toString().trim());
            stack.push(current);
            current = new JsonObject();
            token = Token.OBJECT;
        } else if (c == '[') {
            final Extract extract = extractArrayContent();
            final List<Object> list = extract.elements().stream().map(this::readElement).toList();
            current.set(key.toString().trim(), list);
            pos = extract.nextPos;
            token = Token.OBJECT;
        } else {
            value.append(c);
        }
    }
    
    private Object readElement(final String strValue) {
        if (strValue.startsWith("\"") && strValue.endsWith("\"")) {
            return strValue.substring(1, strValue.length() - 1);
        } else if ("true".equals(strValue)) {
            return Boolean.TRUE;
        } else if ("false".equals(strValue)) {
            return Boolean.FALSE;
        } else if (JsonObject.DIGIT.matcher(strValue).matches()) { // strValue.matches("-?\\d+")
            return Integer.parseInt(strValue);
        } else if (JsonObject.FLOAT.matcher(strValue).matches()) {
            return Double.parseDouble(strValue);
        } else if (strValue.startsWith("{")) {
            final JsonParser parser = new JsonParser(strValue);
            return parser.parse();
        }
        return strValue;
    }
    
    private Extract extractArrayContent() {
        int posNow = pos;
        int bracketCount = 0;
        final boolean endFound = false;
        final StringBuilder content = new StringBuilder();
        while (posNow < input.length() && !endFound) {
            final char c = input.charAt(posNow);
            if (c == '[') {
                bracketCount++;
            } else if (c == ']') {
                bracketCount--;
                if (bracketCount == 0) {
                    return new Extract(extractArrayElements(content.toString().trim()), posNow);
                }
            } else {
                content.append(c);
            }
            posNow++;
        }
        return new Extract(extractArrayElements(content.toString().trim()), posNow);
    }
    
    private List<String> extractArrayElements(final String arrayString) {
        final List<String> elements = new ArrayList<>();
        int pos = 0;
        int objBracketCount = 0;
        int arrayBracketCount = 0;
        StringBuilder current = new StringBuilder();
        while (pos < arrayString.length()) {
            final char c = arrayString.charAt(pos);
            if (c == '{') {
                objBracketCount++;
                current.append(c);
            } else if (c == '[') {
                arrayBracketCount++;
                current.append(c);
            } else if (c == ']') {
                arrayBracketCount--;
                current.append(c);
            } else if (c == '}') {
                objBracketCount--;
                current.append(c);
            } else if (c == ',') {
                if (objBracketCount > 0 || arrayBracketCount > 0) {
                    current.append(c);
                } else {
                    elements.add(current.toString().trim());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
            pos++;
        }
        final String lastElement = current.toString().trim();
        if (lastElement.length() > 0) {
            elements.add(lastElement);
        }
        return elements;
    }
    
    private void handleForValue(final char c) {
        if (c == ':') {
            token = Token.VALUE;
            value = new StringBuilder();
        }
    }
    
    private void handleInKey(final char c) {
        if (c == '\"') {
            token = Token.EXPECT_VALUE;
        } else {
            key.append(c);
        }
    }
    
    private void handleObject(final char c) {
        if (c == '\"') {
            token = Token.START_KEY;
            key = new StringBuilder();
        }
    }
    
    private void handleNone(final char c) {
        if (c == '{') {
            token = Token.OBJECT;
        }
    }
    
}
