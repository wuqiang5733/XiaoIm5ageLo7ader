package org.xuxiaoxiao.xiaoimageloader.bean;

/**
 * Created by WuQiang on 2017/5/28.
 * PopupWindw 数据模型
 */

public class FolderBean {

    /**
     * 当前文件夹的路径
     */
    private String dir;
    // 第一张图片的路径
    private String firstImgPath;
    // 当前文件夹的名称
    private String name;
    // 当前文件夹图片的数量
    private int count;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndexOf = this.dir.lastIndexOf("/");
//        int lastIndexOf = this.dir.indexOf("/");
        this.name = this.dir.substring(lastIndexOf);
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getName() {
        return name;
    }

//    public void setName(String name) {
//        this.name = name;
//    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
