package com.realcloud.view.video.basecomponent;

/**
 * 视频播放器管理器.
 */
public class NineGridVideoPlayerManager {

    private NineGridVideoPlayer mVideoPlayer;

    private NineGridVideoPlayerManager() {
    }

    private static NineGridVideoPlayerManager sInstance;

    public static synchronized NineGridVideoPlayerManager getInstance() {
        if (sInstance == null) {
            sInstance = new NineGridVideoPlayerManager();
        }
        return sInstance;
    }

    public NineGridVideoPlayer getCurrentNineGridVideoPlayer() {
        return mVideoPlayer;
    }

    public void setCurrentNineGridVideoPlayer(NineGridVideoPlayer videoPlayer) {
        if (mVideoPlayer != videoPlayer) {
            releaseNineGridVideoPlayer();
            mVideoPlayer = videoPlayer;
        }
    }

    public void suspendNineGridVideoPlayer() {
        if (mVideoPlayer != null && (mVideoPlayer.isPlaying() || mVideoPlayer.isBufferingPlaying())) {
            mVideoPlayer.pause();
        }
    }

    public void resumeNineGridVideoPlayer() {
        if (mVideoPlayer != null && (mVideoPlayer.isPaused() || mVideoPlayer.isBufferingPaused())) {
            mVideoPlayer.restart();
        }
    }

    public void releaseNineGridVideoPlayer() {
        if (mVideoPlayer != null) {
            mVideoPlayer.release();
            mVideoPlayer = null;
        }
    }

    public boolean onBackPressd() {
        if (mVideoPlayer != null) {
            if (mVideoPlayer.isLandscapeFullScreen() || mVideoPlayer.isProtraitFullScreen()) {
                return mVideoPlayer.exitFullScreen();
            } else if (mVideoPlayer.isTinyWindow()) {
                return mVideoPlayer.exitTinyWindow();
            }
        }
        return false;
    }
}
