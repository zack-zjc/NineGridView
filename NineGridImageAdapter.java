package com.zack.view;

import android.widget.ImageView;

/**
 * Created by zack on 2018/12/7.
 * 九宫图展示图片的控件
 */

public interface NineGridImageAdapter<T> {

    void setImage(T url, ImageView imageView);

}
