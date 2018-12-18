package com.realcloud.view.video.basecomponent;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;


/**
 * Created by zack on 2018/12/17.
 * 实现可拖拽点击的framelayout
 * 其中子控件为texttureview是可以拖拽
 * 功能：实现第一个子view可以拖拽滑动（第一个子view为texttureview）拖动是其他字view隐藏
 * 包含设置点击事件
 */

public class NineGridDragFrameLayout extends FrameLayout {

    //最短触发callback事件的距离
    private static final int DEFAULT_CALLBACK_DISTANCE = 300;
    //拖拽事件的callback
    private IDragCallback callback;
    //draghelper
    private ViewDragHelper viewDragHelper;
    //初始的view的x
    private int originLocationX;
    //初始的view的y
    private int originLocationY;
    //滑动中的view的x
    private int dragLocationX;
    //滑动中的view的y
    private int dragLocationY;
    //是否可以被拖拽
    private boolean canViewDrag;
    //长按事件监听
    private OnLongClickListener onLongClickListener;
    //点击事件监听
    private OnClickListener onClickListener;
    //是否可以触发点击和长按事件
    private boolean canResponseClick;
    //事件分发器
    private Handler mHandler = new Handler(Looper.getMainLooper());
    //点击down触发的时间
    private long clickDownTime;
    //点击事件
    private Runnable clickRunable = new Runnable() {
        @Override
        public void run() {
            if (canResponseClick && onLongClickListener != null){
                onClickListener.onClick(NineGridDragFrameLayout.this);
            }
        }
    };
    //长按事件
    private Runnable longClickRunable = new Runnable() {
        @Override
        public void run() {
            if (canResponseClick && onClickListener != null){
                onLongClickListener.onLongClick(NineGridDragFrameLayout.this);
            }
        }
    };

    public NineGridDragFrameLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public NineGridDragFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NineGridDragFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 解决滑动过程中onlayout导致重绘viewdraghelper触发的offsetLeftAndRight和offsetTopAndBottom之前滑动的距离失效
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed,left,top,right,bottom);
        View view = getChildAt(0);
        //处理当view重新layout的时候会导致部分滑动的距离被重置
        if (view instanceof TextureView && view.getTop() == originLocationY && view.getLeft() == originLocationX){
            ViewCompat.offsetLeftAndRight(view,dragLocationX-originLocationX);
            ViewCompat.offsetTopAndBottom(view,dragLocationY-originLocationY);
        }
    }

    /**
     * 初始化viewdraghelper
     */
    private void init(){
        viewDragHelper = ViewDragHelper.create(this,1f,new DragCallback());
        setBackgroundColor(Color.BLACK);
    }

    /**
     * 设置拖拽事件的callback
     * @param callback callback
     */
    public void setCallback(IDragCallback callback){
        this.callback = callback;
    }

    /**
     * 设置长按自定义监听
     * @param l 监听器
     */
    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {
        this.onLongClickListener = l;
    }

    /**
     * 设置点击自定义监听
     * @param l 点击监听器
     */
    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        this.onClickListener = l;
    }

    /**
     * 添加TextureView到识图中
     * @param textureView textureView
     */
    public void addTextureView(TextureView textureView){
        addView(textureView,0,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
    }

    /**
     * 判断视图是否包含该view
     * @param view view
     * @return 是否包含true or false
     */
    public boolean containInnerView(View view){
        return view != null && indexOfChild(view) > 0;
    }

    /**
     * 设置textureview是否可以被拖拽
     * @param canDrag 是否可以拖拽
     */
    public void setCanViewDrag(boolean canDrag){
        this.canViewDrag = canDrag;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (canViewDrag){
            viewDragHelper.processTouchEvent(event);
        }
        //处理长按和点击事件
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                canResponseClick = true;
                mHandler.removeCallbacks(longClickRunable);
                mHandler.postDelayed(longClickRunable,ViewConfiguration.getLongPressTimeout());
                clickDownTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                long currentTime = System.currentTimeMillis();
                if (currentTime - clickDownTime <= ViewConfiguration.getTapTimeout()){
                    mHandler.removeCallbacks(longClickRunable);
                    mHandler.removeCallbacks(clickRunable);
                    mHandler.post(clickRunable);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                canResponseClick = false;
                mHandler.removeCallbacks(longClickRunable);
                mHandler.removeCallbacks(clickRunable);
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (canViewDrag && viewDragHelper.continueSettling(true)){
            ViewCompat.postInvalidateOnAnimation(this);
        }else{
            super.computeScroll();
        }
    }

    /**
     * 控制滑动的类
     */
    class DragCallback extends ViewDragHelper.Callback{

        @Override
        public int getOrderedChildIndex(int index) {
            return 0;
        }

        @Override
        public boolean tryCaptureView(@NonNull View view, int i) {
            if (view instanceof TextureView){
                originLocationX = view.getLeft();
                originLocationY = view.getTop();
                return true;
            }
            return false;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            return left;
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return top;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            //保存滑动的x，y
            dragLocationX = left;
            dragLocationY = top;
            //判断是否可以相应点击事件
            if (Math.abs(originLocationX - left) > 2* ViewConfiguration.getTouchSlop() ||
                    Math.abs(originLocationY - top) > 2* ViewConfiguration.getTouchSlop()){
                canResponseClick = false;
            }
            //STATE_SETTLING为释放返回时的状态
            if (viewDragHelper.getViewDragState() != ViewDragHelper.STATE_SETTLING){
                //触摸滑动中设置子view不可见
                for (int i = 1;i < getChildCount();i++){
                    View view = getChildAt(i);
                    if (view != null){
                        view.setVisibility(INVISIBLE);
                    }
                }
                //设置背景色透明
                float alpha = Math.max((1 - (top-originLocationY)*1f/changedView.getHeight()),0.2f)*255;
                ColorDrawable colorDrawable = new ColorDrawable(Color.BLACK);
                colorDrawable.setAlpha((int) alpha);
                setBackgroundDrawable(colorDrawable);
            }
            //设置textureview透明度
            float alpha = Math.max(1-(top-originLocationY)*1f/changedView.getHeight(),0.5f);
            changedView.setAlpha(alpha);
            //设置textureview缩放
            float scale = Math.max(1-(top-originLocationY)*1f/changedView.getHeight(),0.7f);
            changedView.setScaleX(scale);
            changedView.setScaleY(scale);
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            //恢复黑色背景
            setBackgroundColor(Color.BLACK);
            //显示子view
            for (int i = 1;i < getChildCount();i++){
                View view = getChildAt(i);
                if (view != null){
                    view.setVisibility(VISIBLE);
                }
            }
            //恢复透明度
            releasedChild.setAlpha(1f);
            //恢复缩放
            releasedChild.setScaleX(1f);
            releasedChild.setScaleY(1f);
            //是否需要退出
            boolean needExit = Math.abs(dragLocationX-originLocationX) > DEFAULT_CALLBACK_DISTANCE
                    || Math.abs(dragLocationY-originLocationY) > DEFAULT_CALLBACK_DISTANCE;
            viewDragHelper.settleCapturedViewAt(originLocationX, originLocationY);
            postInvalidate();
            if (needExit && callback != null){
                callback.exitFullScreen();
            }
        }
    }

}
