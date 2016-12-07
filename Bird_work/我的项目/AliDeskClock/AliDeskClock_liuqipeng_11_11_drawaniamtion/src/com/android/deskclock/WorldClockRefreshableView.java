package com.android.deskclock;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import yunos.support.v4.view.MotionEventCompat;
import yunos.support.v4.view.VelocityTrackerCompat;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class WorldClockRefreshableView extends LinearLayout implements OnTouchListener, AlarmListView.ListViewMoveListener {

    private ListViewRefreshListener mListViewRefreshListener;
    public void setListViewRefreshListener(ListViewRefreshListener listViewRefreshListener) {
        mListViewRefreshListener = listViewRefreshListener;
    }
    public interface ListViewRefreshListener {
        public void onListViewRefreshListener();
    }
    /**
     * 下拉状态
     */
    public static final int STATUS_PULL_TO_REFRESH = 0;

    /**
     * 释放立即刷新状态
     */
    public static final int STATUS_RELEASE_TO_REFRESH = 1;


    /**
     * 刷新完成或未刷新状态
     */
    public static final int STATUS_REFRESH_FINISHED = 3;


    /**
     * 下拉头的View
     */
	private View header;

    /**
     * 需要去下拉刷新的ListView
     */
    //private MultiColumnListView gridView;

    /**
     * 下拉头的布局参数
     */
    private MarginLayoutParams headerLayoutParams;

    /**
     * 下拉头的高度
     */
    private int hideHeaderHeight;

    /**
     * 当前处理什么状态，可选值有STATUS_PULL_TO_REFRESH, STATUS_RELEASE_TO_REFRESH,
     * STATUS_REFRESHING 和 STATUS_REFRESH_FINISHED
     */
    private int currentStatus = STATUS_REFRESH_FINISHED;;

    /**
     * 手指按下时的屏幕纵坐标
     */
    private float yDown;

    /**
     * 在被判定为滚动之前用户手指可以移动的最大值。
     */
    private int touchSlop;

    /**
     * 是否已加载过一次layout，这里onLayout中的初始化只需加载一次
     */
    private boolean loadOnce;

    /**
     * 当前是否可以下拉，只有ListView滚动到头的时候才允许下拉
     */
    private boolean ableToPull;
    private int distance;

    public AlarmListView mCommonList;
    private View mDragView;
    /**
     * 列表上方的padding，用来判断顶端
     */
    private int mListPaddingTop;

    private VelocityTracker mVelocityTracker;
    private int mMaximumVelocity;
    private int mMinimumVelocity;
    /**
     * ID of the active pointer. This is used to retain consistency during drags/flings if multiple
     * pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;
    /**
     * Sentinel value for no current active pointer. Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;
    //private OnItemClickListener mItemClickListener;
    public boolean mDisPlayTwoItem = false;
    /**
     * 下拉刷新控件的构造函数，会在运行时动态添加一个下拉头的布局。
     * 
     * @param context
     * @param attrs
     */
    public WorldClockRefreshableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        header = LayoutInflater.from(context).inflate(R.layout.main_clock_frame, null, true);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
        setOrientation(VERTICAL);
        addView(header, 0);
        mDisPlayTwoItem = true;
    }

    /**
     * 进行一些关键性的初始化操作，比如：将下拉头向上偏移进行隐藏，给ListView注册touch事件。
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && !loadOnce) {
            hideHeaderHeight = -header.getHeight();
            headerLayoutParams = (MarginLayoutParams) header.getLayoutParams();
            //headerLayoutParams.topMargin = hideHeaderHeight;
            headerLayoutParams.topMargin = 0;
            mDragView = (View)getChildAt(2);
            mDragView.setOnTouchListener(this);
            mCommonList = (AlarmListView) getChildAt(4);
            mCommonList.setOnTouchListener(this);
            mCommonList.setScrollable(false);
            mCommonList.setListViewMoveListener(this);
            mCommonList.setOnScrollListener(new OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView arg0, int arg1) {
                    if (arg1 == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        mDragView.setEnabled(true);
                    } else {
                        mDragView.setEnabled(false);
                    }
                }

                @Override
                public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {

                }
            });
            mListPaddingTop = mCommonList.getPaddingTop();
            loadOnce = true;
        }
    }

    /**
     * 当ListView被触摸时调用，其中处理了各种下拉刷新的具体逻辑。
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d("ableToPull = "+ableToPull);
        setIsAbleToPull(event);
        if (ableToPull || v.getId() == R.id.drag_view) {
            acquireVelocityTracker(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    /*if(mItemClickListener!=null && mCommonList.getOnItemClickListener()== null) {
                        mCommonList.setOnItemClickListener(mItemClickListener);
                    }*/
                    yDown = event.getRawY();
                    mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                    Log.d("yDown = "+yDown);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float yMove = event.getRawY();
                    distance = (int) (yMove - yDown);
                    if (mDisPlayTwoItem && firstChild.getTop() != mListPaddingTop) {
                        firstChild.setTop(mListPaddingTop);
                    }
                    // 如果手指是下滑状态，并且下拉头是完全隐藏的，就屏蔽下拉事件
                    //android.util.Log.d("randy","headerLayoutParams.topMargin = "+headerLayoutParams.topMargin+", distance = "+distance
                    //        +", hideHeaderHeight = "+hideHeaderHeight);
                    if ((distance >= 0 && headerLayoutParams.topMargin >= 0) ||
                            (distance <= 0 && headerLayoutParams.topMargin <= hideHeaderHeight)){//SWJ test
                        return true;
                    }
                    Log.d("yMove = "+yMove+", yDown = "+yDown);
                    if (Math.abs(distance) < touchSlop) {
                        return true;
                    }
                    /*if(mCommonList.getOnItemClickListener()!=null) {
                        mItemClickListener = mCommonList.getOnItemClickListener();
                    }*/
                    mCommonList.setOnItemClickListener(null);

                    if (headerLayoutParams.topMargin > 0) {
                        currentStatus = STATUS_RELEASE_TO_REFRESH;
                    } else {
                        currentStatus = STATUS_PULL_TO_REFRESH;
                    }
                    Log.d("distance = "+distance);
                    // 通过偏移下拉头的topMargin值，来实现下拉效果
                    if(!mDisPlayTwoItem && distance>=0) {//Pull header down.
                        setHeaderTopMargin((distance / 2) + hideHeaderHeight);
                        //setHeaderTopMargin((distance) + hideHeaderHeight);
                    } else if(mDisPlayTwoItem && headerLayoutParams.topMargin>hideHeaderHeight){//Push header back.
                        setHeaderTopMargin(distance/2);
                        //setHeaderTopMargin(distance);
                    } else if(headerLayoutParams.topMargin == hideHeaderHeight) {//Scroll the list.
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(
                            velocityTracker, mActivePointerId);
                    //android.util.Log.d("randy","initialVelocity = "+initialVelocity);
                    
                    currentStatus = STATUS_REFRESH_FINISHED;
                    mCommonList.setClickable(true);
                    ableToPull = false;
                    if ((distance >= 0 && headerLayoutParams.topMargin >= 0) ||
                            (distance <= 0 && headerLayoutParams.topMargin <= hideHeaderHeight)){//SWJ test
                        releaseVelocityTracker();
                        break;
                    }
                    if (distance >= 0) {
                        if (headerLayoutParams.topMargin >= (hideHeaderHeight/2)){//Show
                            if (mListViewRefreshListener != null && v.getId() == R.id.drag_view) {
                                mListViewRefreshListener.onListViewRefreshListener();
                            }
                            mDisPlayTwoItem = true;
                            mCommonList.setScrollable(false);
                            setHeaderTopMargin(0);
                        } else {
                            if (Math.abs(initialVelocity) > mMinimumVelocity) {
                                if (mListViewRefreshListener != null && v.getId() == R.id.drag_view) {
                                    mListViewRefreshListener.onListViewRefreshListener();
                                }
                                mDisPlayTwoItem = true;
                                mCommonList.setScrollable(false);
                                setHeaderTopMargin(0);
                            } else {
                                mDisPlayTwoItem = false;
                                mCommonList.setScrollable(true);
                                setHeaderTopMargin(hideHeaderHeight);
                            }
                        }
                    } else if (distance < 0) {
                        if (headerLayoutParams.topMargin <= (hideHeaderHeight/2)){//Show
                            mDisPlayTwoItem = false;
                            mCommonList.setScrollable(true);
                            setHeaderTopMargin(hideHeaderHeight);
                        } else {
                            if (Math.abs(initialVelocity) > mMinimumVelocity) {
                                mDisPlayTwoItem = false;
                                mCommonList.setScrollable(true);
                                setHeaderTopMargin(hideHeaderHeight);
                            } else {
                                mDisPlayTwoItem = true;
                                mCommonList.setScrollable(false);
                                setHeaderTopMargin(0);
                            }
                        }
                    }
                    
                    /*if(headerLayoutParams.topMargin>(hideHeaderHeight/2)){//Show
                        if(distance > 0) {
                            if (Math.abs(initialVelocity) > mMinimumVelocity) {
                                setHeaderTopMargin(hideHeaderHeight);
                            } else {
                                setHeaderTopMargin(0);
                            }
                        } else if (distance < 0) {
                            
                        }
                        
                    } else if (Math.abs(initialVelocity) > mMinimumVelocity) {
                        setHeaderTopMargin(0);
                    } else {//Hide
                        setHeaderTopMargin(hideHeaderHeight);
                    }*/
                    releaseVelocityTracker();
                    break;
            }
            if (currentStatus == STATUS_PULL_TO_REFRESH
                    || currentStatus == STATUS_RELEASE_TO_REFRESH
                    || v.getId() == R.id.drag_view) {
                // 当前正处于下拉或释放状态，要让ListView失去焦点，否则被点击的那一项会一直处于选中状态
                mCommonList.setPressed(false);
                mCommonList.setFocusable(false);
                mCommonList.setFocusableInTouchMode(false);
                // 当前正处于下拉或释放状态，通过返回true屏蔽掉ListView的滚动事件
                return true;
            }
        } /*else if(mItemClickListener!=null && mCommonList.getOnItemClickListener()== null) {
        //    mCommonList.setOnItemClickListener(mItemClickListener);
        //}*/

        return false;
    }

    View firstChild;
    /**
     * 根据当前ListView的滚动状态来设定 {@link #ableToPull}
     * 的值，每次都需要在onTouch中第一个执行，这样可以判断出当前应该是滚动ListView，还是应该进行下拉。
     * 
     * @param event
     */
    private void setIsAbleToPull(MotionEvent event) {
        firstChild = mCommonList.getChildAt(0);
        if (firstChild != null) {
            int firstVisiblePos = mCommonList.getFirstVisiblePosition();
            Log.d("firstVisiblePos = "+firstVisiblePos+", firstChild.getTop() = "+firstChild.getTop()
                    + "mListPaddingTop = "+mListPaddingTop+", headerLayoutParams.topMargin = "+headerLayoutParams.topMargin
                    +", mDisPlayTwoItem = "+mDisPlayTwoItem);
            if (mDisPlayTwoItem) {
                if (firstChild.getTop() != mListPaddingTop) {
                    firstChild.setTop(mListPaddingTop);
                }
                if (firstVisiblePos == 0 && firstChild.getTop() <= mListPaddingTop) {
                    //if (firstVisiblePos == 0) {
                        if (!ableToPull) {
                            yDown = event.getRawY();
                        }
                        // 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新
                        ableToPull = true;
                    } else {
                        Log.d("headerLayoutParams.topMargin 1111111111111111111111= "+headerLayoutParams.topMargin);
                        /*if (headerLayoutParams.topMargin != 0) {
                            setHeaderTopMargin(0);
                        }*/
                        ableToPull = false;
                    }
            } else {
                if (firstVisiblePos == 0 && firstChild.getTop() >= mListPaddingTop) {
                    //if (firstVisiblePos == 0) {
                        if (!ableToPull) {
                            yDown = event.getRawY();
                        }
                        // 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新
                        ableToPull = true;
                    } else {
                        Log.d("headerLayoutParams.topMargin 1111111111111111111111= "+headerLayoutParams.topMargin);
                        /*if (headerLayoutParams.topMargin != hideHeaderHeight) {
                            setHeaderTopMargin(hideHeaderHeight);
                        }*/
                        ableToPull = false;
                    }
            }
        } else {
            // 如果ListView中没有元素，不应该允许下拉
            ableToPull = false;
        }
    }

    /*public SearchView getSearchView() {
        return header;
    }*/
    
    /*public void setGridMode(boolean b) {
        mCommonList = b ? gridView : listView;
        mListPaddingTop = mCommonList.getPaddingTop();
    }*/
    
    private void setHeaderTopMargin(int margin) {
        headerLayoutParams.topMargin = margin;
        header.setLayoutParams(headerLayoutParams);
    }

    public void showHeader(boolean bShow) {
        mDisPlayTwoItem = bShow;
        if (headerLayoutParams == null) {
            return;
        }
        if (bShow) {
            headerLayoutParams.topMargin = 0;
        } else {
            headerLayoutParams.topMargin = -header.getHeight();
        }
        header.setLayoutParams(headerLayoutParams);
    }
    
    public void refreashView() {
        if (headerLayoutParams == null) {
            return;
        }
        currentStatus = STATUS_REFRESH_FINISHED;
        if (mCommonList != null) {
            mCommonList.setClickable(true);
        }
        ableToPull = false;
        releaseVelocityTracker();
        if (headerLayoutParams.topMargin <= (hideHeaderHeight/2)){//Show
            mDisPlayTwoItem = false;
            mCommonList.setScrollable(true);
            setHeaderTopMargin(hideHeaderHeight);
        } else {
            mDisPlayTwoItem = true;
            mCommonList.setScrollable(false);
            setHeaderTopMargin(0);
        }
    }

    /**  
     *  
     * @param event 向VelocityTracker添加MotionEvent  
     *  
     * @see android.view.VelocityTracker#obtain()  
     * @see android.view.VelocityTracker#addMovement(MotionEvent)  
     */  
    private void acquireVelocityTracker(final MotionEvent event) {  
        if(null == mVelocityTracker) {  
            mVelocityTracker = VelocityTracker.obtain();  
        }  
        mVelocityTracker.addMovement(event);  
    }  
  
    /**  
     * 释放VelocityTracker  
     *  
     * @see android.view.VelocityTracker#clear()  
     * @see android.view.VelocityTracker#recycle()  
     */  
    private void releaseVelocityTracker() {  
        if(null != mVelocityTracker) {  
            mVelocityTracker.clear();  
            mVelocityTracker.recycle();  
            mVelocityTracker = null;  
        }  
    } 

    @Override
    public void onListViewMoveListener(View v, MotionEvent event) {
        onTouch(v,event);
    }
}
