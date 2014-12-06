package com.senz.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import com.senz.utils.L;

/**
 * Created by woodie on 14/12/4.
 */
public class DeviceInfo {
    private Context context = null;
    // RAM info
    private String deviceMemory    = "unknown";
    // sdCard info
    private long   sdCardSize      = 0;
    private long   sdCardLeft      = 0;
    // CPU info
    private String CPUmodel        = "unknown";
    private String CPUfreq         = "unknown";
    // System version info
    private String kernelVersion   = "unknown";
    private String firmwareVersion = "unknown";
    private String sysModel        = "unknown";
    private String sysVersion      = "unknown";
    // WIFI info
    private String WIFImac         = "unknown";

    public DeviceInfo(Context ctx)
    {
        L.i("=== DEVICE INFO ===");
        context = ctx;
        getTotalMemory();
        getSDCardMemory();
        getCPUInfo();
        getVersion();
        getWifiMac();
    }

    // Used to get the size of device's memory
    private void getTotalMemory() {
        String str1 = "/proc/meminfo";
        String memory="";
        try {
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
            /*while ((memory = localBufferedReader.readLine()) != null) {
                deviceMemory = memory;
                L.i("Get device total memory : " + memory);
            }*/
            if((memory = localBufferedReader.readLine()) != null) {
                deviceMemory = memory;
                L.i("- MEMORY INFO -");
                L.i("device " + memory);
            }
        } catch (IOException e) {
            L.e("Get device memory error");
        }
    }

    // Used to get the size of sdCard memory
    private void getSDCardMemory() {
        //long[] sdCardInfo=new long[2];
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(sdcardDir.getPath());
            long bSize = sf.getBlockSize();
            long bCount = sf.getBlockCount();
            long availBlocks = sf.getAvailableBlocks();
            sdCardSize = bSize * bCount;//总大小
            sdCardLeft = bSize * availBlocks;//可用大小
            L.i("- SDCARD INFO -");
            L.i("sdCard totol memory : " + sdCardSize + " & sdCard left memory : " + sdCardLeft);
        }
        else{
            L.e("Get sdCard memory error");
        }
    }

    // Used to get cpu infomation.
    private void getCPUInfo() {
        String str1 = "/proc/cpuinfo";
        String str2="";
        String[] cpuInfo={"",""};
        String[] arrayOfString;
        try {
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
            str2 = localBufferedReader.readLine();
            arrayOfString = str2.split("\\s+");
            for (int i = 2; i < arrayOfString.length; i++) {
                cpuInfo[0] = cpuInfo[0] + arrayOfString[i] + " ";
            }
            str2 = localBufferedReader.readLine();
            arrayOfString = str2.split("\\s+");
            cpuInfo[1] += arrayOfString[2];
            localBufferedReader.close();
            CPUmodel = cpuInfo[0];
            CPUfreq  = cpuInfo[1];
            L.i("- CPU INFO -");
            L.i("CPU model : " + CPUmodel + " & CPU frequency : " + CPUfreq);
        } catch (IOException e) {
            L.e("Get device cpu info error");
        }
    }

    // Used to get system's version
    public void getVersion(){
        String str1 = "/proc/version";
        String str2;
        String[] arrayOfString;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(
                    localFileReader, 8192);
            str2 = localBufferedReader.readLine();
            arrayOfString = str2.split("\\s+");
            kernelVersion=arrayOfString[2];//KernelVersion
            localBufferedReader.close();
            L.i("- SYSTEM INFO -");
        } catch (IOException e) {
            L.e("Get device system's kernel version error");
        }
        firmwareVersion = Build.VERSION.RELEASE;// firmware version
        sysModel = Build.MODEL;//model
        sysVersion = Build.DISPLAY;//system version
        L.i("Kernel Version : " + kernelVersion + " & Firmware Version : " + firmwareVersion +
            " & Model : " + sysModel + " & System Version : " + sysVersion);
    }

    // Used to get wifi mac address
    public void getWifiMac(){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if(wifiInfo.getMacAddress()!=null){
            WIFImac=wifiInfo.getMacAddress();
            L.i("- WIFI INFO -");
            L.i("WIFI mac : " + WIFImac);
        }
        else {
            L.i("Get WIFI mac error");
        }
    }

    public String getDeviceMemory() {
        return deviceMemory;
    }

    public long getSdCardSize() {
        return sdCardSize;
    }

    public long getSdCardLeft() {
        return sdCardLeft;
    }

    public String getCPUmodel() {
        return CPUmodel;
    }

    public String getCPUfreq() {
        return CPUfreq;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public String getSysModel() {
        return sysModel;
    }

    public String getSysVersion() {
        return sysVersion;
    }

    public String getWIFImac() {
        return WIFImac;
    }
}
