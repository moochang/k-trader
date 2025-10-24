package com.example.k_trader.base;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import com.example.k_trader.ui.activity.MainActivity;
import com.example.k_trader.KTraderApplication;

import org.apache.log4j.chainsaw.Main;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class Log4jHelper {
    private final static LogConfigurator mLogConfigrator = new LogConfigurator();

    static {
        configureLog4j();
    }

    private static void configureLog4j() {
        // 앱 내부 저장소 사용 (권한 불필요)
        String fileName = getInternalLogFilePath();
        String filePattern = "%m%n";
        int maxBackupSize = 2;
        long maxFileSize = 1 * 1024 * 1024;

        if (!GlobalSettings.getInstance().isFileLogEnabled())
            return;

        configure( fileName, filePattern, maxBackupSize, maxFileSize );
    }

    private static String getInternalLogFilePath() {
        // 앱 내부 저장소 경로 사용
        Context context = KTraderApplication.getAppContext();
        if (context != null) {
            return context.getFilesDir().getAbsolutePath() + "/logs/k-trader.log.txt";
        }
        // fallback: 외부 저장소 사용 (권한 필요)
        return Environment.getExternalStorageDirectory() + "/k-trader/k-trader.log.txt";
    }

    private static void configure( String fileName, String filePattern, int maxBackupSize, long maxFileSize ) {
        mLogConfigrator.setFileName( fileName );
        mLogConfigrator.setMaxFileSize( maxFileSize );
        mLogConfigrator.setFilePattern(filePattern);
        mLogConfigrator.setMaxBackupSize(maxBackupSize);
        mLogConfigrator.setUseLogCatAppender(true);
        mLogConfigrator.configure();

    }

    public static org.apache.log4j.Logger getLogger( String name ) {
        if (!GlobalSettings.getInstance().isFileLogEnabled())
            return null;

        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( name );
        return logger;
    }
}
