package it.polito.mad.easysplit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProfilePictureManager {
    public static final int THUMBNAIL_SIZE = 200;
    public static final long RELOAD_PERIOD_MILLIS = 7000;  // TimeUnit.SECONDS.convert(10, TimeUnit.MINUTES);

    //
    // Maintain a single instance (at the most) for each user, so different activities can share
    // the cache.
    //
    private static final HashMap<String, WeakReference<ProfilePictureManager>> sInstances = new HashMap<>();
    private Bitmap mPicture, mThumbnail;
    private File mLocalFile;
    private final Handler mHandler = new Handler();

    public static ProfilePictureManager forUser(Context ctx, Uri userUri) {
        return forUser(ctx, Utils.getIdFor(Utils.UriType.USER, userUri));
    }
    public static ProfilePictureManager forUser(Context ctx, String userId) {
        WeakReference<ProfilePictureManager> instance = sInstances.get(userId);

        if (instance == null || instance.get() == null) {
            ProfilePictureManager newInstance = new ProfilePictureManager(ctx, userId);
            sInstances.put(userId, new WeakReference<>(newInstance));
            return newInstance;
        }

        return instance.get();
    }

    public interface Listener {
        void onPictureReceived(@Nullable Bitmap pic);
        void onThumbnailReceived(@Nullable Bitmap pic);
        void onFailure(Exception e);
    }

    private StorageReference mStorageRef;
    // Listeners are kept with weak references, so that they're automatically deleted once they're
    // no longer used.
    private Set<Listener> mListeners = Collections.newSetFromMap(new WeakHashMap<Listener, Boolean>());
    private AtomicBoolean mConnected = new AtomicBoolean(false);


    private ProfilePictureManager(Context ctx, String userId) {
        String mProfilePicFilename = "profilepic-" + userId;
        mStorageRef = FirebaseStorage.getInstance().getReference().child(mProfilePicFilename);

        String cacheDir = ctx.getCacheDir().getAbsolutePath();
        mLocalFile = new File(cacheDir + "/" + mProfilePicFilename);

        if (mLocalFile.exists()) {
            // Start by loading the cache
            Bitmap pic = BitmapFactory.decodeFile(mLocalFile.getAbsolutePath());
            notifyProfilePicture(pic);
        } else {
            // No cache, load from Firebase
            reload();
        }

        final WeakReference<ProfilePictureManager> weakSelf = new WeakReference<>(this);
        FirebaseDatabase.getInstance().getReference("/.info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot valueSnap) {
                if (weakSelf.get() == null)
                    return;

                weakSelf.get().mConnected.set(valueSnap.getValue(Boolean.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Reload every RELOAD_PERIOD_MILLIS milliseconds
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                reload();
                mHandler.postDelayed(this, RELOAD_PERIOD_MILLIS);
            }
        }, RELOAD_PERIOD_MILLIS);
    }

    void reload() {
        if (! mConnected.get())
            return;

        mStorageRef.getFile(mLocalFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Bitmap pic = BitmapFactory.decodeFile(mLocalFile.getAbsolutePath());
                        notifyProfilePicture(pic);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        notifyFailure(e);
                    }
                });
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
        if (mPicture != null)
            listener.onPictureReceived(mPicture);
        if (mThumbnail != null)
            listener.onThumbnailReceived(mThumbnail);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private void notifyProfilePicture(@Nullable Bitmap picture) {
        Bitmap thumbnail = null;
        if (picture != null) {
            // Create scaled version of the picture, maintaining the aspect ratio
            double ratio = (double) picture.getWidth() / picture.getHeight();
            int dstWidth, dstHeight;
            if (picture.getWidth() > picture.getHeight()) {
                dstWidth = THUMBNAIL_SIZE;
                dstHeight = (int) (dstWidth / ratio);
            } else {
                dstHeight = THUMBNAIL_SIZE;
                dstWidth = (int) (dstHeight * ratio);
            }

            thumbnail = Bitmap.createScaledBitmap(picture, dstWidth, dstHeight, true);
        }

        mPicture = picture;
        mThumbnail = thumbnail;

        for (Listener listener : new ArrayList<>(mListeners)) {
            listener.onPictureReceived(picture);
            listener.onThumbnailReceived(thumbnail);
        }
    }

    private Bitmap cropCircular(Bitmap originalPicture) {
        int smallestSide = Math.min(originalPicture.getWidth(), originalPicture.getHeight());
        int radius = smallestSide / 2;

        Bitmap output = Bitmap.createBitmap(smallestSide, smallestSide, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, smallestSide, smallestSide);

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.argb(255, 255, 255, 255));
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(originalPicture, rect, rect, paint);

        return output;
    }

    private void notifyFailure(Exception e) {
        for (Listener listener : new ArrayList<>(mListeners))
            listener.onFailure(e);
    }

    public void setPicture(Bitmap originalBitmap) {
        // TODO Possibly more efficient method?
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Bitmap bitmap = cropCircular(originalBitmap);
        bitmap.compress(Bitmap.CompressFormat.WEBP, 90, stream);

        mStorageRef.putStream(new ByteArrayInputStream(stream.toByteArray()))
            .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.getException() != null) {
                        notifyFailure(task.getException());
                        return;
                    }

                    notifyProfilePicture(bitmap);
                }
            });
    }
}
