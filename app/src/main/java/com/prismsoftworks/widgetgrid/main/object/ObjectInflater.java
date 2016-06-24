package com.prismsoftworks.widgetgrid.main.object;


import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.graphics.drawable.Drawable;


/**
 * The Object Inflater holds everything that is a clickable. This is going to get refactored </br>
 * soon.
 * <p/>
 * Created by jamesl on 6/12/2015.
 */
public class ObjectInflater {
    public int id;
    public String uri;
    public String name;
    public String packageName;
    public String badgeLabel;
    public String appLabel;
    public int drawable;
    public Drawable icon;
    public long lastAccessedEpoch;
    public LauncherAppWidgetHostView hostView;
    public AppWidgetProviderInfo widgetProviderInfo;
    public boolean isShowing;//as long as the last epoch hasnt changed - this will be true
    public Intent intent;


    /**
     * @param bookmarkURL - A string that will be parsed down to be used for the bookmark titles and
     *                    the URLs that will be loaded by the web browser.
     */
    public ObjectInflater(String bookmarkURL) {

        int urlLength = bookmarkURL.length();
        //some error checking is needed to keep from throwing out of bounds
        // error.
        if (urlLength >= 30)
            urlLength = 30;
        else if (urlLength < 30)
            urlLength = bookmarkURL.length();

        this.uri = bookmarkURL;
        //parsed string
        this.name = bookmarkURL.substring(7, urlLength);

    }

    //App constructor
    public ObjectInflater(int id, String uri, String packageName, String appLabel, Drawable icon,
                          long lastAccessedEpoch, boolean isShowing, Intent intent) {
        this.id = id;
        this.uri = uri;
        this.packageName = packageName;
        this.appLabel = appLabel;

        //todo: work this part out differently. since we are using applabel instead of name,
        // we can probably remove this.
        String[] arr = this.packageName.split("\\.");
        this.name = arr[arr.length - 1];

        this.icon = icon;
        this.lastAccessedEpoch = lastAccessedEpoch;
        this.isShowing = isShowing;
        this.intent = intent;
    }

    //widget constructor
    public ObjectInflater(int id, LauncherAppWidgetHostView hostView, AppWidgetProviderInfo
            widgetProviderInfo, boolean isShowing) {
        this.id = id;
        this.hostView = hostView;
        this.widgetProviderInfo = widgetProviderInfo;
        this.isShowing = isShowing;
    }
}
