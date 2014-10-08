package com.testing.titans.utils;

public class TitansLog {

    private static final boolean DEBUG = true;

    public static void log(String tag, String msg) {
        if (DEBUG) {
            System.out.println(tag + " " + msg);
        }
    }

}
