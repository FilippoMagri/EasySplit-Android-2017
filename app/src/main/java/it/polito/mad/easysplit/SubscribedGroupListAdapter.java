package it.polito.mad.easysplit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Comparator;

public class SubscribedGroupListAdapter extends ArrayAdapter<SubscribedGroupListAdapter.Item> {
    public static final class Item {
        public String id, name;

        public Item(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }


    private final DatabaseReference root = FirebaseDatabase.getInstance().getReference();
    private SubscribedGroupListener mGroupsListener = new SubscribedGroupListener();
    private Comparator<Item> mComparator = new Comparator<Item>() {
        @Override
        public int compare(Item lhs, Item rhs) {
            return lhs.name.compareTo(rhs.name);
        }
    };


    SubscribedGroupListAdapter(Context context, String userId) {
        super(context, R.layout.row_item_group);
        DatabaseReference groupsRef = root.child("users").child(userId).child("groups_ids");
        groupsRef.addValueEventListener(mGroupsListener);
    }


    private class SubscribedGroupListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot groupsIdsSnap) {
            clear();
            for (DataSnapshot groupId : groupsIdsSnap.getChildren()) {
                final String itemId = groupId.getKey();
                final DatabaseReference nameRef = root.child("groups").child(itemId).child("name");
                nameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot nameSnap) {
                        String itemName = nameSnap.getValue(String.class);
                        add(new Item(itemId, itemName));
                        sort(mComparator);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { cancel(); }
                });
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            cancel();
        }
    }

    private void cancel() {
        clear();
        notifyDataSetInvalidated();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_item_group, parent, false);
        }

        String groupName = getItem(position).name;
        if (groupName != null) {
            TextView label = (TextView) convertView.findViewById(R.id.group_name);
            label.setText(groupName);
        }
        return convertView;
    }
}