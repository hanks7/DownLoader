package com.azhong.downloader;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.azhong.downloader.db.DbHelper;
import com.azhong.downloader.view.OnProgressListener;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/*
 * 项目名:    DownLoaderManger
 * 包名       com.azhong.downloader
 * 文件名:    DownLoaderManger
 * 创建者:    ZSY
 * 创建时间:  2017/2/14 on 15:26
 * 描述:     TODO 线程下载
 */
public class DownLoaderManger {

    public static String FILE_PATH = Environment.getExternalStorageDirectory() + "/azhong";
    private DbHelper helper;//数据库帮助类
    private SQLiteDatabase db;
    private OnProgressListener listener;//进度回调监听
    private Map<String, FileInfo> map = new HashMap<>();//保存正在下载的任务信息
    private static DownLoaderManger manger;

    private DownLoaderManger(DbHelper helper, OnProgressListener listener) {
        this.helper = helper;
        this.listener = listener;
        db = helper.getReadableDatabase();
    }

    /**
     * 单例模式
     *
     * @param helper   数据库帮助类
     * @param listener 下载进度回调接口
     * @return
     */
    public static synchronized DownLoaderManger getInstance(DbHelper helper, OnProgressListener listener) {
        if (manger == null) {
            synchronized (DownLoaderManger.class) {
                if (manger == null) {
                    manger = new DownLoaderManger(helper, listener);
                }
            }
        }
        return manger;
    }

    /**
     * 开始下载任务
     */
    public void start(String url) {
        db = helper.getReadableDatabase();
        FileInfo info = helper.queryData(db, url);
        map.put(url, info);
        //开始任务下载
        new DownLoadTask(map.get(url), helper, listener).start();
    }

    /**
     * 停止下载任务
     */
    public void stop(String url) {
        map.get(url).setStop(true);
    }

    /**
     * 重新下载任务
     */
    public void restart(String url) {
        stop(url);
        try {
            File file = new File(FILE_PATH, map.get(url).getFileName());
            if (file.exists()) {
                file.delete();
            }
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        db = helper.getWritableDatabase();
        helper.resetData(db, url);
        start(url);
    }

    /**
     * 获取当前任务状态
     */
    public boolean getCurrentState(String url) {
        return map.get(url).isDownLoading();
    }

    /**
     * 添加下载任务
     *
     * @param info 下载文件信息
     */
    public void addTask(FileInfo info) {
        //判断数据库是否已经存在这条下载信息
        if (!helper.isExist(db, info)) {
            db = helper.getWritableDatabase();
            helper.insertData(db, info);
            map.put(info.getUrl(), info);
        } else {
            //从数据库获取最新的下载信息
            db = helper.getReadableDatabase();
            FileInfo o = helper.queryData(db, info.getUrl());
            if (!map.containsKey(info.getUrl())) {
                map.put(info.getUrl(), o);
            }
        }
    }
}
