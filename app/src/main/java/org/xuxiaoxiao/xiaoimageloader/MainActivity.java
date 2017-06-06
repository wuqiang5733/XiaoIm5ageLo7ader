package org.xuxiaoxiao.xiaoimageloader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xuxiaoxiao.xiaoimageloader.bean.FolderBean;
import org.xuxiaoxiao.xiaoimageloader.util.ImageAdapter;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private GridView mGridView;
    // GridView 的数据集
    private List<String> mImgs;
    private ImageAdapter mImgAdapter;

    private RelativeLayout mBottomLy;
    // 显示文件夹与对应的图片个数
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<>();

    private ProgressDialog mProgressDialog;

    private static final int DATA_LOADED = 0x110;
    private ListImageDirPopupWindow mDirPopupWindow;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg); // 这儿不一样
            if (msg.what == DATA_LOADED) {
                mProgressDialog.dismiss();
                // 绑定数据到View中
                data2View();
                initDirPopupWindow();
            }
        }
    };


    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};


    private void initDirPopupWindow() {
        mDirPopupWindow = new ListImageDirPopupWindow(this, mFolderBeans);
        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });

        mDirPopupWindow.setOnDirSelectedListener(new ListImageDirPopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelected(FolderBean folderBean) {
                mCurrentDir = new File(folderBean.getDir());
                mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.endsWith(".jpg") || filename.endsWith(".JPG")|| filename.endsWith(".jpeg") || filename.endsWith(".png"))
                            return true;
                        return false;
                    }
                }));

                mImgAdapter = new ImageAdapter(MainActivity.this, mImgs, mCurrentDir.getAbsolutePath());
                mGridView.setAdapter(mImgAdapter);

                mDirCount.setText(mImgs.size() + "");
                mDirName.setText(folderBean.getName());

                mDirPopupWindow.dismiss();
            }
        });
    }

    /**
     * 内容区域变亮
     */
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }

    private void data2View() {
        if (mCurrentDir == null) {
            Toast.makeText(this, "未扫描到图片", Toast.LENGTH_SHORT).show();
            return;
        }
        //  File parentFile = new File(path).getParentFile();
        //  mCurrentDir = parentFile;
        //  private List<String> mImgs;
        //  mImgs 是 GridView 的数据集
        //  mCurrentDir.list() 返回的是一个数组，所以用 asList 包装成一个 List
        /**
         * list()方法是返回某个目录下的所有文件和目录的文件名，返回的是String数组
         * listFiles()方法是返回某个目录下所有文件和目录的绝对路径，返回的是File数组
         * 看本代码底部的例子
         */
        mImgs = Arrays.asList(mCurrentDir.list());
        // public ImageAdapter(Context context, List<String> mData, String dirPath)
        mImgAdapter = new ImageAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImgAdapter);

        mDirCount.setText(mMaxCount + "");
        mDirName.setText(mCurrentDir.getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        verifyStoragePermissions(this);

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
     * 生成 PopupWindow 所需的数据：文件夹的路径，文件夹下第一个图片的路径，文件夹下图片的数量
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

                String[] projection = null;

                // Defines a string to contain the selection clause
                String mSelectionClause = MediaStore.Images.Media.MIME_TYPE + " =? or " + MediaStore.Images.Media.MIME_TYPE + " =? ";

                // Initializes an array to contain selection arguments
                String[] mSelectionArgs = {"image/jpeg", "image/png"};

                Cursor cursor = cr.query(mImgUri, projection, mSelectionClause, mSelectionArgs ,MediaStore.Images.Media.DATE_MODIFIED);

                // 防止重复遍历
                Set<String> mDirPaths = new HashSet<String>();

                while (cursor.moveToNext()) {
                    // 根据索引值获取图片路径
                    // 像这样：/storage/emulated/0/Pictures/Screenshots/Screenshot_2016-10-21-02-39-00.png
                    // DATA :	Path to the file on disk
                    String path = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Images.Media.DATA));
//                    Log.d("WQWQ-path",path);
                    File parentFile = new File(path).getParentFile();
                    // ContentProvider 当中有些图片是找不到父路径的
                    if (parentFile == null)
                        continue;
                    // 像这样：/storage/emulated/0/Pictures/Screenshots
                    String dirPath = parentFile.getAbsolutePath();
//                    Log.d("WQWQ-dirPath",dirPath);
                    FolderBean folderBean = null;

                    if (mDirPaths.contains(dirPath)) // 如果当前的文件夹扫描过了
                    {
                        continue;
                    } else {
                        // 加入 set 当中，认为这是一个文件夹出现了
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgPath(path);
                    }

                    if (parentFile.list() == null) // 的的确确 会有这种奇怪的事情
                        continue;
                    int picSize = parentFile.list(
                            new FilenameFilter() {
                                // 只获得 后缀为 .jpg .jpeg .png 文件的数量
                                @Override
                                public boolean accept(File dir, String filename) {
                                    if (filename.endsWith(".jpg") || filename.endsWith(".JPG")|| filename.endsWith(".jpeg") || filename.endsWith(".png"))
                                        return true;
                                    return false;
                                }
                            }
                    ).length;
                    folderBean.setCount(picSize);
                    // mFolderBeans 用于初始化 popupWindow
                    mFolderBeans.add(folderBean);

                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        // 第一次显示的，是文件数最多的那个文件夹
                        mCurrentDir = parentFile;
                    }
                }
                cursor.close();
                // 扫描完成，释放临时变量的内存
//                mDirPaths = null;
                // 通知Handler扫描图片完成
                mHandler.sendEmptyMessage(DATA_LOADED);
            }
        }.start();
    }

    private void initEvent() {
        mBottomLy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDirPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);
                mDirPopupWindow.showAsDropDown(mBottomLy, 0, 0);
                lightOff();
            }
        });
    }

    /**
     * 内容区域变暗
     */
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = .3f;
        getWindow().setAttributes(lp);
    }

    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
/**
 *  // 创建File对象
 *  File file = new File("D:\\Android");
 *  // 获取该目录下的所有文件
 *  String[] files = file.list();
 */
