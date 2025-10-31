package com.example.pondmatev1;

import java.util.HashMap;
import java.util.Map;

public class MapUtils {
    public static Map<String, String> single(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
