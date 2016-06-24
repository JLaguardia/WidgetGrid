package com.prismsoftworks.widgetgrid.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnHoverListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment for handling the widgets... force refresh is located in the launcher activity mainly </br>
 * because the widgetlist is populated there view selectWidget
 * <p/>
 * Created by jamesl on 5/20/2015.
 */
public class WidgetsFragment extends Fragment {
    private static final String TAG = WidgetsFragment.class.getSimpleName();
    //    private static final int GRID_CEILING = 107;
    public static List<Integer[]> cellDimens = null;
    //    public static RecyclerView recyclerView;
    public static GridLayout gridLayout = null;
    public static ImageButton btnOptionsMenu;
    public static FrameLayout widgetsFrameLayout;
    public static WidgetBuilder widgetBuilder;
    public static RelativeLayout _layout;
    public static boolean gridMode = false;
    private static LauncherActivity mainAct;
    public static final Integer MAX_COLUMNS = 19;
    public static final Integer MAX_ROWS = 8;
    private static WidgetsFragment instance = null;
    private static RelativeLayout widgetLayout = null;
    public static int selectedIndex = 0;
    private static boolean[][] occupiedCells;

    public static boolean inWidgetDragMode = false;

    public static Integer dragColumn = null;
    public static Integer dragRow = null;
    public static Integer dragListIndex = null;
    public static Integer dragCellIndex = null;
    public static Integer dragColSpan = null;
    private static List<Integer> curHighlightedNulls = null;

    public WidgetsFragment() {
        if (instance == null)
            instance = this;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setupViews();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        widgetLayout = (RelativeLayout) inflater.inflate(R.layout.widgets_fragment_layout, container, false);
        return widgetLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public static WidgetsFragment getInstance() {
        return instance;
    }

    /**
     * Setup all of the components for the fragments
     */
    private void setupViews() {
        mainAct = (LauncherActivity) getActivity();
        _layout = (RelativeLayout) mainAct.findViewById(R.id.widget_app_layout);

        if (cellDimens == null) {
            cellDimens = new ArrayList<>();
            LauncherActivity.widgetsList = new ArrayList<>();
            for (int i = 0; i < MAX_COLUMNS * MAX_ROWS; i++) {
                cellDimens.add(new Integer[]{0, 0});
                LauncherActivity.widgetsList.add(null);
            }
        }

        refreshButton();
        widgetsFrameLayout = (FrameLayout) mainAct.findViewById(R.id.middle_frame_layout);
        widgetsFrameLayout.bringToFront();

        btnOptionsMenu.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean isFocused) {
                if (LauncherActivity.inWidgetEditMode)
                    return;
                if (isFocused) {
                    RecentAppsFragment.slidingLayoutTop.setPanelState(SlidingUpPanelLayout
                            .PanelState.COLLAPSED);
                }
            }
        });

        // Hover lister for options menu button
        btnOptionsMenu.setOnHoverListener(new OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        RecentAppsFragment.slidingLayoutTop.setPanelState(SlidingUpPanelLayout
                                .PanelState.COLLAPSED);
                        BadgesFragment.slidingLayoutBottom.setPanelState(SlidingUpPanelLayout
                                .PanelState.COLLAPSED);
                        btnOptionsMenu.setBackgroundResource(R.drawable.option_button_selected);
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        btnOptionsMenu.setBackgroundResource(R.drawable.option_button);
                        break;
                }
                return true;
            }
        });

        populateWidgets();

        // Hover lister for widgets frame layout
        widgetsFrameLayout.setOnHoverListener(new OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        RecentAppsFragment.slidingLayoutTop.setPanelState(SlidingUpPanelLayout
                                .PanelState.COLLAPSED);
                        BadgesFragment.slidingLayoutBottom.setPanelState(SlidingUpPanelLayout
                                .PanelState.COLLAPSED);
                        break;
                }

                return true;
            }
        });
    }

    public void refreshButton() {
        btnOptionsMenu = null;
        btnOptionsMenu = (ImageButton) widgetLayout.findViewById(R.id.btn_widgets_options);
        ((ViewGroup) widgetLayout.getChildAt(0)).removeViewAt(0);
        btnOptionsMenu.setOnClickListener(getWidgetMenuListener());
        ((ViewGroup) widgetLayout.getChildAt(0)).addView(btnOptionsMenu);
        widgetLayout.getChildAt(0).bringToFront();
        btnOptionsMenu.bringToFront();
        btnOptionsMenu.requestFocus();
    }

    /**
     * Moving widgets around when we're in WidgetEditMode
     *
     * @param offset - direction we are going in. 1 for RIGHT, -1 for LEFT.
     */
    public void moveWidgetHotizontally(int offset) {
        int curPos = selectedIndex;
        int newPos = curPos + offset;
        int cellIndex;
        Integer currentSpan = cellDimens.get(curPos)[0];
        Integer selectedCells;
        int accumulatedCells = 0;

        if (newPos < 0) {
            newPos = 0;
        }

        //get the cell index
        for (int i = 0; i < curPos; i++) {
            selectedCells = cellDimens.get(i)[0];
            selectedCells = (selectedCells == 0 ? 1 : selectedCells);
            accumulatedCells += selectedCells;
        }

        cellIndex = accumulatedCells;
        //this number will be between 0 and maxCol
        int cellRefinedColumnIndex = cellIndex % MAX_COLUMNS;

        if (offset < 0 && cellRefinedColumnIndex == 0) {
            return;
        }

        if (offset > 0 && cellRefinedColumnIndex == (MAX_COLUMNS - currentSpan)) {
            return;
        }

//        if (addACell) {
//            mainAct.createWidget(null);
//        }

        Collections.swap(LauncherActivity.widgetsList, selectedIndex, newPos);
        Collections.swap(WidgetsFragment.cellDimens, selectedIndex, newPos);
        Log.i(TAG, "swapped: " + selectedIndex + " with " + newPos);
        selectedIndex = newPos;
        refreshGrid();
    }

    public void moveUp(int displacement) {
        int curListIndex = selectedIndex;
        if (curListIndex == 0) {
            return;
        }

        int targetListIndex;
        int currentColSpan = cellDimens.get(curListIndex)[0];
        int columnOffset = currentColSpan - 1;
        Integer selectedCells;
        int curCellIndex;
        int targetCellIndex;
        int targetColumnIndex;
        int accumulatedCells = 0;


        //get the cell index
        for (int i = 0; i < curListIndex; i++) {
            selectedCells = cellDimens.get(i)[0];
            selectedCells = (selectedCells == 0 ? 1 : selectedCells);
            accumulatedCells += selectedCells;
        }

        curCellIndex = accumulatedCells;
        targetCellIndex = curCellIndex - displacement;
        targetColumnIndex = targetCellIndex % MAX_COLUMNS;
        int endOfGridCheck = (MAX_COLUMNS - 1) - targetColumnIndex;

        if (endOfGridCheck < currentColSpan) {
            Log.e(TAG, "end of grid displacement: " + endOfGridCheck);
            for (int i = 1; i < (currentColSpan - endOfGridCheck); i++) {
                targetCellIndex -= 1;
            }
        }

        //handle when user tries to move up in the first row
        if (targetCellIndex <= 0) {
            targetListIndex = 0;
            if (cellDimens.get(targetListIndex)[0] > 1)
                return;
        }

        accumulatedCells = 0;
        targetListIndex = 0;
        if (targetCellIndex > 0) {
            //convert cell index to list index
            for (int i = 0; i < curListIndex; i++) {
                selectedCells = cellDimens.get(i)[0];
                selectedCells = (selectedCells == 0 ? 1 : selectedCells);
                accumulatedCells += selectedCells;

                //temp cell index
                if (accumulatedCells >= targetCellIndex) {
                    targetListIndex = i + 1;
                    break;
                }
            }
        }

        for (int i = 0; i < currentColSpan - 1; i++)
            if ((accumulatedCells + i) % MAX_COLUMNS == MAX_COLUMNS - 1)
                return;


        //boolean because workaround hax
        boolean fitCheck = true;
        ArrayList<Integer> blankSpaces = new ArrayList<>();
        boolean blockedRight = false, blockedLeft = false;

        //check sides
        for (int i = 0; i < currentColSpan; i++) {
            if (i == 0) {
                selectedCells = cellDimens.get(targetListIndex)[0];
                if (selectedCells > 0) {
                    return;
                }

                if (selectedCells == 0) {
                    blankSpaces.add(targetListIndex);
                }

                if (targetCellIndex % MAX_COLUMNS == 0) {
                    blockedLeft = true;
                }

                continue;
            }

            if (!blockedRight) {
                selectedCells = cellDimens.get(targetListIndex + i)[0];
                if (selectedCells > 0) {
                    blockedRight = true;
                    i = 1;
                }

                if ((i != currentColSpan - 1 && (targetCellIndex + i) % MAX_COLUMNS == MAX_COLUMNS -
                        1)) {
                    blankSpaces.add(targetListIndex + i);
                    blockedRight = true;
                    i = 1;
                } else {
                    blankSpaces.add(targetListIndex + i);
                }
            }

            if (blockedRight && !blockedLeft) {
                if (targetListIndex - i < 0) {
                    blockedLeft = true;
                }

                if (!blockedLeft && blockedRight) {
                    selectedCells = cellDimens.get(targetListIndex - i)[0];
                    if (selectedCells == 0) {
                        blankSpaces.add(targetListIndex - i);
                    } else {
                        blockedLeft = true;
                    }
                }

                //is this check redundant?
                if (blankSpaces.size() == currentColSpan)
                    break;
            }
        }
        if (blockedLeft && blockedRight && blankSpaces.size() < currentColSpan)
            return;

        if (blankSpaces.size() < currentColSpan)
            fitCheck = false;

        String strIndexes = "| ";
        Collections.sort(blankSpaces); //{ 1, 2 , 3 }

        for (Integer c : blankSpaces)
            strIndexes += c + " | ";

//        Log.e(TAG, "Indexes\n" + strIndexes);

        if (fitCheck) {//fit ppl only!
            int indexOffset = 0;
            for (int i = 0; i < columnOffset; i++) {
                cellDimens.add(curListIndex, new Integer[]{0, 0});
                LauncherActivity.widgetsList.add(curListIndex, null);
                cellDimens.remove(blankSpaces.get(i) - indexOffset);
                LauncherActivity.widgetsList.remove(blankSpaces.get(i) - indexOffset);
                indexOffset++;
            }

            targetListIndex = blankSpaces.get(0);
            swapIndexes(curListIndex, targetListIndex);
            selectedIndex = targetListIndex;
        }

        refreshGrid();
    }

    public boolean isRoomyBelow() {
        int accumulatedCells = 0;
        Integer selectedCells;
        List<Integer> highVals = new ArrayList<>();

        //find out how many rows we have.
        int populatedRows = cellDimens.size() / MAX_ROWS;

        for (int i = 0; i < cellDimens.size(); i++) {
            int rowIndex = i / MAX_COLUMNS;
            if (rowIndex > highVals.size() - 1)
                highVals.add(0);
            selectedCells = cellDimens.get(i)[1];
            selectedCells = (selectedCells == 0 ? 1 : selectedCells);

            if (highVals.get(rowIndex) < selectedCells)
                highVals.set(rowIndex, selectedCells);
        }

        for (Integer c : highVals)
            accumulatedCells += c;

        if (accumulatedCells > MAX_ROWS - 1)//why-1? we are offsetting for the badge fragment
            return false;
        return true;
    }

    /**
     * This is called when releasing the mouse on the widgetDrag. This just sets up the displacement
     * parameter for moveDown or moveUp. *note: this is working fine*
     *
     * @param targetCellIndex target cell input from releasing the mouse button
     */
    public void dragMove(int targetCellIndex) {
        int curListIndex = selectedIndex;
        int curCellIndex;
        Integer selectedCells;
        int displacement;
        int accumulatedCells = 0;
        for (int i = 0; i < curListIndex; i++) {
            selectedCells = cellDimens.get(i)[0];
            selectedCells = (selectedCells == 0 ? 1 : selectedCells);
            accumulatedCells += selectedCells;
        }

        curCellIndex = accumulatedCells;
        accumulatedCells = 0;

        int curRowIndex = curCellIndex / MAX_COLUMNS;
//        Log.i(TAG, curListIndex + "\ndrag row/curRow: " + dragRow + " / " + curRowIndex +
//                "\ndrag col/curCol: " + dragColumn + " / " + (curCellIndex % MAX_COLUMNS) +
//                "\ndrag cell/curCell: " + dragCellIndex + " / " + curCellIndex);
        if (dragRow == curRowIndex) {
            if (targetCellIndex > curCellIndex) {
                displacement = targetCellIndex - curCellIndex;
                Log.e(TAG, "moving RIGHT by: " + (displacement));
                for (int i = 0; i < (displacement); i++) {
                    moveWidgetHotizontally(1);
                }
            } else {
                displacement = curCellIndex - targetCellIndex;
                Log.e(TAG, "moving LEFT by: " + (displacement));
                for (int i = 0; i < (displacement); i++) {
                    moveWidgetHotizontally(-1);
                }
            }

            return;
        }

        //if targetIndex is below currentIndex
        if (targetCellIndex > curCellIndex) {
            displacement = -(curCellIndex - targetCellIndex);
            Log.e(TAG, "moving DOWN by: " + (displacement));
            moveDown(displacement);
            return;
        }

        if (targetCellIndex < curCellIndex) {
            displacement = curCellIndex - targetCellIndex;
            Log.e(TAG, "moving UP by: " + (displacement));
            moveUp(displacement);
        }
    }

    public void moveDown(int displacement) {
        int curListIndex = selectedIndex;
        int curCellIndex;
        int targetListIndex = curListIndex + MAX_COLUMNS; //this is the farthest it can be moved
        int targetCellIndex;
        int targetColumnIndex;
        Integer currentColumnSpan = cellDimens.get(curListIndex)[0];
        Integer selectedCells;
        int excessCells = 0;
        int accumulatedCells = 0;
        int columnOffset = currentColumnSpan - 1;

        //find current cell index
        for (int i = 0; i < curListIndex; i++) {
            selectedCells = cellDimens.get(i)[0];
            selectedCells = (selectedCells == 0 ? 1 : selectedCells);
            accumulatedCells += selectedCells;
        }

        curCellIndex = accumulatedCells;
        if (curCellIndex / MAX_COLUMNS >= MAX_ROWS - 1) {
            return;
        }

        targetCellIndex = curCellIndex + displacement;
        targetColumnIndex = targetCellIndex % MAX_COLUMNS;
        int endOfGridCheck = (MAX_COLUMNS - 1) - targetColumnIndex;

        if (endOfGridCheck < currentColumnSpan) {
            Log.e(TAG, "end of grid displacement: " + endOfGridCheck);
            for (int i = 1; i < (currentColumnSpan - endOfGridCheck); i++) {
                targetCellIndex -= 1;
            }
        }

        int targetCellOffset = 0;//unused...
        accumulatedCells = 0;

        for (int i = curListIndex + 1; i < cellDimens.size(); i++) {
            selectedCells = cellDimens.get(i)[0];
            selectedCells = (selectedCells == 0 ? 1 : selectedCells);
            accumulatedCells += selectedCells;
            excessCells += (selectedCells - 1);

            if (accumulatedCells >= targetCellIndex) {
                targetCellOffset = accumulatedCells - targetCellIndex;//this is unnecessary?
                break;
            }
        }

        //add blank spots to avoid displacing the list.
        for (int i = 0; i < columnOffset; i++) {
            cellDimens.add(curListIndex + 1, new Integer[]{0, 0});
            LauncherActivity.widgetsList.add(curListIndex + 1, null);
            accumulatedCells++;
        }

        int cellsToAdd = MAX_COLUMNS - accumulatedCells;

        for (int i = 0; i < cellsToAdd; i++) {
            cellDimens.add(new Integer[]{0, 0});
            LauncherActivity.widgetsList.add(null);
        }

        //convert target cell index to list index
        accumulatedCells = curCellIndex;
        for (int i = curListIndex; i < cellDimens.size(); i++) {
            if (i == curListIndex)
                continue;
            selectedCells = cellDimens.get(i)[0];
            selectedCells = (selectedCells == 0 ? 1 : selectedCells);
            accumulatedCells += selectedCells;
            if (accumulatedCells >= targetCellIndex) {
                targetListIndex = i;
                break;
            }
        }

        boolean blockedLeft = false, blockedRight = false;
        ArrayList<Integer> blankCellIndexes = new ArrayList<>();
        String accumulatedIndexes = "| ";
        int offsetWorkaround = 0;
        for (int i = 0; i < (MAX_COLUMNS * MAX_ROWS); i++) {//why are we looping to this number??
            if (i == 0) {
                if (targetCellIndex % MAX_COLUMNS == 0)
                    blockedLeft = true;
                selectedCells = cellDimens.get(targetListIndex)[0];
                if (selectedCells < 1) {
                    if (blankCellIndexes.indexOf(targetListIndex) == -1) {
                        blankCellIndexes.add(targetListIndex);
                        accumulatedIndexes += targetListIndex + " | ";
                    }
                } else {
                    int targetColSpan = cellDimens.get(targetListIndex)[0];
                    targetColSpan = (targetColSpan > MAX_COLUMNS ? MAX_COLUMNS : targetColSpan);
                    int targetCellSpanIndex = (targetColSpan - 1) + targetCellIndex;
                    int curCellSpandex = MAX_COLUMNS + curCellIndex + (currentColumnSpan - 1);
                    Log.e("spandex", "curcell disambiguation: " + MAX_COLUMNS + " + " + curCellIndex
                            + " + " + (currentColumnSpan - 1) + " = " + curCellSpandex +
                            "\ntargetcell disambiguation: " + (targetColSpan - 1) + " + " +
                            targetCellIndex + " = " + targetCellSpanIndex);
                    if (targetCellSpanIndex == curCellSpandex) {
                        blockedLeft = true;
                        blockedRight = true;
                    } else if (targetCellSpanIndex > curCellSpandex)
                        blockedRight = true;
                    else
                        blockedLeft = true;
                }
                continue;
            }

            //check right side
            if (targetListIndex + i >= cellDimens.size()) {
                cellDimens.add(new Integer[]{0, 0});
                LauncherActivity.widgetsList.add(null);
                offsetWorkaround++;
            }

            if (!blockedRight) {
                if ((blankCellIndexes.size() + 1 != currentColumnSpan &&
                        (targetCellIndex + i) % MAX_COLUMNS == MAX_COLUMNS - 1)) {
                    blockedRight = true;
                    i = -1;
                    continue;
                }
                selectedCells = cellDimens.get(targetListIndex + i)[0];
                if (selectedCells == 0) {
                    if (blankCellIndexes.indexOf(targetListIndex + i) == -1) {
                        blankCellIndexes.add(targetListIndex + i);
                        accumulatedIndexes += (targetListIndex + i) + " | ";
                    }
                } else {
                    blockedRight = true;
                    i = -1;
                    continue;
                }
            }

            //check left side
            if (blockedRight && !blockedLeft) {
                if (targetListIndex - i < 0 || (blankCellIndexes.size() + 1 != currentColumnSpan &&
                        (targetCellIndex - i) % MAX_COLUMNS == 0)) {
                    blockedLeft = true;
                    i = -1;
                    continue;
                }

                selectedCells = cellDimens.get(targetListIndex - i)[0];
                if (selectedCells == 0) {
                    if (blankCellIndexes.indexOf(targetListIndex - i) == -1) {
                        blankCellIndexes.add(targetListIndex - i);
                        accumulatedIndexes += (targetListIndex - i) + " | ";
                    }
                } else {
                    blockedLeft = true;
                    i = -1;
                    continue;
                }
            }

            if (blankCellIndexes.size() == currentColumnSpan) {
                break;
            }

            if (i > MAX_COLUMNS) {
                //remove blank spots to avoid displacing the list.
                for (int j = 0; j < columnOffset; j++) {
                    cellDimens.remove(curListIndex + 1);
                    LauncherActivity.widgetsList.remove(curListIndex + 1);
                    accumulatedCells++;
                }

                return;
            }


            if (blockedLeft && blockedRight && blankCellIndexes.size() != currentColumnSpan) {
                for (int j = 0; j < cellsToAdd; j++) {
                    LauncherActivity.widgetsList.remove(cellDimens.size() - 1);
                    cellDimens.remove(cellDimens.size() - 1);
                }
                for (int j = 0; j < offsetWorkaround; j++) {
                    LauncherActivity.widgetsList.remove(cellDimens.size() - 1);
                    cellDimens.remove(cellDimens.size() - 1);
                }
                for (int j = 0; j < columnOffset; j++) {
                    cellDimens.remove(curListIndex + 1);
                    LauncherActivity.widgetsList.remove(curListIndex + 1);
                }
                return;
            }
        }

        //wondering if THIS is causing issues
        for (int i = 0; i < offsetWorkaround; i++) {
            LauncherActivity.widgetsList.remove(cellDimens.size() - 1);
            cellDimens.remove(cellDimens.size() - 1);
        }

        Collections.sort(blankCellIndexes);

        targetListIndex = blankCellIndexes.get(0);

        String strIndexes = "|";
        for (Integer c : blankCellIndexes)
            strIndexes += " " + c + " |";

        //todo: there is probably a better way around this damn index out of bounds exception
        boolean antiError = false;
        while (!antiError) {
            try {
                swapIndexes(curListIndex, targetListIndex);
                antiError = true;
            } catch (IndexOutOfBoundsException ie) {
                Log.e("error", "index out of bounds caught \n" + ie.toString());
                cellDimens.add(new Integer[]{0, 0});
                LauncherActivity.widgetsList.add(null);
            }
        }

        if (cellsToAdd < 0)
            for (int i = 0; i < columnOffset; i++) {
                if (cellDimens.size() > targetListIndex + 1) {
                    if (cellDimens.get(targetListIndex + 1)[0] == 0) {
                        cellDimens.remove(targetListIndex + 1);
                        LauncherActivity.widgetsList.remove(targetListIndex + 1);
                    }
                }
            }

        selectedIndex = targetListIndex;
        refreshGrid();
    }

    private void swapIndexes(int curPos, int targetPos) {
        Collections.swap(cellDimens, curPos, targetPos);
        Collections.swap(LauncherActivity.widgetsList, curPos, targetPos);
    }

    public static GridLayoutManager getGridLayoutManager() {

        GridLayoutManager glm = new GLMNonScrolling(mainAct, (LauncherActivity.
                widgetsList.size() == 0 ? 1 : LauncherActivity.widgetsList.size()),
                GridLayoutManager.VERTICAL, false);
        glm.setSpanCount(MAX_COLUMNS);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int pos) {
                Integer[] cells = cellDimens.get(pos);
                if (cells[0] == 0) {
                    return 1;
                }

                if (cells[0] < 3) {
                    return 3;//3 seems like an ok size (see the analog clock)
                }

                if (cells[0] > MAX_COLUMNS) {
                    return MAX_COLUMNS;
                }

                return cells[0];
            }
        });

        return glm;
    }

    /**
     * This class was constructed specifically to override the two boolean functions included.
     */
    private static class GLMNonScrolling extends GridLayoutManager {

        public GLMNonScrolling(Context context, int spanCount, int orientation, boolean
                reverseLayout) {
            super(context, spanCount, orientation, reverseLayout);
        }

        @Override
        public boolean canScrollVertically() {
//            Log.e(TAG, "no");
            return false;
        }

        @Override
        public boolean canScrollHorizontally() {
//            Log.e(TAG, "no");
            return false;
        }
    }

    /**
     * ***ACCORDING TO ANDROID DOCUMENTATION/STACKOVERFLOW***
     * CELLS: (70 × n) − 30 where n == amount of cells
     *
     * @param minWidth  - the minWidth of the widget.
     * @param minHeight - the minHeight of the widget.
     * @return - array where value[0] == columnSpan minimum and value[1] == rowSpanMinimum
     */
    public Integer[] getCells(int minWidth, int minHeight) {
        return new Integer[]{(minWidth + 30) / 70, (minHeight + 30) / 70};
    }

    //do we need this?
    public static int getCells(int minSegment) {
        return (minSegment + 30) / 70;
    }

    /**
     * This converts cells into density pixels
     */
    private static int getUsedScreenPixelDp(int cells) {
        int res = (70 * cells) - 30;
        if (res < 100) {
            res = 100;
        }

        return res;
    }

    private boolean gridIsFull(Integer[] incomingCells) {
        int lastCellindex = 0;
        Integer selectedCells;
        for (int i = 0; i < cellDimens.size(); i++) {
            selectedCells = cellDimens.get(i)[0];
            selectedCells = (selectedCells == 0 ? 1 : selectedCells >
                    MAX_COLUMNS ? MAX_COLUMNS : selectedCells);
            lastCellindex += selectedCells;
        }
        Integer cellsIn = incomingCells[0];
        cellsIn = (cellsIn == 0 ? 1 : cellsIn > MAX_COLUMNS ? MAX_COLUMNS :
                cellsIn);

        if (lastCellindex + cellsIn > (MAX_ROWS * MAX_COLUMNS))
            return true;
        else
            return false;
    }

    /**
     * @param curX - cursor X position.
     * @param curY - cursor Y position.
     *             <p/>
     *             Start is 0, ceiling is 107, floor is 999, end is 1919
     *             get targetIndex based on cursor position (curX, curY)
     *             manual method: cell 0 rect: (0,107), (106,107), (0, 400), (106,400)
     *             getting the column index: based on curX: (int) (curX / 106)
     *             getting the rowindex: based on curY: 107 - 400 == row 0,
     *             401 - 700 == row 1, 701 - 1000
     */
    public static void highlightNulls(float curX, float curY) {
        int incomingCol = (int) (curX / 100);
        int incomingRow = (int) ((curY - 110) / 100);

        //check if we've left the cell
        if (dragColumn != null && dragRow != null) {
            if (dragColumn == incomingCol && dragRow == incomingRow) {
                return;
            }
        }

        int curIndex = selectedIndex;
        dragColumn = incomingCol;//these are accurate, but not 100% precise
        dragRow = incomingRow; //these are accurate, but not 100% precise

        //we can only have 0, 1 or 2 as this value. Cancel drag if we get over 2.
        if (dragRow > MAX_ROWS) {
            dragRow = null;
            dragColumn = null;
            dragListIndex = null;
            dragCellIndex = null;
            dragColSpan = null;
            curHighlightedNulls = null;
            return;
        }

        //null check
        if (curHighlightedNulls == null) {
            curHighlightedNulls = new ArrayList<>();
        }

        //init vars we need for highlighting (finally)
        dragCellIndex = (dragRow * MAX_COLUMNS) + dragColumn;

        int curCellIndex = 0;
        int accumCells;
        List<Integer> iterationHighlight;
        Integer selectedCells;
        dragColSpan = cellDimens.get(curIndex)[0];
        accumCells = 0;

        //find selectedWidgetCellIndex
        for (int i = 0; i < cellDimens.size(); i++) {
            if (i == curIndex) {
                curCellIndex = accumCells;
            }

            selectedCells = cellDimens.get(i)[0];
            selectedCells = (selectedCells < 1 ? 1 : selectedCells);
            accumCells += selectedCells;
        }

        accumCells = 0;

        //find the drag(target) list
        for (int i = 0; i < cellDimens.size(); i++) {
            selectedCells = cellDimens.get(i)[0];
            selectedCells = (selectedCells < 1 ? 1 : selectedCells);
            accumCells += selectedCells;

            if (accumCells >= dragCellIndex) {
                dragListIndex = i;
                break;
            }
        }

        //imported this algorithm from moveUp/moveDown
        boolean blockedLeft = false, blockedRight = false;
        ArrayList<Integer> blankCellIndexes = new ArrayList<>();


        //this should be equal to the median of the span. For simplicity sake we will just have an offset of 1 for now
        int dragListOffset = 1;
        int dragLeftCellIndex = dragListIndex + dragListOffset;
        if (dragLeftCellIndex < 0) {
            dragLeftCellIndex = 0;
        }

        if (dragLeftCellIndex >= cellDimens.size()) {
            dragLeftCellIndex = cellDimens.size() - 1;
        }

        for (int i = 0; i < (MAX_COLUMNS * MAX_ROWS); i++) { //loop through the whole grid
            if (dragRow > MAX_ROWS) {
                break;
            }

            if (blankCellIndexes.size() >= dragColSpan || i > MAX_COLUMNS) {
                break;
            }

            if (blockedLeft && blockedRight && blankCellIndexes.size() != dragColSpan) {
                blankCellIndexes.clear();
                break;
            }

            if (i == 0) {
                if (dragCellIndex == curCellIndex) {
                    break;
                }

                //if we are on the left edge of the grid
                if (dragCellIndex % MAX_COLUMNS == 0) {
                    blockedLeft = true;
                    if (dragCellIndex == 0) {//manually add 0 if we're at the first cell
                        blankCellIndexes.add(0);
                    }
                }

                //if we are on the right edge of the grid
                if (dragCellIndex % MAX_COLUMNS == (MAX_COLUMNS - 1)) {
                    if (dragListIndex != curIndex) {
                        blankCellIndexes.add(dragListIndex);
                    }

                    Log.i(TAG, "blocked right");
                    blockedRight = true;
                }

                if (dragListIndex == curIndex) {
                    int occupiedCellCount = dragCellIndex - curCellIndex;
                    for (int j = 0; j < (dragColSpan - occupiedCellCount); j++) {
                        blankCellIndexes.add(null);
                    }
                }

                selectedCells = cellDimens.get(dragLeftCellIndex)[0];
                if (selectedCells < 1) {
                    if (blankCellIndexes.indexOf(dragLeftCellIndex) == -1) {
                        blankCellIndexes.add(dragLeftCellIndex);
                    }
                } else {
                    //this else seems strange. The logic is hardly making sense
                    int targetColSpan = selectedCells;
                    targetColSpan = (targetColSpan > MAX_COLUMNS ? MAX_COLUMNS : targetColSpan);
                    int targetCellSpanIndex = (targetColSpan - 1) + dragCellIndex;
                    int curCellSpandex = MAX_COLUMNS + curCellIndex + (dragColSpan - 1);
                    Log.e("spandex", "curcell disambiguation: " + MAX_COLUMNS + " + " + curCellIndex
                            + " + " + (dragColSpan - 1) + " = " + curCellSpandex +
                            "\ntargetcell disambiguation: " + (targetColSpan - 1) + " + " +
                            dragCellIndex + " = " + targetCellSpanIndex);
                    if (targetCellSpanIndex == curCellSpandex) {
                        blockedLeft = true;
                        blockedRight = true;
                    } else if (targetCellSpanIndex > curCellSpandex)
                        blockedRight = true;
                    else
                        blockedLeft = true;
                }
                continue;
            }

            //check right side first
            if (!blockedRight) {
//                Log.e(TAG, dragLeftCellIndex + " col index: " + ((dragCellIndex ) % MAX_COLUMNS)
//                        + "\nsize/dragcolspan: " + blankCellIndexes.size() + " / " + dragColSpan);
                if ((dragLeftCellIndex + i) >= cellDimens.size()) {
                    blockedRight = true;
                    i = -1;
                    continue;
                }

//                Log.e(TAG, "size: " + blankCellIndexes.size());

                if ((blankCellIndexes.size() + 1) < dragColSpan
                        && ((dragCellIndex + i) % MAX_COLUMNS == (MAX_COLUMNS - 1)
                        || (dragCellIndex + i) % MAX_COLUMNS == 0)) {
                    blankCellIndexes.add(dragLeftCellIndex + i);
                    Log.i(TAG, "blocked right. Cell added: " + (dragLeftCellIndex + i));
                    blockedRight = true;
                    i = -1;
                    continue;
                }

                selectedCells = cellDimens.get(dragLeftCellIndex + i)[0];
                selectedCells = (selectedCells == 0 ? 1 : selectedCells);

                //if selected cell is occupied and we ARE on current index
                if (selectedCells > 1 && (dragLeftCellIndex + i) == curIndex) {
                    for (int j = 0; j < selectedCells - 1; j++) {
                        blankCellIndexes.add(null);
                    }
                } else if (selectedCells < 3) {
                    if (blankCellIndexes.indexOf(dragLeftCellIndex + i) == -1) {
                        blankCellIndexes.add(dragLeftCellIndex + i);
                    }
                } else {
                    blockedRight = true;
                    Log.i(TAG, "blocked right");
                    i = -1;
                    continue;
                }
            }

            //check left side only if we are blocked on the right side
            if (blockedRight && !blockedLeft) {
                if (dragLeftCellIndex - i < 0 || ((blankCellIndexes.size() + 1 < dragColSpan
                        && (dragCellIndex - i) % MAX_COLUMNS == 0))) {
                    blockedLeft = true;
                    Log.i(TAG, "blocked left");
                    i = -1;
                    continue;
                }

                selectedCells = cellDimens.get(dragLeftCellIndex - i)[0];
                selectedCells = (selectedCells == 0 ? 1 : selectedCells);
                if (selectedCells > 1 && (dragLeftCellIndex - i) == curIndex) {
                    for (int j = 0; j < selectedCells - 1; j++) {
                        blankCellIndexes.add(null);
                    }
                } else if (selectedCells < 3) {
                    if (blankCellIndexes.indexOf(dragLeftCellIndex - i) == -1) {
                        blankCellIndexes.add(dragLeftCellIndex - i);
                    }
                }
            }
        }

        iterationHighlight = new ArrayList<>(blankCellIndexes);

        //"unhighlight" previously highlighted
        if (curHighlightedNulls.size() > 1) {
            for (Integer nullIndex : curHighlightedNulls) {
                if (nullIndex == null) {
                    continue;
                }

                gridLayout.getChildAt(nullIndex).setBackgroundColor(0x00FFFFFF);
            }
        }

        //highlight the dragcells
        if (iterationHighlight.size() > 1) {
            for (int i = 0; i < iterationHighlight.size(); i++) {
                if (iterationHighlight.get(i) == null) {
                    continue;
                }

                gridLayout.getChildAt(iterationHighlight.get(i)).setBackgroundColor(0x99FF0000);
            }
        }

        curHighlightedNulls.clear();
        curHighlightedNulls = new ArrayList<>(iterationHighlight);
    }

    /**
     * We set the CellDimens from here based on the outputs from getCells. This determines</br>
     * column span for the most part. I think we can do more here though...
     *
     * @param info           - from the widget, this will be used to get the width/height
     * @param data           - not sure, we could use this in the future?
     * @param widgetInflater - populate the list from the launcher activity because lazy
     */
    public void setLastWidgetCells(AppWidgetProviderInfo info, Intent data, ObjectInflater
            widgetInflater) {
        //do we need an Intent as a param?
        if (info == null) {
            cellDimens.add(new Integer[]{0, 0});
            return;
        }

        //get the width of the hostlayout itself and compare it to the minWidth.
        widgetInflater.hostView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int minWidth = info.minWidth;
        int hostW = widgetInflater.hostView.getMeasuredWidth();
        boolean roomBelow = isRoomyBelow();
        Integer[] cellParams;
        cellParams = getCells((minWidth < hostW ? minWidth : hostW), info.minHeight);
        Log.i(TAG, "cell params: " + cellParams[0] + ", " + cellParams[1]);

        //I honestly cant remember what I was doing past this point with the for loop...
        //therefore: do not touch! lolol
        Integer colSpan = cellParams[0];
        colSpan = (colSpan > MAX_COLUMNS ? MAX_COLUMNS : colSpan);
        int nulCounter = 0;
        int targetCellIndex = 0;
        Integer startIndex = null, endIndex = null, selectedCells;

        //this loops through the whole list, dynamically counting the cell spans of every element,
        //and checking if there is room for the incoming widget's column span.
        for (int indexRef = 0; indexRef < cellDimens.size(); indexRef++) {
            selectedCells = cellDimens.get(indexRef)[0];
            selectedCells = (selectedCells == 0 ? 1 : selectedCells > MAX_COLUMNS ? MAX_COLUMNS :
                    selectedCells);
            targetCellIndex += selectedCells;
            if (selectedCells == 1) {
                nulCounter++;
                if (startIndex == null) {
                    //check the edges
                    boolean fitCheck = true;
                    for (int colIndex = 0; colIndex < colSpan; colIndex++) {
                        int targetColumnIndex = (targetCellIndex - 1 + colIndex) % MAX_COLUMNS;
                        if (colIndex == 0) {
                            if (targetColumnIndex == MAX_COLUMNS - 1) {
                                fitCheck = false;
                            }
                        }

                        if ((targetColumnIndex == MAX_COLUMNS - 1 && colIndex != colSpan - 1) ||
                                (targetColumnIndex == 0 && colIndex != 0))
                            fitCheck = false;

                        if (!fitCheck) {
                            nulCounter = 0;
                            startIndex = null;
                            break;
                        }
                    }

                    if (fitCheck)
                        startIndex = indexRef;
                } else if (nulCounter == colSpan) {
                    endIndex = indexRef;
                    break;
                }
            } else {
                //cell is occupied., try a different start location
                nulCounter = 0;
                startIndex = null;
            }
        }
        //handle adding to lists as well as removing the null spaces.
        if (startIndex != null && endIndex != null) {
            cellDimens.add(startIndex, cellParams);
            LauncherActivity.widgetsList.add(startIndex, widgetInflater);
            for (int i = 0; i < colSpan; i++) {
                cellDimens.remove(startIndex + 1);
                LauncherActivity.widgetsList.remove(startIndex + 1);
            }
        } else {
            if (gridIsFull(cellParams)) {
                //play sound here?
                Toast.makeText(mainAct, "There is not enough room for this widget!",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            //below can probably be refactored and removed but let's not and say we did
            if (roomBelow) {
                LauncherActivity.widgetsList.add(widgetInflater);
                cellDimens.add(cellParams);
            }
        }

        refreshGrid();
    }

    private void populateWidgets() {
//        if (LauncherActivity.widgetsList == null) {
//            LauncherActivity.widgetsList = new ArrayList<>();
//        }
        gridLayout = (GridLayout) mainAct.findViewById(R.id.widget_grid);
        refreshGrid();

        if (LauncherActivity.widgetsList.size() < 1) {
            btnOptionsMenu.setNextFocusLeftId(R.id.btn_recent_apps_menu);
        }
        btnOptionsMenu.setNextFocusDownId(R.id.up_arrow);
    }

    public View.OnClickListener getWidgetMenuListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Creating the instance of PopupMenu
                ContextThemeWrapper wrapper = new ContextThemeWrapper(mainAct,
                        R.style.PopupMenu);
                final PopupMenu widgetPopupMenu = new PopupMenu(wrapper, view);
                //Inflating the Popup using xml file
                widgetPopupMenu.getMenuInflater().inflate(R.menu.widgets_options_menu,
                        widgetPopupMenu.getMenu());

                widgetPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        LauncherActivity.creatingWidget = true;

                        switch (item.getItemId()) {

                            case R.id.optionAddWidgets:
                                mainAct.selectWidget();

                                // Makes edit mode false when adding a widget
                                if (LauncherActivity.inWidgetRemoveMode) {
                                    WidgetBuilder.WidgetsViewHolder.toggleRemoveMode();
                                }

                                return true;

                            //Enables or disables remove mode
                            case R.id.optionRemoveWidget:
                                if (gridLayout.getChildCount() == 162) {
                                    Toast.makeText(mainAct, "There are no Widgets to remove!",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    WidgetBuilder.WidgetsViewHolder.toggleRemoveMode();
                                }

                                return true;

                            //Removes all widgets
                            case R.id.optionRemoveAllWidgets:

                                if (gridLayout.getChildCount() == 162) {
                                    Toast.makeText(mainAct, "There are no Widgets to remove!",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    removeAllWidgetsConfirmation();
                                }

                                return true;

                            case R.id.optionGridMode:
                                gridMode = !gridMode;
                                refreshGrid();
                                return true;

                            default:
                                return false;
                        }
                    }
                });

                widgetPopupMenu.show(); //shows popup menu
            }
        };
    }

    /**
     * @param index - widget index in list
     * @param code  - only set this to 'a' if we are removing ALL widgets. Otherwise, anything.
     */
    public static void removeWidget(int index, char code) {
        ObjectInflater del = LauncherActivity.widgetsList.get(index);
        if (del != null) {
            LauncherActivity.appWidgetHost.deleteAppWidgetId(del.id);
        }

        if (code != 'a') {//fix recursion
            Integer removedColSpan = cellDimens.get(index)[0];
            removedColSpan = (removedColSpan > MAX_COLUMNS ? MAX_COLUMNS : removedColSpan);
            for (int i = 0; i < removedColSpan; i++) {
                cellDimens.add(index + 1, new Integer[]{0, 0});
                LauncherActivity.widgetsList.add(index + 1, null);
            }
        }

        cellDimens.remove(index);
        LauncherActivity.widgetsList.remove(index);
        if (code != 'a') {
            populateGrid();
        }
    }

    private static void populateGrid() {
//        Log.e(TAG, "widgetlist size: " + LauncherActivity.widgetsList.size() + " | " + cellDimens.size());

        widgetBuilder = new WidgetBuilder(LauncherActivity.widgetsList, mainAct);
        widgetBuilder.bindViewHolders(gridLayout);
        int colCount = gridLayout.getColumnCount();
        int rowCount = gridLayout.getRowCount();
        occupiedCells = new boolean[MAX_ROWS][MAX_COLUMNS];

        for (int r = 0; r < MAX_ROWS; r++) {
            for (int c = 0; c < MAX_COLUMNS; c++) {
                occupiedCells[r][c] = false;
            }
        }

        int columnIndex = 0;
        int rowIndex = 0;
        for (int i = 0; i < widgetBuilder.getItemLength(); i++) {
            View[] raw = widgetBuilder.getItem(i);
            RelativeLayout vToAdd = (RelativeLayout) raw[0];
            ImageButton btnWorkAround = null;
            if (raw[1] instanceof ImageButton) {
                btnWorkAround = (ImageButton) raw[1];
            }

            Integer[] selectedCell = cellDimens.get(i);
            selectedCell = new Integer[]{selectedCell[0] == 0 ? 1 : selectedCell[0],
                    selectedCell[1] == 0 ? 1 : selectedCell[1]};


            if (columnIndex + selectedCell[0] > colCount) {
                columnIndex = 0;
                rowIndex++;
                if (rowIndex >= MAX_ROWS) {
                    break;//todo: handle end of grid msg
                }

                i--;
                continue;
            }

            GridLayout.Spec rowSpan;
            GridLayout.Spec colSpan;

            rowSpan = GridLayout.spec(rowIndex, selectedCell[1]);
            colSpan = GridLayout.spec(columnIndex, selectedCell[0]);


            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.rowSpec = rowSpan;
            glp.columnSpec = colSpan;
            glp.width = getUsedScreenPixelDp(selectedCell[0]);
            glp.height = getUsedScreenPixelDp(selectedCell[1]);
            if (cellDimens.get(i)[0] > 0) {
                Log.i(TAG, "view info: w/h :" + glp.width + " | " + glp.height +
                        "\ncelldimens: " + selectedCell[0] + " | " + selectedCell[1] +
                        "\nrow/col: " + rowIndex + " | " + columnIndex);
            }

            vToAdd.setLayoutParams(glp);
            gridLayout.addView(vToAdd, i, glp);

            for (int c = columnIndex; c < columnIndex + selectedCell[0]; c++) {
                occupiedCells[rowIndex][c] = true;
            }

            columnIndex += selectedCell[0];
            if (columnIndex >= gridLayout.getColumnCount()) {
                columnIndex = 0;
                rowIndex++;
                if (rowIndex >= gridLayout.getRowCount()) {
                    Log.e(TAG, "end of grid yo");
                    //reached end of grid, todo: handle it
                    break;
                }
            }
        }

        Log.i(TAG, "items in grid: " + gridLayout.getChildCount());
    }


    /**
     * This is checking if there are sufficient vacant cells for the params
     *
     * @param inRowIndex - current row index
     * @param inColIndex - current column index
     * @param spans      - column and row spans incoming
     * @return false if all cells are vacant, true if 1 cell is occupied
     */
    private static boolean checkOccupiedCells(int inRowIndex, int inColIndex, Integer[] spans) {
        try {
            for (int col = inColIndex; col <= inColIndex + spans[0]; col++) {
                for (int row = inRowIndex; row < inRowIndex + spans[1]; row++) {
                    if (occupiedCells[row][col]) {
                        return true;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException aiob) {
//            Log.d(TAG, "array index out of bounds caught (no room in grid)" );
            return true;
        }

        return false;
    }

    public static void refreshGrid() {
        Log.i(TAG, "refreshing grid");
        final int gId = gridLayout.getId();
        ViewGroup par = (ViewGroup) gridLayout.getParent();
        gridLayout.removeAllViews();
        par.removeView(gridLayout);
        GridLayout newGrid = new GridLayout(mainAct);
        newGrid.setColumnCount(MAX_COLUMNS);
        newGrid.setRowCount(MAX_ROWS);
        Log.e(TAG, "total grid size c/r: " + newGrid.getColumnCount() + " | " + newGrid.getRowCount());
        newGrid.setId(gId);
        gridLayout = newGrid;
        par.addView(gridLayout);
        populateGrid();

        instance.refreshButton();
    }

    public void removeAllWidgets() {
        for (int i = 0; i < LauncherActivity.widgetsList.size(); i++) {
            removeWidget(i, 'a');
            i--;
        }

        for (int i = 0; i < MAX_COLUMNS * MAX_ROWS; i++) {
            LauncherActivity.widgetsList.add(null);
            cellDimens.add(new Integer[]{0, 0});
        }

        refreshGrid();
    }

    public void removeAllWidgetsConfirmation() {
        ContextThemeWrapper ctw = new ContextThemeWrapper(mainAct, R.style.Theme_AlertDialog);
        AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
        builder.setTitle("Remove All Widgets");
        builder.setMessage("Are you sure you want to remove all widgets?");
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                removeAllWidgets();
                btnOptionsMenu.setNextFocusLeftId(R.id.btn_recent_apps_menu);
            }
        });

        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });

        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.show();
    }
}