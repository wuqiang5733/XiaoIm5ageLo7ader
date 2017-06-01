package org.xuxiaoxiao.xiaoimageloader.util;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.xuxiaoxiao.xiaoimageloader.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends BaseAdapter {
    // 这儿用 static 是能够做到切换到不同的文件夹之后，
    // 上次选中的图片文件依然保留着，
    // 并且在切换文件夹的时候 ImageAdapter 会被重新 New ，static 不受这些影响，
    // 并且注意这儿用的是 Set
    private static Set<String> mSelectedImg = new HashSet<String>();
    private String mDirPath;
    // 包含目录下的所有图片路径的List
    private List<String> mImgPaths;
    private LayoutInflater mInflater;
    // mData 是图片名称的集合，而不是完整路径的集合，这样做是为了省内存
    // dirPath 图片所在文件夹的路径
    public ImageAdapter(Context context, List<String> mData, String dirPath) {
        this.mDirPath = dirPath;
        this.mImgPaths = mData;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mImgPaths.size();
    }

    @Override
    public Object getItem(int position) {
        return mImgPaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_gridview, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.mImg = (ImageView) convertView.findViewById(R.id.id_item_image);
            viewHolder.mSelect = (ImageButton) convertView.findViewById(R.id.id_item_select);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // 重置状态
        viewHolder.mImg.setImageResource(R.drawable.pictures_no);
        viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
        viewHolder.mImg.setColorFilter(null);
        // 下面这一句代码实现了从 图片路径 到 显示图片
        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).LoadImage(
                mDirPath + "/" + mImgPaths.get(position), viewHolder.mImg);

        final String filePath = mDirPath + "/" + mImgPaths.get(position);
//        Log.d("WQWQ_filePath",filePath);
//        Log.d("WQWQ_mDirPath",mDirPath);
//        Log.d("WQWQ_position",mImgPaths.get(position));
        viewHolder.mImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 已经被选择
                if (mSelectedImg.contains(filePath)) {
                    mSelectedImg.remove(filePath);
                    viewHolder.mImg.setColorFilter(null);
//                    viewHolder.mImg.setColorFilter(Color.parseColor(null));
                    viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
                } else // 未被选择
                {
                    mSelectedImg.add(filePath);
                    viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
                    viewHolder.mSelect.setImageResource(R.drawable.pictures_selected);
                }
//                notifyDataSetChanged();  // 这么做会有闪屏出现
            }
        });

        if (mSelectedImg.contains(filePath)) {
            viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
            viewHolder.mSelect.setImageResource(R.drawable.pictures_selected);
        }

        return convertView;
    }

    private class ViewHolder {
        ImageView mImg;
        ImageButton mSelect;
    }
}
