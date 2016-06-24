package com.prismsoftworks.widgetgrid.main.adapter;

import android.appwidget.AppWidgetProviderInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.Toast;

import com.prismsoftworks.widgetgrid.main.object.ObjectInflater;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;


import java.util.ArrayList;
import java.util.List;

/**
 * This is the recyclerview widgetBuilder for the Widget fragment. This class - along with the other <br/>
 * widgetBuilder classes - will handle the populating and assigning listeners to all objects on the <br/>
 * activity/main screen.
 * <p/>
 * Created by jamesl on 6/12/2015.
 */
public class WidgetBuilder {
    private static final String TAG = WidgetBuilder.class.getSimpleName();

    //data passed in via constructor
    public static List<ObjectInflater> items;

    //Handle widgets with multiple controllers.
    public static int innerSelectedIndex = 0;

    //List for hostview's children
    public static List<View> hostViewGrandChildren;

    //this probably doesn't need to be static. This is essentially rowspan for all items.
//    public static int fixedHeight = 297;//todo: 10/2/2015 make this dynamic based on height

    //We aren't doing anything with this list. Consider the possible utility for this.
    public static List<WidgetsViewHolder> allViewHolders = null;

    //reference to the Launcher Activity. This might be unnecessary.
    private static LauncherActivity mainAct;
    private static List<View> innerFocusables = null;
    private static Drawable selector[] = {null, null, null};
    private static ImageView cursorGhost = null;

    public WidgetBuilder(List<ObjectInflater> items, LauncherActivity mainAct) {
        WidgetBuilder.items = items;
        WidgetBuilder.mainAct = mainAct;
        if (allViewHolders == null) {
            allViewHolders = new ArrayList<>();
        } else {
            allViewHolders.clear();
        }
    }

    public View[] getItem(int index){
        RelativeLayout container  = allViewHolders.get(index).container;
        ViewGroup par = (ViewGroup) container.getParent();
        if(par != null) {
            par.removeView(container);
        }

        View[] res = new View[] { container, allViewHolders.get(index).btnWorkAround};
        return res;
    }

    public int getItemLength(){
        return allViewHolders.size();
    }

    /**
     * This is where we get the viewholder and pass it the data from our items list.
     *
     * @param parent - the parent/gridview
     */
    public void bindViewHolders(final ViewGroup parent) {
        Log.i(TAG, "items size: " + items.size());
        for(int i=0; i < items.size(); i++) {
            final int index = i;
            View itemView = LayoutInflater.from(parent.getContext()).inflate
                    (R.layout.widget_card_layout, parent, false);

            final WidgetsViewHolder holder = new WidgetsViewHolder(itemView);

            final ObjectInflater widget = items.get(index);

            if (widget == null) {
                ImageView img = new ImageView(mainAct);
                img.setImageResource(R.drawable.widget_temp_grid);
                img.setId(R.id.NULL_GRID_ID);
                img.setScaleType(ImageView.ScaleType.FIT_XY);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                img.setLayoutParams(lp);
                holder.container.setFocusable(false);
                holder.btnWorkAround = null;
                holder.container.removeAllViews();
                if (WidgetsFragment.gridMode) {
                    holder.container.addView(img);
                }

                Space spacer = new Space(mainAct);
                holder.container.addView(spacer);
                allViewHolders.add(holder);
                continue;
            }

            try {
                ViewGroup vg = (ViewGroup) widget.hostView.getParent();
                vg.removeAllViews();
            } catch (Exception e) {
                //just ignore the stupid widget crap. no idea why this keeps happening
            }
            holder.hostLayout.addView(widget.hostView);

            holder.btnWorkAround.bringToFront();
            holder.btnWorkAround.getParent().requestDisallowInterceptTouchEvent(true);
//        ((ViewParent)holder.btnWorkAround).requestDisallowInterceptTouchEvent(true);
            holder.btnWorkAround.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    final float xPos = event.getRawX();
                    final float yPos = event.getRawY();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN://todo: make this a long click
                            Log.i(TAG, "down coords: " + xPos + " | " + yPos);
                            WidgetsFragment.selectedIndex = getWidgetIndex(view);
                            break;
                        case MotionEvent.ACTION_MOVE:
//                        Log.i(TAG, "dragging coords: " + xPos + " | " + yPos);
                            if (!WidgetsFragment.inWidgetDragMode) {
                                Log.i(TAG, "generating bitmap...");
                                WidgetsFragment.inWidgetDragMode = true;
                                cursorGhost = new ImageView(mainAct);
                                cursorGhost.setBackground(snapView((View) holder.btnWorkAround.getParent()));
                                LauncherActivity.superOverlay.addView(cursorGhost);
                                cursorGhost.bringToFront();
                            }

                            break;
                        case MotionEvent.ACTION_HOVER_ENTER:
                        case MotionEvent.ACTION_HOVER_MOVE:
                            Log.i(TAG, "hover coords: " + xPos + " | " + yPos);
                            break;
                        case MotionEvent.ACTION_UP:
                            Log.i(TAG, "up coords: " + xPos + " | " + yPos);
                            if (WidgetsFragment.inWidgetDragMode) {
                                WidgetsFragment.inWidgetDragMode = false;
                                LauncherActivity.superOverlay.removeView(cursorGhost);
                                cursorGhost = null;
                                if (WidgetsFragment.dragColumn != null && WidgetsFragment.dragRow != null) {
                                    int targetCell = WidgetsFragment.dragColumn + (
                                            WidgetsFragment.dragRow * WidgetsFragment.MAX_COLUMNS);
                                    WidgetsFragment.getInstance().dragMove(targetCell);
                                    WidgetsFragment.dragColumn = null;
                                    WidgetsFragment.dragRow = null;
                                }
                            } else {
                                return false;//dispatch click event?
                            }

                            break;
                        default:
//                        Log.i(TAG, "default coords: " + xPos + " | " + yPos + "\nAction Value: " + event.getAction());

                    }

                    if (WidgetsFragment.inWidgetDragMode) {
                        //move the ghostly figure with the cursor
                        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) cursorGhost.getLayoutParams();
                        lp.leftMargin = (int) xPos;// (xPos - (cursorGhost.getWidth() / 2));
                        lp.topMargin = (int) (yPos - (cursorGhost.getHeight() / 2));
                        cursorGhost.setLayoutParams(lp);

                        //todo: highlight null views in WidgetGrid
                        WidgetsFragment.highlightNulls(xPos, yPos);
                    }

                    return true;
                }
            });

            //Hover listener for work around button
            holder.btnWorkAround.setOnHoverListener(new View.OnHoverListener() {
                @Override
                public boolean onHover(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_HOVER_ENTER:
//                        holder.btnWorkAround.setLongClickable(false);//why were we setting this ?
                            holder.container.setBackgroundColor(0x6339538D);
                            holder.btnRemove.setImageDrawable(v.getResources().getDrawable(R.drawable
                                    .remove_button_selected));
                            WidgetsFragment.selectedIndex = getWidgetIndex(v);
                            Log.e("Index stuff:", "selected index: " + WidgetsFragment.selectedIndex +
                                    "\nindex Parameter: " + index);
                            break;
                        case MotionEvent.ACTION_HOVER_EXIT:
                            holder.btnRemove.setImageDrawable(v.getResources().getDrawable(R.drawable
                                    .remove_button));
                            holder.container.setBackgroundColor(0x00FFFFFF);
                            WidgetsFragment.selectedIndex = 0;
                            break;
                    }
                    return false;
                }
            });

            holder.btnWorkAround.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (LauncherActivity.inWidgetEditMode)
                        return;

                    if (LauncherActivity.inInnerWidgetSelectMode) {
                        executeInnerSelected();
                        return;
                    }

                    //I honestly think we should go by the boolean variable remove mode, and not the
                    // button's visibility for this statement.
                    if (holder.btnRemove.getVisibility() == View.VISIBLE) {
                        holder.btnRemove.setImageDrawable(view.getResources().getDrawable(R.drawable
                                .remove_button_clicked));
                        WidgetsFragment.removeWidget(WidgetsFragment.selectedIndex, 'x');
                        WidgetsFragment.btnOptionsMenu.setNextFocusLeftId(R.id.btn_recent_apps_menu);
                    } else {
                        if (widget.hostView.getChildAt(0).hasOnClickListeners()) {
                            widget.hostView.getChildAt(0).performClick();
                            return;
                        }
                        //if hostview's child has over 2 children, we are assuming they are components
                        if (((ViewGroup) widget.hostView.getChildAt(0)).getChildCount() > 1) {
                            ViewGroup hostViewChild = (ViewGroup) widget.hostView.getChildAt(0);
                            hostViewGrandChildren = new ArrayList<>();
                            Log.e(TAG, "hostview child 0 name: " + hostViewChild.getClass().getSimpleName() + " #: " + hostViewChild.getChildCount());
                            for (int i = 0; i < hostViewChild.getChildCount(); i++) {
                                Log.e(TAG, "child name: " + hostViewChild.getChildAt(i).getClass().getSimpleName() + " added");
                                hostViewGrandChildren.add(hostViewChild.getChildAt(i));
                            }

                            LauncherActivity.inInnerWidgetSelectMode = true;
                            mainAct.changeAllFocusables(false, 'i');
                            widgetComponentFocus();
                        } else {
                            //if no conditions are met, just emulate a touch down/touch up
                            MotionEvent touchDown, touchUp;
                            long downTime = SystemClock.uptimeMillis();
                            long eventTime = SystemClock.uptimeMillis() + 200;
                            float xPos = 0;
                            float yPos = 0;
                            int metaState = 0;
                            touchDown = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN,
                                    xPos, yPos, metaState);
                            touchUp = MotionEvent.obtain(eventTime + 50, eventTime + 150, MotionEvent
                                    .ACTION_UP, xPos, yPos, metaState);
                            widget.hostView.getChildAt(0).dispatchTouchEvent(touchDown);
                            widget.hostView.getChildAt(0).dispatchTouchEvent(touchUp);
                        }
                    }
                }
            });

            if (LauncherActivity.inWidgetEditMode) {
                holder.container.setBackgroundColor(0x4EAA04B1);
                if (WidgetsFragment.selectedIndex == index) {
                    holder.container.setBackgroundColor(0x6339538D);
                }
            } else {
                holder.container.setBackgroundColor(0x00FFFFFF);
            }

            //explain why we need a "work around button" - it is the middleman between the hostview and the overlay.
            holder.btnWorkAround.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean isFocused) {
                    Log.e(TAG, "focus index: " + index);
                    if (!LauncherActivity.inWidgetEditMode && !LauncherActivity.inWidgetRemoveMode &&
                            isFocused) {
                        holder.container.setBackgroundColor(0x6339538D);
                        // holder.container.setBackgroundColor(0x6339538D); //0x4EAA04B1
                        WidgetsFragment.selectedIndex = getWidgetIndex(view);
                    } else if (!LauncherActivity.inWidgetEditMode) {
                        holder.container.setBackgroundColor(0x00FFFFFF);
                        holder.btnRemove.setImageDrawable(view.getResources().getDrawable(R.drawable
                                .remove_button));

                        WidgetsFragment.selectedIndex = 0;
                    }

                    if (LauncherActivity.inWidgetEditMode && isFocused || LauncherActivity
                            .inWidgetRemoveMode && isFocused) {
                        holder.container.setBackgroundColor(0x6339538D);
                        //holder.container.setBackgroundColor(0x6339538D);
                        holder.btnRemove.setImageDrawable(view.getResources().getDrawable(R.drawable
                                .remove_button_selected));
                        WidgetsFragment.selectedIndex = getWidgetIndex(view);

                    } else if (LauncherActivity.inWidgetEditMode) {
                        holder.container.setBackgroundColor(0x4EAA04B1);

                    }
                }
            });

            holder.btnRemove.setVisibility(LauncherActivity.inWidgetRemoveMode ? View.VISIBLE :
                    View.INVISIBLE);

            holder.container.getLayoutParams().width = widget.hostView.getLayoutParams().width;
            holder.container.getLayoutParams().height = widget.hostView.getLayoutParams().height;
            holder.btnWorkAround.setId(widget.id);

            // Hover listener for Widgets options menu
            WidgetsFragment.btnOptionsMenu.setOnHoverListener(new View.OnHoverListener() {
                @Override
                public boolean onHover(View v, MotionEvent event) {
                    if (LauncherActivity.inWidgetEditMode)
                        return true;
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_HOVER_ENTER:

                            WidgetsFragment.btnOptionsMenu.setBackgroundResource(R.drawable
                                    .option_button_selected);

                            RecentAppsFragment.slidingLayoutTop.setPanelState
                                    (SlidingUpPanelLayout.PanelState.COLLAPSED);
                            BadgesFragment.upArrow.setVisibility(View.VISIBLE);
                            break;

                        case MotionEvent.ACTION_HOVER_EXIT:
                            WidgetsFragment.btnOptionsMenu.setBackgroundResource
                                    (R.drawable.option_button);
                            break;
                    }
                    return true;
                }
            });

            WidgetsFragment.btnOptionsMenu.requestFocus();
            if (!LauncherActivity.inWidgetEditMode)
                BadgesFragment.upArrow.setFocusable(true);
            if (index == items.size() - 1) {
                int badgeButtonId = R.id.up_arrow;
                LauncherActivity.frameLayouts.get(2).getChildAt(0).
                        findViewById(R.id.up_arrow).getId();//is this redundant?
                holder.btnWorkAround.setNextFocusDownId(badgeButtonId);
            }
            if (index == 0) {
                int widgetButtonId = LauncherActivity.frameLayouts.get(1).getChildAt(0)
                        .findViewById(R.id.btn_widgets_options).getId();//is this redundant?
                holder.btnWorkAround.setNextFocusUpId(widgetButtonId);
            }

//            holder.container.getLayoutParams().height = fixedHeight;

            //we arent doing anything with allViewHolders yet...
            allViewHolders.add(holder);

        }
    }

    /**
     * This method just takes a screencap of just a view
     *
     * @param v - the view to snap
     * @return - a snapshot of the view
     */
    private Drawable snapView(View v) {
        Log.e(TAG, "ghost dimens: " + v.getWidth() + " | " + v.getHeight());
        Bitmap bmp = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas fart = new Canvas(bmp);
        v.draw(fart);
        return new BitmapDrawable(bmp);
    }

    /**
     * Ths is for determining the position of where the inner focus icon when in inner select <br/>
     * mode.
     *
     * @param position - where we are in the list
     */
    public static void selectInnerWidgetView(int position) {
        if (position + innerSelectedIndex >= innerFocusables.size()) {
            position = 0;
        } else if (position + innerSelectedIndex < 0) {
            position = innerFocusables.size() - 1;
        }

//        Log.e(TAG, "inner size: " + innerFocusables.size() + "\nclass name: " + innerFocusables.get(innerSelectedIndex).getClass().getSimpleName());
        View selected = innerFocusables.get(innerSelectedIndex);
        selected.setBackground(selector[0]);

        WidgetBuilder.innerSelectedIndex += position;

        selected = innerFocusables.get(innerSelectedIndex);
        selected.setBackground(selector[1]);
    }

    /**
     * Sets the innerfocusables focusable and clickable values to newVal. Currently only being </br>
     * used for the second bit, setting the background to null if we're false.
     *
     * @param newVal - the value in which to set the focusables and clickables.
     */
    public static void setCurrentInnerFocusables(boolean newVal) {
        for (View v : innerFocusables) {
            v.setFocusable(newVal);
            v.setClickable(newVal);
        }
        if (!newVal) {
            for (View v : innerFocusables)
                v.setBackground(null);
        }
    }

    /**
     * Populates the innerfocusables list, sets the background of the contents to create the </br>
     * illusion of component focus. We are using innerSelectedIndex var to keep track of what </br>
     * currently has "focus", which is changed in the Launcher Activity via left and right keys.
     */
    public static void widgetComponentFocus() {
        innerFocusables = new ArrayList<>();
        for (View v : hostViewGrandChildren) {
            if (v instanceof ViewGroup || v.hasOnClickListeners())
                innerFocusables.add(v);
        }

        for (View vi : innerFocusables) {
            vi.measure(0, 0);
            int measuredWidth = vi.getMeasuredWidth();
            int measuredHeight = vi.getMeasuredHeight();
            measuredWidth = (measuredWidth < 1 ? 1 : measuredWidth);
            measuredHeight = (measuredHeight < 1 ? 1 : measuredHeight);

            selector = new Drawable[]{
                    resize(mainAct.getResources().getDrawable(R.drawable.widget_normal), measuredWidth,
                            measuredHeight),
                    resize(mainAct.getResources().getDrawable(R.drawable.widget_focus), measuredWidth,
                            measuredHeight),
                    resize(mainAct.getResources().getDrawable(R.drawable.widget_press), measuredWidth,
                            measuredHeight)
            };

            vi.setBackground(selector[0]);
            if (innerFocusables.indexOf(vi) == innerSelectedIndex)
                vi.setBackground(selector[1]);
        }
    }

    private static Drawable resize(Drawable image, int width, int height) {
        Bitmap b = ((BitmapDrawable) image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, width, height, true);
        return new BitmapDrawable(mainAct.getResources(), bitmapResized);
    }

    /**
     * Calling the onclick or emulating a touch event for the selected component.
     * We might need to add more cases to this...
     */

    public static void executeInnerSelected() {
        View selectedComponent = innerFocusables.get(innerSelectedIndex);
        if (selectedComponent.hasOnClickListeners()) {
            selectedComponent.performClick();
        } else {
            //handle touch event if no click event...
            MotionEvent touchDown, touchUp;
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis() + 200;
            float xPos = 0;
            float yPos = 0;
            int metaState = 0;

            touchDown = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, xPos, yPos,
                    metaState);
            touchUp = MotionEvent.obtain(eventTime + 50, eventTime + 150, MotionEvent.ACTION_UP,
                    xPos, yPos, metaState);
            selectedComponent.dispatchTouchEvent(touchDown);
            selectedComponent.dispatchTouchEvent(touchUp);
        }

        int widgetId = LauncherActivity.widgetsList.get(WidgetsFragment.selectedIndex).hostView.getAppWidgetId();
        LauncherAppWidgetHost host = LauncherActivity.appWidgetHost;
        AppWidgetProviderInfo info = LauncherActivity.widgetsList.get(WidgetsFragment.selectedIndex).hostView
                .getAppWidgetInfo();

        LauncherActivity.widgetsList.get(WidgetsFragment.selectedIndex).hostView = (LauncherAppWidgetHostView)
                host.createView(mainAct, widgetId, info);
        LauncherActivity.inInnerWidgetSelectMode = false;
        mainAct.changeAllFocusables(true, 'i');
        WidgetBuilder.innerSelectedIndex = 0;
        WidgetBuilder.setCurrentInnerFocusables(false);
        WidgetsFragment.refreshGrid();
    }

    /**
     * How this works: we check if v is a relative layout (the selector) of the widget. If it <br/>
     * is, we get it's grandchild (child's chid) and we know that is the hostview. We then <br/>
     * take the hostview and compare it to all of the hostviews in the list. It will return <br/>
     * value of the index of which hostview it is equal to from the list.
     *
     * @param v - the view we are looking at.
     * @return - the index of where the index is in the list.
     */
    public static int getWidgetIndex(View v) {
        //if v is the btnWorkAround
        if (v instanceof ImageButton) {
            //get its sibling
            RelativeLayout relativeLayout = (RelativeLayout) v.getParent().getParent();
            FrameLayout frameLayout = (FrameLayout) relativeLayout.getChildAt(0);
            LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) frameLayout.
                    getChildAt(0);
            v = hostView;
        }

        if (v instanceof RelativeLayout) {
            FrameLayout frameLayout = (FrameLayout) ((ViewGroup) v).getChildAt(0);
            LauncherAppWidgetHostView host = (LauncherAppWidgetHostView) frameLayout.getChildAt(0);
            v = host;
        }

        //return items.indexOf(v);this didnt work. We have to check via equals function.
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == null)
                continue;
            if ((v).equals(items.get(i).hostView))
                return i;
        }
        return -1;
    }

    //test to get the container via index. We can use this I think at some point...
    public static RelativeLayout getWidgetFromIndex(int index) {
        return allViewHolders.get(index).container;
    }

    /**
     * Nested custom ViewHolder
     */
    public static class WidgetsViewHolder extends View {
        protected FrameLayout hostLayout;
        private ImageView btnRemove;
        private RelativeLayout container;

        // This button overlays the whole widget. This is the only way hovering would work properly
        private ImageButton btnWorkAround;

        public WidgetsViewHolder(View itemView) {
            super(LauncherActivity.getInstance());
            btnRemove = (ImageView) itemView.findViewById(R.id.btn_remove);
            hostLayout = (FrameLayout) itemView.findViewById(R.id.host_layout);
            container = (RelativeLayout) itemView.findViewById(R.id.widget_container);
            btnWorkAround = (ImageButton) itemView.findViewById(R.id.btn_work_around);

            btnWorkAround.setFocusable(!(LauncherActivity.inWidgetEditMode));

            btnWorkAround.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (LauncherActivity.inInnerWidgetSelectMode) {
                        return false;
                    }

                    Log.i(TAG, "widget edit mode: " + LauncherActivity.inWidgetEditMode);
                    if (!btnWorkAround.isHovered()) {
                        if (!LauncherActivity.inWidgetEditMode) {
//                            if (mainAct.onKeyDown(view, KeyEvent.KEYCODE_DPAD_RIGHT) )
                            LauncherActivity.inWidgetEditMode = true;
                            Toast.makeText(view.getContext(), "Widget Swap Mode - Press the back " +
                                            "button to leave.",
                                    Toast.LENGTH_SHORT).show();
                            mainAct.changeAllFocusables(false, 'w');

                            //This is the only way I could make the containers the right color
                            int count = WidgetsFragment.gridLayout.getChildCount();
                            for (int i = 0; i < count; i++) {
                                RelativeLayout overlay = (RelativeLayout) WidgetsFragment.gridLayout.getChildAt(i);
                                if (overlay.getChildCount() > 0)
                                    overlay.setBackgroundColor(0x4EAA04B1);
                            }
                            container.setBackgroundColor(0x6339538D);
                            view.setFocusable(true);
                            view.requestFocus();
                            return true;
                        } else {
                            LauncherActivity.inWidgetEditMode = false;
                            WidgetsFragment.refreshGrid();
                            mainAct.changeAllFocusables(true, 'w');
                            WidgetsFragment.selectedIndex = 0;
                            return true;
                        }
                    }
                    return true;
                }
            });

            btnWorkAround.setLongClickable(true);
            btnWorkAround.setClickable(true);
        }

        public static void toggleRemoveMode() {
            LauncherActivity.inWidgetRemoveMode = !LauncherActivity.inWidgetRemoveMode;
            int newVisibility = LauncherActivity.inWidgetRemoveMode ? View.VISIBLE : View.INVISIBLE;
            int childCount = WidgetsFragment.gridLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                RelativeLayout overlay = (RelativeLayout) WidgetsFragment.gridLayout.getChildAt(i);
                if (overlay.getChildCount() < 2)
                    continue;
                View btnRemove = overlay.findViewById(R.id.btn_remove);
                btnRemove.setVisibility(newVisibility);
            }
        }
    }

    /**
     * Found this function from stackoverflow.
     *
     * @param px pixels
     * @return - converted to DP based on the system's physical pixels(per inch) and high density
     * constant value (240). This proved to work on the TV I am using.
     * -James
     */
    public int pxToDp(int px, Resources res) {
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        int dp = (int) ((double) px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_HIGH));
        return dp;
    }
}