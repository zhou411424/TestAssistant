package com.ta.cloud.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.android.ddmlib.AndroidDebugBridge;

public class TAUtils {

	private static final String TAG = "TAUtils";
	private static AndroidDebugBridge mAdb = null;
	private static final int OS_TYPE_UNKNOWN = -1;
	private static final int OS_TYPE_WINDOWS = 0;
	private static final int OS_TYPE_LINUX = 1;
	private static final int OS_TYPE_MACOSX = 2;
	
	/**
	 * 判断当前系统是Windows/Unix
	 * @return
	 */
	public static int getOSType() {
		String osName = System.getProperty("os.name").toLowerCase();
		Logger.d("osName="+osName);
		if(osName.indexOf("win") >= 0) {
			return OS_TYPE_WINDOWS;
		} else if (osName.indexOf("linux") >= 0) {
			return OS_TYPE_LINUX;
		} else if (osName.indexOf("mac") >= 0) {
			return OS_TYPE_MACOSX;
		}
		return OS_TYPE_UNKNOWN;
	}

	/**
	 * 获取当前系统中Android环境的根目录
	 * @return
	 */
	public static String getAndroidHome() {
		String androidHome = "";
		int osType = getOSType();
		switch (osType) {
		case OS_TYPE_WINDOWS:
			String result = "";
			try {
				androidHome = System.getenv("ANDROID_HOME");
				Logger.d("androidHome="+androidHome);
				if (androidHome == null || androidHome.equals("")) {
					result = System.getenv("PATH");
					if (result.length() > 0
							&& result.indexOf("platform-tools") >= 0) {
						result = result.substring(0,
								result.indexOf("platform-tools"));
						result = result.substring(
								result.lastIndexOf(";") + 1,
								result.length() - 1);
					}
				}
				androidHome = result.replace("\\", "/");
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case OS_TYPE_LINUX:
			androidHome = "/home/zhoulc/work/tools/android-sdks";
			break;
		case OS_TYPE_MACOSX:
			androidHome = "/Users/zhouliancheng/work/tools/android-sdk-macosx";
			break;
		case OS_TYPE_UNKNOWN:
			androidHome = "unknown os type, can't get android home.";
			break;
		}
		return androidHome;
	}

	/**
	 * 获取adb在当前系统中的位置
	 * @return
	 */
	public static String getAdbLocation() {
		String adbLocation = "";
		String androidHome = getAndroidHome(); 
		Logger.d("android_home="+androidHome);
		int osType = getOSType();
		switch (osType) {
		case OS_TYPE_WINDOWS:
			adbLocation = androidHome + File.separator + "platform-tools"
					+ File.separator + "adb.exe";
			break;
		case OS_TYPE_LINUX:
		case OS_TYPE_MACOSX:
			adbLocation = androidHome + File.separator + "platform-tools"
					+ File.separator + "adb";
			if(adbLocation.equals("")) {
				adbLocation = "assets/macosx_adb/adb";
			}
			break;
		case OS_TYPE_UNKNOWN:
			adbLocation = "unknown os type, can't get adb location.";
			break;
		}
		return adbLocation;
	}

	/**
	 * 获取PC端的调试桥
	 * @return
	 */
	public static AndroidDebugBridge getAdb() {
		Logger.i(TAG+"==>getAdb...");
		String adbLocation = getAdbLocation();
		if(mAdb != null) {
			return mAdb;
		}

		try {
			AndroidDebugBridge.init(false);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}

		try {
			mAdb = AndroidDebugBridge.createBridge(adbLocation, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mAdb;
	}

	// get current connected devices by usb
	public static ArrayList<String> getConnectedDevices() {
		Logger.i(TAG+"==>getConnectedDevices");
		ArrayList<String> devices = new ArrayList<String>();
		String result = executeCommand(getAdbLocation()+" devices");
		Logger.d("result="+result);
		result = result.replaceAll("List of devices attached \n", "");
		if (result.equals("\n")) {
			Logger.i("There is no devcie connected to this agent.");
			return devices;
		}
		
		String[] lineTemp = result.split("\n");
		for (int i = 0; i < lineTemp.length; i++) {
			if (lineTemp[i].contains("\tdevice")) {
				String device = lineTemp[i].substring(0,
						lineTemp[i].indexOf("\tdevice"));
				if (checkDeviceStatus(device)){
					devices.add(device);
				}
			}
		}
		return devices;
	}
	
	/**
	 * 检查设备当前状态
	 * @param deviceSN
	 * @return
	 */
	public static boolean checkDeviceStatus(String deviceSN) {
		return isRecognizableDevice(deviceSN) && checkCmdExecution(deviceSN) && isConnectedDevice(deviceSN);
	}
	
	/**
	 * 查看设备是否为可识别设备，一般如果包含??????则为不可识别设备。
	 * @param deviceSN
	 * @return
	 */
	private static boolean isRecognizableDevice(String deviceSN) {
		if (deviceSN.contains("????")) {
			return false;
		}
		return true;
	}

	/**
	 * 检查adb命令是否执行异常
	 * @param deviceSN
	 * @return
	 */
	public static boolean checkCmdExecution(String deviceSN) {
		// Check if device can execute command normally.
		String result = "";
		String cmd = getAdbLocation() +" -s " + deviceSN + " shell cd /";

		try {
			result = executeCmd(cmd, 5);
			if (result.contains("timeout") || result.toLowerCase().trim().startsWith("error")) {
				return false;
			}
		} catch (Exception e) {
			Logger.e(e);
			return false;
		}
		return true;
	}
	
	/**
	 * 设备是否处于连接状态
	 * @param deviceSN
	 * @return
	 */
	public static boolean isConnectedDevice(String deviceSN) {
		String result = "";
		try {
			result = executeCmd(getAdbLocation()+" devices", 5);
		} catch (Exception e) {
			Logger.e(e);
			e.printStackTrace();
			return false;
		}
		
		if (result.indexOf(deviceSN + "\tdevice") >= 0) {		
			Logger.i("Device " + deviceSN + " is connected.");
			return true;
		}
		return false;
	}
	
	/**
	 * 处理内存不足现象
	 * @param result
	 * @param isHandleLowMemory
	 */
	public static void handleLowMemory(boolean isHandleLowMemory) {
		Logger.i(TAG+"==>handleLowMemory...isHandleLowMemory="+isHandleLowMemory);
		if(isHandleLowMemory) {
			// do nothing
		}
	}
	
	/**
	 * 安装apk到设备上
	 * @param apkPath
	 * @param deviceSN
	 * @return
	 */
	public static boolean installApk(String apkPath, String deviceSN, boolean isHandleLowMemory) {
		Logger.i(TAG+"==>installApk...getAdbLocation="+getAdbLocation());
		String cmd = getAdbLocation()+" -s " + deviceSN + " install " + apkPath;
		String result = null;
		try {
			result = executeCommand(cmd);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		// 如果apk已经安装了，需要重新安装，路径pkg: /data/local/tmp/xxx.apk
		if (result.contains("[INSTALL_FAILED_ALREADY_EXISTS]") && Preferences.isReInstall){
			Logger.i(TAG+"==>installApk...apk already exists, needs to reinstall.");
			return reinstallApk(apkPath, deviceSN,false);
		}
		
		//处理内存不足现象
		if (result.indexOf("INSTALL_FAILED_INSUFFICIENT_STORAGE") >= 0) {
			handleLowMemory(isHandleLowMemory);
		}
		
		if(result.contains("Success")) {
			return true;
		}
		return false;
	}

	/**
	 * 重新安装apk到设备上
	 * @param apkPath
	 * @param deviceSN
	 * @param isHandleLowMemory
	 * @return
	 */
	public static boolean reinstallApk(String apkPath, String deviceSN, boolean isHandleLowMemory) {
		Logger.i(TAG+"==>reinstallApk...");
		String cmd = getAdbLocation()+" -s " + deviceSN + " install -r " + apkPath;
		String result = null;
		try {
			result = executeCommand(cmd);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		//处理内存不足现象
		if (result.indexOf("INSTALL_FAILED_INSUFFICIENT_STORAGE") >= 0) {
			handleLowMemory(isHandleLowMemory);
		}
		
		if(result.contains("Success")) {
			return true;
		}
		return false;
	}
	
	/**
	 * 批量安装apk到各个设备上
	 */
	public static void batchInstallTestApk() {
		Logger.i(TAG + "==>batchInstallTestApk");
		final ArrayList<String> devices = getConnectedDevices();
		String apkPath = "assets/PingCe.apk";
		
		for (int i = 0; i < devices.size(); i++) {
			String deviceSN = devices.get(i);
			boolean isSuccess = installApk(apkPath, deviceSN, false);
			Logger.d("Install PingCe.apk to device, deviceSN: " + deviceSN
					+" is " +(isSuccess ? "success." : "failed."));
		}
	}
	
	/**
	 * 执行命令时的超时时间为300秒
	 * @param cmd
	 * @return
	 */
	public static String executeCommand(String cmd) {
		int timeout = 300;
		String result = executeCmd(cmd, timeout);
		if(result.equals("timeout")) {
			Logger.i("Try to execute command again. cmd: " + cmd);
			result = executeCmd(cmd, timeout);
		}
		return result;
	}
	
	public static String executeCmd(String cmd, int timeout) {
		Process exec = null;
		try {
			exec = Runtime.getRuntime().exec(cmd);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
		
		// Start a process killer to kill the process after timeout.
		ProcessKillThread pt = new ProcessKillThread(exec, timeout);
		pt.start();
		
		InputStream inputStream = exec.getInputStream();
		InputStream errorStream = exec.getErrorStream();

		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		InputStreamReader errorStreamReader = new InputStreamReader(errorStream);

		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		BufferedReader errBufferedReader = new BufferedReader(errorStreamReader);

		StringBuilder sb = new StringBuilder(200000);
		StringBuilder sbErr = new StringBuilder(200000);

		try {
			String tmp;
			while ((tmp = bufferedReader.readLine()) != null) {
				sb.append(tmp + "\n");
				// Reset the timeout.
				pt.timeout = timeout;
			}

		} catch (IOException e) {
			Logger.e(e);
			e.printStackTrace();
			// make sure all streams are closed, and variants are handled.
			try {
				inputStream.close();
				errorStream.close();
				inputStreamReader.close();
				errorStreamReader.close();
				bufferedReader.close();
				errBufferedReader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			sb = null;
			sbErr = null;
			pt = null;
			return "timeout";
		}

		try {
			String tmpErr;
			while ((tmpErr = errBufferedReader.readLine()) != null) {
				sbErr.append(tmpErr + "\n");
				// Reset the timeout.
				pt.timeout = timeout;
			}
		} catch (IOException e) {
			Logger.e(e);
			e.printStackTrace();
			// make sure all streams are closed, and variants are handled.
			try {
				inputStream.close();
				errorStream.close();
				inputStreamReader.close();
				errorStreamReader.close();
				bufferedReader.close();
				errBufferedReader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			sb = null;
			sbErr = null;
			pt = null;
			return "timeout";
		}	finally {
			destroyProcess(exec);
		}

		String returnVal = sb.toString();

		// make sure all streams are closed, and all variants are set to null.
		try {
			inputStream.close();
			errorStream.close();
			inputStreamReader.close();
			errorStreamReader.close();
			bufferedReader.close();
			errBufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sb = null;
		sbErr = null;
		pt.timeout = 0;
		pt = null;

		
		return returnVal;
	}
	
	/**
	 * Fix too many open files Error.
	 * @param process
	 */
	public static void destroyProcess(Process process){
		if (process != null){
			try{
				process.destroy();
			}catch(Exception e){
				Logger.e(e);
			}			
		}
	}
	
	/*
	 * 当前线程休眠ms毫秒
	 */
	public static void sleep(int ms){
		try {
			Logger.d("Thread " + Thread.currentThread().getName() + " sleep " + ms + " ms");			
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Logger.e("Thread " + Thread.currentThread().getName() + " is interrupted, unable to sleep " + ms + " ms");
		}
	}

	public static void sleep(long ms){
		try {
			Logger.d("Thread " + Thread.currentThread().getName() + " sleep " + ms + " ms");			
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Logger.e("Thread " + Thread.currentThread().getName() + " is interrupted, unable to sleep " + ms + " ms");			
		}
	}
	
}
