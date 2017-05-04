package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;

import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.models.Money;

/**
 * Created by fil on 03/05/17.
 */

public class GroupBalanceAdapter extends ArrayAdapter<GroupBalanceAdapter.ListItem> {

    public static final class ListItem {
        public String id, name;
        public Money residue;

        public ListItem(String id, String name, Money residue) {
            this.id = id;
            this.name = name;
            this.residue = residue;
        }
    }

    public GroupBalanceAdapter(Context ctx, ArrayList<ListItem> members) {
        super(ctx, 0, members);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_member, parent, false);
        }

        final GroupBalanceAdapter.ListItem item = getItem(position);
        TextView name = (TextView) convertView.findViewById(R.id.name);
        TextView residue = (TextView) convertView.findViewById(R.id.residue);

        //TODO implement general Version For All Currencies
        name.setText(item.name);
        if (!item.residue.toString().equals("0.00 EUR")) {
            residue.setText(item.residue.toString());
            if(item.residue.getAmount().compareTo(new BigDecimal("0.00"))>0) {
                residue.setTextColor(Color.GREEN);
            } else {
                residue.setTextColor(Color.RED);
            }
        } else {
            residue.setText("Catch up");
            residue.setTextColor(Color.GREEN);
        }
        return convertView;
    }
}
