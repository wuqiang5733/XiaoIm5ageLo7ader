package org.xuxiaoxiao.xiaoimageloader.util;

/**
 * Created by WuQiang on 2017/5/27.
 */

public class ImageLoader {
    private static ImageLoader mInstance;

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
}
