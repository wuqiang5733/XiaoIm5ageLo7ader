package org.xuxiaoxiao.xiaoimageloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by WuQiang on 2017/5/27.
 */

public class ImageLoader {
    private static ImageLoader mInstance;

    /**
     * 图片缓存的核心对象
     * 管理所有图片占用的内存
     */
    private LruCache<String, Bitmap> mLruCache;

    /**
     * 线程池
     * 去执行加载图片的任务
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
     * 之所以用 LinkedList 是因为 LinkedList 有从 头部 取一个对象和从 尾部 取一个对象的方法
     * 并且 LinkedList 是用 链表的方式，不需要连续的内存
     */
    private LinkedList<Runnable> mTaskQueue;

    /**
     * 后台轮询线程
     * 以及 与之绑定的 给这个 Thread 的 MessageQueue 发送消息的 Handler
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;

    /**
     * UI线程中的Handler
     * 根据图片的 path 去设置 Bitmap
     */
    private Handler mUIHandler;

    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    private Semaphore mSemaphoreThreadPool;

    public enum Type {
        FIFO, LIFO;
    }

    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    /**
     * 初始化操作
     *
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {
        // 后台轮询线程
        mPoolThread = new Thread() {
            @Override
            public void run() {
                // Attach a Looper to the current Thread
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        // 去线程池取出一个任务去执行
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                // 释放一个信号量
                mSemaphorePoolThreadHandler.release();
                // Start the message processing
                Looper.loop();
            }
        };
        mPoolThread.start();
        // 获取我们应用的最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                // 返回每个元素所占用的内存字节数
                /**
                 * getRowBytes()
                 * Return the number of bytes between rows in the bitmap's pixels.
                 */
                return value.getRowBytes() * value.getHeight();
//                return value.getByteCount(); // 官方视频上的方法
            }
        };
        // 创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;

        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    /**
     * 从任务队列取出一个方法
     *
     * @return
     */
    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    public static ImageLoader getInstance(int threadCount, Type type) {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    /**
     * 根据path为imageView设置图片
     * 这个方法是运行在UI线程的，所以其 mUIHandler 是可以去操作UI线程的
     *
     * @param path
     * @param imageView
     */
    public void LoadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                public void handleMessage(Message msg) {
                    // 获取得到图片，为imageView回调设置图片
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView imageview = holder.imageView;
                    String path = holder.path;

                    // 将path与getTag存储路径进行比较
                    if (imageview.getTag().toString().equals(path)) {
                        imageview.setImageBitmap(bm);
                    }
                }
            };
        }
        // 根据path在缓存中获取bitmap
        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            // 如果缓存当中有图片的话，让 mUIHandler 发送一个消息，
            // 这个消息当中有图片Path ，ImageView ，Bitmap ，
            // 然后 mUIHandler 在其 handleMessage 方法当中就可以把 Bitmap 设置到 ImageView 上
            refreashBitmap(path, imageView, bm);
        } else { // 如果图片在缓存当中找不到
            addTask(new Runnable() {
                @Override
                public void run() {
                    // 加载图片
                    // 图片的压缩
                    // 1 获取图片需要显示的大小
                    ImageSize imageSize = getImageViewSize(imageView);
                    // 2 压缩图片，并生成Bitmap
                    Bitmap bm = decodeSampledBitmapFromPath(path, imageSize.width, imageSize.height);
                    // 3 把图片回到缓存
                    addBitmapToLruCache(path, bm);
                    // 让UI线程的Handler发送一个消息
                    refreashBitmap(path, imageView, bm);
                    mSemaphoreThreadPool.release();
                }
            });
        }
    }

    private void refreashBitmap(String path, ImageView imageView, Bitmap bm) {
        Message message = Message.obtain();
        ImgBeanHolder holder = new ImgBeanHolder();
        holder.bitmap = bm;
        holder.path = path;
        holder.imageView = imageView;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    /**
     * 将图片加入LruCache
     *
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == null) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    /**
     * 根据图片需要显示的宽和高对图片进行压缩
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        // 获得图片的宽和高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 不返回Bitmap，但可以获得Bitmpa的参数
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // 根据实际图片大小与所需图片大小获得采样比
        options.inSampleSize = caculateInSampleSize(options, width, height);
        // 使用获得的InSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        // Decode a file path into a bitmap
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    /**
     * 根据实际图片大小与所需图片大小获得采样比
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;

        if (width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);

            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        // 下面这个是官方的Demo : https://developer.android.com/topic/performance/graphics/load-bitmap.html ，
        // 但发现其实内存使用增加了一倍，当然清晰度也有很大的提升
//        if (height > reqHeight || width > reqWidth) {
//
//            final int halfHeight = height / 2;
//            final int halfWidth = width / 2;
//
//            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
//            // height and width larger than the requested height and width.
//            while ((halfHeight / inSampleSize) >= reqHeight
//                    && (halfWidth / inSampleSize) >= reqWidth) {
//                inSampleSize *= 2;
//            }
//        }
        // 加大 inSampleSize 对于减少内存使用是没有什么作用的
        return inSampleSize;
    }

    /**
     * 根据imageView获取适当的压缩的宽和高
     *
     * @param imageView
     * @return
     */
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        int width = imageView.getWidth();
//        int width = (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT ? 0 : imageView.getWidth());
        if (width <= 0) {
            width = lp.width; // 获取imageView在layout中声明的宽度
        }
        if (width <= 0) {
            // 就是这个方法用的反射
            width = imageView.getMaxWidth(); // 检查最大值
        }
        if (width <= 0) {
            width = displayMetrics.widthPixels;  // 用屏幕的宽度做为 width
        }

        int height = imageView.getHeight();
//        int width = (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT ? 0 : imageView.getWidth());
        if (height <= 0) {
            height = lp.height; // 获取imageView在layout中声明的高度
        }
        if (height <= 0) {
            height = imageView.getMaxHeight(); // 检查最大值
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }

        imageSize.width = width;
        imageSize.height = height;
        // 下面这一段代码也应该是可以工作的
//        ViewTreeObserver vto = imageView.getViewTreeObserver();
//        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            public boolean onPreDraw() {
//                int finalHeight = imageView.getMeasuredHeight();
//                int finalWidth = imageView.getMeasuredWidth();
//                Log.e("hilength","Height: " + finalHeight + " Width: " + finalWidth);
//                return true;
//            }
//        });

        return imageSize;
    }

    /**
     * 通过反射获取imageView的某个属性值
     *
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;

        Field field = null;
        try {
            field = ImageView.class.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        /**
         * 如果取得的Field是private的，那么就要调用setAccessible(true)，否则会报IllegalAccessException
         */
        field.setAccessible(true);

        try {
            // 获得一个属性的值
            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }

    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mPoolThreadHandler == null)
                // 当一个类里面使用了两个线程，并且某个线程使用了其它线程某个变量的时候，
                // 一定要注意你使用这个变量的时候，能不能确保这个变量已经初始化完毕，
                // 比如 mPoolThreadHandler 是在一个后台线程初始化的，初始化的速度我们无法确定，
                // 所以我们使用了一个信号量机制 ：private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0); 一开始是为 0
                //
                // mPoolThreadHandler 初始化完成之后，通过  mSemaphorePoolThreadHandler.release();方法
                // mSemaphorePoolThreadHandler 会为1，然后通过 mSemaphorePoolThreadHandler.acquire(); 请求我们请求就会得到。
                // 如果 mPoolThreadHandler 初始化还没有完成，mSemaphorePoolThreadHandler.acquire(); 就会被阻塞
                mSemaphorePoolThreadHandler.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }


    /**
     * 根据path在缓存中获取bitmap
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    private class ImageSize {
        int width;
        int height;
    }

    private class ImgBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
//https://www.mkyong.com/java/java-thread-mutex-and-semaphore-example/
// LruCache的使用及原理 : http://www.cnblogs.com/huhx/p/useLruCache.html

/**
 *  UI线程当中的Handler，是根据LruCache当中的Bitmap去设置ImageView
 *
 *  LoadImage 的主要工作是：在缓存当中找Bitmap，如果没有找到，就把
 *  计算ImageView大小、根据采样比生成Bitmap、把Bitmap放到缓存当中、通知UIHnadler可以用Bitmap设置ImageView了
 *  上面四个任务通过 addTask 方法 放到  private LinkedList<Runnable> mTaskQueue 当中
 *  在 addTask 方法当中有一句 ： mPoolThreadHandler.sendEmptyMessage(0x110);
 *
 *  而整个这个类是运行在一个 有Looper 的线程当中，
 *  在 handleMessage 当中开辟了一个线程池，去不停的取用任务队列当中的任务：  mThreadPool.execute(getTask());
 */
