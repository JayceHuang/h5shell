package com.tencent.doh.plugins.cloud;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Shibinhuang on 2016/8/1.
 * 用于生成腾讯云的秘钥
 */
public class CloudUtils {


    /**
     * 获取签名
     */
    public static String getSign(String bucket) {
        Random random = new Random();
        long expired = System.currentTimeMillis() / 1000 + CloudManager.SIGN_EXPIRED_SECOND;
        long current = System.currentTimeMillis() / 1000;
        int r = random.nextInt();
        String s1 = String.format("a=%s&b=%s&k=%s&e=%s&t=%s&r=%s&f=", CloudManager.APP_ID, bucket, CloudManager.SECRET_ID, expired, current, r);
        String s2 = hmacSha1(s1, CloudManager.SECRET_KEY);
        String s3 = s2 + s1;
        String s4 = null;
        try {
            s4 = Base64.encodeToString(s3.getBytes("ISO8859-1"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return s4;
    }

    /**
     * 生成HmacSHA1
     * @param data
     * @param key
     * @return
     */
    public static String hmacSha1(String data, String key) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("ISO8859-1"), "HmacSHA1"); // NOTE: 必须是ISO8859-1获取原始的byte数据
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(data.getBytes("ISO8859-1"));
            return new String(bytes, "ISO8859-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String md5(String str) {
        try {
            byte[] bytes = str.getBytes();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            BigInteger bigInt = new BigInteger(1, md5.digest());
            return bigInt.toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
