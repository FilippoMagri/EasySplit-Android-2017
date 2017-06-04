package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

import it.polito.mad.easysplit.ProfilePictureManager;
import it.polito.mad.easysplit.Payment;
import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.R.id;
import it.polito.mad.easysplit.R.layout;
import it.polito.mad.easysplit.models.GroupBalanceModel;
import it.polito.mad.easysplit.models.GroupBalanceModel.MemberRepresentation;
import it.polito.mad.easysplit.models.Money;

class GroupBalanceAdapter extends ArrayAdapter<MemberRepresentation> implements GroupBalanceModel.Listener {

    GroupBalanceAdapter(Context ctx, GroupBalanceModel balanceModel) {
        super(ctx, 0);
        balanceModel.addListener(this);
    }

    @Override
    public void onBalanceChanged(Map<String, MemberRepresentation> balance) {
        clear();
        addAll(balance.values());
    }

    private WeakHashMap<MemberRepresentation, ProfilePictureListener> mListeners = new WeakHashMap<>();

    private final class ProfilePictureListener implements ProfilePictureManager.Listener {
        private ImageView mImageView;

        public ProfilePictureListener(ImageView imageView) {
            mImageView = imageView;
        }

        @Override
        public void onPictureReceived(@Nullable Bitmap pic) { /* Nothing to do */ }

        @Override
        public void onThumbnailReceived(@Nullable Bitmap pic) {
            mImageView.setImageBitmap(pic);
        }

        @Override
        public void onFailure(Exception e) {
            mImageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_default_profile_pic));
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layout.list_item_member, parent, false);
        }

        final MemberRepresentation memberRepr = getItem(position);
        TextView name = (TextView) convertView.findViewById(id.name);
        TextView residueText = (TextView) convertView.findViewById(id.residue);

        if (! mListeners.containsKey(memberRepr)) {
            ImageView picView = (ImageView) convertView.findViewById(id.profile_picture);

            ProfilePictureListener listener = new ProfilePictureListener(picView);
            mListeners.put(memberRepr, listener);

            ProfilePictureManager picManager = ProfilePictureManager.forUser(getContext(), memberRepr.getId());
            picManager.addListener(new ProfilePictureListener(picView));
        }

        // access to nested linear layout, in order to attach the list of creditors or debtors
        LinearLayout layout = (LinearLayout) convertView.findViewById(id.linearLayout_catch_up_list);
        layout.removeAllViews();

        //TODO implement general Version For All Currencies
        name.setText(memberRepr.getName());

        Resources res = getContext().getResources();

        if (memberRepr.getResidue().isZero()) {
            residueText.setText(R.string.caught_up);
            residueText.setTextColor(res.getColor(R.color.balance_caught_up));
            return convertView;
        }

        residueText.setText(memberRepr.getConvertedResidue().toString());
        int cmp = memberRepr.getResidue().cmpZero();
        if (cmp > 0) {
            // This member is a creditor
            residueText.setTextColor(res.getColor(R.color.balance_creditor));
        } else {
            // Member is a debtor
            residueText.setTextColor(res.getColor(R.color.balance_debtor));
        }

        for (Map.Entry<MemberRepresentation, Money> entry : memberRepr.getConvertedAssignments().entrySet()) {
            final MemberRepresentation debtor = entry.getKey();
            final Money debt = entry.getValue();
            if (!debt.isZero()) {
                View child = LayoutInflater.from(getContext()).inflate(R.layout.catch_up_group_item, parent, false);
                TextView messageText = (TextView) child.findViewById(R.id.text_view_catch_up_item);
                TextView debtText = (TextView) child.findViewById(R.id.residue_catch_up_item);
                ImageView imageView = (ImageView) child.findViewById(R.id.ic_next_catch_up_item);

                int messageRes = cmp > 0 ? R.string.balance_debtor_message : R.string.balance_creditor_message;
                String messageFmt = getContext().getResources().getString(messageRes);
                String message = String.format(messageFmt, debtor.getName());
                messageText.setText(message);

                debtText.setText(debt.abs().toString());

                OnClickCatchUpItemListener onClickCatchUpItemListener = new OnClickCatchUpItemListener(memberRepr, debtor, debt, getContext());
                messageText.setOnClickListener(onClickCatchUpItemListener);
                debtText.setOnClickListener(onClickCatchUpItemListener);
                imageView.setOnClickListener(onClickCatchUpItemListener);
                layout.addView(child);
            } else continue;
        }

        return convertView;
    }
}

class OnClickCatchUpItemListener implements View.OnClickListener {
    MemberRepresentation memberRepr,debtor;
    Money debt;
    Context context;

    public OnClickCatchUpItemListener(MemberRepresentation memberRepr, MemberRepresentation debtor,Money debt, Context context) {
        this.memberRepr = memberRepr;
        this.debtor = debtor;
        this.debt = debt;
        this.context = context;
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(context, Payment.class);
        //Root Member
        intent.putExtra("RootMemberName",memberRepr.getName());
        intent.putExtra("RootMemberId",memberRepr.getId());
        intent.putExtra("RootMemberMoney",memberRepr.getResidue().toString());
        intent.putExtra("RootMemberSymbol",memberRepr.getResidue().getCurrency().getSymbol());
        intent.putExtra("RootMemberCurrency",memberRepr.getResidue().getCurrency().toString());
        //Sub Member
        intent.putExtra("SubMemberName",debtor.getName());
        intent.putExtra("SubMemberId",debtor.getId());
        intent.putExtra("SubMemberMoney",debt.getAmount().toString());
        intent.putExtra("SubMemberSymbol",debt.getCurrency().getSymbol());
        intent.putExtra("SubMemberCurrency",debt.getCurrency().toString());
        //GroupId Of The Balance
        intent.putExtra("GroupId",memberRepr.getGroupId());
        context.startActivity(intent);
    }
}
