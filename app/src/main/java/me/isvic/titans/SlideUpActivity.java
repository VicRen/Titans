package me.isvic.titans;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toolbar;

import me.isvic.titans.utils.SchedulingUtils;
import me.isvic.titans.widgets.MultiShrinkScroller;

/**
 * Created by Vic on 15/9/6.
 */
public class SlideUpActivity extends Activity {

    private MultiShrinkScroller mMultiShrinkScroller;

    private boolean mIsExitAnimationInProgress;
    private boolean mIsEntranceAnimationFinished;
    private boolean mHasAlreadyBeenOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scroll_activity);

        mMultiShrinkScroller = (MultiShrinkScroller) findViewById(R.id.slide_up_scroller);
        mMultiShrinkScroller.initialize(mSlideUpScrollerListener, false);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
//        getActionBar().setTitle(null);

        mHasAlreadyBeenOpened = savedInstanceState != null;
        mIsEntranceAnimationFinished = mHasAlreadyBeenOpened;
        showActivity();
    }

    @Override
    public void onBackPressed() {
        if (mMultiShrinkScroller != null) {
            if (!mIsExitAnimationInProgress) {
                mMultiShrinkScroller.scrollOffBottom();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        super.finish();

        // override transitions to skip the standard window animations
        overridePendingTransition(0, 0);
    }

    private void runEntranceAnimation() {
        if (mHasAlreadyBeenOpened) {
            return;
        }
        mHasAlreadyBeenOpened = true;
        mMultiShrinkScroller.scrollUpForEntranceAnimation(true);
    }

    private void showActivity() {
        if (mMultiShrinkScroller != null) {
            mMultiShrinkScroller.setVisibility(View.VISIBLE);
            SchedulingUtils.doOnPreDraw(mMultiShrinkScroller, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            runEntranceAnimation();
                        }
                    });
        }
    }

    private MultiShrinkScroller.SlideUpScrollerListener mSlideUpScrollerListener = new MultiShrinkScroller.SlideUpScrollerListener() {
        @Override
        public void onScrolledOffBottom() {
            finish();
        }

        @Override
        public void onStartScrollOffBottom() {
            mIsExitAnimationInProgress = true;
        }

        @Override
        public void onTransparentViewHeightChange(float ratio) {

        }

        @Override
        public void onEntranceAnimationDone() {
            mIsEntranceAnimationFinished = true;
        }

        @Override
        public void onEnterFullscreen() {

        }

        @Override
        public void onExitFullscreen() {

        }
    };
}
