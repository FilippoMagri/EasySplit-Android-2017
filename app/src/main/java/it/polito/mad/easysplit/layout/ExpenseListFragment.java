package it.polito.mad.easysplit.layout;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import it.polito.mad.easysplit.AddExpenses;
import it.polito.mad.easysplit.ExpenseDetailsActivity;
import it.polito.mad.easysplit.ItemAdapter;
import it.polito.mad.easysplit.MyApplication;
import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.models.ExpenseModel;
import it.polito.mad.easysplit.models.GroupModel;

public class ExpenseListFragment extends Fragment {
    /// TODO Return the specific group this fragment reads
    public GroupModel getGroup() {
        MyApplication app = (MyApplication) getActivity().getApplication();
        return app.getGroupModel();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_expenses_list, container, false);
        ListView lv = (ListView) view.findViewById(R.id.expensesList);

        List<ExpenseModel> expenses = getGroup().getExpenses();
        ItemAdapter<ExpenseModel> adapter = new ItemAdapter<>(getContext(), R.layout.expense_item, expenses);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ItemAdapter<ExpenseModel> adapter = (ItemAdapter<ExpenseModel>) parent.getAdapter();
                ExpenseModel expense = adapter.getItem(position);

                MyApplication app = (MyApplication) getContext().getApplicationContext();
                app.setCurrentExpense(expense);

                Intent showExpense = new Intent(getContext(), ExpenseDetailsActivity.class);
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
