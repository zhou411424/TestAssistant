package com.ta.cloud.utils;

import com.android.ddmlib.AndroidDebugBridge;

public class BatchInstallThread extends Thread {

	private static final String TAG = "BatchInstallThread";

	@Override
	public void run() {
		super.run();
		// 判断adb是否可用
//		AndroidDebugBridge mAdb = TAUtils.getAdb();
		TAUtils.batchInstallTestApk();
	}

}
