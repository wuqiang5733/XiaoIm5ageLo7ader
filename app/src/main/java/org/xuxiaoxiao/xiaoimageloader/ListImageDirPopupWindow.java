package org.xuxiaoxiao.xiaoimageloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.xuxiaoxiao.xiaoimageloader.bean.FolderBean;
import org.xuxiaoxiao.xiaoimageloader.util.ImageLoader;

import java.util.List;

/**
 * Created by WuQiang on 2017/5/29.
 */

public class ListImageDirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    // 它自己的布局
    private View mConvertView;
    private ListView mListView;
    private List<FolderBean> mData;

    public interface OnDirSelectedListener
    {
        void onSelected(FolderBean folderBean);
    }

    private OnDirSelectedListener mListener;

    public void setOnDirSelectedListener(OnDirSelectedListener mListener) {
        this.mListener = mListener;
    }

    public ListImageDirPopupWindow(Context context, List<FolderBean> data) {
//        super(context);
//        mData = data;
        calWidthAndHeight(context);

        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_main, null);
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
                // 如果点击是发生在 PopupWindow 外面，让 PopupWindow 消失
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        //  如果要做一个 BasePopupWindow 出来，下面两个方法要做成抽象方法
        initViews(context);
        initEvent();
    }

    private void initViews(Context context) {
        mListView = (ListView) mConvertView.findViewById(R.id.id_list_dir);
        mListView.setAdapter(new ListDirAdapter(context, 0, mData));

    }

    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null)
                {
                    mListener.onSelected(mData.get(position));
                }
            }
        });
    }

    /**
     * 计算popupWindow的宽度和高度
     *
     * @param context
     */
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.7);
    }

    private class ListDirAdapter extends ArrayAdapter<FolderBean> {
        private LayoutInflater mInflater;
        private List<FolderBean> mData;

        public ListDirAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<FolderBean> objects) {
            super(context, 0, objects);
            mInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            ViewHolder holder = null;

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.item_popup_main, parent, false);
                holder.mImg = (ImageView) convertView.findViewById(R.id.id_id_dir_item_image);
                holder.mDirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
                holder.mDirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);

                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FolderBean bean = getItem(position);
            // 重置
            holder.mImg.setImageResource(R.drawable.pictures_no);
            // 回调加载
            ImageLoader.getInstance(3, ImageLoader.Type.LIFO).LoadImage(bean.getFirstImgPath(), holder.mImg);
            holder.mDirCount.setText(bean.getCount() + "");
            holder.mDirName.setText(bean.getName());


            return convertView;
        }

        private class ViewHolder {
            ImageView mImg;
            TextView mDirName;
            TextView mDirCount;
        }
    }
}
