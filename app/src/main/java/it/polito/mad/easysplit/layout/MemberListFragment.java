package it.polito.mad.easysplit.layout;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.models.GroupBalanceModel;

public class MemberListFragment extends Fragment {
    private Uri mGroupUri;
    private GroupBalanceAdapter mAdapter;
    private ValueEventListener mConnectionListener;
    private DatabaseReference mConnectedRef;

    public static MemberListFragment newInstance(Uri groupUri) {
        MemberListFragment frag = new MemberListFragment();
        Bundle args = new Bundle();
        args.putCharSequence("groupUri", groupUri.toString());
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mGroupUri = Uri.parse(args.getCharSequence("groupUri").toString());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final View connectionWarning = view.findViewById(R.id.offline_warning);
        mConnectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot valueSnap) {
                connectionWarning.setVisibility(
                        valueSnap.getValue(Boolean.class) ?
                        View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        };

        mConnectedRef = FirebaseDatabase.getInstance().getReference("/.info/connected");
        mConnectedRef.addValueEventListener(mConnectionListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mConnectedRef.removeEventListener(mConnectionListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_member_list, container, false);

        ListView listView = (ListView) view.findViewById(R.id.membersList);

        GroupBalanceModel model = GroupBalanceModel.forGroup(mGroupUri);
        mAdapter = new GroupBalanceAdapter(getContext(), model);
        listView.setAdapter(mAdapter);

        return view;
    }
}
