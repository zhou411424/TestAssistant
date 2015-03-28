package com.ta.cloud;

import com.ta.cloud.utils.BatchInstallThread;

public class Start {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		runInstallApkTask();
	}
	
	private static void runInstallApkTask() {
		BatchInstallThread batchInstallThread = new BatchInstallThread();
		batchInstallThread.start();
	}

}
