package com.cun.kaptcha.utils;

import java.util.HashMap;

public class R extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public static R error(String msg) {
        R r = new R();
        r.put("code", 400);
        r.put("msg", msg);
        return r;
    }

    public static R error(Integer code, String message) {
        R r = new R();
        r.put("code", code);
        r.put("msg", message);
        return r;
    }

    public static R ok(String msg, Object data) {
        R r = new R();
        r.put("msg", msg);
        r.put("code", 200);
        r.put("data", data);
        return r;
    }
}
