package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

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

        MemberRepresentation memberRepr = getItem(position);
        TextView name = (TextView) convertView.findViewById(id.name);
        TextView residueText = (TextView) convertView.findViewById(id.residue);

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
            MemberRepresentation debtor = entry.getKey();
            Money debt = entry.getValue();

            View child = LayoutInflater.from(getContext()).inflate(R.layout.catch_up_group_item, parent, false);
            TextView messageText = (TextView) child.findViewById(R.id.text_view_catch_up_item);
            TextView debtText = (TextView) child.findViewById(R.id.residue_catch_up_item);

            int messageRes = cmp > 0 ? R.string.balance_debtor_message : R.string.balance_creditor_message;
            String messageFmt = getContext().getResources().getString(messageRes);
            String message = String.format(messageFmt, debtor.getName());
            messageText.setText(message);

            debtText.setText(debt.abs().toString());
            layout.addView(child);
        }

        return convertView;
    }
}
