package org.xuxiaoxiao.xiaoimageloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.PopupWindow;

import org.xuxiaoxiao.xiaoimageloader.bean.FolderBean;

import java.util.List;

/**
 * Created by WuQiang on 2017/5/29.
 */

public class ListImageDirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;
    private List<FolderBean> mData;

    public ListImageDirPopupWindow(Context context, List<FolderBean> data) {
//        super(context);
//        mData = data;
        calWidthAndHeight(context);
        
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_main,null);
        mData = data;
        
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);
        
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE)
                {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        initViews();
        initEvent();
    }

    private void initViews() {
    }

    private void initEvent() {
    }

    /**
     * 计算popupWindow的宽度和高度
     * @param context
     */
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        
        mWidth = outMetrics.widthPixels;
        mHeight = (int)(outMetrics.heightPixels * 0.7);
    }
}
