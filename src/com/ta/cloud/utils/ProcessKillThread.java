package com.ta.cloud.utils;

public class ProcessKillThread extends Thread {

	private static final String TAG = "ProcessKillThread";
	Process process = null;
	int timeout = 0;

	public ProcessKillThread(Process p, int timeout) {
		this.setName("ProcessKillThread");
		this.process = p;
		this.timeout = timeout;
	}

	@Override
	public void run() {
		super.run();
		while (this.timeout > 0) {
			TAUtils.sleep(1000);
			this.timeout--;
		}
		try {
			if (this.process != null) {
				Logger.d(TAG+"==>kill process");
				this.process.destroy();
			}

		} catch (Exception e) {
			Logger.i("exec process haved destroy,this exception may not care it");
		}
	}
}
