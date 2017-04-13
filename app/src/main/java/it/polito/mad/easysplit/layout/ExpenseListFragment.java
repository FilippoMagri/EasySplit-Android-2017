package it.polito.mad.easysplit.layout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.easysplit.AddExpenses;
import it.polito.mad.easysplit.ExpenseDetailsActivity;
import it.polito.mad.easysplit.ItemAdapter;
import it.polito.mad.easysplit.MyApplication;
import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.models.Database;
import it.polito.mad.easysplit.models.ExpenseModel;
import it.polito.mad.easysplit.models.GroupModel;

public class ExpenseListFragment extends Fragment {

    public static ExpenseListFragment newInstance(GroupModel group) {
        ExpenseListFragment frag = new ExpenseListFragment();

        Bundle args = new Bundle();
        args.putCharSequence("groupUri", group.getUri().toString());
        frag.setArguments(args);

        return frag;
    }


    private @Nullable  GroupModel mGroup = null;

    public @Nullable GroupModel getGroup() {
        return mGroup;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = savedInstanceState;
        if (savedInstanceState == null)
            args = getArguments();

        Database db = ((MyApplication) getContext().getApplicationContext()).getDatabase();
        String groupUri = (String) args.getCharSequence("groupUri");
        mGroup = db.findByUri(Uri.parse(groupUri), GroupModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_expenses_list, container, false);
        ListView lv = (ListView) view.findViewById(R.id.expensesList);

        GroupModel group = getGroup();
        List<ExpenseModel> expenses = group == null ?
                new ArrayList<ExpenseModel>() :
                group.getExpenses();
        /// TODO Make this Adapter observe the group
        ItemAdapter<ExpenseModel> adapter = new ItemAdapter<>(getContext(), R.layout.expense_item, expenses);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ExpenseModel expense = (ExpenseModel) parent.getItemAtPosition(position);
                Intent showExpense = new Intent(getContext(), ExpenseDetailsActivity.class);
                showExpense.setData(expense.getUri());
                startActivity(showExpense);
            }
        });

        /// TODO Support multiple groups
        View btnAdd = view.findViewById(R.id.add_button_expense);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), AddExpenses.class);
                startActivity(i);
            }
        });

        return view;
    }
}
