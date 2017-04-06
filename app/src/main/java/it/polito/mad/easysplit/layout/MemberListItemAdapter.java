package it.polito.mad.easysplit.layout;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.layout.MemberListFragment.OnListFragmentInteractionListener;
import it.polito.mad.easysplit.models.GroupBalanceModel;
import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.Observer;
import it.polito.mad.easysplit.models.PersonModel;

public class MemberListItemAdapter extends RecyclerView.Adapter<MemberListItemAdapter.ViewHolder> {

    private final GroupModel mGroup;
    private final OnListFragmentInteractionListener mListener;
    private final Drawable defaultProfilePic = null;

    public MemberListItemAdapter(GroupModel group, OnListFragmentInteractionListener listener) {
        mGroup = group;
        mListener = listener;

        mGroup.registerObserver(new GroupObserver());
    }

    private class GroupObserver implements Observer {
        @Override
        public void onChanged() {
            /// TODO Do all that is needed to use more specific notifications
            MemberListItemAdapter.this.notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            /// TODO
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        PersonModel person = mGroup.getMembers().get(position);
        GroupBalanceModel balance = mGroup.getBalance();

        holder.mItem = person;

        Drawable picture = person.getProfilePicture();
        if (picture == null)
            holder.mPicView.setImageDrawable(sDefaultProfilePic);
        else
            holder.mPicView.setImageDrawable(picture);

        holder.mNameView.setText(person.getIdentifier());
        Money residue = balance.getResidueFor(person);
        holder.mResidueView.setText(residue == null ? "-" : residue.toString());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mGroup.getMembers().size();
    }


    static Drawable sDefaultProfilePic = null;

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final ImageView mPicView;
        final TextView mNameView;
        final TextView mResidueView;
        PersonModel mItem;


        ViewHolder(View view) {
            super(view);
            mView = view;

            if (sDefaultProfilePic == null) {
                Resources res = view.getResources();
                sDefaultProfilePic = res.getDrawable(R.drawable.ic_person_default);
            }

            mPicView = (ImageView) view.findViewById(R.id.profile_picture);
            mNameView = (TextView) view.findViewById(R.id.name);
            mResidueView = (TextView) view.findViewById(R.id.residue);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
    }
}
