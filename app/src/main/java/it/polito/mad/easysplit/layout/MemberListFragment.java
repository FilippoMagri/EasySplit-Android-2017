package it.polito.mad.easysplit.layout;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.models.GroupBalanceModel;

public class MemberListFragment extends Fragment {
    private Uri mGroupUri;
    private GroupBalanceAdapter mAdapter;

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
