package it.polito.mad.easysplit;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MyListViewAdapterGroup extends ArrayAdapter {

    public MyListViewAdapterGroup(Context context, ArrayList<String> groups) {
        super(context, 0, groups);
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final Context context = parent.getContext();
        String groupName = (String) getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_item_group, parent, false);
        }
        // Lookup view for data population
        final TextView tvName = (TextView) convertView.findViewById(R.id.label);

        tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tvName.getText().equals("Gruppo MAD")) {
                    Intent i = new Intent(context, ExpensesListActivity.class);
                    context.startActivity(i);
                } else {
                    Snackbar snackbar = Snackbar.make(tvName, "Testing Version Use Only Gruppo MAD", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
        });

        ImageButton imgB = (ImageButton) convertView.findViewById(R.id.icon_row_group);
        imgB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tvName.getText().equals("Gruppo MAD")) {
                    Intent i = new Intent(context, ExpensesListActivity.class);
                    context.startActivity(i);
                } else {
                    Snackbar snackbar = Snackbar.make(tvName, "Testing Version Use Only Gruppo MAD", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
        });
        // Populate the data into the template view using the data object
        tvName.setText(groupName);
        // Return the completed view to render on screen
        return convertView;
    }
}