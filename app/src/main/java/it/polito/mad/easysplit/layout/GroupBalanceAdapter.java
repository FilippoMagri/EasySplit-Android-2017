package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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

import java.util.Map;

import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.layout.MemberListFragment.OnListFragmentInteractionListener;
import it.polito.mad.easysplit.models.GroupBalanceModel;
import it.polito.mad.easysplit.models.Money;

public class GroupBalanceAdapter extends ArrayAdapter<GroupBalanceAdapter.ListItem> {

    static final class ListItem {
        public String id, name;
        public Money residue;

        public ListItem(String id, String name, Money residue) {
            this.id = id;
            this.name = name;
            this.residue = residue;
        }
    }

    private static final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();
    private final GroupBalanceModel mBalance;
    private final OnListFragmentInteractionListener mListener;
    private final Drawable defaultProfilePic = null;

    public GroupBalanceAdapter(Context ctx, Uri groupUri, OnListFragmentInteractionListener listener) {
        super(ctx, R.layout.list_item_member);
        mListener = listener;
        mBalance = new GroupBalanceModel(groupUri);
        mBalance.addListener(new BalanceListener());
    }

    public class BalanceListener implements GroupBalanceModel.Listener {
        @Override
        public synchronized void onChanged(Map<String, Money> balances) {
            clear();
            for (Map.Entry<String, Money> entry : balances.entrySet()) {
                final String userId = entry.getKey();
                final Money residue = entry.getValue();
                final DatabaseReference nameRef = mRoot.child("users/" + userId + "/name");
                nameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot nameSnap) {
                        String userName = nameSnap.getValue(String.class);
                        add(new ListItem(userId, userName, residue));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        add(new ListItem(userId, "???", residue));
                    }
                });
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            notifyDataSetInvalidated();
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_member, parent, false);
        }

        final ListItem item = getItem(position);
        TextView name = (TextView) convertView.findViewById(R.id.name);
        TextView residue = (TextView) convertView.findViewById(R.id.residue);

        name.setText(item.name);
        residue.setText(item.residue.toString());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(item.id);
                }
            }
        });

        return convertView;
    }
}
