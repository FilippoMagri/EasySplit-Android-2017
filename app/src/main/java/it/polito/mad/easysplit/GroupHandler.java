package it.polito.mad.easysplit;

import android.net.Uri;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import it.polito.mad.easysplit.models.GroupBalanceModel;

/**
 * Created by fgiobergia on 09/05/17.
 */

public class GroupHandler {
    private DatabaseReference mGroup;
    private Uri groupUri;

    public GroupHandler (Uri group) {
        groupUri = group;
        mGroup = Utils.findByUri(groupUri);
    }

    public void getUserBalance (String userId, GroupBalanceModel.UserBalanceListener listener) {
        new GroupBalanceModel(groupUri, null).getUserBalance(userId, listener);
    }

    public void deleteUser(String userId) {
        Uri userUri = Utils.getUriFor(Utils.UriType.USER, userId);
        HashMap<String, Object> childUpdates = new HashMap<>();

        childUpdates.put(userUri.getPath() + "/groups_ids/" + groupUri.getLastPathSegment(), null); // remove group from user
        childUpdates.put(groupUri.getPath() + "/members_ids/" + userId, null); // remove user from group

        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates);
    }
}