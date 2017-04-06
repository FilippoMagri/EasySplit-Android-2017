package it.polito.mad.easysplit;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import it.polito.mad.easysplit.models.Amountable;
import it.polito.mad.easysplit.models.ExpenseModel;

/**
 * Created by fgiobergia on 06/04/17.
 */

public class ItemAdapter<T extends Amountable> extends ArrayAdapter<T> {
    private Context ctx;
    private List<T> items;
    private int resource;

    public ItemAdapter (Context ctx, int resource, List<T> items) {
        super(ctx, resource, items);
        this.ctx = ctx;
        this.items = items;
        this.resource = resource;
    }

    @Override
    public View getView (int position, View convertView, ViewGroup parent) {
        //ExpenseModel expense = expenses.get(position);
        T item = items.get(position);

        if (convertView == null) { // not recycling an existing view, inflate it!
            convertView = LayoutInflater.from(ctx).inflate(resource, parent, false);
        }
        TextView name = (TextView) convertView.findViewById(R.id.name);
        TextView amount = (TextView) convertView.findViewById(R.id.amount);
        name.setText(item.getName());
        float cents = item.getAmount().getCents();

        amount.setTextColor((cents > 0) ? 0xff64fa64 : 0xfffa6464);
        amount.setText(item.getAmount().toString());

        return convertView;
    }

    public T getItem(int position) {
        return items.get(position);
    }

}