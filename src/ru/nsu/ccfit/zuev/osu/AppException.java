package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.reco1l.osu.multiplayer.Multiplayer;
import org.apache.http.HttpException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;

/**
 * 应用程序异常类：用于捕获异常和提示错误信息
 *
 * @author liux (http://my.oschina.net/liux)
 * @version 1.0
 * @created 2012-3-21
 */
public class AppException extends Exception implements Thread.UncaughtExceptionHandler {

    /**
     * 定义异常类型
     */
    public final static byte TYPE_NETWORK = 0x01;
    public final static byte TYPE_SOCKET = 0x02;
    public final static byte TYPE_HTTP_CODE = 0x03;
    public final static byte TYPE_HTTP_ERROR = 0x04;
    public final static byte TYPE_XML = 0x05;
    public final static byte TYPE_IO = 0x06;
    public final static byte TYPE_RUN = 0x07;
    /**
     *
     */
    private static final long serialVersionUID = 6243307165131877535L;
    private final static boolean Debug = true;// 是否保存错误日志
    private byte type;
    private int code;

    /**
     * 系统默认的UncaughtException处理类
     */
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private AppException() {
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    private AppException(byte type, int code, Exception excp) {
        super(excp);
        this.type = type;
        this.code = code;
        if (Debug) {
            this.saveErrorLog(excp);
        }
    }

    public static AppException http(int code) {
        return new AppException(TYPE_HTTP_CODE, code, null);
    }

    public static AppException http(Exception e) {
        return new AppException(TYPE_HTTP_ERROR, 0, e);
    }

    public static AppException socket(Exception e) {
        return new AppException(TYPE_SOCKET, 0, e);
    }

    public static AppException io(Exception e) {
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return new AppException(TYPE_NETWORK, 0, e);
        } else if (e instanceof IOException) {
            return new AppException(TYPE_IO, 0, e);
        }
        return run(e);
    }

    public static AppException xml(Exception e) {
        return new AppException(TYPE_XML, 0, e);
    }

    public static AppException network(Exception e) {
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return new AppException(TYPE_NETWORK, 0, e);
        } else if (e instanceof HttpException) {
            return http(e);
        } else if (e instanceof SocketException) {
            return socket(e);
        }
        return http(e);
    }

    public static AppException run(Exception e) {
        return new AppException(TYPE_RUN, 0, e);
    }

    /**
     * 获取APP异常崩溃处理对象
     *
     * @return
     */
    public static AppException getAppExceptionHandler() {
        return new AppException();
    }

    public static StringBuffer getTraceInfo(Activity a, Throwable e) {
        StringBuffer sb = new StringBuffer();

        Throwable ex = e.getCause() == null ? e : e.getCause();
        StackTraceElement[] stacks = ex.getStackTrace();
        for (StackTraceElement stack : stacks) {
            sb.append("class: ").append(stack.getClassName()).append("; method: ")
                    .append(stack.getMethodName()).append("; line: ").append(stack.getLineNumber())
                    .append(";  Exception: ").append(ex.toString()).append("\n");
        }
        return sb;
    }

    public int getCode() {
        return this.code;
    }

    public int getType() {
        return this.type;
    }

    /**
     * 保存异常日志
     *
     * @param excp
     */
    public void saveErrorLog(Exception excp) {
        saveErrorLog(excp.getLocalizedMessage());
    }

    /**
     * 保存异常日志
     *
     * @param excpMessage
     */
    public void saveErrorLog(String excpMessage) {
        String errorlog = "errorlog.txt";
        String savePath = "";
        String logFilePath = "";
        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            // 判断是否挂载了SD卡
            String storageState = Environment.getExternalStorageState();
            if (storageState.equals(Environment.MEDIA_MOUNTED)) {
                savePath = Config.getCorePath() + File.separator + "Log/";
                File file = new File(savePath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                logFilePath = savePath + errorlog;
            }
            // 没有挂载SD卡，无法写文件
            if (logFilePath.isEmpty()) {
                return;
            }
            File logFile = new File(logFilePath);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            fw = new FileWriter(logFile, true);
            pw = new PrintWriter(fw);
            pw.println("--------------------" + (DateFormat.format("yyyy-MM-dd hh:mm:ss", new Date()))
                    + "---------------------");
            pw.println(excpMessage);
            pw.close();
            fw.close();
        } catch (Exception e) {
            Log.e("AppException", "[Exception]" + e.getLocalizedMessage());
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                }
            }
        }

    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (Multiplayer.isMultiplayer)
                Multiplayer.log("CRASH");

            // 结束所有Activity
            SaveServiceObject.finishAllActivities();
            //退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义异常处理:收集错误信息&发送错误报告
     *
     * @param ex
     * @return true:处理了该异常信息;否则返回false
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }

        final Context context = GlobalManager.getInstance().getMainActivity();

        if (context == null) {
            return false;
        }

        if (Multiplayer.isMultiplayer)
            Multiplayer.log(ex);

        final String crashReport = getCrashReport(context, ex);
        // 显示异常信息&发送报告
        new Thread() {
            public void run() {
                Looper.prepare();
                Toast.makeText(context, StringTable.get(com.osudroid.resources.R.string.crash), Toast.LENGTH_SHORT).show();
                Looper.loop();
            }

        }.start();

        saveErrorLog(crashReport);

        return true;
    }

    /**
     * 获取APP崩溃异常报告
     *
     * @param ex
     * @return
     */
    private String getCrashReport(Context context, Throwable ex) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pinfo = null;
        StringBuilder exceptionStr = new StringBuilder();
        try {
            pinfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            exceptionStr.append("Version: ").append(pinfo.versionName).append("(").append(pinfo.versionCode).append(")\n").append("\n");
            exceptionStr.append("Android: ").append(Build.VERSION.RELEASE).append("(").append(Build.MODEL).append(")\n").append("\n");
            exceptionStr.append("System Package Info:").append(collectDeviceInfo(context)).append("\n").append("\n");
            exceptionStr.append("System Screen Info:").append(getScreenInfo(context)).append("\n").append("\n");
            exceptionStr.append("System OS Info:").append(getMobileInfo()).append("\n").append("\n");
            exceptionStr.append("Exception: ").append(ex.getMessage()).append("\n").append("\n");
            exceptionStr.append("Exception stack：").append(getTraceInfo((Activity) context, ex)).append("\n").append("\n");
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        ex.printStackTrace();
        return exceptionStr.toString();
    }

    /**
     * 收集设备参数信息
     *
     * @param ctx
     */
    public String collectDeviceInfo(Context ctx) {
        StringBuilder sb = new StringBuilder();
        JSONObject activePackageJson = new JSONObject();

        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = String.valueOf(pi.versionCode);

                activePackageJson.put("versionName", versionName);
                activePackageJson.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e("AppException", "an error occured when collect package info", e);
        } catch (JSONException e) {
            Log.e("AppException", "jsonException", e);
        }
        sb.append("[active Package]");
        sb.append(activePackageJson.toString());

        return sb.toString();
    }

    /**
     * 获取手机的硬件信息
     *
     * @return
     */
    public String getMobileInfo() {
        JSONObject osJson = new JSONObject();
        // 通过反射获取系统的硬件信息

        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                osJson.put(field.getName(), field.get(null).toString());
                Log.d("AppException", field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e("AppException", "an error occured when collect crash info", e);
            }
        }

        try {
            return osJson.toString(4);
        } catch (JSONException e) {
            e.printStackTrace();
            return osJson.toString();
        }
    }

    public String getScreenInfo(Context ctx) {
        JSONObject osJson = new JSONObject();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        ((Activity) ctx).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        for (Field field : displaymetrics.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                osJson.put(field.getName(), field.get(displaymetrics).toString());
                Log.d("AppException", field.getName() + " : " + field.get(displaymetrics));
            } catch (Exception e) {
                Log.e("AppException", "an error occured when collect crash info", e);
            }
        }

        try {
            return osJson.toString(4);
        } catch (JSONException e) {
            e.printStackTrace();
            return osJson.toString();
        }
    }

}