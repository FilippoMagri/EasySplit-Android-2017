package it.polito.mad.easysplit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

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
            new AlertDialog.Builder(ctx)
                    .setTitle("Unsaved changes")
                    .setMessage("There are unsaved changes. Are you sure you want to leave?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            currentActivity.finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
        else {
            currentActivity.finish();
        }
    }
}
