package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
        final TextView typeOfMember = (TextView) convertView.findViewById(id.typeOfMember);
        TextView keyItemMember = (TextView) convertView.findViewById(id.key_item_member);

        // access to nested linear layout, in order to attach the list of creditors or debtors
        LinearLayout layout = (LinearLayout) convertView.findViewById(id.linearLayout_catch_up_list);
        layout.removeAllViews();

        //TODO implement general Version For All Currencies
        name.setText(memberRepr.getName());
        keyItemMember.setText(memberRepr.getId());

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
            typeOfMember.setText(R.string.balance_creditor);
        } else {
            // Member is a debtor
            residueText.setTextColor(res.getColor(R.color.balance_debtor));
            typeOfMember.setText(R.string.balance_debtor);
        }

        for (Map.Entry<MemberRepresentation, Money> entry : memberRepr.getConvertedAssignments().entrySet()) {
            final MemberRepresentation debtor = entry.getKey();
            final Money debt = entry.getValue();

            View child = LayoutInflater.from(getContext()).inflate(R.layout.catch_up_group_item, parent, false);
            TextView messageText = (TextView) child.findViewById(R.id.text_view_catch_up_item);
            TextView debtText = (TextView) child.findViewById(R.id.residue_catch_up_item);
            ImageView imageView = (ImageView) child.findViewById(R.id.ic_next_catch_up_item);

            int messageRes = cmp > 0 ? R.string.balance_debtor_message : R.string.balance_creditor_message;
            String messageFmt = getContext().getResources().getString(messageRes);
            String message = String.format(messageFmt, debtor.getName());
            messageText.setText(message);

            debtText.setText(debt.abs().toString());

            OnClickCatchUpItemListener onClickCatchUpItemListener = new OnClickCatchUpItemListener(memberRepr,debtor,debt,getContext());
            messageText.setOnClickListener(onClickCatchUpItemListener);
            debtText.setOnClickListener(onClickCatchUpItemListener);
            imageView.setOnClickListener(onClickCatchUpItemListener);
            layout.addView(child);
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
        //This Will be "Credtor","Name" or "Debtor","Name"
        //Root Member
        intent.putExtra("RootMemberName",memberRepr.getName());
        intent.putExtra("RootMemberId",memberRepr.getId());
        intent.putExtra("RootMemberMoney",memberRepr.getResidue().toString());
        intent.putExtra("RootMemberSymbol",memberRepr.getResidue().getCurrency().getSymbol());
        intent.putExtra("RootMemberCurrency",memberRepr.getResidue().getCurrency().toString());
        //Sub Member
        intent.putExtra("SubMemberName",debtor.getName());
        intent.putExtra("SubMemberId",debtor.getId());
        intent.putExtra("SubMemberMoney",debt.toString());
        intent.putExtra("SubMemberSymbol",debt.getCurrency().getSymbol());
        intent.putExtra("SubMemberCurrency",debt.getCurrency().toString());
        context.startActivity(intent);
    }
}
