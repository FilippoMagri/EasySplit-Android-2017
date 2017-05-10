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
                    .setTitle(R.string.unsaved_confirm_title)
                    .setMessage(R.string.unsaved_confirm_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            currentActivity.finish();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
        else {
            currentActivity.finish();
        }
    }
}
