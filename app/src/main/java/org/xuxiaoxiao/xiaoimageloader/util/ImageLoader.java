package org.xuxiaoxiao.xiaoimageloader.util;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.LruCache;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;

/**
 * Created by WuQiang on 2017/5/27.
 */

public class ImageLoader {
    private static ImageLoader mInstance;

    /**
     * 图片缓存的核心对象
     */
    private LruCache<String,Bitmap> mLruCache;

    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    // 默认线程数
    private static final int DEFAULT_THREAD_COUNT = 1;

    /**
     * 队列的调度方式
     */
    private Type mType = Type.LIFO;
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;

    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;

    /**
     * UI线程中的Handler
     */
    private Handler mUIHandler;
    public enum Type
    {
        FIFI,LIFO;
    }

    private ImageLoader(){

    }
    public static ImageLoader getmInstance(){
        if (mInstance == null){
            synchronized (ImageLoader.class)
            {
                if (mInstance == null){
                    mInstance = new ImageLoader();
                }
            }
        }
        return mInstance;
    }
    // 第二次commit错了
}
