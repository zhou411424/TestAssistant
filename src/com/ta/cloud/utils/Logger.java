package com.ta.cloud.utils;

import java.sql.Timestamp;

public class Logger {

	private static final boolean DEBUG = true;

	public static void i(Object o) {
		System.out.println("I: " + o.toString() + "("
				+ Thread.currentThread().getName() + ":" + getMethodName()
				+ ":" + getCurrentDatetime() + ")");

		// Set o = null, so that it can be collected by GC.
		o = null;
	}
	
	public static void e(Object o) {
		System.err.println("E: " + o.toString() + "("
				+ Thread.currentThread().getName() + ":" + getMethodName()
				+ ":" + getCurrentDatetime() + ")");

		// Set o = null, so that it can be collected by GC.
		o = null;
	}

	public static void e(Exception e) {
		System.err.println("E:\n" + getExceptionStack(e) + "("
				+ Thread.currentThread().getName() + ":" + getMethodName()
				+ ":" + getCurrentDatetime() + ")");
		e = null;
	}

	public static void d(Object o) {
		if (DEBUG) {
			System.out.println("D: " + o.toString() + "("
					+ Thread.currentThread().getName() + ":" + getMethodName()
					+ ":" + getCurrentDatetime() + ")");

			// Set o = null, so that it can be collected by GC.
			o = null;
		}
	}

	private static String getCurrentDatetime() {
		Timestamp stamp = new Timestamp(System.currentTimeMillis());
		return stamp.toString();
	}

	public static String getExceptionStack(Exception e) {
		StackTraceElement[] stes = e.getStackTrace();
		String returnVal = Thread.currentThread().getName() + ":" + "\n"
				+ e.toString() + "\n";
		for (StackTraceElement ste : stes) {
			returnVal += ste.toString() + "\n";
		}
		return returnVal;
	}

	public static String getMethodName() {
		StackTraceElement stack[] = (new Throwable()).getStackTrace();
		if (stack.length > 2) {
			return stack[2].getMethodName();
		}
		return "";
	}

}