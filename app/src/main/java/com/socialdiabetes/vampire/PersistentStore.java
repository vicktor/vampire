package com.socialdiabetes.vampire;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;

/**
 * Created by jamorham on 23/09/2016.
 * <p>
 * This is for internal data which is never backed up,
 * separate file means it doesn't clutter prefs
 * we can afford to lose it, it is for internal states
 * and is alternative to static variables which get
 * flushed when classes are destroyed by garbage collection
 * <p>
 * It is suitable for cache type variables where losing
 * state will cause problems. Obviously it will be slower than
 * pure in-memory state variables.
 */


public class PersistentStore {

    private static final String DATA_STORE_INTERNAL = "persist_internal_store";
    private static SharedPreferences prefs;
    private static final boolean d = false; // debug flag

    public static String getString(final String name) {
        return prefs.getString(name, "");
    }

    public static String getString(final String name, String defaultValue) {
        return prefs.getString(name, defaultValue);
    }

    public static int getStringToInt(final String name, final int defaultValue) {
        try {
            return Integer.parseInt(getString(name, Integer.toString(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean removeItem(final String pref) {
        if (prefs != null) {
            prefs.edit().remove(pref).apply();
            return true;
        }
        return false;
    }


    public static void setString(final String name, String value) {
        prefs.edit().putString(name, value).apply();
    }

    // if string is different to what we have stored then update and return true
    public static boolean updateStringIfDifferent(final String name, final String current) {
        if (current == null) return false; // can't handle nulls
        if (PersistentStore.getString(name).equals(current)) return false;
        PersistentStore.setString(name, current);
        return true;
    }

    public static void appendString(String name, String value) {
        setString(name, getString(name) + value);
    }

    public static void appendString(String name, String value, String delimiter) {
        String current = getString(name);
        if (current.length() > 0) current += delimiter;
        setString(name, current + value);
    }

    public static void appendBytes(String name, byte[] value) {
      //  setBytes(name, Bytes.concat(getBytes(name), value));
    }

    public static byte[] getBytes(String name) {
        return base64decodeBytes(getString(name));
    }

    public static byte[] base64decodeBytes(String input) {
        try {
            return Base64.decode(input.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            Log.e("TAG", "Got unsupported encoding: " + e);
            return new byte[0];
        }
    }

    public static String base64decode(String input) {
        try {
            return new String(Base64.decode(input.getBytes("UTF-8"), Base64.NO_WRAP), "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            Log.e("", "Got unsupported encoding: " + e);
            return "decode-error";
        }
    }

    public static byte getByte(Context mContext, String name) {
        return (byte)getLong(mContext, name);
    }

    public static void setBytes(String name, byte[] value) {
        setString(name, base64encodeBytes(value));
    }

    public static String base64encodeBytes(byte[] input) {
        try {
            return new String(Base64.encode(input, Base64.NO_WRAP), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("TAG", "Got unsupported encoding: " + e);
            return "encode-error";
        }
    }
    public static void setByte(Context mContext, String name, byte value) {
        setLong(mContext, name, value);
    }

    public static long getLong(Context mContext, String name) {
        prefs = mContext.getSharedPreferences(DATA_STORE_INTERNAL, Context.MODE_PRIVATE);
        return prefs.getLong(name, 0);
    }

    public static float getFloat(Context mContext, String name) {
        prefs = mContext.getSharedPreferences(DATA_STORE_INTERNAL, Context.MODE_PRIVATE);
        return prefs.getFloat(name, 0);
    }

    public static void setLong(Context mContext, String name, long value) {
        prefs = mContext.getSharedPreferences(DATA_STORE_INTERNAL, Context.MODE_PRIVATE);
        prefs.edit().putLong(name, value).apply();
    }

    public static void setFloat(Context mContext, String name, float value) {
        prefs = mContext.getSharedPreferences(DATA_STORE_INTERNAL, Context.MODE_PRIVATE);
        prefs.edit().putFloat(name, value).apply();
    }

    public static void setDouble(Context mContext, String name, double value) {
        setLong(mContext, name, Double.doubleToRawLongBits(value));
    }

    public static double getDouble(Context mContext, String name) {
        return Double.longBitsToDouble(getLong(mContext, name));
    }

    public static boolean getBoolean(String name) {
        return prefs.getBoolean(name, false);
    }

    public static boolean getBoolean(String name, boolean value) {
        return prefs.getBoolean(name, value);
    }

    public static void setBoolean(String name, boolean value) {
        prefs.edit().putBoolean(name, value).apply();
    }

    public static long incrementLong(Context mContext, String name) {
        final long val = getLong(mContext, name) + 1;
        setLong(mContext, name, val);
        return val;
    }

    public static void setLongZeroIfSet(Context mContext, String name) {
        if (getLong(mContext, name) > 0) setLong(mContext, name, 0);
    }

    @SuppressLint("ApplySharedPref")
    public static void commit() {
        prefs.edit().commit();
    }
}