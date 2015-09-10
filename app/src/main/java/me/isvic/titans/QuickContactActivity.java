package me.isvic.titans;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toolbar;

import me.isvic.titans.utils.SchedulingUtils;
import me.isvic.titans.widgets.MultiShrinkLayout;

/**
 * Created by Vic on 15/9/6.
 */
public class QuickContactActivity extends Activity {

    private MultiShrinkLayout mMultiShrinkLayout;

    private boolean mIsExitAnimationInProgress;
    private boolean mIsEntranceAnimationFinished;
    private boolean mHasAlreadyBeenOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shrink_activity);

        mMultiShrinkLayout = (MultiShrinkLayout) findViewById(R.id.slide_up_scroller);
        mMultiShrinkLayout.initialize(mMultiShrinkLayoutListener, false);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
//        getActionBar().setTitle(null);

        mHasAlreadyBeenOpened = savedInstanceState != null;
        mIsEntranceAnimationFinished = mHasAlreadyBeenOpened;
        showActivity();
    }

    @Override
    public void onBackPressed() {
        if (mMultiShrinkLayout != null) {
            if (!mIsExitAnimationInProgress) {
                mMultiShrinkLayout.scrollOffBottom();
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
        mMultiShrinkLayout.scrollUpForEntranceAnimation(true);
    }

    private void showActivity() {
        if (mMultiShrinkLayout != null) {
            mMultiShrinkLayout.setVisibility(View.VISIBLE);
            SchedulingUtils.doOnPreDraw(mMultiShrinkLayout, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            runEntranceAnimation();
                        }
                    });
        }
    }

    private MultiShrinkLayout.MultiShrinkListener mMultiShrinkLayoutListener = new MultiShrinkLayout.MultiShrinkListener() {
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
