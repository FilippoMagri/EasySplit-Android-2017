package it.polito.mad.easysplit;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ActivityUtils {
    private ActivityUtils() { }

    public static void requestLogin(Context ctx) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null)
            return;

        Intent i = new Intent(ctx, LoginActivity.class);
        ctx.startActivity(i);
    }
}
