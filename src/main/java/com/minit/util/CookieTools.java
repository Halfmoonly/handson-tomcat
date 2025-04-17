package com.minit.util;

import javax.servlet.http.Cookie;

public class CookieTools {
    public static String getCookieHeaderName(Cookie cookie) {
        return "Set-Cookie";
    }

    public static void getCookieHeaderValue(Cookie cookie, StringBuffer buf) {
        String name = cookie.getName();
        if (name == null)
            name = "";
        String value = cookie.getValue();
        if (value == null)
            value = "";
        buf.append(name);
        buf.append("=");
        buf.append(value);
    }

    static void maybeQuote (int version, StringBuffer buf,String value){
        if (version == 0 || isToken (value))
            buf.append (value);
        else {
            buf.append ('"');
            buf.append (value);
            buf.append ('"');
        }
    }

    private static final String tspecials = "()<>@,;:\\\"/[]?={} \t";

    private static boolean isToken (String value) {
        int len = value.length ();
        for (int i = 0; i < len; i++) {
            char c = value.charAt (i);

            if (c < 0x20 || c >= 0x7f || tspecials.indexOf (c) != -1)
                return false;
        }
        return true;
    }
}
