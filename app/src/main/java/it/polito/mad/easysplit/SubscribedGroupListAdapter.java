package it.polito.mad.easysplit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
            /* workaround */
            if (lhs.name != null && rhs.name != null) {
                return lhs.name.compareTo(rhs.name);
            }
            /* 0 if they are both null, -1 otherwise */
            return (lhs.name == rhs.name) ? 0 : -1;
        }
    };


    SubscribedGroupListAdapter(Context context, String userId) {
        super(context, R.layout.row_item_group);
        DatabaseReference groupsRef = root.child("users").child(userId).child("groups_ids");
        groupsRef.addChildEventListener(mGroupsListener);
    }


    private class SubscribedGroupListener implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot item, String s) {
            add(new Item(item.getKey(), item.getValue(String.class)));
            sort(mComparator);
        }

        @Override
        public void onChildChanged(DataSnapshot itemSnap, String s) {
            int count = getCount();
            for (int i = 0; i < count; i++) {
                Item item = getItem(i);
                if (item.id.equals(itemSnap.getKey())) {
                    item.name = itemSnap.getValue(String.class);
                    sort(mComparator);
                    return;
                }
            }

            onChildAdded(itemSnap, null);
        }

        @Override
        public void onChildRemoved(DataSnapshot itemSnap) {
            int count = getCount();
            for (int i = 0; i < count; i++) {
                Item item = getItem(i);
                if (item.id.equals(itemSnap.getKey())) {
                    remove(item);
                    return;
                }
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

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