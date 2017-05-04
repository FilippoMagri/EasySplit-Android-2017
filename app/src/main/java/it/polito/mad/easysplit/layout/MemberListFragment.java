package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.models.GroupBalanceModel;

/**
 * Created by fil on 03/05/17.
 */

public class MemberListFragment extends Fragment {
    static String TAG="MemberListFragment";
    Uri mGroupUri;

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

        // Set the adapter
        if (view instanceof ListView) {
            Context context = view.getContext();
            ListView listView = (ListView) view;
            Log.d(TAG,"Inside onCreateView");
            ArrayList<GroupBalanceAdapter.ListItem> members= new ArrayList<GroupBalanceAdapter.ListItem>();
            GroupBalanceAdapter groupBalanceAdapter = new GroupBalanceAdapter(context, members);
            GroupBalanceModel groupBalanceModel = new GroupBalanceModel(mGroupUri, groupBalanceAdapter);
            listView.setAdapter(groupBalanceAdapter);
        }
        return view;
    }
}
