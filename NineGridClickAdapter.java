package com.zack.view;

import android.widget.ImageView;

import java.util.List;

/**
 * Created by zack on 2018/12/7.
 * 九宫格图片点击事件
 */

public interface NineGridClickAdapter<T> {

    /**
     * 点击事件
     * @param position 点击position
     * @param data 当前九宫图所有数据
     * @param imageView 当前点击的view
     */
    void onImageClick(int position, List<T> data, ImageView imageView);

}
