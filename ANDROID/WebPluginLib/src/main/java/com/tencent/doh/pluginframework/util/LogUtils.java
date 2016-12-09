package com.tencent.doh.pluginframework.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;


import android.content.Context;
import android.util.Log;
import com.tencent.doh.pluginframework.annotation.Public;

/**
 * Tencent.
 * Author: raezlu
 * Date: 13-7-8
 */
@Public
public class LogUtils {

    public static final int VERBOSE = Log.VERBOSE;
    
    public static final int DEBUG = Log.DEBUG;
    
    public static final int INFO = Log.INFO;
    
    public static final int WARN = Log.WARN;
    
    public static final int ERROR = Log.ERROR;

    public static  boolean mDebug = false;

    private static int LOG_LEVEL = ERROR;

    private final static String LOG_DIR_NAME = "log";
    
    private final static String LOG_FILE_NAME = "runtime.log";

    private final static String[] LOGCAT_COMMAND = new String[]{
        "logcat",
        "-d",
        "-v",
        "time"
    };

    public static void setDebug(boolean mDebug) {
        LogUtils.mDebug = mDebug;
        changeLogLevel();
    }

    //根据debug状态设置loglevel，debug为ture，大于debug的都输出，否则全都不输出
    private static void changeLogLevel() {
        if (mDebug)
            LOG_LEVEL = WARN;
        else
            LOG_LEVEL = ERROR + 1;

    }

    public interface LogProxy {

        void v(String tag, String msg);

        void d(String tag, String msg);

        void i(String tag, String msg);

        void w(String tag, String msg);

        void e(String tag, String msg);

        void flush();
    }

    private final static LogProxy DEFAULT_PROXY = new LogProxy() {
        @Override
        public void v(String tag, String msg) {
            if (Log.VERBOSE >= LOG_LEVEL)
                Log.v(tag, String.valueOf(msg));
        }

        @Override
        public void d(String tag, String msg) {
            if (Log.DEBUG >= LOG_LEVEL)
                Log.d(tag, String.valueOf(msg));
        }

        @Override
        public void i(String tag, String msg) {
            if (Log.INFO >= LOG_LEVEL)
                Log.i(tag, String.valueOf(msg));
        }

        @Override
        public void w(String tag, String msg) {
            if (Log.WARN >= LOG_LEVEL)
                Log.w(tag, String.valueOf(msg));
        }

        @Override
        public void e(String tag, String msg) {
            if (Log.ERROR >= LOG_LEVEL)
                Log.e(tag, String.valueOf(msg));
        }

        @Override
        public void flush() {
            // empty.
        }
    };

    private static volatile LogProxy sProxy = DEFAULT_PROXY;

    @Public
    public static void v(String tag, String msg) {
        LogProxy proxy = getProxy();
        proxy.v(tag, msg);
    }

    @Public
    public static void v(String tag, String msg, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.v(tag, msg + '\n' + getStackTraceString(tr));
    }

    @Public
    public static void d(String tag, String msg) {
        LogProxy proxy = getProxy();
        proxy.d(tag, msg);
    }

    @Public
    public static void d(String tag, String msg, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.d(tag, msg + '\n' + getStackTraceString(tr));
    }

    @Public
    public static void i(String tag, String msg) {
        LogProxy proxy = getProxy();
        proxy.i(tag, msg);
    }

    @Public
    public static void i(String tag, String msg, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.i(tag, msg + '\n' + getStackTraceString(tr));
    }

    @Public
    public static void w(String tag, String msg) {
        LogProxy proxy = getProxy();
        proxy.w(tag, msg);
    }

    @Public
    public static void w(String tag, String msg, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.w(tag, msg + '\n' + getStackTraceString(tr));
    }

    @Public
    public static void w(String tag, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.w(tag, getStackTraceString(tr));
    }

    @Public
    public static void e(String tag, String msg) {
        LogProxy proxy = getProxy();
        proxy.e(tag, msg);
    }

    @Public
    public static void e(String tag, String msg, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.e(tag, msg + '\n' + getStackTraceString(tr));
    }

    @Public
    public static void flush() {
        LogProxy proxy = getProxy();
        proxy.flush();
    }

    public static void setProxy(LogProxy proxy) {
        synchronized (LogUtils.class) {
            sProxy = proxy;
        }
    }

    private static LogProxy getProxy() {
        LogProxy proxy = sProxy;
        return proxy != null ? proxy : DEFAULT_PROXY;
    }

    private static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    @Public
    public static void saveCurrentLogcat(Context context) {
        LogWriter logWriter = null;
        try {
            File logFile = getLogFile(context);
            if (logFile == null || !logFile.exists()) {
                return;
            }
            logWriter = new LogFileWriter(logFile);
            saveLogcat(logWriter);
        } catch (Throwable t) {
        } finally {
            if (logWriter != null) {
                try {
                    logWriter.flush();
                    logWriter.close();
                } catch (IOException e) {
                    // empty.
                }
            }
        }
    }

    private static void  saveLogcat(LogWriter logWriter) {

        final StringBuilder log = new StringBuilder();

        Process process = null;
        BufferedReader reader = null;
        int count = 0;
        try {
            process = Runtime.getRuntime().exec(LOGCAT_COMMAND);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                logWriter.write(line);
                ++ count;
            }

        } catch (Throwable e) {
            e.printStackTrace();
            // empty.
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // empty.
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
        try {
            if(count == 0) {
                ProcessBuilder pb = new ProcessBuilder(LOGCAT_COMMAND);
                pb.redirectErrorStream(true);
                if (process != null) {
                    process.destroy();
                }
                process = pb.start();
                process.getOutputStream().close();
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    logWriter.write(line);
                    ++ count;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            // empty.
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // empty.
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

    }

    public static File getLogFile(Context context) {
        String path = StorageUtils.getExternalCacheDirExt(context, LOG_DIR_NAME, true);
        if (path == null) {
            return null;
        }
        File file = new File(path + File.separator + LOG_FILE_NAME);
        File dir = file.getParentFile();
        if (dir == null)
            return null;
        if ( !dir.exists()) {
            dir.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
            }
        }
        return file;
    }

    private interface LogWriter {

        void close() throws IOException;

        void flush() throws IOException;

        void write(String str) throws IOException;
    }

    private static class LogFileWriter implements LogWriter {

        private final Writer mWriter;

        public LogFileWriter(File file) throws IOException {
            mWriter = new BufferedWriter(new FileWriter(file));
        }

        @Override
        public void close() throws IOException {
            mWriter.close();
        }

        @Override
        public void flush() throws IOException {
            LogUtils.flush();
            mWriter.flush();
        }

        @Override
        public void write(String str) throws IOException {
            mWriter.write(str);
            mWriter.flush();
        }
    }

}
