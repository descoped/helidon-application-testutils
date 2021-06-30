package io.descoped.helidon.application.test.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTraceUtils {

    public static String printStackTrace() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        int skip = 2;
        for (StackTraceElement ste : st) {
            if (skip > 0) {
                skip--;
                continue;
            }
            pw.println("    " + ste.toString());
        }
        return sw.toString();
    }
}
