package io.media.sdk;

/**
 * Created by sunhui on 2017/9/5.
 */


import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class xRTCDevice {
    public static final int DEVICE_INFO_UNKNOWN = -1;
    private static final String TAG = "xRTCDevice";
    private static final int READER_BUF_SIZE = 256 ;

    public static String getDeviceId() {
        String deviceId = Build.MANUFACTURER + "/" + Build.MODEL + "/" + Build.PRODUCT + "/" + Build.DEVICE + "/" + Build.VERSION.SDK_INT + "/" + System.getProperty("os.version");
        if (deviceId != null) {
            deviceId = deviceId.toLowerCase();
        }
        return deviceId;
    }

    private static final String[] H264_HW_BLACKLIST = {"SAMSUNG-SGH-I337", "Nexus 7", "Nexus 4", "P6-C00", "HM 2A", "XT105", "XT109", "XT1060"};

    public static int getRecommendedEncoderType() {
        List<String> exceptionModels = Arrays.asList(H264_HW_BLACKLIST);
        if (exceptionModels.contains(Build.MODEL)) {
            Log.w(TAG, "Model: " + Build.MODEL + " has black listed H.264 encoder.");
            return 1;
        }
        if (Build.VERSION.SDK_INT <= 18) {
            return 1;
        }
        return 0;
    }

    public static int getNumberOfCPUCores() {
        if (Build.VERSION.SDK_INT <= 10) {
            return 1;
        }
        int cores;
        try {
            cores = getCoresFromFileInfo("/sys/devices/system/cpu/possible");
            if (cores == -1) {
                cores = getCoresFromFileInfo("/sys/devices/system/cpu/present");
            }
            if (cores == -1) {
                cores = getCoresFromCPUFileList();
            }
        } catch (SecurityException e) {
            cores = -1;
        } catch (NullPointerException e) {
            cores = -1;
        }
        return cores ;
    }

    public static String getCpuName() {
        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text = br.readLine();
            String[] array = text.split(":\\s+", 2);
            for (int i = 0; i < array.length; i++) {
            }
            return array[1];
        } catch (FileNotFoundException e) {
            Log.e(TAG, "getCpuName failed, no /proc/cpuinfo found in system", e);
        } catch (IOException e) {
            Log.e(TAG, "getCpuName failed,", e);
        }
        return null;
    }

    public static String getCpuABI() {
        return Build.CPU_ABI;
    }

    private static int getCoresFromFileInfo(String fileLocation) {
        try {
            InputStream is = new FileInputStream(fileLocation);
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String fileContents = buf.readLine();
            return getCoresFromFileString(fileContents);
        } catch (IOException e) {
        }
        return -1;
    }

    private static int getCoresFromFileString(String str) {
        if ((str == null) || (!str.matches("0-[\\d]+$"))) {
            return -1;
        }
        int cores = Integer.valueOf(str.substring(2)).intValue() + 1;
        return cores;
    }

    private static int getCoresFromCPUFileList() {
        return new File("/sys/devices/system/cpu").listFiles(CPU_FILTER).length;
    }

    private static final FileFilter CPU_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            String path = pathname.getName();
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (!Character.isDigit(path.charAt(i))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    public static int getCPUMaxFreqKHz() {
        int maxFreq = -1;
        try {
            for (int i = 0; i < getNumberOfCPUCores(); i++) {
                String filename = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq";

                File cpuInfoMaxFreqFile = new File(filename);
                if (cpuInfoMaxFreqFile.exists()) {
                    byte[] buffer = new byte[READER_BUF_SIZE];
                    FileInputStream stream = new FileInputStream(cpuInfoMaxFreqFile);
                    try {
                        stream.read(buffer);
                        int endIndex = 0;
                        while ((Character.isDigit(buffer[endIndex])) && (endIndex < buffer.length)) {
                            endIndex++;
                        }
                        String str = new String(buffer, 0, endIndex);
                        Integer freqBound = Integer.valueOf(Integer.parseInt(str));
                        if (freqBound.intValue() > maxFreq) {
                            maxFreq = freqBound.intValue();
                        }
                    } catch (NumberFormatException localNumberFormatException) {
                    } finally {
                        stream.close();
                    }
                }
            }
            if (maxFreq == -1) {
                FileInputStream stream = new FileInputStream("/proc/cpuinfo");
                try {
                    int freqBound = parseFileForValue("cpu MHz", stream);
                    freqBound *= 1000;
                    if (freqBound > maxFreq) {
                        maxFreq = freqBound;
                    }
                } finally {
                    stream.close();
                }
            }
        } catch (IOException e) {
            maxFreq = -1;
        }
        return maxFreq;
    }

    private static int parseFileForValue(String textToMatch, FileInputStream stream) {
        byte[] buffer = new byte[READER_BUF_SIZE];
        try {
            int length = stream.read(buffer);
            for (int i = 0; i < length; i++) {
                if ((buffer[i] == 10) || (i == 0)) {
                    if (buffer[i] == 10) {
                        i++;
                    }
                    for (int j = i; j < length; j++) {
                        int textIndex = j - i;
                        if (buffer[j] != textToMatch.charAt(textIndex)) {
                            break;
                        }
                        if (textIndex == textToMatch.length() - 1) {
                            return extractValue(buffer, j);
                        }
                    }
                }
            }
        } catch (IOException localIOException) {
        } catch (NumberFormatException localNumberFormatException) {
        }
        return -1;
    }

    private static int extractValue(byte[] buffer, int index) {
        while ((index < buffer.length) && (buffer[index] != 10)) {
            if (Character.isDigit(buffer[index])) {
                int start = index;
                index++;
                while ((index < buffer.length) && (Character.isDigit(buffer[index]))) {
                    index++;
                }
                String str = new String(buffer, 0, start, index - start);
                return Integer.parseInt(str);
            }
            index++;
        }
        return -1;
    }
}

