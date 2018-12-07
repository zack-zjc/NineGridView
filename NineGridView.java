package com.realcloud.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zack on 2018/12/7.
 * 九宫格展示图片
 * 风格：
 * 一张图：正方形match_parent
 * 两张图：均分正方形
 * 三张图：均分正方形
 * 四张图：均分两行正方形
 * 五张图：三张均分两张下一行顺序排放
 * 六张图：分两个三张均分顺序排放
 * 七张图：分两个三张均分顺序排放剩一个下一行顺序排放
 * 八张图：分两个三张均分顺序排放剩两个下一行顺序排放
 * 九张图：分三个三张均分顺序排放
 */

public class NineGridView<T> extends ViewGroup {

    public static final int DEFAULT_MAX_IMAGE = 9;//最多默认展示九张图片

    private int mGap;//图片之间的距离

    private List<T> mImageDatas;//图片存储数据

    private List<ImageView> imageViews = new ArrayList<>(); //存储view的list

    private NineGridImageAdapter<T> imageAdapter; //图片加载器

    private NineGridClickAdapter<T> clickAdapter; //点击事件处理器

    public NineGridView(Context context) {
        super(context);
    }

    public NineGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NineGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 默认最大size展示9张图片
     * @param size 当前数量
     * @return 最大展示数量
     */
    private int getNeedShowCount(int size) {
        return size > DEFAULT_MAX_IMAGE ? DEFAULT_MAX_IMAGE : size;
    }

    /**
     * 获取imageview
     * @param position 对应position
     * @return 返回imageview
     */
    private ImageView getImageView(final int position){
        ImageView imageView;
        if (position < imageViews.size()) {
            imageView = imageViews.get(position);
        } else {
            imageView = new ImageView(getContext());
            imageViews.add(imageView);
            imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickAdapter != null){
                        clickAdapter.onImageClick(position,mImageDatas, (ImageView) v);
                    }
                }
            });
        }
        return imageView;
    }

    //根据布局规则计算高度
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int totalWidth = width - getPaddingLeft() - getPaddingRight();
        int height =  width - getPaddingLeft() - getPaddingRight();
        if (mImageDatas != null && mImageDatas.size() > 0){
            switch (mImageDatas.size()){
                case 2:
                    height = (totalWidth-mGap)/2;
                    break;
                case 3:
                    height = (totalWidth-2*mGap)/3;
                    break;
                case 5:
                case 6:
                    height = (totalWidth-2*mGap)/3*2+mGap;
                    break;
            }
        }
        height = height + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mImageDatas == null) return;
        switch (getNeedShowCount(mImageDatas.size())){
            case 1:
                layoutSingleView(l,t,r,b);
                break;
            case 2:
                layoutTwoView(l,t,r,b);
                break;
            case 4:
                layoutFourView(l,t,r,b);
                break;
            default:
                layoutNormalView(l,t,r,b);
                break;
        }
    }

    /**
     * 展示一个图片
     * @param left 左边
     * @param top 上边
     * @param right 右边
     * @param bottom 底部
     */
    private void layoutSingleView(int left,int top,int right,int bottom){
        ImageView imageView = (ImageView) getChildAt(0);
        imageView.layout(left + getPaddingLeft(),top+getPaddingTop(),right - getPaddingRight(),bottom - getPaddingBottom());
        if (imageAdapter != null){
            imageAdapter.setImage(mImageDatas.get(0),imageView);
        }
    }

    /**
     * 展示二个图片
     * @param left 左边
     * @param top 上边
     * @param right 右边
     * @param bottom 底部
     */
    private void layoutTwoView(int left,int top,int right,int bottom) {
        int imageleft = left + getPaddingLeft();
        int imageTop = top + getPaddingTop();
        int imageWidth = (right - getPaddingRight() - getPaddingLeft()  - left - mGap) / 2;
        ImageView imageView1 = (ImageView) getChildAt(0);
        imageView1.layout(imageleft,imageTop,imageleft+imageWidth,imageTop + imageWidth);
        if (imageAdapter != null){
            imageAdapter.setImage(mImageDatas.get(0),imageView1);
        }
        ImageView imageView2 = (ImageView) getChildAt(1);
        imageView2.layout(imageleft+imageWidth + mGap ,imageTop,imageleft+imageWidth*2+mGap,imageTop + imageWidth);
        if (imageAdapter != null){
            imageAdapter.setImage(mImageDatas.get(1),imageView2);
        }
    }

    /**
     * 展示四个图片
     * @param left 左边
     * @param top 上边
     * @param right 右边
     * @param bottom 底部
     */
    private void layoutFourView(int left,int top,int right,int bottom){
        int imageWidth = (right - getPaddingRight() - getPaddingLeft()  - left - mGap) / 2;
        for (int i= 0; i < getNeedShowCount(mImageDatas.size()) ;i++){
            int imageleft = left + getPaddingLeft() + (i%2) * (imageWidth + mGap);
            int imageTop = top + getPaddingTop() + (i%2) * (imageWidth + mGap);
            ImageView imageView = (ImageView) getChildAt(i);
            imageView.layout(imageleft,imageTop,imageleft + imageWidth,imageTop + imageWidth);
            if (imageAdapter != null){
                imageAdapter.setImage(mImageDatas.get(i),imageView);
            }
        }
    }

    /**
     * 展示通用九宫格图片
     * @param left 左边
     * @param top 上边
     * @param right 右边
     * @param bottom 底部
     */
    private void layoutNormalView(int left,int top,int right,int bottom){
        int imageWidth = (right - getPaddingRight() - getPaddingLeft()  - left - 2*mGap) / 3;
        for (int i= 0; i < getNeedShowCount(mImageDatas.size()) ;i++){
            int imageleft = left + getPaddingLeft() + (i%3) * (imageWidth + mGap);
            int imageTop = top + getPaddingTop() + (i%3) * (imageWidth + mGap);
            ImageView imageView = (ImageView) getChildAt(i);
            imageView.layout(imageleft,imageTop,imageleft + imageWidth,imageTop + imageWidth);
            if (imageAdapter != null){
                imageAdapter.setImage(mImageDatas.get(i),imageView);
            }
        }
    }

    /**
     * 设置图片数据源
     * @param imageData 展示的图片不为空
     */
    public void setImageData(@NotNull List<T> imageData){
        int oldCount = mImageDatas != null ? mImageDatas.size() : 0;
        int newCount = getNeedShowCount(imageData.size());
        //控制图片展示只有对应数量的view
        if (oldCount < newCount){
            for (int i=0;i<newCount - oldCount;i++){
                ImageView imageView = getImageView(oldCount + i);
                addView(imageView,generateDefaultLayoutParams());
            }
        }else if (oldCount > newCount){
            removeViews(newCount,oldCount - newCount);
        }
        if (mImageDatas != null){
            mImageDatas.clear();
        }else{
            mImageDatas = new ArrayList<>();
        }
        if (mImageDatas != null) {
            mImageDatas.addAll(imageData);
        }
        requestLayout();
    }

    /**
     * 设置图片间隔
     * @param mGap 图片间隔px
     */
    public void setGap(int mGap) {
        this.mGap = mGap;
    }

    /**
     * 设置图片加载adapter
     */
    public void setImageAdapter(NineGridImageAdapter<T> imageAdapter) {
        this.imageAdapter = imageAdapter;
    }

    /**
     * 设置图片点击事件
     * @param clickAdapter 事件处理
     */
    public void setClickAdapter(NineGridClickAdapter<T> clickAdapter) {
        this.clickAdapter = clickAdapter;
    }
}
