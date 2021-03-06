/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hk.hku.qboy.catcher;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EdgeEffect;
import android.widget.OverScroller;
import android.widget.PopupWindow;
import android.widget.ViewSwitcher;

/**
 * View for multi-day view. So far only 1 and 7 day have been tested.
 */
public class DayView extends View implements View.OnCreateContextMenuListener,
        ScaleGestureDetector.OnScaleGestureListener {
    /* package */static final int MINUTES_PER_HOUR = 60;
    /* package */static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * 24;
    /* package */static final int MILLIS_PER_MINUTE = 60 * 1000;
    /* package */static final int MILLIS_PER_HOUR = (3600 * 1000);
    /* package */static final int MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;
    private static final float GRID_LINE_INNER_WIDTH = 1;
    private static final int DAY_GAP = 1;
    private static final int HOUR_GAP = 1;
    private static final int TOUCH_MODE_INITIAL_STATE = 0;
    private static final boolean DEBUG_SCALING = true;
    private static int MAX_CELL_HEIGHT = 150;
    private static int MIN_Y_SPAN = 100;
    private int mTouchMode = TOUCH_MODE_INITIAL_STATE;
    /**
     * Indicates we just received the touch event and we are waiting to see if
     * it is a tap or a scroll gesture.
     */
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int GOTO_SCROLL_DURATION = 200;
    private static final long ANIMATION_DURATION = 400;

    private static final int TOUCH_MODE_VSCROLL = 0x20;
    private static final int TOUCH_MODE_HSCROLL = 0x40;
    /**
     * The selection modes are HIDDEN, PRESSED, SELECTED, and LONGPRESS.
     */
    private static final int SELECTION_HIDDEN = 0;
    private int mSelectionMode = SELECTION_HIDDEN;
    private static final int SELECTION_PRESSED = 1; // D-pad down but not up yet
    private static final int SELECTION_SELECTED = 2;
    private static final int SELECTION_LONGPRESS = 3;
    // Update the current time line every five minutes if the window is left
    // open that long
    private static final int UPDATE_CURRENT_TIME_DELAY = 1000;// 300000;
    // duration for events' cross-fade animation
    private static final int EVENTS_CROSS_FADE_DURATION = 400;
    private static int EVENT_RECT_RIGHT_MARGIN = 0;
    private static final int MAX_EVENT_TEXT_LEN = 500;
    private static final int MINIMUM_SNAP_VELOCITY = 2200;
    private static String TAG = "DayView";
    private static boolean DEBUG = true;
    private static float mScale = 0; // Used for supporting different screen densities
    private static int mHorizontalSnapBackThreshold = 128;
    private static int mBgColor;
    private static int mScaledPagingTouchSlop = 0;
    private static int mCellHeight = 0; // shared among all DayViews
    private static int mMinCellHeight = 1;
    private static float HOURS_TEXT_SIZE = 12;
    private static float GRID_LINE_LEFT_MARGIN = 0;
    private static float AMPM_TEXT_SIZE = 9;
    private static int MIN_HOURS_WIDTH = 96;
    private static int HOURS_TOP_MARGIN = 2;
    private static int HOURS_LEFT_MARGIN = 2;
    private static int HOURS_RIGHT_MARGIN = 4;
    private static int HOURS_MARGIN = HOURS_LEFT_MARGIN + HOURS_RIGHT_MARGIN;
    private static int DAY_HEADER_HEIGHT = 45;
    /**
     * The height of the day names/numbers for multi-day views
     */
    private static int MULTI_DAY_HEADER_HEIGHT = DAY_HEADER_HEIGHT;
    private static int DAY_HEADER_RIGHT_MARGIN = 4;
    private static int DAY_HEADER_BOTTOM_MARGIN = 3;
    private static float DAY_HEADER_FONT_SIZE = 14;
    private static float DATE_HEADER_FONT_SIZE = 32;
    private static int DEFAULT_CELL_HEIGHT = 20;
    // smallest height to draw an event with
    private static float MIN_EVENT_HEIGHT = 1.0F; // in pixels
    private static int mCalendarGridAreaSelected;
    private static int mCalendarGridLineInnerHorizontalColor;
    private static int mCalendarGridLineInnerVerticalColor;
    private static int mFutureBgColor;
    private static int mFutureBgColorRes;
    private static int mCalendarHourLabelColor;
    private static int mWeek_sundayColor;
    private static int EVENT_RECT_STROKE_WIDTH = 2;
    private static int MIN_CELL_WIDTH_FOR_TEXT = 20;
    private static int MIN_CELL_HEIGHT_FOR_TEXT = 10;
    private static int EVENT_RECT_TOP_MARGIN = 1;
    private static int EVENT_RECT_BOTTOM_MARGIN = 0;
    private static int EVENT_RECT_LEFT_MARGIN = 1;
    private static int EVENT_TEXT_TOP_MARGIN = 2;
    private static int EVENT_TEXT_BOTTOM_MARGIN = 2;
    private static int EVENT_TEXT_LEFT_MARGIN = 6;
    private static int EVENT_TEXT_RIGHT_MARGIN = 6;
    private static int mOnDownDelay;
    private static int sCounter = 0;
    private static int mEventTextColor;
    private static int mPressedColor;
    private static int mClickedColor;
    protected final EventGeometry mEventGeometry;
    protected final Resources mResources;
    private final GestureDetector mGestureDetector;
    private final Rect mRect = new Rect();
    private final Rect mDestRect = new Rect();
    private final Rect mPrevBox = new Rect();
    private final Paint mPaint = new Paint();
    private final Paint mSelectionPaint = new Paint();
    private final ViewSwitcher mViewSwitcher;
    private final OverScroller mScroller;
    private final EdgeEffect mEdgeEffectTop;
    private final EdgeEffect mEdgeEffectBottom;
    private final int OVERFLING_DISTANCE;
    private final EventLoader mEventLoader;
    private final Typeface mBold = Typeface.DEFAULT_BOLD;
    private final UpdateCurrentTime mUpdateCurrentTime = new UpdateCurrentTime();
    private final ContinueScroll mContinueScroll = new ContinueScroll();
    ScaleGestureDetector mScaleGestureDetector;
    private final ScrollInterpolator mHScrollInterpolator;
    // Sets the "clicked" color from the clicked event
    private final Runnable mSetClick = new Runnable() {
        @Override
        public void run() {
            // TODO mClickedEvent = mSavedClickedEvent;
            // mSavedClickedEvent = null;
            DayView.this.invalidate();
        }
    };
    private final ArrayList<Event> mSelectedEvents = new ArrayList<Event>();
    private final Runnable mCancelCallback = new Runnable() {
        public void run() {
            clearCachedEvents();
        }
    };
    private final Paint mEventTextPaint = new Paint();
    private final Rect mSelectionRect = new Rect();
    private final Pattern drawTextSanitizerFilter = Pattern.compile("[\t\n],");
    protected Context mContext;
    protected boolean mPaused = true;
    protected int mNumDays = 7;
    Time mBaseDate;
    private boolean mOnFlingCalled;
    private int mFirstCell;
    private int mFirstHour = -1;
    private float mAnimationDistance = 0;
    private int mViewStartX;
    private int mViewStartY;
    private int mMaxViewStartY;
    private int mViewHeight;
    private int mViewWidth;
    private int mScrollStartY;
    private int mPreviousDirection;
    private int mGridAreaHeight = -1;
    private float mGestureCenterHour = 0;
    private boolean mRecalCenterHour = false;
    private int mHoursTextHeight;
    private String[] mHourStrs = {"00", "01", "02", "03", "04", "05", "06",
            "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17",
            "18", "19", "20", "21", "22", "23", "00"};
    private String mAmString;
    private String mPmString;
    private int mCellWidth;
    private int mSelectionDay; // Julian day
    private int mSelectionHour;
    private boolean mCallEdgeEffectOnAbsorb;
    private float mLastVelocity;
    private PopupWindow mPopup;
    private View mPopupView;
    private boolean mStartingScroll = false;
    private Handler mHandler;
    private float[] mLines;
    private int mFirstDayOfWeek; // First day of the week
    private boolean mRemeasure = true;
    private int[] mEarliestStartHour; // indexed by the week day offset
    /**
     * Flag to decide whether to handle the up event. Cases where up events
     * should be ignored are 1) right after a scale gesture and 2) finger was
     * down before app launch
     */
    private boolean mHandleActionUp = true;
    private boolean mScrolling = false;
    // Pixels scrolled
    private float mInitialScrollX;
    private float mInitialScrollY;
    private int mNumHours = 10;
    private int mDateStrWidth;
    private int mHoursWidth;
    private String[] mDayStrs;
    private String[] mDayStrs2Letter;
    private Time mCurrentTime;
    private int mTodayJulianDay;
    private int mFirstJulianDay;
    private int mLastJulianDay;
    private int mMonthLength;
    private int mFirstVisibleDate;
    private int mFirstVisibleDayOfWeek;
    private int mEventsAlpha = 255;
    private long mDownTouchTime;
    private Event mSelectedEvent;
    private Event mPrevSelectedEvent;
    /**
     * This variable helps to avoid unnecessarily reloading events by keeping
     * track of the start millis parameter used for the most recent loading of
     * events. If the next reload matches this, then the events are not
     * reloaded. To force a reload, set this to zero (this is set to zero in the
     * method clearCachedEvents()).
     */
    private long mLastReloadMillis;
    private int mLoadedFirstJulianDay = -1;
    private ArrayList<Event> mEvents = new ArrayList<Event>();
    private StaticLayout[] mLayouts = null;
    private boolean mComputeSelectedEvents;
    private ObjectAnimator mEventsCrossFadeAnimation;
    private Event mClickedEvent; // The event the user clicked on
    private float EVENT_TEXT_FONT_SIZE;
    /**
     * Vertical distance or span between the two touch points at the start of a
     * scaling gesture
     */
    private float mStartingSpanY = 0;
    /**
     * Height of 1 hour in pixels at the start of a scaling gesture
     */
    private int mCellHeightBeforeScaleGesture;
    private ObjectAnimator mTodayAnimator;
    private final TodayAnimatorListener mTodayAnimatorListener = new TodayAnimatorListener();

    private boolean mAnimateToday = false;
    private int mAnimateTodayAlpha = 0;

    // public DayView(Context context, CalendarController controller,
    // ViewSwitcher viewSwitcher, EventLoader eventLoader, int numDays) {
    public DayView(Context context, ViewSwitcher viewSwitcher,
                   EventLoader eventLoader, int numDays) {
        super(context);
        mContext = context;
        mResources = context.getResources();
        // mCreateNewEventString = mResources.getString(R.string.event_create);
        // mNewEventHintString =
        // mResources.getString(R.string.day_view_new_event_hint);
        mNumDays = numDays;

        DATE_HEADER_FONT_SIZE = (int) mResources
                .getDimension(R.dimen.date_header_text_size);
        DAY_HEADER_FONT_SIZE = (int) mResources
                .getDimension(R.dimen.day_label_text_size);
        DAY_HEADER_BOTTOM_MARGIN = (int) mResources
                .getDimension(R.dimen.day_header_bottom_margin);
        HOURS_TEXT_SIZE = (int) mResources
                .getDimension(R.dimen.hours_text_size);
        AMPM_TEXT_SIZE = (int) mResources.getDimension(R.dimen.ampm_text_size);
        MIN_HOURS_WIDTH = (int) mResources
                .getDimension(R.dimen.min_hours_width);
        HOURS_LEFT_MARGIN = (int) mResources
                .getDimension(R.dimen.hours_left_margin);
        HOURS_RIGHT_MARGIN = (int) mResources
                .getDimension(R.dimen.hours_right_margin);
        MULTI_DAY_HEADER_HEIGHT = (int) mResources
                .getDimension(R.dimen.day_header_height);
        int eventTextSizeId;
        eventTextSizeId = R.dimen.week_view_event_text_size;
        EVENT_TEXT_FONT_SIZE = (int) mResources.getDimension(eventTextSizeId);
        // NEW_EVENT_HINT_FONT_SIZE = (int)
        // mResources.getDimension(R.dimen.new_event_hint_text_size);
        MIN_EVENT_HEIGHT = mResources.getDimension(R.dimen.event_min_height);
        // MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT = MIN_EVENT_HEIGHT;
        EVENT_TEXT_TOP_MARGIN = (int) mResources
                .getDimension(R.dimen.event_text_vertical_margin);
        EVENT_TEXT_BOTTOM_MARGIN = EVENT_TEXT_TOP_MARGIN;
        // EVENT_ALL_DAY_TEXT_TOP_MARGIN = EVENT_TEXT_TOP_MARGIN;
        // EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN = EVENT_TEXT_TOP_MARGIN;
        //
        EVENT_TEXT_LEFT_MARGIN = (int) mResources
                .getDimension(R.dimen.event_text_horizontal_margin);
        EVENT_TEXT_RIGHT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
        // EVENT_ALL_DAY_TEXT_LEFT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
        // EVENT_ALL_DAY_TEXT_RIGHT_MARGIN = EVENT_TEXT_LEFT_MARGIN;

        if (mScale == 0) {

            mScale = mResources.getDisplayMetrics().density;
            if (mScale != 1) {
                GRID_LINE_LEFT_MARGIN *= mScale;
                HOURS_TOP_MARGIN *= mScale;
                MIN_CELL_WIDTH_FOR_TEXT *= mScale;

                MIN_Y_SPAN *= mScale;
                MAX_CELL_HEIGHT *= mScale;
                DEFAULT_CELL_HEIGHT *= mScale;
                DAY_HEADER_HEIGHT *= mScale;
                DAY_HEADER_RIGHT_MARGIN *= mScale;
                EVENT_RECT_TOP_MARGIN *= mScale;
                EVENT_RECT_BOTTOM_MARGIN *= mScale;
                EVENT_RECT_LEFT_MARGIN *= mScale;
                EVENT_RECT_RIGHT_MARGIN *= mScale;
                EVENT_RECT_STROKE_WIDTH *= mScale;
            }
        }
        HOURS_MARGIN = HOURS_LEFT_MARGIN + HOURS_RIGHT_MARGIN;
        DAY_HEADER_HEIGHT = MULTI_DAY_HEADER_HEIGHT;

        // mCurrentTimeLine =
        // mResources.getDrawable(R.drawable.timeline_indicator_holo_light);
        // mCurrentTimeAnimateLine = mResources
        // .getDrawable(R.drawable.timeline_indicator_activated_holo_light);
        // mTodayHeaderDrawable =
        // mResources.getDrawable(R.drawable.today_blue_week_holo_light);
        // mExpandAlldayDrawable =
        // mResources.getDrawable(R.drawable.ic_expand_holo_light);
        // mCollapseAlldayDrawable =
        // mResources.getDrawable(R.drawable.ic_collapse_holo_light);
        // mNewEventHintColor =
        // mResources.getColor(R.color.new_event_hint_text_color);
        // mAcceptedOrTentativeEventBoxDrawable = mResources
        // .getDrawable(R.drawable.panel_month_event_holo_light);

        mEventLoader = eventLoader;
        mEventGeometry = new EventGeometry();
        mEventGeometry.setMinEventHeight(MIN_EVENT_HEIGHT);
        mEventGeometry.setHourGap(HOUR_GAP);
        mEventGeometry.setCellMargin(DAY_GAP);
        // mLongPressItems = new CharSequence[] {
        // mResources.getString(R.string.new_event_dialog_option)
        // };
        // mLongPressTitle =
        // mResources.getString(R.string.new_event_dialog_label);
        // mDeleteEventHelper = new DeleteEventHelper(context, null, false /*
        // don't exit when done */);
        // mLastPopupEventID = INVALID_EVENT_ID;
        // mController = controller;
        mViewSwitcher = viewSwitcher;
        mGestureDetector = new GestureDetector(context,new CalendarGestureListener());
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        if (mCellHeight == 0) {
            mCellHeight = DEFAULT_CELL_HEIGHT;
        }
        mScroller = new OverScroller(context);
        mHScrollInterpolator = new ScrollInterpolator();
        mEdgeEffectTop = new EdgeEffect(context);
        mEdgeEffectBottom = new EdgeEffect(context);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mScaledPagingTouchSlop = vc.getScaledPagingTouchSlop();
        mOnDownDelay = ViewConfiguration.getTapTimeout();
        OVERFLING_DISTANCE = vc.getScaledOverflingDistance();

        init(context);
    }

    @Override
    protected void onAttachedToWindow() {
        if (mHandler == null) {
            mHandler = getHandler();
            mHandler.post(mUpdateCurrentTime);
        }
    }

    private void init(Context context) {
        setFocusable(true);

        // Allow focus in touch mode so that we can do keyboard shortcuts
        // even after we've entered touch mode.
        setFocusableInTouchMode(true);
        setClickable(true);
        setOnCreateContextMenuListener(this);

        mFirstDayOfWeek = Time.SUNDAY;

        mCurrentTime = new Time();
        long currentTime = System.currentTimeMillis();
        mCurrentTime.set(currentTime);
        mTodayJulianDay = Time.getJulianDay(currentTime, mCurrentTime.gmtoff);

        // mWeek_saturdayColor = mResources.getColor(R.color.week_saturday);
        mWeek_sundayColor = mResources.getColor(R.color.week_sunday);
        // mCalendarDateBannerTextColor =
        // mResources.getColor(R.color.calendar_date_banner_text_color);
        mFutureBgColorRes = mResources
                .getColor(R.color.calendar_future_bg_color);
        mBgColor = mResources.getColor(R.color.calendar_hour_background);
        // mCalendarAmPmLabel =
        // mResources.getColor(R.color.calendar_ampm_label);
        mCalendarGridAreaSelected = mResources
                .getColor(R.color.calendar_grid_area_selected);
        mCalendarGridLineInnerHorizontalColor = mResources
                .getColor(R.color.calendar_grid_line_inner_horizontal_color);
        mCalendarGridLineInnerVerticalColor = mResources
                .getColor(R.color.calendar_grid_line_inner_vertical_color);
        mCalendarHourLabelColor = mResources
                .getColor(R.color.calendar_hour_label);
        // mPressedColor = mResources.getColor(R.color.pressed);
        // mClickedColor =
        // mResources.getColor(R.color.day_event_clicked_background_color);
        mEventTextColor = mResources
                .getColor(R.color.calendar_event_text_color);
        // mMoreEventsTextColor =
        // mResources.getColor(R.color.month_event_other_color);

        mEventTextPaint.setTextSize(EVENT_TEXT_FONT_SIZE);
        mEventTextPaint.setTextAlign(Paint.Align.LEFT);
        mEventTextPaint.setAntiAlias(true);

        int gridLineColor = mResources
                .getColor(R.color.calendar_grid_line_highlight_color);
        Paint p = mSelectionPaint;
        p.setColor(gridLineColor);
        p.setStyle(Style.FILL);
        p.setAntiAlias(false);

        p = mPaint;
        p.setAntiAlias(true);

        // Allocate space for 2 weeks worth of weekday names so that we can
        // easily start the week display at any week day.
        mDayStrs = new String[14];

        // Also create an array of 2-letter abbreviations.
        mDayStrs2Letter = new String[14];

        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            int index = i - Calendar.SUNDAY;
            // e.g. Tue for Tuesday
            mDayStrs[index] = DateUtils.getDayOfWeekString(i,
                    DateUtils.LENGTH_MEDIUM).toUpperCase();
            mDayStrs[index + 7] = mDayStrs[index];
            // e.g. Tu for Tuesday
            mDayStrs2Letter[index] = DateUtils.getDayOfWeekString(i,
                    DateUtils.LENGTH_SHORT).toUpperCase();

            // If we don't have 2-letter day strings, fall back to 1-letter.
            if (mDayStrs2Letter[index].equals(mDayStrs[index])) {
                mDayStrs2Letter[index] = DateUtils.getDayOfWeekString(i,
                        DateUtils.LENGTH_SHORTEST);
            }

            mDayStrs2Letter[index + 7] = mDayStrs2Letter[index];
        }

        // Figure out how much space we need for the 3-letter abbrev names
        // in the worst case.
        p.setTextSize(DATE_HEADER_FONT_SIZE);
        p.setTypeface(mBold);
        String[] dateStrs = {" 28", " 30"};
        mDateStrWidth = computeMaxStringWidth(0, dateStrs, p);
        p.setTextSize(DAY_HEADER_FONT_SIZE);
        mDateStrWidth += computeMaxStringWidth(0, mDayStrs, p);

        p.setTextSize(HOURS_TEXT_SIZE);
        p.setTypeface(null);
        handleOnResume();

        mAmString = DateUtils.getAMPMString(Calendar.AM).toUpperCase();
        mPmString = DateUtils.getAMPMString(Calendar.PM).toUpperCase();
        String[] ampm = {mAmString, mPmString};
        p.setTextSize(AMPM_TEXT_SIZE);
        mHoursWidth = Math.max(HOURS_MARGIN,
                computeMaxStringWidth(mHoursWidth, ampm, p)
                        + HOURS_RIGHT_MARGIN
        );
        mHoursWidth = Math.max(MIN_HOURS_WIDTH, mHoursWidth);

        LayoutInflater inflater;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.bubble_event, null);
        mPopupView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mPopup = new PopupWindow(context);
        mPopup.setContentView(mPopupView);
        Resources.Theme dialogTheme = getResources().newTheme();
        dialogTheme.applyStyle(android.R.style.Theme_Dialog, true);
        TypedArray ta = dialogTheme
                .obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
        mPopup.setBackgroundDrawable(ta.getDrawable(0));
        ta.recycle();

        mBaseDate = new Time();
        long millis = System.currentTimeMillis();
        mBaseDate.set(millis);

        mEarliestStartHour = new int[mNumDays];
        // mHasAllDayEvent = new boolean[mNumDays];

        // mLines is the array of points used with Canvas.drawLines() in
        // drawGridBackground() and drawAllDayEvents(). Its size depends
        // on the max number of lines that can ever be drawn by any single
        // drawLines() call in either of those methods.
        final int maxGridLines = (24 + 1) // max horizontal lines we might draw
                + (mNumDays + 1); // max vertical lines we might draw
        mLines = new float[maxGridLines * 4];
    }

    public void handleOnResume() {
        mFutureBgColor = mFutureBgColorRes;
        mSelectionMode = SELECTION_HIDDEN;
    }

    public void updateTitle() {
        Time start = new Time(mBaseDate);
        start.normalize(true);
        Time end = new Time(start);
        end.monthDay += mNumDays - 1;
        // Move it forward one minute so the formatter doesn't lose a day
        end.minute += 1;
        end.normalize(true);

        long formatFlags = DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_YEAR;
        if (mNumDays != 1) {
            // Don't show day of the month if for multi-day view
            formatFlags |= DateUtils.FORMAT_NO_MONTH_DAY;

            // Abbreviate the month if showing multiple months
            if (start.month != end.month) {
                formatFlags |= DateUtils.FORMAT_ABBREV_MONTH;
            }
        }
        String date = DateUtils.formatDateRange(
                mContext,
                new Formatter(new StringBuilder(10), Locale.getDefault()),
                mBaseDate.toMillis(false),
                mBaseDate.toMillis(false),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                        | DateUtils.FORMAT_SHOW_YEAR, mBaseDate.timezone
        )
                .toString();
        ((MainActivity) mContext).setTitle(date);
    }

    private void recalc() {
        // Set the base date to the beginning of the week if we are displaying
        // 7 days at a time.
        if (mNumDays == 7) {
            adjustToBeginningOfWeek(mBaseDate);
        }

        final long start = mBaseDate.toMillis(false /* use isDst */);
        mFirstJulianDay = Time.getJulianDay(start, mBaseDate.gmtoff);
        mLastJulianDay = mFirstJulianDay + mNumDays - 1;

        mMonthLength = mBaseDate.getActualMaximum(Time.MONTH_DAY);
        mFirstVisibleDate = mBaseDate.monthDay;
        mFirstVisibleDayOfWeek = mBaseDate.weekDay;
    }

    private void adjustToBeginningOfWeek(Time time) {
        int dayOfWeek = time.weekDay;
        int diff = dayOfWeek - mFirstDayOfWeek;
        if (diff != 0) {
            if (diff < 0) {
                diff += 7;
            }
            time.monthDay -= diff;
            time.normalize(true /* ignore isDst */);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        mViewWidth = width;
        mViewHeight = height;
        mEdgeEffectTop.setSize(mViewWidth, mViewHeight);
        mEdgeEffectBottom.setSize(mViewWidth, mViewHeight);
        int gridAreaWidth = width - mHoursWidth;
        mCellWidth = (gridAreaWidth - (mNumDays * DAY_GAP)) / mNumDays;

        // // This would be about 1 day worth in a 7 day view
        // mHorizontalSnapBackThreshold = width / 7;

        Paint p = new Paint();
        p.setTextSize(HOURS_TEXT_SIZE);
        mHoursTextHeight = (int) Math.abs(p.ascent());
        remeasure(width, height);
    }

    /**
     * Measures the space needed for various parts of the view after
     * loading new events.  This can change if there are all-day events.
     */
    private void remeasure(int width, int height) {

        // First, clear the array of earliest start times
        for (int day = 0; day < mNumDays; day++) {
            mEarliestStartHour[day] = 25; // some big number
        }

        // The min is where 24 hours cover the entire visible area
        mMinCellHeight = Math.max((height - DAY_HEADER_HEIGHT) / 24,
                (int) MIN_EVENT_HEIGHT);
        if (mCellHeight < mMinCellHeight) {
            mCellHeight = mMinCellHeight;
        }

        // Calculate mAllDayHeight
        mFirstCell = DAY_HEADER_HEIGHT;

        mGridAreaHeight = height - mFirstCell;

        mNumHours = mGridAreaHeight / (mCellHeight + HOUR_GAP);
        mEventGeometry.setHourHeight(mCellHeight);

        final long minimumDurationMillis = (long) (MIN_EVENT_HEIGHT
                * DateUtils.MINUTE_IN_MILLIS / (mCellHeight / 60.0f));
        Event.computePositions(mEvents, minimumDurationMillis);

        // Compute the top of our reachable view
        mMaxViewStartY = HOUR_GAP + 24 * (mCellHeight + HOUR_GAP)
                - mGridAreaHeight;
        if (DEBUG) {
            Log.e(TAG, "mViewStartY: " + mViewStartY);
            Log.e(TAG, "mMaxViewStartY: " + mMaxViewStartY);
        }
        if (mViewStartY > mMaxViewStartY) {
            mViewStartY = mMaxViewStartY;
            computeFirstHour();
        }

        if (mFirstHour == -1) {
            initFirstHour();
        }

        final int eventAreaWidth = mNumDays * (mCellWidth + DAY_GAP);
        // When we get new events we don't want to dismiss the popup unless the
        // event changes
        // TODO
        // if (mSelectedEvent != null && mLastPopupEventID != mSelectedEvent.id)
        // {
        // mPopup.dismiss();
        // }
        mPopup.setWidth(eventAreaWidth - 20);
        mPopup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
    }

    /**
     * Initialize the state for another view.  The given view is one that has
     * its own bitmap and will use an animation to replace the current view.
     * The current view and new view are either both Week views or both Day
     * views.  They differ in their base date.
     *
     * @param view the view to initialize.
     */
    private void initView(DayView view) {
        view.setSelectedHour(mSelectionHour);
        view.mSelectedEvents.clear();
        // view.mComputeSelectedEvents = true;
        view.mFirstHour = mFirstHour;
        view.remeasure(getWidth(), getHeight());

        // view.setSelectedEvent(null);
        // view.mPrevSelectedEvent = null;
        view.mFirstDayOfWeek = mFirstDayOfWeek;
        // if (view.mEvents.size() > 0) {
        // view.mSelectionAllday = mSelectionAllday;
        // } else {
        // view.mSelectionAllday = false;
        // }

        // Redraw the screen so that the selection box will be redrawn. We may
        // have scrolled to a different part of the day in some other view
        // so the selection box in this view may no longer be visible.
        view.clearCachedEvents();
        view.reloadEvents();
        view.recalc();
    }

    private View switchViews(boolean forward, float xOffSet, float width,
                             float velocity) {
        mAnimationDistance = width - xOffSet;
        if (DEBUG) {
            Log.d(TAG, "switchViews(" + forward + ") O:" + xOffSet + " Dist:"
                    + mAnimationDistance);
        }

        float progress = Math.abs(xOffSet) / width;
        if (progress > 1.0f) {
            progress = 1.0f;
        }

        float inFromXValue, inToXValue;
        float outFromXValue, outToXValue;
        if (forward) {
            inFromXValue = 1.0f - progress;
            inToXValue = 0.0f;
            outFromXValue = -progress;
            outToXValue = -1.0f;
        } else {
            inFromXValue = progress - 1.0f;
            inToXValue = 0.0f;
            outFromXValue = progress;
            outToXValue = 1.0f;
        }

        final Time start = new Time(mBaseDate.timezone);
        start.set(mBaseDate.toMillis(false));
        if (forward) {
            start.monthDay += mNumDays;
        } else {
            start.monthDay -= mNumDays;
        }
        // mController.setTime(start.normalize(true));

        Time newSelected = start;

        if (mNumDays == 7) {
            newSelected = new Time(start);
            adjustToBeginningOfWeek(start);
        }

        final Time end = new Time(start);
        end.monthDay += mNumDays - 1;

        // We have to allocate these animation objects each time we switch views
        // because that is the only way to set the animation parameters.
        TranslateAnimation inAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, inFromXValue,
                Animation.RELATIVE_TO_SELF, inToXValue, Animation.ABSOLUTE,
                0.0f, Animation.ABSOLUTE, 0.0f);

        TranslateAnimation outAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, outFromXValue,
                Animation.RELATIVE_TO_SELF, outToXValue, Animation.ABSOLUTE,
                0.0f, Animation.ABSOLUTE, 0.0f);

        long duration = calculateDuration(width - Math.abs(xOffSet), width,
                velocity);
        inAnimation.setDuration(duration);
        inAnimation.setInterpolator(mHScrollInterpolator);
        outAnimation.setInterpolator(mHScrollInterpolator);
        outAnimation.setDuration(duration);
        outAnimation.setAnimationListener(new GotoBroadcaster(start, end));
        mViewSwitcher.setInAnimation(inAnimation);
        mViewSwitcher.setOutAnimation(outAnimation);

        DayView view = (DayView) mViewSwitcher.getCurrentView();
        view.cleanup();
        mViewSwitcher.showNext();
        view = (DayView) mViewSwitcher.getCurrentView();
        view.requestFocus();
        view.reloadEvents();
        view.updateTitle();
        view.restartCurrentTimeUpdates();

        return view;
    }

    // This is called after scrolling stops to move the selected hour
    // to the visible part of the screen.
    private void resetSelectedHour() {
        if (mSelectionHour < mFirstHour + 1) {
            setSelectedHour(mFirstHour + 1);
            setSelectedEvent(null);
            mSelectedEvents.clear();
            mComputeSelectedEvents = true;
        } else if (mSelectionHour > mFirstHour + mNumHours - 3) {
            setSelectedHour(mFirstHour + mNumHours - 3);
            setSelectedEvent(null);
            mSelectedEvents.clear();
            mComputeSelectedEvents = true;
        }
    }

    private void initFirstHour() {
        mFirstHour = mSelectionHour - mNumHours / 5;
        if (mFirstHour < 0) {
            mFirstHour = 0;
        } else if (mFirstHour + mNumHours > 24) {
            mFirstHour = 24 - mNumHours;
        }
    }

    /**
     * Recomputes the first full hour that is visible on screen after the
     * screen is scrolled.
     */
    private void computeFirstHour() {
        // Compute the first full hour that is visible on screen
        mFirstHour = (mViewStartY + mCellHeight + HOUR_GAP - 1)
                / (mCellHeight + HOUR_GAP);
        // TODO mFirstHourOffset = mFirstHour * (mCellHeight + HOUR_GAP) -
        // mViewStartY;
    }

    void clearCachedEvents() {
        mLastReloadMillis = 0;
    }

    /* package */void reloadEvents() {
        // Protect against this being called before this view has been
        // initialized.
        // if (mContext == null) {
        // return;
        // }

        setSelectedEvent(null);
        mPrevSelectedEvent = null;
        mSelectedEvents.clear();

        // The start date is the beginning of the week at 12am
        Time weekStart = new Time();
        weekStart.set(mBaseDate);
        weekStart.hour = 0;
        weekStart.minute = 0;
        weekStart.second = 0;
        long millis = weekStart.normalize(true /* ignore isDst */);

        // Avoid reloading events unnecessarily.
        if (millis == mLastReloadMillis) {
            return;
        }
        mLastReloadMillis = millis;

        // load events in the background
        // mContext.startProgressSpinner();
        final ArrayList<Event> events = new ArrayList<Event>();
        mEventLoader.loadEventsInBackground(mNumDays, events, mFirstJulianDay,
                new Runnable() {

                    public void run() {
                        boolean fadeinEvents = mFirstJulianDay != mLoadedFirstJulianDay;
                        mEvents = events;
                        mLoadedFirstJulianDay = mFirstJulianDay;

                        // New events, new layouts
                        if (mLayouts == null || mLayouts.length < events.size()) {
                            mLayouts = new StaticLayout[events.size()];
                        } else {
                            Arrays.fill(mLayouts, null);
                        }

                        computeEventRelations();

                        mRemeasure = true;
                        mComputeSelectedEvents = true;
                        recalc();

                        // Start animation to cross fade the events
                        if (fadeinEvents) {
                            if (mEventsCrossFadeAnimation == null) {
                                mEventsCrossFadeAnimation = ObjectAnimator
                                        .ofInt(DayView.this, "EventsAlpha", 0,
                                                255);
                                mEventsCrossFadeAnimation
                                        .setDuration(EVENTS_CROSS_FADE_DURATION);
                            }
                            mEventsCrossFadeAnimation.start();
                        } else {
                            invalidate();
                        }
                    }
                }, mCancelCallback
        );
    }

    private void computeEventRelations() {
        // Compute the layout relation between each event before measuring cell
        // width, as the cell width should be adjusted along with the relation.
        //
        // Examples: A (1:00pm - 1:01pm), B (1:02pm - 2:00pm)
        // We should mark them as "overwapped". Though they are not overwapped
        // logically, but
        // minimum cell height implicitly expands the cell height of A and it
        // should look like
        // (1:00pm - 1:15pm) after the cell height adjustment.

        // Compute the space needed for the all-day events, if any.
        // Make a pass over all the events, and keep track of the maximum
        // number of all-day events in any one day. Also, keep track of
        // the earliest event in each day.
        final ArrayList<Event> events = mEvents;
        final int len = events.size();
        // Num of all-day-events on each day.
        final int eventsCount[] = new int[mLastJulianDay - mFirstJulianDay + 1];
        Arrays.fill(eventsCount, 0);
        for (int ii = 0; ii < len; ii++) {
            Event event = events.get(ii);
            if (event.startDay > mLastJulianDay
                    || event.endDay < mFirstJulianDay) {
                continue;
            }
            int daynum = event.startDay - mFirstJulianDay;
            int hour = event.startTime / 60;
            if (daynum >= 0 && hour < mEarliestStartHour[daynum]) {
                mEarliestStartHour[daynum] = hour;
            }

            // Also check the end hour in case the event spans more than
            // one day.
            daynum = event.endDay - mFirstJulianDay;
            hour = event.endTime / 60;
            if (daynum < mNumDays && hour < mEarliestStartHour[daynum]) {
                mEarliestStartHour[daynum] = hour;
            }
        }
    }

    @SuppressLint("WrongCall")
    @Override
    protected void onDraw(Canvas canvas) {
        if (mRemeasure) {
            remeasure(getWidth(), getHeight());
            mRemeasure = false;
        }
        canvas.save();

        float yTranslate = -mViewStartY + DAY_HEADER_HEIGHT;
        // offset canvas by the current drag and header position
        canvas.translate(-mViewStartX, yTranslate);
        // clip to everything below the allDay area
        Rect dest = mDestRect;
        dest.top = (int) (mFirstCell - yTranslate);
        dest.bottom = (int) (mViewHeight - yTranslate);
        dest.left = 0;
        dest.right = mViewWidth;
        canvas.save();
        canvas.clipRect(dest);
        // Draw the movable part of the view
        doDraw(canvas);
        // restore to having no clip
        canvas.restore();

        if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
            float xTranslate;
            if (mViewStartX > 0) {
                xTranslate = mViewWidth;
            } else {
                xTranslate = -mViewWidth;
            }
            // Move the canvas around to prep it for the next view
            // specifically, shift it by a screen and undo the
            // yTranslation which will be redone in the nextView's onDraw().
            canvas.translate(xTranslate, -yTranslate);
            DayView nextView = (DayView) mViewSwitcher.getNextView();

            // Prevent infinite recursive calls to onDraw().
            nextView.mTouchMode = TOUCH_MODE_INITIAL_STATE;

            nextView.onDraw(canvas);
            // Move it back for this view
            canvas.translate(-xTranslate, 0);
        } else {
            // If we drew another view we already translated it back
            // If we didn't draw another view we should be at the edge of the
            // screen
            canvas.translate(mViewStartX, -yTranslate);
        }

        // Draw the fixed areas (that don't scroll) directly to the canvas.
        drawAfterScroll(canvas);
        if (mComputeSelectedEvents) {
            updateEventDetails();
        }
        mComputeSelectedEvents = false;

        // Draw overscroll glow
        if (!mEdgeEffectTop.isFinished()) {
            if (DAY_HEADER_HEIGHT != 0) {
                canvas.translate(0, DAY_HEADER_HEIGHT);
            }
            if (mEdgeEffectTop.draw(canvas)) {
                invalidate();
            }
            if (DAY_HEADER_HEIGHT != 0) {
                canvas.translate(0, -DAY_HEADER_HEIGHT);
            }
        }
        if (!mEdgeEffectBottom.isFinished()) {
            canvas.rotate(180, mViewWidth / 2, mViewHeight / 2);
            if (mEdgeEffectBottom.draw(canvas)) {
                invalidate();
            }
        }
        canvas.restore();
    }

    private void drawAfterScroll(Canvas canvas) {
        Paint p = mPaint;
        Rect r = mRect;

        drawScrollLine(r, canvas, p);
        drawDayHeaderLoop(r, canvas, p);

        // Draw the AM and PM indicators if we're in 12 hour mode
        // if (!mIs24HourFormat) {
        // drawAmPm(canvas, p);
    }

    private void drawScrollLine(Rect r, Canvas canvas, Paint p) {
        final int right = computeDayLeftPosition(mNumDays);
        final int y = mFirstCell - 1;

        p.setAntiAlias(false);
        p.setStyle(Style.FILL);

        p.setColor(mCalendarGridLineInnerHorizontalColor);
        p.setStrokeWidth(GRID_LINE_INNER_WIDTH);
        canvas.drawLine(GRID_LINE_LEFT_MARGIN, y, right, y, p);
        p.setAntiAlias(true);
    }

    // Computes the x position for the left side of the given day (base 0)
    private int computeDayLeftPosition(int day) {
        int effectiveWidth = mViewWidth - mHoursWidth;
        return day * effectiveWidth / mNumDays + mHoursWidth;
    }

    private void drawDayHeaderLoop(Rect r, Canvas canvas, Paint p) {
        // Draw the horizontal day background banner
        // p.setColor(mCalendarDateBannerBackground);
        // r.top = 0;
        // r.bottom = DAY_HEADER_HEIGHT;
        // r.left = 0;
        // r.right = mHoursWidth + mNumDays * (mCellWidth + DAY_GAP);
        // canvas.drawRect(r, p);
        //
        // Fill the extra space on the right side with the default background
        // r.left = r.right;
        // r.right = mViewWidth;
        // p.setColor(mCalendarGridAreaBackground);
        // canvas.drawRect(r, p);

        p.setTypeface(mBold);
        p.setTextAlign(Paint.Align.RIGHT);
        int cell = mFirstJulianDay;

        String[] dayNames;
        if (mDateStrWidth < mCellWidth) {
            dayNames = mDayStrs;
        } else {
            dayNames = mDayStrs2Letter;
        }

        p.setAntiAlias(true);
        for (int day = 0; day < mNumDays; day++, cell++) {
            int dayOfWeek = day + mFirstVisibleDayOfWeek;
            if (dayOfWeek >= 14) {
                dayOfWeek -= 14;
            }
            p.setColor(mWeek_sundayColor);
            drawDayHeader(dayNames[dayOfWeek], day, cell, canvas, p);
        }
        p.setTypeface(null);
    }

    private void drawCurrentTimeLine(Rect r, int day, int lineY, Canvas canvas,
                                     Paint p) {
        // TODO Auto-generated method stub

    }

    private void doDraw(Canvas canvas) {
        Paint p = mPaint;
        Rect r = mRect;

        if (mFutureBgColor != 0) {
            drawBgColors(r, canvas, p);
        }
        drawGridBackground(r, canvas, p);
        drawHours(r, canvas, p);

        // Draw each day
        int cell = mFirstJulianDay;
        p.setAntiAlias(false);
        int alpha = p.getAlpha();
        p.setAlpha(mEventsAlpha);
        for (int day = 0; day < mNumDays; day++, cell++) {
            // TODO Wow, this needs cleanup. drawEvents loop through all the
            // events on every call.
            drawEvents(cell, day, HOUR_GAP, canvas, p);
            // If this is today
            if (cell == mTodayJulianDay) {
                int lineY = mCurrentTime.hour * (mCellHeight + HOUR_GAP)
                        + ((mCurrentTime.minute * mCellHeight) / 60) + 1;

                // And the current time shows up somewhere on the screen
                if (lineY >= mViewStartY
                        && lineY < mViewStartY + mViewHeight - 2) {
                    drawCurrentTimeLine(r, day, lineY, canvas, p);
                }
            }
        }
        p.setAntiAlias(true);
        p.setAlpha(alpha);

        drawSelectedRect(r, canvas, p);
    }

    private void drawSelectedRect(Rect r, Canvas canvas, Paint p) {
        // Draw a highlight on the selected hour (if needed)
        // if (mSelectionMode != SELECTION_HIDDEN) {
        // int daynum = mSelectionDay - mFirstJulianDay;
        // r.top = mSelectionHour * (mCellHeight + HOUR_GAP);
        // r.bottom = r.top + mCellHeight + HOUR_GAP;
        // r.left = computeDayLeftPosition(daynum) + 1;
        // r.right = computeDayLeftPosition(daynum + 1) + 1;
        //
        // saveSelectionPosition(r.left, r.top, r.right, r.bottom);
        //
        // // Draw the highlight on the grid
        // p.setColor(mCalendarGridAreaSelected);
        // r.top += HOUR_GAP;
        // r.right -= DAY_GAP;
        // p.setAntiAlias(false);
        // canvas.drawRect(r, p);
        // }
    }

    private void drawHours(Rect r, Canvas canvas, Paint p) {
        setupHourTextPaint(p);

        int y = HOUR_GAP + mHoursTextHeight + HOURS_TOP_MARGIN;

        for (int i = 0; i < 24; i++) {
            String time = mHourStrs[i];
            canvas.drawText(time, HOURS_LEFT_MARGIN, y, p);
            y += mCellHeight + HOUR_GAP;
        }
    }

    private void setupHourTextPaint(Paint p) {
        p.setColor(mCalendarHourLabelColor);
        p.setTextSize(HOURS_TEXT_SIZE);
        p.setTypeface(Typeface.DEFAULT);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setAntiAlias(true);
    }

    private void drawDayHeader(String dayStr, int day, int cell, Canvas canvas,
                               Paint p) {
        int dateNum = mFirstVisibleDate + day;
        int x;
        if (dateNum > mMonthLength) {
            dateNum -= mMonthLength;
        }
        p.setAntiAlias(true);

        int todayIndex = mTodayJulianDay - mFirstJulianDay;
        // Draw day of the month
        String dateNumStr = String.valueOf(dateNum);

        float y = DAY_HEADER_HEIGHT - DAY_HEADER_BOTTOM_MARGIN;

        // Draw day of the month
        x = computeDayLeftPosition(day + 1) - DAY_HEADER_RIGHT_MARGIN;
        p.setTextAlign(Align.RIGHT);
        p.setTextSize(DATE_HEADER_FONT_SIZE);

        p.setTypeface(todayIndex == day ? mBold : Typeface.DEFAULT);
        canvas.drawText(dateNumStr, x, y, p);

        // Draw day of the week
        x -= p.measureText(" " + dateNumStr);
        p.setTextSize(DAY_HEADER_FONT_SIZE);
        p.setTypeface(Typeface.DEFAULT);
        canvas.drawText(dayStr, x, y, p);
    }

    private void drawGridBackground(Rect r, Canvas canvas, Paint p) {
        Paint.Style savedStyle = p.getStyle();

        final float stopX = computeDayLeftPosition(mNumDays);
        float y = 0;
        final float deltaY = mCellHeight + HOUR_GAP;
        int linesIndex = 0;
        final float startY = 0;
        final float stopY = HOUR_GAP + 24 * (mCellHeight + HOUR_GAP);
        float x = mHoursWidth;

        // Draw the inner horizontal grid lines
        p.setColor(mCalendarGridLineInnerHorizontalColor);
        p.setStrokeWidth(GRID_LINE_INNER_WIDTH);
        p.setAntiAlias(false);
        y = 0;
        linesIndex = 0;
        for (int hour = 0; hour <= 24; hour++) {
            mLines[linesIndex++] = GRID_LINE_LEFT_MARGIN;
            mLines[linesIndex++] = y;
            mLines[linesIndex++] = stopX;
            mLines[linesIndex++] = y;
            y += deltaY;
        }
        if (mCalendarGridLineInnerVerticalColor != mCalendarGridLineInnerHorizontalColor) {
            canvas.drawLines(mLines, 0, linesIndex, p);
            linesIndex = 0;
            p.setColor(mCalendarGridLineInnerVerticalColor);
        }

        // Draw the inner vertical grid lines
        for (int day = 0; day <= mNumDays; day++) {
            x = computeDayLeftPosition(day);
            mLines[linesIndex++] = x;
            mLines[linesIndex++] = startY;
            mLines[linesIndex++] = x;
            mLines[linesIndex++] = stopY;
        }
        canvas.drawLines(mLines, 0, linesIndex, p);

        // Restore the saved style.
        p.setStyle(savedStyle);
        p.setAntiAlias(true);
    }

    private void drawBgColors(Rect r, Canvas canvas, Paint p) {
        int todayIndex = mTodayJulianDay - mFirstJulianDay;
        // Draw the hours background color
        r.top = mDestRect.top;
        r.bottom = mDestRect.bottom;
        r.left = 0;
        r.right = mHoursWidth;
        p.setColor(mBgColor);
        p.setStyle(Style.FILL);
        p.setAntiAlias(false);
        canvas.drawRect(r, p);

        // Draw background for grid area
        if (mNumDays == 1 && todayIndex == 0) {
            // Draw a white background for the time later than current time
            int lineY = mCurrentTime.hour * (mCellHeight + HOUR_GAP)
                    + ((mCurrentTime.minute * mCellHeight) / 60) + 1;
            if (lineY < mViewStartY + mViewHeight) {
                lineY = Math.max(lineY, mViewStartY);
                r.left = mHoursWidth;
                r.right = mViewWidth;
                r.top = lineY;
                r.bottom = mViewStartY + mViewHeight;
                p.setColor(mFutureBgColor);
                canvas.drawRect(r, p);
            }
        } else if (todayIndex >= 0 && todayIndex < mNumDays) {
            // Draw today with a white background for the time later than
            // current time
            int lineY = mCurrentTime.hour * (mCellHeight + HOUR_GAP)
                    + ((mCurrentTime.minute * mCellHeight) / 60) + 1;
            if (lineY < mViewStartY + mViewHeight) {
                lineY = Math.max(lineY, mViewStartY);
                r.left = computeDayLeftPosition(todayIndex) + 1;
                r.right = computeDayLeftPosition(todayIndex + 1);
                r.top = lineY;
                r.bottom = mViewStartY + mViewHeight;
                p.setColor(mFutureBgColor);
                canvas.drawRect(r, p);
            }

            // Paint Tomorrow and later days with future color
            if (todayIndex + 1 < mNumDays) {
                r.left = computeDayLeftPosition(todayIndex + 1) + 1;
                r.right = computeDayLeftPosition(mNumDays);
                r.top = mDestRect.top;
                r.bottom = mDestRect.bottom;
                p.setColor(mFutureBgColor);
                canvas.drawRect(r, p);
            }
        } else if (todayIndex < 0) {
            // Future
            r.left = computeDayLeftPosition(0) + 1;
            r.right = computeDayLeftPosition(mNumDays);
            r.top = mDestRect.top;
            r.bottom = mDestRect.bottom;
            p.setColor(mFutureBgColor);
            canvas.drawRect(r, p);
        }
        p.setAntiAlias(true);
    }

    private void setSelectedEvent(Event e) {
        mSelectedEvent = e;
    }


    private int computeMaxStringWidth(int currentMax, String[] strings, Paint p) {
        float maxWidthF = 0.0f;

        int len = strings.length;
        for (int i = 0; i < len; i++) {
            float width = p.measureText(strings[i]);
            maxWidthF = Math.max(width, maxWidthF);
        }
        int maxWidth = (int) (maxWidthF + 0.5);
        if (maxWidth < currentMax) {
            maxWidth = currentMax;
        }
        return maxWidth;
    }


    private void setupTextRect(Rect r) {
        if (r.bottom <= r.top || r.right <= r.left) {
            r.bottom = r.top;
            r.right = r.left;
            return;
        }

        if (r.bottom - r.top > EVENT_TEXT_TOP_MARGIN + EVENT_TEXT_BOTTOM_MARGIN) {
            r.top += EVENT_TEXT_TOP_MARGIN;
            r.bottom -= EVENT_TEXT_BOTTOM_MARGIN;
        }
        if (r.right - r.left > EVENT_TEXT_LEFT_MARGIN + EVENT_TEXT_RIGHT_MARGIN) {
            r.left += EVENT_TEXT_LEFT_MARGIN;
            r.right -= EVENT_TEXT_RIGHT_MARGIN;
        }
    }


    /**
     * Return the layout for a numbered event. Create it if not already existing
     */
    private StaticLayout getEventLayout(StaticLayout[] layouts, int i,
                                        Event event, Paint paint, Rect r) {
        if (i < 0 || i >= layouts.length) {
            return null;
        }

        StaticLayout layout = layouts[i];
        // Check if we have already initialized the StaticLayout and that
        // the width hasn't changed (due to vertical resizing which causes
        // re-layout of events at min height)
        if (layout == null || r.width() != layout.getWidth()) {
            SpannableStringBuilder bob = new SpannableStringBuilder();
            if (event.title != null) {
                // MAX - 1 since we add a space
                bob.append(drawTextSanitizer(event.title.toString(),
                        MAX_EVENT_TEXT_LEN - 1));
                bob.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0,
                        bob.length(), 0);
                bob.append(' ');
            }
            // if (event.location != null) {
            // bob.append(drawTextSanitizer(event.location.toString(),
            // MAX_EVENT_TEXT_LEN - bob.length()));
            // }

            paint.setColor(mEventTextColor);

            // Leave a one pixel boundary on the left and right of the rectangle for the event
            layout = new StaticLayout(bob, 0, bob.length(), new TextPaint(paint), r.width(),
                    Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true, null, r.width());

            layouts[i] = layout;
        }
        layout.getPaint().setAlpha(mEventsAlpha);
        return layout;
    }


    private void drawEvents(int date, int dayIndex, int top, Canvas canvas,
                            Paint p) {
        Paint eventTextPaint = mEventTextPaint;
        int left = computeDayLeftPosition(dayIndex) + 1;
        int cellWidth = computeDayLeftPosition(dayIndex + 1) - left + 1;
        int cellHeight = mCellHeight;

        // Use the selected hour as the selection region
        // Rect selectionArea = mSelectionRect;
        // selectionArea.top = top + mSelectionHour * (cellHeight + HOUR_GAP);
        // selectionArea.bottom = selectionArea.top + cellHeight;
        // selectionArea.left = left;
        // selectionArea.right = selectionArea.left + cellWidth;

        final ArrayList<Event> events = mEvents;
        int numEvents = events.size();
        EventGeometry geometry = mEventGeometry;

        final int viewEndY = mViewStartY + mViewHeight - DAY_HEADER_HEIGHT;

        int alpha = eventTextPaint.getAlpha();
        eventTextPaint.setAlpha(mEventsAlpha);
        for (int i = 0; i < numEvents; i++) {
            Event event = events.get(i);
            if (!geometry.computeEventRect(date, left, top, cellWidth, event)) {
                continue;
            }

            // Don't draw it if it is not visible
            if (event.bottom < mViewStartY || event.top > viewEndY) {
                continue;
            }

            // if (date == mSelectionDay && mComputeSelectedEvents
            // && geometry.eventIntersectsSelection(event, selectionArea)) {
            // mSelectedEvents.add(event);
            // }

            Rect r = drawEventRect(event, canvas, p, eventTextPaint,
                    mViewStartY, viewEndY);
            setupTextRect(r);

            // Don't draw text if it is not visible
            if (r.top > viewEndY || r.bottom < mViewStartY) {
                continue;
            }
            StaticLayout layout = getEventLayout(mLayouts, i, event,
                    eventTextPaint, r);
            // TODO: not sure why we are 4 pixels off
            drawEventText(layout, r, canvas, mViewStartY + 4, mViewStartY
                    + mViewHeight - DAY_HEADER_HEIGHT, false);
        }
        eventTextPaint.setAlpha(alpha);

        if (date == mSelectionDay && isFocused()
                && mSelectionMode != SELECTION_HIDDEN) {
            computeNeighbors();
        }
    }


    private void computeNeighbors() {
        // TODO Auto-generated method stub

    }


    private Rect drawEventRect(Event event, Canvas canvas, Paint p,
                               Paint eventTextPaint, int visibleTop, int visibleBot) {
        // Draw the Event Rect
        Rect r = mRect;
        r.top = Math.max((int) event.top + EVENT_RECT_TOP_MARGIN, visibleTop);
        r.bottom = Math.min((int) event.bottom - EVENT_RECT_BOTTOM_MARGIN,
                visibleBot);
        r.left = (int) event.left + EVENT_RECT_LEFT_MARGIN;
        r.right = (int) event.right;

        int color;
        if (event == mClickedEvent) {
            color = mClickedColor;
        } else {
            color = event.color;
        }

        p.setStyle(Style.FILL_AND_STROKE);
        p.setAntiAlias(false);

        int floorHalfStroke = (int) Math.floor(EVENT_RECT_STROKE_WIDTH / 2.0f);
        int ceilHalfStroke = (int) Math.ceil(EVENT_RECT_STROKE_WIDTH / 2.0f);
        r.top = Math.max((int) event.top + EVENT_RECT_TOP_MARGIN
                + floorHalfStroke, visibleTop);
        r.bottom = Math.min((int) event.bottom - EVENT_RECT_BOTTOM_MARGIN
                - ceilHalfStroke, visibleBot);
        r.left += floorHalfStroke;
        r.right -= ceilHalfStroke;
        p.setStrokeWidth(EVENT_RECT_STROKE_WIDTH);
        p.setColor(color);
        int alpha = p.getAlpha();
        p.setAlpha(mEventsAlpha);
        canvas.drawRect(r, p);
        p.setAlpha(alpha);
        p.setStyle(Style.FILL);

        // If this event is selected, then use the selection color
        if (mSelectedEvent == event && mClickedEvent != null) {
            boolean paintIt = false;
            color = 0;
            if (mSelectionMode == SELECTION_PRESSED) {
                // Also, remember the last selected event that we drew
                mPrevSelectedEvent = event;
                color = mPressedColor;
                paintIt = true;
            } else if (mSelectionMode == SELECTION_SELECTED) {
                // Also, remember the last selected event that we drew
                mPrevSelectedEvent = event;
                color = mPressedColor;
                paintIt = true;
            }

            if (paintIt) {
                p.setColor(color);
                canvas.drawRect(r, p);
            }
            p.setAntiAlias(true);
        }

        // Draw cal color square border
        // r.top = (int) event.top + CALENDAR_COLOR_SQUARE_V_OFFSET;
        // r.left = (int) event.left + CALENDAR_COLOR_SQUARE_H_OFFSET;
        // r.bottom = r.top + CALENDAR_COLOR_SQUARE_SIZE + 1;
        // r.right = r.left + CALENDAR_COLOR_SQUARE_SIZE + 1;
        // p.setColor(0xFFFFFFFF);
        // canvas.drawRect(r, p);

        // Draw cal color
        // r.top++;
        // r.left++;
        // r.bottom--;
        // r.right--;
        // p.setColor(event.color);
        // canvas.drawRect(r, p);

        // Setup rect for drawEventText which follows
        r.top = (int) event.top + EVENT_RECT_TOP_MARGIN;
        r.bottom = (int) event.bottom - EVENT_RECT_BOTTOM_MARGIN;
        r.left = (int) event.left + EVENT_RECT_LEFT_MARGIN;
        r.right = (int) event.right - EVENT_RECT_RIGHT_MARGIN;
        return r;
    }


    // Sanitize a string before passing it to drawText or else we get little
    // squares. For newlines and tabs before a comma, delete the character.
    // Otherwise, just replace them with a space.
    private String drawTextSanitizer(String string, int maxEventTextLen) {
        Matcher m = drawTextSanitizerFilter.matcher(string);
        string = m.replaceAll(",");

        int len = string.length();
        if (maxEventTextLen <= 0) {
            string = "";
            len = 0;
        } else if (len > maxEventTextLen) {
            string = string.substring(0, maxEventTextLen);
            len = maxEventTextLen;
        }

        return string.replace('\n', ' ');
    }


    private void drawEventText(StaticLayout eventLayout, Rect rect,
                               Canvas canvas, int top, int bottom, boolean center) {
        // drawEmptyRect(canvas, rect, 0xFFFF00FF); // for debugging

        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;

        // If the rectangle is too small for text, then return
        if (eventLayout == null || width < MIN_CELL_WIDTH_FOR_TEXT
                || height < MIN_CELL_HEIGHT_FOR_TEXT) {
            return;
        }

        int totalLineHeight = 0;
        int lineCount = eventLayout.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            int lineBottom = eventLayout.getLineBottom(i);
            if (lineBottom <= height) {
                totalLineHeight = lineBottom;
            } else {
                break;
            }
        }

        // + 2 is small workaround when the font is slightly bigger then the
        // rect. This will
        // still allow the text to be shown without overflowing into the other
        // all day rects.
        if (totalLineHeight == 0 || rect.top > bottom
                || rect.top + totalLineHeight + 2 < top) {
            return;
        }

        // Use a StaticLayout to format the string.
        canvas.save();
        // canvas.translate(rect.left, rect.top + (rect.bottom - rect.top / 2));
        int padding = center ? (rect.bottom - rect.top - totalLineHeight) / 2
                : 0;
        canvas.translate(rect.left, rect.top + padding);
        rect.left = 0;
        rect.right = width;
        rect.top = 0;
        rect.bottom = totalLineHeight;

        // There's a bug somewhere. If this rect is outside of a previous
        // cliprect, this becomes a no-op. What happens is that the text draw
        // past the event rect. The current fix is to not draw the staticLayout
        // at all if it is completely out of bound.
        canvas.clipRect(rect);
        eventLayout.draw(canvas);
        canvas.restore();
    }

    private void updateEventDetails() {
        // TODO Auto-generated method stub
    }

    private void doDown(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_DOWN;
        mViewStartX = 0;
        mOnFlingCalled = false;
        mHandler.removeCallbacks(mContinueScroll);
        int x = (int) ev.getX();
        int y = (int) ev.getY();

        // Save selection information: we use setSelectionFromPosition to find the selected event
        // in order to show the "clicked" color. But since it is also setting the selected info
        // for new events, we need to restore the old info after calling the function.
        Event oldSelectedEvent = mSelectedEvent;
        int oldSelectionDay = mSelectionDay;
        int oldSelectionHour = mSelectionHour;
        if (setSelectionFromPosition(x, y, false)) {
            // If a time was selected (a blue selection box is visible) and the click location
            // is in the selected time, do not show a click on an event to prevent a situation
            // of both a selection and an event are clicked when they overlap.
            boolean pressedSelected = (mSelectionMode != SELECTION_HIDDEN)
                    && oldSelectionDay == mSelectionDay
                    && oldSelectionHour == mSelectionHour;
            if (!pressedSelected) {// && mSelectedEvent != null) {
                // mSavedClickedEvent = mSelectedEvent;
                mDownTouchTime = System.currentTimeMillis();
                postDelayed(mSetClick, mOnDownDelay);
            } else {
                // eventClickCleanup();
            }
        }
        mSelectedEvent = oldSelectedEvent;
        mSelectionDay = oldSelectionDay;
        mSelectionHour = oldSelectionHour;
        invalidate();
    }

    public void doSingleTapUp(MotionEvent ev) {
        // TODO Auto-generated method stub

    }

    private void doLongPress(MotionEvent ev) {
        // // eventClickCleanup();
        // if (mScrolling) {
        // return;
        // }
        //
        // // // Scale gesture in progress
        // // if (mStartingSpanY != 0) {
        // // return;
        // // }
        //
        // int x = (int) ev.getX();
        // int y = (int) ev.getY();
        //
        // boolean validPosition = setSelectionFromPosition(x, y, false);
        // if (!validPosition) {
        // // return if the touch wasn't on an area of concern
        // return;
        // }
        //
        // mSelectionMode = SELECTION_LONGPRESS;
        // invalidate();
        // performLongClick();
    }

    private void doScroll(MotionEvent e1, MotionEvent e2, float deltaX,
                          float deltaY) {
        cancelAnimation();
        if (mStartingScroll) {
            mInitialScrollX = 0;
            mInitialScrollY = 0;
            mStartingScroll = false;
        }

        mInitialScrollX += deltaX;
        mInitialScrollY += deltaY;
        int distanceX = (int) mInitialScrollX;
        int distanceY = (int) mInitialScrollY;

        final float focusY = getAverageY(e2);
        if (mRecalCenterHour) {
            // Calculate the hour that correspond to the average of the Y touch points
            mGestureCenterHour = (mViewStartY + focusY - DAY_HEADER_HEIGHT)
                    / (mCellHeight + DAY_GAP);
            mRecalCenterHour = false;
        }

        // If we haven't figured out the predominant scroll direction yet,
        // then do it now.
        if (mTouchMode == TOUCH_MODE_DOWN) {
            int absDistanceX = Math.abs(distanceX);
            int absDistanceY = Math.abs(distanceY);
            mScrollStartY = mViewStartY;
            mPreviousDirection = 0;

            if (absDistanceX > absDistanceY) {
                int slopFactor = mScaleGestureDetector.isInProgress() ? 20 : 2;
                if (absDistanceX > mScaledPagingTouchSlop * slopFactor) {
                    mTouchMode = TOUCH_MODE_HSCROLL;
                    mViewStartX = distanceX;
                    initNextView(-mViewStartX);
                }
            } else {
                mTouchMode = TOUCH_MODE_VSCROLL;
            }
        } else if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
            // We are already scrolling horizontally, so check if we
            // changed the direction of scrolling so that the other week
            // is now visible.
            mViewStartX = distanceX;
            if (distanceX != 0) {
                int direction = (distanceX > 0) ? 1 : -1;
                if (direction != mPreviousDirection) {
                    // The user has switched the direction of scrolling
                    // so re-init the next view
                    initNextView(-mViewStartX);
                    mPreviousDirection = direction;
                }
            }
        }

        if ((mTouchMode & TOUCH_MODE_VSCROLL) != 0) {
            // Calculate the top of the visible region in the calendar grid.
            // Increasing/decrease this will scroll the calendar grid up/down.
            mViewStartY = (int) ((mGestureCenterHour * (mCellHeight + DAY_GAP))
                    - focusY + DAY_HEADER_HEIGHT);

            // If dragging while already at the end, do a glow
            final int pulledToY = (int) (mScrollStartY + deltaY);
            if (pulledToY < 0) {
                mEdgeEffectTop.onPull(deltaY / mViewHeight);
                if (!mEdgeEffectBottom.isFinished()) {
                    mEdgeEffectBottom.onRelease();
                }
            } else if (pulledToY > mMaxViewStartY) {
                mEdgeEffectBottom.onPull(deltaY / mViewHeight);
                if (!mEdgeEffectTop.isFinished()) {
                    mEdgeEffectTop.onRelease();
                }
            }

            if (mViewStartY < 0) {
                mViewStartY = 0;
                mRecalCenterHour = true;
            } else if (mViewStartY > mMaxViewStartY) {
                mViewStartY = mMaxViewStartY;
                mRecalCenterHour = true;
            }
            if (mRecalCenterHour) {
                // Calculate the hour that correspond to the average of the Y
                // touch points
                mGestureCenterHour = (mViewStartY + focusY - DAY_HEADER_HEIGHT)
                        / (mCellHeight + DAY_GAP);
                mRecalCenterHour = false;
            }
            computeFirstHour();
        }

        mScrolling = true;

        mSelectionMode = SELECTION_HIDDEN;
        invalidate();
    }

    private float getAverageY(MotionEvent me) {
        int count = me.getPointerCount();
        float focusY = 0;
        for (int i = 0; i < count; i++) {
            focusY += me.getY(i);
        }
        focusY /= count;
        return focusY;
    }

    private void cancelAnimation() {
        Animation in = mViewSwitcher.getInAnimation();
        if (in != null) {
            // cancel() doesn't terminate cleanly.
            in.scaleCurrentDuration(0);
        }
        Animation out = mViewSwitcher.getOutAnimation();
        if (out != null) {
            // cancel() doesn't terminate cleanly.
            out.scaleCurrentDuration(0);
        }
    }

    private void doFling(MotionEvent e1, MotionEvent e2, float velocityX,
                         float velocityY) {
        cancelAnimation();

        mSelectionMode = SELECTION_HIDDEN;
        // TODO eventClickCleanup();

        mOnFlingCalled = true;

        if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
            // Horizontal fling.
            mTouchMode = TOUCH_MODE_INITIAL_STATE;
            if (DEBUG)
                Log.d(TAG, "doFling: velocityX " + velocityX);
            int deltaX = (int) e2.getX() - (int) e1.getX();
            initNextView(deltaX);
            switchViews(deltaX < 0, mViewStartX, mViewWidth, velocityX);
            mViewStartX = 0;
            return;
        }

        if ((mTouchMode & TOUCH_MODE_VSCROLL) == 0) {
            if (DEBUG)
                Log.d(TAG, "doFling: no fling");
            return;
        }

        // Vertical fling.
        mTouchMode = TOUCH_MODE_INITIAL_STATE;
        mViewStartX = 0;

        if (DEBUG) {
            Log.d(TAG, "doFling: mViewStartY" + mViewStartY + " velocityY "
                    + velocityY);
        }

        // Continue scrolling vertically
        mScrolling = true;
        mScroller.fling(0 /* startX */, mViewStartY /* startY */,
                0 /* velocityX */, (int) -velocityY, 0 /* minX */, 0 /* maxX */,
                0 /* minY */, mMaxViewStartY /* maxY */, OVERFLING_DISTANCE,
                OVERFLING_DISTANCE);

        // When flinging down, show a glow when it hits the end only if it
        // wasn't started at the top
        if (velocityY > 0 && mViewStartY != 0) {
            mCallEdgeEffectOnAbsorb = true;
        }
        // When flinging up, show a glow when it hits the end only if it wasn't
        // started at the bottom
        else if (velocityY < 0 && mViewStartY != mMaxViewStartY) {
            mCallEdgeEffectOnAbsorb = true;
        }
        mHandler.post(mContinueScroll);
    }

    private boolean initNextView(int deltaX) {
        // Change the view to the previous day or week
        DayView view = (DayView) mViewSwitcher.getNextView();
        Time date = view.mBaseDate;
        date.set(mBaseDate);
        boolean switchForward;
        if (deltaX > 0) {
            date.monthDay -= mNumDays;
            view.setSelectedDay(mSelectionDay - mNumDays);
            switchForward = false;
        } else {
            date.monthDay += mNumDays;
            view.setSelectedDay(mSelectionDay + mNumDays);
            switchForward = true;
        }
        date.normalize(true /* ignore isDst */);
        initView(view);
        view.layout(getLeft(), getTop(), getRight(), getBottom());
        view.reloadEvents();
        return switchForward;
    }

    // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mHandleActionUp = false;
        float gestureCenterInPixels = detector.getFocusY() - DAY_HEADER_HEIGHT;
        mGestureCenterHour = (mViewStartY + gestureCenterInPixels) / (mCellHeight + DAY_GAP);

        mStartingSpanY = Math.max(MIN_Y_SPAN, Math.abs(detector.getCurrentSpanY()));
        mCellHeightBeforeScaleGesture = mCellHeight;

        if (DEBUG_SCALING) {
            float ViewStartHour = mViewStartY / (float) (mCellHeight + DAY_GAP);
            Log.d(TAG, "onScaleBegin: mGestureCenterHour:" + mGestureCenterHour
                    + "\tViewStartHour: " + ViewStartHour + "\tmViewStartY:" + mViewStartY
                    + "\tmCellHeight:" + mCellHeight + " SpanY:" + detector.getCurrentSpanY());
    }

        return true;
    }

    // ScaleGestureDetector.OnScaleGestureListener
    public boolean onScale(ScaleGestureDetector detector) {
        float spanY = Math.max(MIN_Y_SPAN, Math.abs(detector.getCurrentSpanY()));

        mCellHeight = (int) (mCellHeightBeforeScaleGesture * spanY / mStartingSpanY);

        if (mCellHeight < mMinCellHeight) {
            // If mStartingSpanY is too small, even a small increase in the
            // gesture can bump the mCellHeight beyond MAX_CELL_HEIGHT
            mStartingSpanY = spanY;
            mCellHeight = mMinCellHeight;
            mCellHeightBeforeScaleGesture = mMinCellHeight;
        } else if (mCellHeight > MAX_CELL_HEIGHT) {
            mStartingSpanY = spanY;
            mCellHeight = MAX_CELL_HEIGHT;
            mCellHeightBeforeScaleGesture = MAX_CELL_HEIGHT;
    }

        int gestureCenterInPixels = (int) detector.getFocusY() - DAY_HEADER_HEIGHT;
        mViewStartY = (int) (mGestureCenterHour * (mCellHeight + DAY_GAP)) - gestureCenterInPixels;
        mMaxViewStartY = HOUR_GAP + 24 * (mCellHeight + HOUR_GAP) - mGridAreaHeight;

        if (DEBUG_SCALING) {
            float ViewStartHour = mViewStartY / (float) (mCellHeight + DAY_GAP);
            Log.d(TAG, "onScale: mGestureCenterHour:" + mGestureCenterHour + "\tViewStartHour: "
                    + ViewStartHour + "\tmViewStartY:" + mViewStartY + "\tmCellHeight:"
                    + mCellHeight + " SpanY:" + detector.getCurrentSpanY());
        }

        if (mViewStartY < 0) {
            mViewStartY = 0;
            mGestureCenterHour = (mViewStartY + gestureCenterInPixels)
                    / (float) (mCellHeight + DAY_GAP);
        } else if (mViewStartY > mMaxViewStartY) {
            mViewStartY = mMaxViewStartY;
            mGestureCenterHour = (mViewStartY + gestureCenterInPixels)
                    / (float) (mCellHeight + DAY_GAP);
        }
        computeFirstHour();

        mRemeasure = true;
        invalidate();
        return true;
    }

    // ScaleGestureDetector.OnScaleGestureListener
    public void onScaleEnd(ScaleGestureDetector detector) {
        mScrollStartY = mViewStartY;
        mInitialScrollY = 0;
        mInitialScrollX = 0;
        mStartingSpanY = 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (DEBUG)
            Log.e(TAG,
                    "" + action + " ev.getPointerCount() = "
                            + ev.getPointerCount()
            );

        if ((ev.getActionMasked() == MotionEvent.ACTION_DOWN)
                || (ev.getActionMasked() == MotionEvent.ACTION_UP)
                || (ev.getActionMasked() == MotionEvent.ACTION_POINTER_UP)
                || (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN)) {
            mRecalCenterHour = true;
        }

        if ((mTouchMode & TOUCH_MODE_HSCROLL) == 0) {
            mScaleGestureDetector.onTouchEvent(ev);
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mStartingScroll = true;
                if (DEBUG) {
                    Log.e(TAG, "ACTION_DOWN ev.getDownTime = " + ev.getDownTime()
                            + " Cnt=" + ev.getPointerCount());
                }

                // int bottom = DAY_HEADER_HEIGHT;
                mHandleActionUp = true;
                mGestureDetector.onTouchEvent(ev);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (DEBUG)
                    Log.e(TAG, "ACTION_MOVE Cnt=" + ev.getPointerCount()
                            + DayView.this);
                mGestureDetector.onTouchEvent(ev);
                return true;

            case MotionEvent.ACTION_UP:
                if (DEBUG)
                    Log.e(TAG, "ACTION_UP Cnt=" + ev.getPointerCount()
                            + mHandleActionUp);
                mEdgeEffectTop.onRelease();
                mEdgeEffectBottom.onRelease();
                mStartingScroll = false;
                mGestureDetector.onTouchEvent(ev);
                if (!mHandleActionUp) {
                    mHandleActionUp = true;
                    mViewStartX = 0;
                    invalidate();
                    return true;
                }

                if (mOnFlingCalled) {
                    return true;
                }

                // If we were scrolling, then reset the selected hour so that it
                // is visible.
                if (mScrolling) {
                    mScrolling = false;
                    resetSelectedHour();
                    invalidate();
                }

                if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
                    mTouchMode = TOUCH_MODE_INITIAL_STATE;
                    if (Math.abs(mViewStartX) > mHorizontalSnapBackThreshold) {
                        // The user has gone beyond the threshold so switch views
                        if (DEBUG)
                            Log.d(TAG, "- horizontal scroll: switch views");
                        switchViews(mViewStartX > 0, mViewStartX, mViewWidth, 0);
                        mViewStartX = 0;
                        return true;
                    } else {
                        // Not beyond the threshold so invalidate which will cause
                        // the view to snap back. Also call recalc() to ensure
                        // that we have the correct starting date and title.
                        if (DEBUG)
                            Log.d(TAG, "- horizontal scroll: snap back");
                        recalc();
                        invalidate();
                        mViewStartX = 0;
                    }
                }

                return true;

            // This case isn't expected to happen.
            case MotionEvent.ACTION_CANCEL:
                if (DEBUG)
                    Log.e(TAG, "ACTION_CANCEL");
                mGestureDetector.onTouchEvent(ev);
                mScrolling = false;
                resetSelectedHour();
                return true;

            default:
                if (DEBUG)
                    Log.e(TAG, "Not MotionEvent " + ev.toString());
                if (mGestureDetector.onTouchEvent(ev)) {
                    return true;
                }
                return super.onTouchEvent(ev);
        }
    }

    // long press menu, create view edit event?
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenuInfo menuInfo) {
        // MenuItem item;
        //
        // final long startMillis = getSelectedTimeInMillis();
        // int flags = DateUtils.FORMAT_SHOW_TIME |
        // DateUtils.FORMAT_SHOW_WEEKDAY;
        // final String title = DateUtils.formatDateRange(mContext, startMillis,
        // startMillis, flags).toString();
        // menu.setHeaderTitle(title);
        // //
        // // int numSelectedEvents = mSelectedEvents.size();
        // //
        // // if (numSelectedEvents >= 1) {
        // // item = menu.add(0, MENU_EVENT_VIEW, 0, R.string.event_view);
        // // item.setOnMenuItemClickListener(mContextMenuHandler);
        // // item.setIcon(android.R.drawable.ic_menu_info_details);
        // // }
        // //
        // mPopup.dismiss();
    }

    /**
     * Sets mSelectionDay and mSelectionHour based on the (x,y) touch position.
     * If the touch position is not within the displayed grid, then this
     * method returns false.
     *
     * @param x                the x position of the touch
     * @param y                the y position of the touch
     * @param keepOldSelection - do not change the selection info (used for invoking accessibility
     *                         messages)
     * @return true if the touch position is valid
     */
    private boolean setSelectionFromPosition(int x, final int y,
                                             boolean keepOldSelection) {

        Event savedEvent = null;
        int savedDay = 0;
        int savedHour = 0;
        if (keepOldSelection) {
            // Store selection info and restore it at the end. This way, we can invoke the
            // right accessibility message without affecting the selection.
            savedEvent = mSelectedEvent;
            savedDay = mSelectionDay;
            savedHour = mSelectionHour;
        }
        if (x < mHoursWidth) {
            x = mHoursWidth;
        }

        int day = (x - mHoursWidth) / (mCellWidth + DAY_GAP);
        if (day >= mNumDays) {
            day = mNumDays - 1;
        }
        day += mFirstJulianDay;

        if (y < DAY_HEADER_HEIGHT) {
            return false;
        }

        int adjustedY = y - mFirstCell;

        // TODO findSelectedEvent(x, y);

        // Log.i("Cal", "setSelectionFromPosition( " + x + ", " + y + " ) day: "
        // + day + " hour: "
        // + mSelectionHour + " mFirstCell: " + mFirstCell +
        // " mFirstHourOffset: "
        // + mFirstHourOffset);
        // if (mSelectedEvent != null) {
        // Log.i("Cal", "  num events: " + mSelectedEvents.size() + " event: "
        // + mSelectedEvent.title);
        // for (Event ev : mSelectedEvents) {
        // int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL
        // | DateUtils.FORMAT_CAP_NOON_MIDNIGHT;
        // String timeRange = formatDateRange(mContext, ev.startMillis,
        // ev.endMillis, flags);
        //
        // Log.i("Cal", "  " + timeRange + " " + ev.title);
        // }
        // }

        // Restore old values
        if (keepOldSelection) {
            mSelectedEvent = savedEvent;
            mSelectionDay = savedDay;
            mSelectionHour = savedHour;
        }
        return true;
    }


    /**
     * Cleanup the pop-up and timers.
     */
    public void cleanup() {
        // Protect against null-pointer exceptions
        if (mPopup != null) {
            mPopup.dismiss();
        }
        mPaused = true;
        // mLastPopupEventID = INVALID_EVENT_ID;
        if (mHandler != null) {
            // mHandler.removeCallbacks(mDismissPopup);
            mHandler.removeCallbacks(mUpdateCurrentTime);
        }

        // Utils.setSharedPreference(mContext,
        // GeneralPreferences.KEY_DEFAULT_CELL_HEIGHT,
        // mCellHeight);
        // Clear all click animations
        // eventClickCleanup();
        // Turn off redraw
        mRemeasure = false;
        // Turn off scrolling to make sure the view is in the correct state if we fling back to it
        mScrolling = false;
    }

    private void setSelectedHour(int h) {
        mSelectionHour = h;
    }

    /**
     * Restart the update timer
     */
    public void restartCurrentTimeUpdates() {
        mPaused = false;
        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateCurrentTime);
            mHandler.post(mUpdateCurrentTime);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cleanup();
        super.onDetachedFromWindow();
    }

    private long calculateDuration(float delta, float width, float velocity) {
        /*
		 * Here we compute a "distance" that will be used in the computation of
		 * the overall snap duration. This is a function of the actual distance
		 * that needs to be traveled; we keep this value close to half screen
		 * size in order to reduce the variance in snap duration as a function
		 * of the distance the page needs to travel.
		 */
        final float halfScreenSize = width / 2;
        float distanceRatio = delta / width;
        float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
        float distance = halfScreenSize + halfScreenSize
                * distanceInfluenceForSnapDuration;

        velocity = Math.abs(velocity);
        velocity = Math.max(MINIMUM_SNAP_VELOCITY, velocity);

		/*
		 * we want the page's snap velocity to approximately match the velocity
		 * at which the user flings, so we scale the duration by a value near to
		 * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
		 * make it a little slower.
		 */
        long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));
        if (DEBUG) {
            Log.e(TAG, "halfScreenSize:" + halfScreenSize + " delta:" + delta
                    + " distanceRatio:" + distanceRatio + " distance:"
                    + distance + " velocity:" + velocity + " duration:"
                    + duration + " distanceInfluenceForSnapDuration:"
                    + distanceInfluenceForSnapDuration);
        }
        return duration;
    }

    /*
     * We want the duration of the page snap animation to be influenced by the
     * distance that the screen has to travel, however, we don't want this
     * duration to be effected in a purely linear fashion. Instead, we use this
     * method to moderate the effect that the distance of travel has on the
     * overall snap duration.
     */
    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    class TodayAnimatorListener extends AnimatorListenerAdapter {
        private volatile Animator mAnimator = null;
        private volatile boolean mFadingIn = false;

        @Override
        public void onAnimationEnd(Animator animation) {
            synchronized (this) {
                if (mAnimator != animation) {
                    animation.removeAllListeners();
                    animation.cancel();
                    return;
                }
                if (mFadingIn) {
                    if (mTodayAnimator != null) {
                        mTodayAnimator.removeAllListeners();
                        mTodayAnimator.cancel();
                    }
                    mTodayAnimator = ObjectAnimator
                            .ofInt(DayView.this, "animateTodayAlpha", 255, 0);
                    mAnimator = mTodayAnimator;
                    mFadingIn = false;
                    mTodayAnimator.addListener(this);
                    mTodayAnimator.setDuration(600);
                    mTodayAnimator.start();
                } else {
                    mAnimateToday = false;
                    mAnimateTodayAlpha = 0;
                    mAnimator.removeAllListeners();
                    mAnimator = null;
                    mTodayAnimator = null;
                    invalidate();
                }
            }
        }

        public void setAnimator(Animator animation) {
            mAnimator = animation;
        }

        public void setFadingIn(boolean fadingIn) {
            mFadingIn = fadingIn;
        }

    }

    private class GotoBroadcaster implements Animation.AnimationListener {
        private final int mCounter;
        private final Time mStart;
        private final Time mEnd;

        public GotoBroadcaster(Time start, Time end) {
            mCounter = ++sCounter;
            mStart = start;
            mEnd = end;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            DayView view = (DayView) mViewSwitcher.getCurrentView();
            view.mViewStartX = 0;
            view = (DayView) mViewSwitcher.getNextView();
            view.mViewStartX = 0;

            // if (mCounter == sCounter) {
            // mController.sendEvent(this, EventType.GO_TO, mStart, mEnd, null,
            // -1,
            // ViewType.CURRENT, CalendarController.EXTRA_GOTO_DATE, null,
            // null);
            // }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }

    private class ContinueScroll implements Runnable {

        public void run() {
            mScrolling = mScrolling && mScroller.computeScrollOffset();
            if (!mScrolling || mPaused) {
                resetSelectedHour();
                invalidate();
                return;
            }

            mViewStartY = mScroller.getCurrY();

            if (mCallEdgeEffectOnAbsorb) {
                if (mViewStartY < 0) {
                    mEdgeEffectTop.onAbsorb((int) mLastVelocity);
                    mCallEdgeEffectOnAbsorb = false;
                } else if (mViewStartY > mMaxViewStartY) {
                    mEdgeEffectBottom.onAbsorb((int) mLastVelocity);
                    mCallEdgeEffectOnAbsorb = false;
                }
                mLastVelocity = mScroller.getCurrVelocity();
            }

            if (mScrollStartY == 0 || mScrollStartY == mMaxViewStartY) {
                // Allow overscroll/springback only on a fling,
                // not a pull/fling from the end
                if (mViewStartY < 0) {
                    mViewStartY = 0;
                } else if (mViewStartY > mMaxViewStartY) {
                    mViewStartY = mMaxViewStartY;
                }
            }

            computeFirstHour();
            mHandler.post(this);
            invalidate();
        }
    }

    class UpdateCurrentTime implements Runnable {

        public void run() {
            long currentTime = System.currentTimeMillis();
            mCurrentTime.set(currentTime);
            //% causes update to occur on 5 minute marks (11:10, 11:15, 11:20, etc.)
            if (!DayView.this.mPaused) {
                mHandler.postDelayed(mUpdateCurrentTime,
                        UPDATE_CURRENT_TIME_DELAY
                                - (currentTime % UPDATE_CURRENT_TIME_DELAY)
                );
            }
            mTodayJulianDay = Time.getJulianDay(currentTime,
                    mCurrentTime.gmtoff);
            invalidate();
        }
    }

    class CalendarGestureListener extends
            GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            if (DEBUG)
                Log.e(TAG, "GestureDetector.onSingleTapUp");
            DayView.this.doSingleTapUp(ev);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent ev) {
            if (DEBUG)
                Log.e(TAG, "GestureDetector.onLongPress");
            DayView.this.doLongPress(ev);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            if (DEBUG)
                Log.e(TAG, "GestureDetector.onScroll");
            eventClickCleanup();
            DayView.this.doScroll(e1, e2, distanceX, distanceY);
            return true;
        }

        private void eventClickCleanup() {
            // this.removeCallbacks(mClearClick);
            // this.removeCallbacks(mSetClick);
            // TODO mClickedEvent = null;
            // mSavedClickedEvent = null;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            if (DEBUG)
                Log.e(TAG, "GestureDetector.onFling");

            DayView.this.doFling(e1, e2, velocityX, velocityY);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            if (DEBUG)
                Log.e(TAG, "GestureDetector.onDown");
            DayView.this.doDown(ev);
            return true;
        }
    }

    private class ScrollInterpolator implements Interpolator {
        public ScrollInterpolator() {
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            t = t * t * t * t * t + 1;

            if ((1 - t) * mAnimationDistance < 1) {
                cancelAnimation();
            }

            return t;
        }
    }

    public void setSelected(Time time, boolean ignoreTime, boolean animateToday) {
        mBaseDate.set(time);
        setSelectedHour(mBaseDate.hour);
        setSelectedEvent(null);
        mPrevSelectedEvent = null;
        long millis = mBaseDate.toMillis(false /* use isDst */);
        setSelectedDay(Time.getJulianDay(millis, mBaseDate.gmtoff));
        mSelectedEvents.clear();
        mComputeSelectedEvents = true;

        int gotoY = Integer.MIN_VALUE;

        if (!ignoreTime && mGridAreaHeight != -1) {
            int lastHour = 0;

            if (mBaseDate.hour < mFirstHour) {
                // Above visible region
                gotoY = mBaseDate.hour * (mCellHeight + HOUR_GAP);
            } else {
                lastHour = (mGridAreaHeight) / (mCellHeight + HOUR_GAP)
                        + mFirstHour;

                if (mBaseDate.hour >= lastHour) {
                    // Below visible region

                    // target hour + 1 (to give it room to see the event) -
                    // grid height (to get the y of the top of the visible
                    // region)
                    gotoY = (int) ((mBaseDate.hour + 1 + mBaseDate.minute / 60.0f)
                            * (mCellHeight + HOUR_GAP) - mGridAreaHeight);
                }
            }

            if (DEBUG) {
                Log.e(TAG, "Go " + gotoY + " 1st " + mFirstHour + "CH "
                        + (mCellHeight + HOUR_GAP) + " lh " + lastHour + " gh " + mGridAreaHeight
                        + " ymax " + mMaxViewStartY);
            }

            if (gotoY > mMaxViewStartY) {
                gotoY = mMaxViewStartY;
            } else if (gotoY < 0 && gotoY != Integer.MIN_VALUE) {
                gotoY = 0;
            }
        }

        recalc();

        mRemeasure = true;
        invalidate();

        boolean delayAnimateToday = false;
        if (gotoY != Integer.MIN_VALUE) {
            ValueAnimator scrollAnim = ObjectAnimator.ofInt(this, "viewStartY", mViewStartY, gotoY);
            scrollAnim.setDuration(GOTO_SCROLL_DURATION);
            scrollAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            scrollAnim.addListener(mAnimatorListener);
            scrollAnim.start();
            delayAnimateToday = true;
        }
        if (animateToday) {
            synchronized (mTodayAnimatorListener) {
                if (mTodayAnimator != null) {
                    mTodayAnimator.removeAllListeners();
                    mTodayAnimator.cancel();
                }
                mTodayAnimator = ObjectAnimator.ofInt(this, "animateTodayAlpha",
                        mAnimateTodayAlpha, 255);
                mAnimateToday = true;
                mTodayAnimatorListener.setFadingIn(true);
                mTodayAnimatorListener.setAnimator(mTodayAnimator);
                mTodayAnimator.addListener(mTodayAnimatorListener);
                mTodayAnimator.setDuration(150);
                if (delayAnimateToday) {
                    mTodayAnimator.setStartDelay(GOTO_SCROLL_DURATION);
                }
                mTodayAnimator.start();
            }
        }
    }

	private void setSelectedDay(int d) {
        mSelectionDay = d;
    }
	
	AnimatorListenerAdapter mAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
            mScrolling = true;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mScrolling = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mScrolling = false;
            resetSelectedHour();
            invalidate();
        }
    };
}
