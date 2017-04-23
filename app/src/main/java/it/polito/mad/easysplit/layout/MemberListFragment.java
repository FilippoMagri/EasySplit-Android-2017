package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import it.polito.mad.easysplit.R;

public class MemberListFragment extends Fragment {

    public static MemberListFragment newInstance(Uri groupUri) {
        MemberListFragment frag = new MemberListFragment();

        Bundle args = new Bundle();
        args.putCharSequence("groupUri", groupUri.toString());
        frag.setArguments(args);

        return frag;
    }


    private OnListFragmentInteractionListener mListener;
    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();
    private Uri mGroupUri;

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
            listView.setAdapter(new MemberListItemAdapter(context, mGroupUri, mListener));
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(String personId);
    }
}
