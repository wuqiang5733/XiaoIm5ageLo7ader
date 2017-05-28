package org.xuxiaoxiao.xiaoimageloader;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xuxiaoxiao.xiaoimageloader.bean.FolderBean;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private GridView mGridView;
    private List<String> mImgs;

    private RelativeLayout mBottomLy;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<>();

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mGridView = (GridView) findViewById(R.id.id_gridView);
        mBottomLy = (RelativeLayout) findViewById(R.id.id_bottom_ly);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
    }

    /**
     * 利用ContentProvider扫描手机中的所有图片
     */
    private void initData() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前储存卡不可用！", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this, null, "正在加载 ...");

        new Thread() {
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();

                Cursor cursor = cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE + " =? or " + MediaStore.Images.Media.MIME_TYPE + " =? ", new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);

                // 防止重复遍历
                Set<String> mDirPaths = new HashSet<String>();

                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null)
                        continue;
                    String dirPath = parentFile.getAbsolutePath();
                    FolderBean folderBean = null;

                    if (mDirPaths.contains(dirPath)) // 如果当前的文件夹扫描过了
                    {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgPath(path);
                    }

                    if (parentFile.list() == null)
                        continue;
                    int picSize = parentFile.list(
                            new FilenameFilter() {
                                @Override
                                public boolean accept(File dir, String filename) {
                                    if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))
                                        return true;
                                    return false;
                                }
                            }
                    ).length;
                    folderBean.setCount(picSize);
                    mFolderBeans.add(folderBean);

                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }
                }
                cursor.close();
                // 扫描完成，释放临时变量的内存
//                mDirPaths = null;
            }
        }.start();
    }

    private void initEvent() {
    }


}
