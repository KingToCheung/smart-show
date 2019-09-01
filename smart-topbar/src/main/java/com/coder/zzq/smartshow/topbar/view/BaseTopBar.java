package com.coder.zzq.smartshow.topbar.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;

import com.coder.zzq.smartshow.topbar.R;
import com.coder.zzq.smartshow.topbar.utils.ThemeUtils;
import com.coder.zzq.toolkit.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static com.coder.zzq.smartshow.topbar.utils.AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR;


public abstract class BaseTopBar<B extends BaseTopBar<B>> {

    public abstract static class BaseCallback<B> {

        public static final int DISMISS_EVENT_SWIPE = 0;

        public static final int DISMISS_EVENT_ACTION = 1;

        public static final int DISMISS_EVENT_TIMEOUT = 2;

        public static final int DISMISS_EVENT_MANUAL = 3;

        public static final int DISMISS_EVENT_CONSECUTIVE = 4;


        @IntDef({DISMISS_EVENT_SWIPE, DISMISS_EVENT_ACTION, DISMISS_EVENT_TIMEOUT,
                DISMISS_EVENT_MANUAL, DISMISS_EVENT_CONSECUTIVE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface DismissEvent {
        }


        public void onDismissed(B transientBottomBar, @DismissEvent int event) {

        }


        public void onShown(B transientBottomBar) {

        }
    }

    public interface ContentViewCallback {

        void animateContentIn(int delay, int duration);

        void animateContentOut(int delay, int duration);
    }


    @IntDef({LENGTH_INDEFINITE, LENGTH_SHORT, LENGTH_LONG})
    @IntRange(from = 1)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {
    }

    public static final int LENGTH_INDEFINITE = -2;

    public static final int LENGTH_SHORT = -1;


    public static final int LENGTH_LONG = 0;

    static final int ANIMATION_DURATION = 250;
    static final int ANIMATION_FADE_DURATION = 180;

    static final Handler sHandler;
    static final int MSG_SHOW = 0;
    static final int MSG_DISMISS = 1;


    private static final boolean USE_OFFSET_API = (Build.VERSION.SDK_INT >= 16)
            && (Build.VERSION.SDK_INT <= 19);

    static {
        sHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case MSG_SHOW:
                        ((BaseTopBar) message.obj).showView();
                        return true;
                    case MSG_DISMISS:
                        ((BaseTopBar) message.obj).hideView(message.arg1);
                        return true;
                }
                return false;
            }
        });
    }

    private final ViewGroup mTargetParent;
    private final Context mContext;
    protected final TopbarBaseLayout mView;
    private final ContentViewCallback mContentViewCallback;
    private int mDuration;

    private List<BaseCallback<B>> mCallbacks;

    private final AccessibilityManager mAccessibilityManager;


    interface OnLayoutChangeListener {
        void onLayoutChange(View view, int left, int top, int right, int bottom);
    }


    interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View v);

        void onViewDetachedFromWindow(View v);
    }

    protected BaseTopBar(@NonNull ViewGroup parent, @NonNull View content,
                         @NonNull ContentViewCallback contentViewCallback) {
        if (parent == null) {
            throw new IllegalArgumentException("Transient bottom bar must have non-null parent");
        }
        if (content == null) {
            throw new IllegalArgumentException("Transient bottom bar must have non-null content");
        }
        if (contentViewCallback == null) {
            throw new IllegalArgumentException("Transient bottom bar must have non-null callback");
        }

        mTargetParent = parent;
        mContentViewCallback = contentViewCallback;
        mContext = parent.getContext();

        ThemeUtils.checkAppCompatTheme(mContext);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        // Note that for backwards compatibility reasons we inflate a layout that is defined
        // in the extending Snackbar class. This is to prevent breakage of apps that have custom
        // coordinator layout behaviors that depend on that layout.
        mView = (TopbarBaseLayout) inflater.inflate(
                R.layout.topbar, mTargetParent, false);
        mView.addView(content);

        ViewCompat.setAccessibilityLiveRegion(mView,
                ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        ViewCompat.setImportantForAccessibility(mView,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

        // Make sure that we fit system windows and have a listener to apply any insets
        ViewCompat.setFitsSystemWindows(mView, true);
        ViewCompat.setOnApplyWindowInsetsListener(mView,
                new android.support.v4.view.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(View v,
                                                                  WindowInsetsCompat insets) {
                        // Copy over the bottom inset as padding so that we're displayed
                        // above the navigation bar
                        v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                                v.getPaddingRight(), insets.getSystemWindowInsetBottom());
                        return insets;
                    }
                });

        mAccessibilityManager = (AccessibilityManager)
                mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    /**
     * Set how long to show the view for.
     *
     * @param duration either be one of the predefined lengths:
     *                 {@link #LENGTH_SHORT}, {@link #LENGTH_LONG}, or a custom duration
     *                 in milliseconds.
     */
    @NonNull
    public B setDuration(@Duration int duration) {
        mDuration = duration;
        return (B) this;
    }

    /**
     * Return the duration.
     *
     * @see #setDuration
     */
    @Duration
    public int getDuration() {
        return mDuration;
    }

    /**
     * Returns the {@link BaseTopBar}'s context.
     */
    @NonNull
    public Context getContext() {
        return mContext;
    }

    /**
     * Returns the {@link BaseTopBar}'s view.
     */
    @NonNull
    public View getView() {
        return mView;
    }


    public void show() {
        TopBarManager.getInstance().show(mDuration, mManagerCallback);
    }

    public void dismiss() {
        dispatchDismiss(BaseCallback.DISMISS_EVENT_MANUAL);
    }

    void dispatchDismiss(@BaseCallback.DismissEvent int event) {
        TopBarManager.getInstance().dismiss(mManagerCallback, event);
    }


    @NonNull
    public B addCallback(@NonNull BaseCallback<B> callback) {
        if (callback == null) {
            return (B) this;
        }
        if (mCallbacks == null) {
            mCallbacks = new ArrayList<BaseCallback<B>>();
        }
        mCallbacks.add(callback);
        return (B) this;
    }

    @NonNull
    public B removeCallback(@NonNull BaseCallback<B> callback) {
        if (callback == null) {
            return (B) this;
        }
        if (mCallbacks == null) {
            // This can happen if this method is called before the first call to addCallback
            return (B) this;
        }
        mCallbacks.remove(callback);
        return (B) this;
    }


    public boolean isShown() {
        return TopBarManager.getInstance().isCurrent(mManagerCallback);
    }

    /**
     * Returns whether this {@link BaseTopBar} is currently being shown, or is queued
     * to be shown next.
     */
    public boolean isShownOrQueued() {
        return TopBarManager.getInstance().isCurrentOrNext(mManagerCallback);
    }

    final TopBarManager.Callback mManagerCallback = new TopBarManager.Callback() {
        @Override
        public void show() {
            sHandler.sendMessage(sHandler.obtainMessage(MSG_SHOW, BaseTopBar.this));
        }

        @Override
        public void dismiss(int event) {
            sHandler.sendMessage(sHandler.obtainMessage(MSG_DISMISS, event, 0,
                    BaseTopBar.this));
        }
    };

    final void showView() {
        if (mView.getParent() == null) {
            if (mView.getOnSwipeCallback() == null) {
                mView.setOnSwipeCallback(new OnSwipeCallback() {
                    @Override
                    public void onSwipe() {
                        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.topbar_swipe);
                        animation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                mView.setVisibility(View.GONE);
                                dispatchDismiss(BaseCallback.DISMISS_EVENT_SWIPE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                        mView.startAnimation(animation);
                    }

                    @Override
                    public void onPauseTimeout() {
                        TopBarManager.getInstance().pauseTimeout(mManagerCallback);
                    }

                    @Override
                    public void onRestoreTimeout() {
                        TopBarManager.getInstance().restoreTimeoutIfPaused(mManagerCallback);
                    }
                });
            }
            mTargetParent.addView(mView);
        }

        mView.setOnAttachStateChangeListener(
                new OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        if (isShownOrQueued()) {
                            // If we haven't already been dismissed then this event is coming from a
                            // non-user initiated action. Hence we need to make sure that we callback
                            // and keep our state up to date. We need to post the call since
                            // removeView() will call through to onDetachedFromWindow and thus overflow.
                            sHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    onViewHidden(BaseCallback.DISMISS_EVENT_MANUAL);
                                }
                            });
                        }
                    }
                });

        if (ViewCompat.isLaidOut(mView)) {
            if (shouldAnimate()) {
                // If animations are enabled, animate it in
                animateViewIn();
            } else {
                // Else if anims are disabled just call back now
                onViewShown();
            }
        } else {
            // Otherwise, add one of our layout change listeners and show it in when laid out
            mView.setOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View view, int left, int top, int right, int bottom) {
                    mView.setOnLayoutChangeListener(null);

                    if (shouldAnimate()) {
                        // If animations are enabled, animate it in
                        animateViewIn();
                    } else {
                        // Else if anims are disabled just call back now
                        onViewShown();
                    }
                }
            });
        }
    }

    private int getAnimHeight() {
        return -mView.getHeight();
    }


    void animateViewIn() {
        final int viewHeight = getAnimHeight();
        if (USE_OFFSET_API) {
            ViewCompat.offsetTopAndBottom(mView, viewHeight);
        } else {
            mView.setTranslationY(viewHeight);
        }
        final ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(viewHeight, 0);
        animator.setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR);
        animator.setDuration(ANIMATION_DURATION);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                mContentViewCallback.animateContentIn(
                        ANIMATION_DURATION - ANIMATION_FADE_DURATION,
                        ANIMATION_FADE_DURATION);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                onViewShown();
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int mPreviousAnimatedIntValue = viewHeight;

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int currentAnimatedIntValue = (int) animator.getAnimatedValue();
                if (USE_OFFSET_API) {
                    ViewCompat.offsetTopAndBottom(mView,
                            currentAnimatedIntValue - mPreviousAnimatedIntValue);
                } else {
                    mView.setTranslationY(currentAnimatedIntValue);
                }
                mPreviousAnimatedIntValue = currentAnimatedIntValue;
            }
        });
        animator.start();

    }

    private void animateViewOut(final int event) {
        final ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(0, getAnimHeight());
        animator.setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR);
        animator.setDuration(ANIMATION_DURATION);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                mContentViewCallback.animateContentOut(0, ANIMATION_FADE_DURATION);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                onViewHidden(event);
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int mPreviousAnimatedIntValue = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int currentAnimatedIntValue = (int) animator.getAnimatedValue();
                if (USE_OFFSET_API) {
                    ViewCompat.offsetTopAndBottom(mView,
                            currentAnimatedIntValue - mPreviousAnimatedIntValue);
                } else {
                    mView.setTranslationY(currentAnimatedIntValue);
                }
                mPreviousAnimatedIntValue = currentAnimatedIntValue;
            }
        });
        animator.start();

    }

    final void hideView(@BaseCallback.DismissEvent final int event) {
        if (shouldAnimate() && mView.getVisibility() == View.VISIBLE) {
            animateViewOut(event);
        } else {
            // If anims are disabled or the view isn't visible, just call back now
            onViewHidden(event);
        }
    }

    void onViewShown() {
        TopBarManager.getInstance().onShown(mManagerCallback);
        if (mCallbacks != null) {
            // Notify the callbacks. Do that from the end of the list so that if a callback
            // removes itself as the result of being called, it won't mess up with our iteration
            int callbackCount = mCallbacks.size();
            for (int i = callbackCount - 1; i >= 0; i--) {
                mCallbacks.get(i).onShown((B) this);
            }
        }
    }

    void onViewHidden(int event) {
        // First tell the SnackbarManager that it has been dismissed
        TopBarManager.getInstance().onDismissed(mManagerCallback);
        if (mCallbacks != null) {
            // Notify the callbacks. Do that from the end of the list so that if a callback
            // removes itself as the result of being called, it won't mess up with our iteration
            int callbackCount = mCallbacks.size();
            for (int i = callbackCount - 1; i >= 0; i--) {
                mCallbacks.get(i).onDismissed((B) this, event);
            }
        }
        if (Build.VERSION.SDK_INT < 11) {
            // We need to hide the Snackbar on pre-v11 since it uses an old style Animation.
            // ViewGroup has special handling in removeView() when getAnimation() != null in
            // that it waits. This then means that the calculated insets are wrong and the
            // any dodging views do not return. We workaround it by setting the view to gone while
            // ViewGroup actually gets around to removing it.
            mView.setVisibility(View.GONE);
        }
        // Lastly, hide and remove the view from the parent (if attached)
        final ViewParent parent = mView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(mView);
        }
    }

    boolean shouldAnimate() {
        return !mAccessibilityManager.isEnabled();
    }

    public interface OnSwipeCallback {
        void onSwipe();

        void onPauseTimeout();

        void onRestoreTimeout();
    }

    public static class TopbarBaseLayout extends FrameLayout {
        private BaseTopBar.OnLayoutChangeListener mOnLayoutChangeListener;
        private BaseTopBar.OnAttachStateChangeListener mOnAttachStateChangeListener;
        private GestureDetector mGestureDetector;
        private OnSwipeCallback mOnSwipeCallback;
        private Button mActionBtn;

        TopbarBaseLayout(Context context) {
            this(context, null);
        }

        TopbarBaseLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TopbarLayout);
            if (a.hasValue(R.styleable.TopbarLayout_elevation)) {
                ViewCompat.setElevation(this, a.getDimensionPixelSize(
                        R.styleable.TopbarLayout_elevation, 0));
            }
            a.recycle();

            setClickable(true);
            mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (isOutOfActionBtn(e1) && e2.getX() - e1.getX() > Utils.dpToPx(20)) {
                        mOnSwipeCallback.onSwipe();
                    }
                    return true;
                }
            });
        }

        private boolean isOutOfActionBtn(MotionEvent e1) {
            if (mActionBtn == null) {
                mActionBtn = findViewById(R.id.topbar_action);
            }
            return (mActionBtn.getVisibility() != View.VISIBLE)
                    || e1.getX() < mActionBtn.getX();
        }


        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mOnSwipeCallback.onPauseTimeout();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mOnSwipeCallback.onRestoreTimeout();
                    break;
            }
            mGestureDetector.onTouchEvent(ev);
            return super.dispatchTouchEvent(ev);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (mOnLayoutChangeListener != null) {
                mOnLayoutChangeListener.onLayoutChange(this, l, t, r, b);
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (mOnAttachStateChangeListener != null) {
                mOnAttachStateChangeListener.onViewAttachedToWindow(this);
            }

            ViewCompat.requestApplyInsets(this);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (mOnAttachStateChangeListener != null) {
                mOnAttachStateChangeListener.onViewDetachedFromWindow(this);
            }
        }

        void setOnLayoutChangeListener(
                BaseTopBar.OnLayoutChangeListener onLayoutChangeListener) {
            mOnLayoutChangeListener = onLayoutChangeListener;
        }

        void setOnAttachStateChangeListener(
                BaseTopBar.OnAttachStateChangeListener listener) {
            mOnAttachStateChangeListener = listener;
        }

        public OnSwipeCallback getOnSwipeCallback() {
            return mOnSwipeCallback;
        }

        public void setOnSwipeCallback(OnSwipeCallback onSwipeCallback) {
            mOnSwipeCallback = onSwipeCallback;
        }
    }
}
