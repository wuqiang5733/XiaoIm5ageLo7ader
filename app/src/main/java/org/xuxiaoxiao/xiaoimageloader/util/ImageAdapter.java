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
    private static Set<String> mSelectedImg = new HashSet<String>();
    private String mDirPath;
    private List<String> mImgPaths;
    private LayoutInflater mInflater;

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

        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).LoadImage(
                mDirPath + "/" + mImgPaths.get(position), viewHolder.mImg);

        final String filePath = mDirPath + "/" + mImgPaths.get(position);
        viewHolder.mImg.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                // 已经被选择
                if (mSelectedImg.contains(filePath)) {
                    mSelectedImg.remove(filePath);
                    viewHolder.mImg.setColorFilter(Color.parseColor(null));
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
//            viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
//            viewHolder.mSelect.setImageResource(R.drawable.pictures_selected);
        }

        return convertView;
    }

    private class ViewHolder {
        ImageView mImg;
        ImageButton mSelect;
    }
}
