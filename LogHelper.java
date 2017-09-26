package com.xiaomi.oga.utils;


import android.Manifest;
import android.content.Context;
import android.os.Environment;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogHelper {

    private static final int WRITE_SYSTEM_LOG_LEVEL = LogOptions.LEVEL_VERBOSE;
    private static final int WRITE_FILE_LEVEL = LogOptions.LEVEL_DEBUG;

    private static final String TAG = "Oga";
    public static final int USED_STACK_DEPTH = 6;

    public static final String LOG_FILE_NAME = "logs.txt";
    public static final String LOGS_DIR = "logs";

    // Method Log.x(String, String), key is log level
    private static SparseArray<Method> mMethodArrays = new SparseArray<Method>();

    // Method Log.x(String, String, Throwable), key is log level
    private static SparseArray<Method> mMethodArraysEx = new SparseArray<Method>();

    // helps to build up mMethodArrays and mMethodArraysEx.
    // Key is log level, value is Log method names.
    private static SparseArray<String> mMethodNames = new SparseArray<String>();

    private static volatile boolean mEnableStackTrace = true;

    private static final LogExternalStorage STORAGE = new LogExternalStorage();

    private enum PermissionState {
        Unknown,
        Denied,
        Permitted
    }

    private static volatile PermissionState mPermissionState = PermissionState.Unknown;

    private static FlushRunnable sFlushScheduler = new FlushRunnable();

    static {
        // build up method arrays.
        mMethodNames.put(LogOptions.LEVEL_VERBOSE, "v");
        mMethodNames.put(LogOptions.LEVEL_DEBUG, "d");
        mMethodNames.put(LogOptions.LEVEL_INFO, "i");
        mMethodNames.put(LogOptions.LEVEL_WARN, "w");
        mMethodNames.put(LogOptions.LEVEL_ERROR, "e");

        for (int i = 0; i < mMethodNames.size(); i++) {
            int level = mMethodNames.keyAt(i);
            String method = mMethodNames.valueAt(i);

            mMethodArrays.put(level, getMethod(method));
            mMethodArraysEx.put(level, getMethodEx(method));
        }
    }

    private LogHelper() {}

    public static void enableStackTrace() {
        mEnableStackTrace = true;
    }

    public static void disableStackTrace() {
        mEnableStackTrace = false;
    }

    public static void setPermissionState(PermissionState permissionState) {
        LogHelper.mPermissionState = permissionState;
    }

    private static Method getMethod(String methodName) {
        Method method = null;
        try {
            method = Log.class.getMethod(methodName, String.class, String.class);
        } catch (NoSuchMethodException e) {
            Log.e(sOptions.uniformTag, "getMethod failed", e);
        }
        return method;
    }

    private static Method getMethodEx(String methodName) {
        Method method = null;
        try {
            method = Log.class.getMethod(methodName, String.class, String.class, Throwable.class);
        } catch (NoSuchMethodException e) {
            Log.e(sOptions.uniformTag, "getMethodEx failed", e);
        }
        return method;
    }

    /**
     * Log options.
     */
    public static class LogOptions {
        public static final int LEVEL_VERBOSE = 1;
        public static final int LEVEL_DEBUG = 2;
        public static final int LEVEL_INFO = 3;
        public static final int LEVEL_WARN = 4;
        public static final int LEVEL_ERROR = 5;

        public static final int LEVEL_NEVER_WRITE_FILE = 100;

        /**
         * Uniform tag to be used as log tag; null-ok, if this is null, will use
         * the tag argument in log methods.
         */
        public String uniformTag = TAG;

        /**
         * When it is null, all stack traces will be output. Usually this can be
         * set the application package name.
         */
        public String stackTraceFilterKeyword;

        /**
         * The level at which the log method really works(output to DDMS).
         *
         * MUST be one of the LEVEL_* constants.
         */
        public int logLevel = WRITE_SYSTEM_LOG_LEVEL;

        /**
         * The level at which log will be output to file.
         * MUST be one of the LEVEL_* constants.
         */
        public int writeFileLevel = WRITE_FILE_LEVEL;

        public boolean performanceEnabled = true;

        public String logDir;

        public boolean hidePrivacyInfo = false;
    }

    public static class LogOutputPaths {
        /**
         * The log directory, under which log files are put.
         */
        public String dir;

        /**
         * Current log file absolute file path. NOTE it may be empty.
         *
         */
        public String currentLogFile;

        /**
         * Latest back up file path. null if there is none such file.
         */
        public String latestBackupFile;
    }

    private static LogOptions sOptions = new LogOptions();

    /**
     * Mapping from performance data ID to start time.
     * key is performance data ID, value is start time in millis. */
    private static ConcurrentHashMap<Long, Long> sPerformanceData
            = new ConcurrentHashMap<Long, Long>();

    private static final AtomicLong idCounter = new AtomicLong(0);

    public static void initializeByDefaultOptions(Context context) {
        if (!PermissionUtils.isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return;
        }
        try {
            LogHelper.LogOptions options = new LogHelper.LogOptions();
            options.logDir = FileUtils.createOgaSubDir(LOGS_DIR);
            options.uniformTag = TAG;
            initialize(options);
        } catch (Exception e) {
            Log.e(TAG, "failed to initialize LogHelper", e);
        }
    }

    /**
     *
     * Initialize log.
     * @param options
     *            null-ok. Options for log methods.
     */
    public static void initialize(LogOptions options) {
        setOptions(options);
        STORAGE.setLogPath(options.logDir);

        // log limit size : 40M for alpha version, about 6M for dev version(default).
        if (options.writeFileLevel <= LogOptions.LEVEL_DEBUG) {
            STORAGE.setBackupLogLimitInMB(40);
        }
    }

    /**
     * Make sure initialize is called before calling this.
     */
    public static void setUniformTag(String tag) {
        if (!TextUtils.isEmpty(tag)) {
            sOptions.uniformTag = tag;
        }
    }

    public static LogOptions getOptions() {
        return sOptions;
    }

    /**
     * Get log output paths.
     * @param out  Output destination.
     * @return True for success, false otherwise.
     */
    public static boolean getLogOutputPaths(LogOutputPaths out) {
        return STORAGE.getLogOutputPaths(out, LOG_FILE_NAME);
    }

    private static void setOptions(LogOptions options) {
        sOptions = options == null ? new LogOptions() : options;
    }

    /**
     * Output verbose log. Exception will be caught if input arguments have
     * format error.

     *
     * @param format
     *            The format string such as "This is the %d sample : %s".
     * @param args
     *            The args for format.
     *
     *            Reference : boolean : %b. byte, short, int, long, Integer, Long
     *            : %d. NOTE %x for hex. String : %s. Object : %s, for this
     *            occasion, toString of the object will be called, and the
     *            object can be null - no exception for this occasion.
     *
     */
    public static void v(Object obj, String format, Object... args) {
        outputLazily(obj, LogOptions.LEVEL_VERBOSE, format, args);
        printStackIfLastArgIsThrowable(obj, args);
    }

    /**
     * Output debug log. This version aims to improve performance by removing
     * the string concatenated costs on release version. Exception will be
     * caught if input arguments have format error.
     *
     *
     *
     * @param format
     *            The format string such as "This is the %d sample : %s".
     * @param args
     *            The args for format.
     *
     *            Reference : boolean : %b. byte, short, int, long, Integer, Long
     *            : %d. NOTE %x for hex. String : %s. Object : %s, for this
     *            occasion, toString of the object will be called, and the
     *            object can be null - no exception for this occasion.
     *
     */
    public static void d(Object obj, String format, Object... args) {
        outputLazily(obj, LogOptions.LEVEL_DEBUG, format, args);
        printStackIfLastArgIsThrowable(obj, args);
    }

    /**
     * Output information log. Exception will be caught if input arguments have
     * format error.

     *
     * @param format
     *            The format string such as "This is the %d sample : %s".
     * @param args
     *            The args for format.
     *
     *            Reference : boolean : %b. byte, short, int, long, Integer, Long
     *            : %d. NOTE %x for hex. String : %s. Object : %s, for this
     *            occasion, toString of the object will be called, and the
     *            object can be null - no exception for this occasion.
     *
     */
    public static void i(Object obj, String format, Object... args) {
        outputLazily(obj, LogOptions.LEVEL_INFO, format, args);
        printStackIfLastArgIsThrowable(obj, args);
    }

    public static void i(Object obj, String format, Object arg0) {
        outputLazily(obj, LogOptions.LEVEL_INFO, format, arg0);
    }

    /**
     * Output warning log. Exception will be caught if input arguments have
     * format error.
     *
     *
     * @param format
     *            The format string such as "This is the %d sample : %s".
     * @param args
     *            The args for format.
     *
     *            Reference : boolean : %b. byte, short, int, long, Integer, Long
     *            : %d. NOTE %x for hex. String : %s. Object : %s, for this
     *            occasion, toString of the object will be called, and the
     *            object can be null - no exception for this occasion.
     *
     */
    public static void w(Object obj, String format, Object... args) {
        outputLazily(obj, LogOptions.LEVEL_WARN, format, args);
        printStackIfLastArgIsThrowable(obj, args);
    }

    /**
     * Output error log. Exception will be caught if input arguments have format
     * error.
     *
     *
     * @param format
     *            The format string such as "This is the %d sample : %s".
     * @param args
     *            The args for format.
     *
     *            Reference : boolean : %b. byte, short, int, long, Integer, Long
     *            : %d. NOTE %x for hex. String : %s. Object : %s, for this
     *            occasion, toString of the object will be called, and the
     *            object can be null - no exception for this occasion.
     *
     */
    public static void e(Object obj, String format, Object... args) {
        outputLazily(obj, LogOptions.LEVEL_ERROR, format, args);
        printStackIfLastArgIsThrowable(obj, args);
    }

    private static void printStackIfLastArgIsThrowable(Object obj, Object[] args) {
        if (ContainerUtil.hasData(args) && args[args.length - 1] instanceof Throwable) {
            e(obj, (Throwable) (args[args.length - 1]));
        }
    }

    /**
     * Output an error log with contents of a Throwable.
     *
     * @param t
     *            An Throwable instance.
     */
    public static void e(Object obj, Throwable t) {
        outputLazily(obj, LogOptions.LEVEL_ERROR, t, null);
    }

    /**
     * Flush the written logs. The log methods write logs to a buffer.
     *
     * NOTE this will be called if close is called.
     */
    public static void flush() {
        Runnable command = new Runnable() {
            @Override
            public void run() {
                STORAGE.flush();
            }
        };
        executeCommand(command);
    }

    /**
     * Close the logging task. Flush will be called here. Failed to call this
     * may cause some logs lost.
     */
    public static void close() {
        Runnable command = new Runnable() {
            @Override
            public void run() {
                if (externalStorageExist()) {
                    STORAGE.close();
                }
            }
        };

        executeCommand(command);
    }

    private static void executeCommand(final Runnable command) {
        SoftTaskSingleThreadExecutor.getInstance().post(command);
    }

    private static String msgForTextLog(String className, String filename, int line,
                                        String msg, String threadName) {
        return msg + "(P:" + Process.myPid() + ")" + "(T:" + threadName + ")" + "(C:" + className + ")" + "at (" + filename + ":" + line + ")";
    }

    private static StackTraceElement getLogStackTraceElement() {
        try {
            return Thread.currentThread().getStackTrace()[USED_STACK_DEPTH];
        } catch (OutOfMemoryError e) {
            Log.e(sOptions.uniformTag, "getLogStackTraceElement FAILED, %s", e);
            return null;
        }
    }

    private static String getThreadStacksKeyword() {
        return sOptions.stackTraceFilterKeyword;
    }

    public static void printThreadStacks() {
        printThreadStacks(tagOfStack(), getThreadStacksKeyword(), false, false);
    }

    public static void printThreadStacks(String tag) {
        printThreadStacks(tag, getThreadStacksKeyword(),
                TextUtils.isEmpty(getThreadStacksKeyword()), false);
    }

    public static void printThreadStacks(Throwable e, String tag) {
        printStackTraces(e.getStackTrace(), tag);
    }

    public static void printThreadStacks(String tag, String keyword) {
        printThreadStacks(tag, keyword, false, false);
    }

    // tag is for output identifier.
    // keyword is for filtering irrelevant logs.
    public static void printThreadStacks(String tag, String keyword,
                                         boolean fullLog, boolean release) {
        printStackTraces(Thread.currentThread().getStackTrace(), tag, keyword,
                fullLog, release);
    }

    public static void printStackTraces(StackTraceElement[] traces, String tag) {
        printStackTraces(traces, tag, getThreadStacksKeyword(),
                TextUtils.isEmpty(sOptions.stackTraceFilterKeyword), false);
    }

    private static void printStackTraces(StackTraceElement[] traces,
                                         String tag, String keyword, boolean fullLog, boolean release) {
        printLog(tag, "------------------------------------", release);
        for (StackTraceElement e : traces) {
            String info = e.toString();
            if (fullLog
                    || (!TextUtils.isEmpty(keyword) && info.contains(keyword))) {
                printLog(tag, info, release);
            }
        }
        printLog(tag, "------------------------------------", release);
    }

    private static void printLog(String tag, String log, boolean release) {
        if (release) {
            i(tag, log);
        } else {
            d(tag, log);
        }
    }

    public static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    public static String stackTrace() {
        try {
            throw new RuntimeException();
        } catch (Exception e) {
            return stackTraceOf(e);
        }
    }

    private static String tag(String backupTag) {
        final LogOptions options = sOptions;
        return options.uniformTag == null ? backupTag
                : options.uniformTag;
    }

    private static String tagOfStack() {
        return sOptions.uniformTag == null ? "CallStack" : sOptions.uniformTag;
    }

    private static boolean shouldWriteSystemLog(int level) {
        return level >= sOptions.logLevel;
    }

    private static boolean shouldWriteToES(int level) {
        return level >= sOptions.writeFileLevel;
    }

    private static boolean externalStorageExist() {
        return Environment.getExternalStorageState().equalsIgnoreCase(
                Environment.MEDIA_MOUNTED);
    }

    private static void outputLazily(final Object obj,
                                     final int level,
                                     final Throwable throwable,
                                     final String format, final Object... args) {
        if (shouldWriteSystemLog(level) || shouldWriteToES(level)) {
            writeToLog(CommonUtils.getNameOf(obj), level, throwable, format, args);
        }
    }

    private static void outputLazily(final Object obj,
                                     final int level,
                                     final String format, final Object... args) {
        if (shouldWriteSystemLog(level) || shouldWriteToES(level)) {
            writeToLog(CommonUtils.getNameOf(obj), level, null, format, args);
        }
    }

    private static void writeToLog(
            final String className,
            final int level,
            final Throwable throwable,
            final String format, final Object... args) {
        final long timeMillis = System.currentTimeMillis();
        final StackTraceElement stackElem = mEnableStackTrace || !CommonUtils.isInMainThread() ? getLogStackTraceElement() : null;
        final String threadName = Thread.currentThread().getName();

        final Runnable command = new Runnable() {
            @Override
            public void run() {
                String logText = createLogText(className, stackElem, format, args, threadName);
                String text = logText;
                if (throwable != null) {
                    StringWriter sw = new StringWriter();
                    sw.write(logText);
                    sw.write("\n");
                    throwable.printStackTrace(new PrintWriter(sw));
                    text = sw.toString();
                }


                if (shouldWriteSystemLog(level)) {
                    Method method = throwable == null ? mMethodArrays.get(level) : mMethodArraysEx.get(level);
                    try {
                        if (throwable == null) {
                            method.invoke(null, tag(className), text);
                        } else {
                            method.invoke(null, tag(className), logText, throwable);
                        }
                    } catch (Exception e) {
                        Log.e(sOptions.uniformTag, "writeToLog failed", e);
                    }
                }

                if (mPermissionState != PermissionState.Denied && shouldWriteToES(level) && externalStorageExist()) {
                    try {
                        STORAGE.writeLogToFile(STORAGE.getLogPath(),
                                LOG_FILE_NAME, text, false, timeMillis);
                    } catch (Exception e) {
                        Log.e(sOptions.uniformTag, "writeToLog fail", e);
                    }
                }
            }

        };
        executeCommand(command);

        sFlushScheduler.scheduleFlush();
    }

    private static class FlushRunnable implements Runnable {
        private static final long FLUSH_DELAY = TimeUnit.SECONDS.toSeconds(5);
        private AtomicBoolean mIsScheduled = new AtomicBoolean(false);
        private Runnable flushTask = new Runnable() {
            @Override
            public void run() {
                STORAGE.flush();
            }
        };

        public void scheduleFlush() {
            if (mIsScheduled.get()) {
                return;
            }
            mIsScheduled.set(true);
            ThreadPoolManager.getSoftTaskSingleThreadExecutor().postDelay(sFlushScheduler, FLUSH_DELAY);
        }

        @Override
        public void run() {
            SoftTaskSingleThreadExecutor.getInstance().post(flushTask);
            mIsScheduled.set(false);
        }
    }

    private static String createLogText(String className, StackTraceElement stackElem, String format, Object[] args, String threadName) {
        // for performance, sometimes we just disable get stack trace.
        String filename = stackElem == null ? "" : stackElem.getFileName();
        int line = stackElem == null ? -1 : stackElem.getLineNumber();
        String msg = "";

        if (ContainerUtil.hasData(format)) {
            try {
                msg = ContainerUtil.isEmpty(args) ? format : String.format(format, args);
            } catch (Exception e) {
                Log.e(sOptions.uniformTag, "createLogText failed " + format, e);
            }
        }

        String logText = msgForTextLog(className, filename, line, msg, threadName);
        return sOptions.hidePrivacyInfo ? hidePrivacyInfo(logText) : logText;
    }

    private static final String FIND_PRIVACY = "(\"?(userid|userId|name|userName|phoneNum|userPhoneNum|imei|xiaomiid)\"?\\s*(=|:)\\s*[\"']?[\\d]+[\"']?)" +
            "|((\\[|,)\\s*\"?[\\d]{6,}\"?)";
    private static final Pattern PRIVACY_P = Pattern.compile(FIND_PRIVACY);

    private static String hidePrivacyInfo(String logText) {
        Matcher m = PRIVACY_P.matcher(logText);
        if (m.find()) {
            return m.replaceAll("userIdOrPhoneNum");
        }
        return logText;
    }

    /**
     *
     * Begin performance timing.
     * Use must be matched with endTiming.
     * @return unique ID to be passed as endTiming arg.
     */
    public static long beginTiming() {
        return beginTiming("");
    }

    /**
     * Begin performance timing.
     * Use must be matched with endTiming.
     * @param format    Format for message.
     * @param args      Args for message.
     * @return unique ID to be passed as endTiming arg.
     */
    public static long beginTiming(String format, Object... args) {
        if (!sOptions.performanceEnabled) {
            return -1;
        }

        String msg = "";
        try {
            msg = (ContainerUtil.isEmpty(format) || ContainerUtil.isEmpty(args)) ? format : String.format(format, args);
        } catch (Exception e) {
            Log.e(sOptions.uniformTag, "beginTiming failed, " + e);
        }

        if (ContainerUtil.hasData(msg)) {
            d(sOptions.uniformTag, " performance(%s) starts", msg);
        }

        long id = getPerfId();
        long start = SystemClock.elapsedRealtime();
        sPerformanceData.put(id, start);
        return id;
    }

    private static long getPerfId() {
        return idCounter.getAndIncrement();
    }

    /**
     * End performance timing.
     * Use must be matched with beginTiming.
     * @param perfId    Must be the ID returned by beginTiming.
     * @param format    Format for message.
     * @param args      Args for message.
     */
    public static void endTiming(long perfId, String format, Object... args) {
        endTiming(perfId, false, format, args);
    }

    /**
     * End performance timing.
     * Use must be matched with beginTiming.
     * @param perfId        Must be the ID returned by beginTiming.
     * @param keepTimeData  Whether keep timing data, if true, you can call endTiming with
     *                      the same perfId later without call startTiming again.
     *                      NOTE you must call endTiming with this arg as false for the last
     *                      time with the same perfId, otherwise there will be unintentional
     *                      object reference, i.e. : memory leak.
     * @param format        Format for message.
     * @param args          Args for message.
     */
    public static void endTiming(long perfId, boolean keepTimeData, String format, Object... args) {
        if (!sOptions.performanceEnabled) {
            return;
        }

        if (perfId < 0) {
            return;
        }

        long end = SystemClock.elapsedRealtime();
        Long start = sPerformanceData.get(perfId);
        if (start == null) {
            return;
        }

        String msg = "";
        try {
            msg = (ContainerUtil.isEmpty(format) || ContainerUtil.isEmpty(args)) ? format : String.format(format, args);
        } catch (Exception e) {
            Log.e(sOptions.uniformTag, "endTiming failed, " + e);
        }
        d(sOptions.uniformTag, "performance(%s) starts at %s, end at %s, costs %s millis",
                msg, start, end, end - start);

        if (!keepTimeData) {
            sPerformanceData.remove(perfId);
        }
    }

    /**
     * Get log output paths.
     * @return null if not ready.
     */
    public static LogOutputPaths getLogOutputPaths() {
        LogOutputPaths ret = new LogOutputPaths();
        if (!getLogOutputPaths(ret)) {
            e(sOptions.uniformTag, "failed to get log output paths.");
        }
        return ret;
    }
    public static class MapFormatter {
        private final Map<?, ?> mMap;

        public static MapFormatter obtain(Map<?, ?> map) {
            return new MapFormatter(map);
        }

        private MapFormatter(Map<?, ?> map) {
            mMap = map;
        }

        @Override
        public String toString() {
            if (mMap == null) {
                return "";
            }

            String result = "";
            for (Map.Entry e : mMap.entrySet()) {
                result += e.getKey();
                result += "=";
                result += e.getValue();
                result += " ";
            }
            return result;
        }
    }

    public static class ListFormatter<T> {
        private final List<T> mList;
        private final String mDelimiter;

        public static <T> ListFormatter obtain(List<T> list, String delimiter) {
            return new ListFormatter<T>(list, delimiter);
        }

        public static <T> ListFormatter obtain(List<T> list) {
            return new ListFormatter<T>(list, ",");
        }

        private ListFormatter(List<T> list, String delimiter) {
            // will visit mList in other threads, clone to avoid multi-thread access problem.
            this.mList = ContainerUtil.isEmpty(list) ? list : new ArrayList<T>(list);
            this.mDelimiter = ContainerUtil.isEmpty(delimiter) ? " " : delimiter;
        }

        @Override
        public String toString() {
            if (ContainerUtil.isEmpty(mList)) {
                return "";
            }

            return TextUtils.join(mDelimiter, mList);
        }
    }

    public static class ArrayFormatter<T> {
        private final T [] mArray;

        public static <T> ArrayFormatter obtain(T [] array) {
            return new ArrayFormatter<T>(array);
        }

        private ArrayFormatter(T[] list) {
            this.mArray = list;
        }

        @Override
        public String toString() {
            if (ContainerUtil.isEmpty(mArray)) {
                return "";
            }

            return Arrays.toString(mArray);
        }
    }

}
