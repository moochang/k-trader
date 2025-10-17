package com.example.k_trader_eth.bitthumb.lib;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import android.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by 김무창 on 2017-12-17.
 */

public class Util {
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final String HMAC_SHA512 = "HmacSHA512";

    public static String base64Encode(byte[] bytes) {
        String bytesEncoded = Base64.encodeToString(bytes, Base64.DEFAULT);
        return bytesEncoded;
    }

    public static String hashToString(String data, byte[] key) {
        String result = null;
        Mac sha512_HMAC;

        try {
            sha512_HMAC = Mac.getInstance("HmacSHA512");
            System.out.println("key : " + new String(key));
            SecretKeySpec secretkey = new SecretKeySpec(key, "HmacSHA512");
            sha512_HMAC.init(secretkey);

            byte[] mac_data = sha512_HMAC.doFinal(data.getBytes());
            System.out.println("hex : " + bin2hex(mac_data));
            result = Base64.encodeToString(mac_data, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] hmacSha512(String value, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(DEFAULT_ENCODING), HMAC_SHA512);
            Mac mac = Mac.getInstance(HMAC_SHA512);
            mac.init(keySpec);
            return mac.doFinal(value.getBytes(DEFAULT_ENCODING));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String asHex(byte[] bytes) {
        return new String(Base64.encodeToString(bytes, Base64.DEFAULT));
    }

    public static String bin2hex(byte[] data) {
        return String.format("%0" + (data.length * 2) + "X", new BigInteger(1, data));
    }

    public static String mapToQueryString(Map<String, String> map) {
        StringBuilder string = new StringBuilder();

        if (map.size() > 0) {
            string.append("?");
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
            string.append(entry.getKey());
            string.append("=");
            string.append(entry.getValue());
            string.append("&");
        }

        return string.toString();
    }
}
