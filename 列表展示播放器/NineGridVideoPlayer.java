package com.realcloud.view.video.basecomponent;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 播放器，单纯的控制界面的播放视频，只包含textureview和mediaplayer播放视频
 * ref : nicevideoplayer
 */
public class NineGridVideoPlayer extends FrameLayout implements INineGridVideoPlayer,TextureView.SurfaceTextureListener {

    private int mPlayerType = TYPE_IJK;
    private boolean loopPlay = false;
    private int mCurrentState = STATE_IDLE;
    private int mCurrentMode = MODE_NORMAL;

    private Context mContext;
    private AudioManager mAudioManager;
    private IMediaPlayer mMediaPlayer;
    private NineGridDragFrameLayout mContainer;
    private NineGridVideoTextureView mTextureView;
    private NineGridVideoPlayerController mController; //控制界面
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private String mUrl;
    private Map<String, String> mHeaders;
    private int mBufferPercentage;
    private boolean continueFromLastPosition = true;
    private long skipToPosition;
    private boolean isVolumeEnable; //音量是否打开

    public NineGridVideoPlayer(Context context) {
        this(context, null);
    }

    public NineGridVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context);
    }

    private void init(Context context) {
        isVolumeEnable = NineGridUtil.getSavedVolumeEnable(context);
        mContainer = new NineGridDragFrameLayout(mContext);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mContainer, params);
    }

    /**
     * 自定义实现监听器
     * @param listener 监听器
     */
    @Override
    public void setCustomClickListener(OnClickListener listener) {
        if (mContainer != null){
            mContainer.setOnClickListener(listener);
        }
    }

    /**
     * 自定义实现监听器
     * @param listener 监听器
     */
    @Override
    public void setCustomLongClickListener(OnLongClickListener listener) {
        if (mContainer != null){
            mContainer.setOnLongClickListener(listener);
        }
    }

    /**
     * 设置播放路径
     * @param url     视频地址，可以是本地，也可以是网络视频
     * @param headers 请求header.
     */
    @Override
    public void setUrl(String url, Map<String, String> headers) {
        mUrl = url;
        mHeaders = headers;
    }

    /**
     * 添加对应的播放器控制界面到view上
     * @param controller 控制器界面
     */
    @Override
    public void setController(NineGridVideoPlayerController controller) {
        if (mController != null){
            mContainer.removeView(mController);
        }
        mController = controller;
        mController.reset();
        mController.setNineGridVideoPlayer(this);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(mController, params);
    }

    /**
     * 设置播放器类型
     *
     * @param playerType IjkPlayer or MediaPlayer.
     */
    @Override
    public void setPlayerType(int playerType) {
        mPlayerType = playerType;
    }

    @Override
    public void setLoop(boolean loop) {
        loopPlay = loop;
    }

    /**
     * 是否从上一次的位置继续播放
     *
     * @param continueFromLastPosition true从上一次的位置继续播放
     */
    @Override
    public void continueFromLastPosition(boolean continueFromLastPosition) {
        this.continueFromLastPosition = continueFromLastPosition;
    }

    @Override
    public void setSpeed(float speed) {
        if (mMediaPlayer instanceof IjkMediaPlayer) {
            ((IjkMediaPlayer) mMediaPlayer).setSpeed(speed);
        }
    }

    @Override
    public void start() {
        //只有在mCurrentState == STATE_IDLE时才能调用start方法
        if (mCurrentState == STATE_IDLE) {
            NineGridVideoPlayerManager.getInstance().setCurrentNineGridVideoPlayer(this);
            initAudioManager();
            initMediaPlayer();
            initTextureView();
        }
        if (mController != null){
            mController.startUpdateProgressTimer();
        }
    }

    @Override
    public void start(long position) {
        skipToPosition = position;
        start();
    }

    @Override
    public void restart() {
        if (mCurrentState == STATE_PAUSED) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            if (mController != null){
                mController.onPlayStateChanged(mCurrentState);
            }
        } else if (mCurrentState == STATE_BUFFERING_PAUSED) {
            mMediaPlayer.start();
            mCurrentState = STATE_BUFFERING_PLAYING;
            if (mController != null){
                mController.onPlayStateChanged(mCurrentState);
            }
        } else if (mCurrentState == STATE_COMPLETED || mCurrentState == STATE_ERROR) {
            mMediaPlayer.reset();
            openMediaPlayer();
        }
        if (mController != null){
            mController.startUpdateProgressTimer();
        }
    }

    @Override
    public void pause() {
        if (mCurrentState == STATE_PLAYING) {
            mMediaPlayer.pause();
            mCurrentState = STATE_PAUSED;
            if (mController != null){
                mController.onPlayStateChanged(mCurrentState);
            }
        }
        if (mCurrentState == STATE_BUFFERING_PLAYING) {
            mMediaPlayer.pause();
            mCurrentState = STATE_BUFFERING_PAUSED;
            if (mController != null){
                mController.onPlayStateChanged(mCurrentState);
            }
        }
        if (mController != null){
            mController.cancelUpdateProgressTimer();
        }
    }

    @Override
    public void seekTo(long pos) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(pos);
        }
    }

    @Override
    public void setVolume(int volume) {
        if (mAudioManager != null) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        }
    }

    @Override
    public void setVolumeEnable(boolean enable) {
        this.isVolumeEnable = enable;
        NineGridUtil.savePlayVolumeEnable(getContext(),enable);
        if (mMediaPlayer != null){
            mMediaPlayer.setVolume(enable?1f:0f,enable?1f:0f);
        }
    }

    @Override
    public boolean isIdle() {
        return mCurrentState == STATE_IDLE;
    }

    @Override
    public boolean isPreparing() {
        return mCurrentState == STATE_PREPARING;
    }

    @Override
    public boolean isPrepared() {
        return mCurrentState == STATE_PREPARED;
    }

    @Override
    public boolean isBufferingPlaying() {
        return mCurrentState == STATE_BUFFERING_PLAYING;
    }

    @Override
    public boolean isBufferingPaused() {
        return mCurrentState == STATE_BUFFERING_PAUSED;
    }

    @Override
    public boolean isPlaying() {
        return mCurrentState == STATE_PLAYING;
    }

    @Override
    public boolean isPaused() {
        return mCurrentState == STATE_PAUSED;
    }

    @Override
    public boolean isError() {
        return mCurrentState == STATE_ERROR;
    }

    @Override
    public boolean isCompleted() {
        return mCurrentState == STATE_COMPLETED;
    }

    @Override
    public boolean isLandscapeFullScreen() {
        return mCurrentMode == MODE_FULL_SCREEN_LANDSCAPE;
    }

    @Override
    public boolean isProtraitFullScreen() {
        return mCurrentMode == MODE_FULL_SCREEN_PORTRAIT;
    }

    @Override
    public boolean isTinyWindow() {
        return mCurrentMode == MODE_TINY_WINDOW;
    }

    @Override
    public boolean isNormal() {
        return mCurrentMode == MODE_NORMAL;
    }

    @Override
    public int getMaxVolume() {
        if (mAudioManager != null) {
            return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    @Override
    public int getVolume() {
        if (mAudioManager != null) {
            return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    @Override
    public boolean getVolumeEnable() {
        return isVolumeEnable;
    }

    @Override
    public long getDuration() {
        return mMediaPlayer != null ? mMediaPlayer.getDuration() : 0;
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public int getBufferPercentage() {
        return mBufferPercentage;
    }

    @Override
    public float getSpeed(float speed) {
        if (mMediaPlayer instanceof IjkMediaPlayer) {
            return ((IjkMediaPlayer) mMediaPlayer).getSpeed(speed);
        }
        return 0;
    }

    @Override
    public long getTcpSpeed() {
        if (mMediaPlayer instanceof IjkMediaPlayer) {
            return ((IjkMediaPlayer) mMediaPlayer).getTcpSpeed();
        }
        return 0;
    }

    private void initAudioManager() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if (mAudioManager != null){
                mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        }
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            switch (mPlayerType) {
                case TYPE_NATIVE:
                    mMediaPlayer = new AndroidMediaPlayer();
                    break;
                case TYPE_IJK:
                default:
                    mMediaPlayer = new IjkMediaPlayer(getContext());
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", 842225234L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 12L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 0);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 0L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 131072L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "live-streaming", 0);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "get-av-frame-timeout", 10000000L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "delay-optimization", 0L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "cache-buffer-duration", 2000L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-cache-buffer-duration", 4000L);
                    ((IjkMediaPlayer)mMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1L);
                    ((IjkMediaPlayer)mMediaPlayer).setWakeMode(getContext().getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                    break;
            }
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setVolume(isVolumeEnable?1f:0f,isVolumeEnable?1f:0f);
        }
    }

    private void initTextureView() {
        if (mTextureView == null) {
            mTextureView = new NineGridVideoTextureView(mContext);
            mTextureView.setSurfaceTextureListener(this);
        }
        if (!mContainer.containInnerView(mTextureView)){
            mContainer.addTextureView(mTextureView);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (mSurfaceTexture == null) {
            mSurfaceTexture = surfaceTexture;
            openMediaPlayer();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mTextureView.setSurfaceTexture(mSurfaceTexture);
            }
        }
    }

    private void openMediaPlayer() {
        // 屏幕常亮
        mContainer.setKeepScreenOn(true);
        // 设置监听
        mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
        mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
        mMediaPlayer.setOnErrorListener(mOnErrorListener);
        mMediaPlayer.setOnInfoListener(mOnInfoListener);
        mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mMediaPlayer.setLooping(loopPlay);
        // 设置dataSource
        try {
            mMediaPlayer.setDataSource(mContext.getApplicationContext(), Uri.parse(mUrl),
                    mHeaders != null ?mHeaders : new HashMap<String, String>());
            if (mSurface == null) {
                mSurface = new Surface(mSurfaceTexture);
            }
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
            if (mController != null){
                mController.onPlayStateChanged(mCurrentState);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return mSurfaceTexture == null;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    private IMediaPlayer.OnPreparedListener mOnPreparedListener
            = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            mCurrentState = STATE_PREPARED;
            if (mController != null){
                mController.onPlayStateChanged(mCurrentState);
            }
            mp.start();
            // 从上次的保存位置播放
            if (continueFromLastPosition) {
                long savedPlayPosition = NineGridUtil.getSavedPlayPosition(mContext, mUrl);
                mp.seekTo(savedPlayPosition);
            }
            // 跳到指定位置播放
            if (skipToPosition != 0) {
                mp.seekTo(skipToPosition);
            }
        }
    };

    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener
            = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
            mTextureView.adaptVideoSize(width, height);
        }
    };

    private IMediaPlayer.OnCompletionListener mOnCompletionListener
            = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {
            mCurrentState = STATE_COMPLETED;
            if (mController != null){
                mController.onPlayStateChanged(mCurrentState);
            }
            // 清除屏幕常亮
            mContainer.setKeepScreenOn(false);
        }
    };

    private IMediaPlayer.OnErrorListener mOnErrorListener
            = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            // 直播流播放时去调用mediaPlayer.getDuration会导致-38和-2147483648错误，忽略该错误
            if (what != -38 && what != -2147483648 && extra != -38 && extra != -2147483648) {
                mCurrentState = STATE_ERROR;
                if (mController != null){
                    mController.onPlayStateChanged(mCurrentState);
                }
            }
            return true;
        }
    };

    private IMediaPlayer.OnInfoListener mOnInfoListener
            = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer mp, int what, int extra) {
            if (what == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                // 播放器开始渲染
                mCurrentState = STATE_PLAYING;
                if (mController != null){
                    mController.onPlayStateChanged(mCurrentState);
                }
            } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                // MediaPlayer暂时不播放，以缓冲更多的数据
                if (mCurrentState == STATE_PAUSED || mCurrentState == STATE_BUFFERING_PAUSED) {
                    mCurrentState = STATE_BUFFERING_PAUSED;
                } else {
                    mCurrentState = STATE_BUFFERING_PLAYING;
                }
                if (mController != null){
                    mController.onPlayStateChanged(mCurrentState);
                }
            } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                // 填充缓冲区后，MediaPlayer恢复播放/暂停
                if (mCurrentState == STATE_BUFFERING_PLAYING) {
                    mCurrentState = STATE_PLAYING;
                    if (mController != null){
                        mController.onPlayStateChanged(mCurrentState);
                    }
                }
                if (mCurrentState == STATE_BUFFERING_PAUSED) {
                    mCurrentState = STATE_PAUSED;
                    if (mController != null){
                        mController.onPlayStateChanged(mCurrentState);
                    }
                }
            } else if (what == IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED) {
                // 视频旋转了extra度，需要恢复
                if (mTextureView != null) {
                    mTextureView.setRotation(extra);
                }
            } else if (what == IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
                //视频不能seekTo，为直播视频
            }
            return true;
        }
    };

    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener
            = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            mBufferPercentage = percent;
        }
    };

    /**
     * 全屏，将mContainer(内部包含mTextureView和mController)从当前容器中移除，并添加到android.R.content中.
     * 切换横屏时需要在manifest的activity标签下添加android:configChanges="orientation|keyboardHidden|screenSize"配置，
     * 以避免Activity重新走生命周期
     */
    @Override
    public void enterLandscapeFullScreen() {
        if (mCurrentMode == MODE_FULL_SCREEN_LANDSCAPE) return;
        // 设置屏幕横屏
        NineGridUtil.scanForActivity(mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (mCurrentMode == MODE_FULL_SCREEN_PORTRAIT){
            mCurrentMode = MODE_FULL_SCREEN_LANDSCAPE;
            if (mController != null){
                mController.onPlayModeChanged(mCurrentMode);
            }
            return;
        }
        ViewGroup contentView = NineGridUtil.scanForActivity(mContext).findViewById(android.R.id.content);
        if (mCurrentMode == MODE_TINY_WINDOW) {
            contentView.removeView(mContainer);
        } else if (mCurrentMode == MODE_NORMAL){
            this.removeView(mContainer);
        }
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.setCanViewDrag(false);
        contentView.addView(mContainer, params);
        mCurrentMode = MODE_FULL_SCREEN_LANDSCAPE;
        if (mController != null){
            mController.onPlayModeChanged(mCurrentMode);
        }
    }

    @Override
    public void enterPortraitFullScreen() {
        if (mCurrentMode == MODE_FULL_SCREEN_PORTRAIT) return;
        // 设置屏幕竖屏
        NineGridUtil.scanForActivity(mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (mCurrentMode == MODE_FULL_SCREEN_LANDSCAPE){
            mCurrentMode = MODE_FULL_SCREEN_PORTRAIT;
            if (mController != null){
                mController.onPlayModeChanged(mCurrentMode);
            }
            return;
        }
        ViewGroup contentView = NineGridUtil.scanForActivity(mContext).findViewById(android.R.id.content);
        if (mCurrentMode == MODE_TINY_WINDOW) {
            contentView.removeView(mContainer);
        } else if (mCurrentMode == MODE_NORMAL){
            this.removeView(mContainer);
        }
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.setCanViewDrag(true);
        contentView.addView(mContainer, params);
        mCurrentMode = MODE_FULL_SCREEN_PORTRAIT;
        if (mController != null){
            mController.onPlayModeChanged(mCurrentMode);
        }
    }

    /**
     * 退出全屏，移除mTextureView和mController，并添加到非全屏的容器中。
     * 切换竖屏时需要在manifest的activity标签下添加android:configChanges="orientation|keyboardHidden|screenSize"配置，
     * 以避免Activity重新走生命周期.
     * @return true退出全屏.
     */
    @Override
    public boolean exitFullScreen() {
        if (mCurrentMode == MODE_FULL_SCREEN_PORTRAIT || mCurrentMode == MODE_FULL_SCREEN_LANDSCAPE) {
            NineGridUtil.scanForActivity(mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            ViewGroup contentView = NineGridUtil.scanForActivity(mContext).findViewById(android.R.id.content);
            contentView.removeView(mContainer);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            mContainer.setCanViewDrag(false);
            this.addView(mContainer, params);
            mCurrentMode = MODE_NORMAL;
            if (mController != null){
                mController.onPlayModeChanged(mCurrentMode);
            }
            return true;
        }
        return false;
    }

    /**
     * 进入小窗口播放，小窗口播放的实现原理与全屏播放类似。
     */
    @Override
    public void enterTinyWindow() {
        if (mCurrentMode == MODE_TINY_WINDOW) return;
        this.removeView(mContainer);
        ViewGroup contentView = (ViewGroup) NineGridUtil.scanForActivity(mContext).findViewById(android.R.id.content);
         //小窗口的宽度为屏幕宽度的60%，长宽比默认为16:9，右边距、下边距为8dp。
        LayoutParams params = new LayoutParams(
                (int) (NineGridUtil.getScreenWidth(mContext) * 0.6f),
                (int) (NineGridUtil.getScreenWidth(mContext) * 0.6f * 9f / 16f));
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.rightMargin = NineGridUtil.dp2px(mContext, 8f);
        params.bottomMargin = NineGridUtil.dp2px(mContext, 8f);
        contentView.addView(mContainer, params);
        mCurrentMode = MODE_TINY_WINDOW;
        if (mController != null){
            mController.onPlayModeChanged(mCurrentMode);
        }
    }

    /**
     * 退出小窗口播放
     */
    @Override
    public boolean exitTinyWindow() {
        if (mCurrentMode == MODE_TINY_WINDOW) {
            ViewGroup contentView = NineGridUtil.scanForActivity(mContext).findViewById(android.R.id.content);
            contentView.removeView(mContainer);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            this.addView(mContainer, params);

            mCurrentMode = MODE_NORMAL;
            if (mController != null){
                mController.onPlayModeChanged(mCurrentMode);
            }
            return true;
        }
        return false;
    }

    @Override
    public void releasePlayer() {
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
            mAudioManager = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mContainer.removeView(mTextureView);
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        mCurrentState = STATE_IDLE;
        if (mController != null){
            mController.cancelUpdateProgressTimer();
        }
    }

    @Override
    public void release() {
        // 保存播放位置
        if (isPlaying() || isBufferingPlaying() || isBufferingPaused() || isPaused()) {
            NineGridUtil.savePlayPosition(mContext, mUrl, getCurrentPosition());
        } else if (isCompleted()) {
            NineGridUtil.savePlayPosition(mContext, mUrl, 0);
        }
        // 退出全屏或小窗口
        if (isLandscapeFullScreen() || isProtraitFullScreen()) {
            exitFullScreen();
        }
        if (isTinyWindow()) {
            exitTinyWindow();
        }
        mCurrentMode = MODE_NORMAL;

        // 释放播放器
        releasePlayer();

        // 恢复控制器
        if (mController != null) {
            mController.reset();
            mController.cancelUpdateProgressTimer();
        }
        Runtime.getRuntime().gc();
    }
}
