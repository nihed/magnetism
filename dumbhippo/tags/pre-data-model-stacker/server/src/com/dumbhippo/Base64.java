package com.dumbhippo;

import java.io.UnsupportedEncodingException;

/*********************************************************************
 * Base64 - a simple base64 encoder and decoder.
 *
 *     Copyright (c) 1999, Bob Withers - bwit@pobox.com
 *
 * This code may be freely used for any purpose, either personal
 * or commercial, provided the authors copyright notice remains
 * intact.
 *********************************************************************/
public class Base64 {

    /**
     * Encodes a String as a base64 String.
     *
     * @param data a String to encode.
     * @return a base64 encoded String.
     */
    public static String encode(String data) {
        byte[] bytes = null;
        try {
            bytes = data.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
        return encode(bytes);
    }

    /**
     * Encodes a byte array into a base64 String.
     *
     * @param data a byte array to encode.
     * @return a base64 encode String.
     */
    public static String encode(byte[] data) {
        int c;
        int len = data.length;
        StringBuilder ret = new StringBuilder(((len / 3) + 1) * 4);
        for (int i = 0; i < len; ++i) {
            c = (data[i] >> 2) & 0x3f;
            ret.append(cvt.charAt(c));
            c = (data[i] << 4) & 0x3f;
            if (++i < len)
                c |= (data[i] >> 4) & 0x0f;

            ret.append(cvt.charAt(c));
            if (i < len) {
                c = (data[i] << 2) & 0x3f;
                if (++i < len)
                    c |= (data[i] >> 6) & 0x03;

                ret.append(cvt.charAt(c));
            }
            else {
                ++i;
                ret.append((char)fillchar);
            }

            if (i < len) {
                c = data[i] & 0x3f;
                ret.append(cvt.charAt(c));
            }
            else {
                ret.append((char)fillchar);
            }
        }
        return ret.toString();
    }

    /**
     * Decodes a base64 String.
     *
     * @param data a base64 encoded String to decode.
     * @return the decoded String.
     */
    public static String decode(String data) {
        byte[] bytes = null;
        try {
            bytes = data.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
        return decode(bytes);
    }

    /**
     * Decodes a base64 aray of bytes.
     *
     * @param data a base64 encode byte array to decode.
     * @return the decoded String.
     */
    public static String decode(byte[] data) {
        int c, c1;
        int len = data.length;
        StringBuilder ret = new StringBuilder((len * 3) / 4);
        for (int i = 0; i < len; ++i) {
            c = cvt.indexOf(data[i]);
            ++i;
            c1 = cvt.indexOf(data[i]);
            c = ((c << 2) | ((c1 >> 4) & 0x3));
            ret.append((char)c);
            if (++i < len) {
                c = data[i];
                if (fillchar == c)
                    break;

                c = cvt.indexOf(c);
                c1 = ((c1 << 4) & 0xf0) | ((c >> 2) & 0xf);
                ret.append((char)c1);
            }

            if (++i < len) {
                c1 = data[i];
                if (fillchar == c1)
                    break;

                c1 = cvt.indexOf(c1);
                c = ((c << 6) & 0xc0) | c1;
                ret.append((char)c);
            }
        }
        return ret.toString();
    }

    private static final int fillchar = '=';
    private static final String cvt = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz"
            + "0123456789+/";

}
