package it.polito.mad.easysplit;

import android.app.Activity;
import android.content.Context;

/**
 * Created by fgiobergia on 23/04/17.
 */

public class UnsavedChangesNotifier {
    private boolean changed;
    private Context ctx;
    private Activity currentActivity;

    public UnsavedChangesNotifier(Context ctx, Activity currentActivity) {
        this.ctx = ctx;
        this.currentActivity = currentActivity;
    }

    public void setChanged() {
        changed = true;
    }

    /* if there have been changes, show a dialog
     * asking the user whether they're sure they
     * want to go back.
     * Otherwise, just close the current activity
     */
    public void handleBackButton () {
        if (changed == true) {
            ActivityUtils.confirmDiscardChanges(currentActivity);
        }
        else {
            currentActivity.finish();
        }
    }

}