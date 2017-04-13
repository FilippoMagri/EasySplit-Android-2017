package it.polito.mad.easysplit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;

import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.Observer;
import it.polito.mad.easysplit.models.PersonModel;

class SubscribedGroupListAdapter extends ArrayAdapter<GroupModel> implements Observer {
    private PersonModel mPerson;
    private Comparator<GroupModel> mComparator;

    private final class OrderByName implements Comparator<GroupModel> {
        @Override
        public int compare(GroupModel lhs, GroupModel rhs) {
            if (lhs == rhs)
                return 0;
            return lhs.getName().compareTo(rhs.getName());
        }
    }

    SubscribedGroupListAdapter(Context context, PersonModel person) {
        super(context, R.layout.row_item_group, new ArrayList<>(person.getMemberships()));
        mComparator = new OrderByName();
        mPerson = person;
        mPerson.registerObserver(this);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        GroupModel group = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.row_item_group, parent, false);
        }

        TextView label = (TextView) convertView.findViewById(R.id.group_name);
        label.setText(group.getName());
        return convertView;
    }

    @Override
    public void onChanged() {
        this.clear();
        this.addAll(mPerson.getMemberships());
        this.sort(mComparator);
        notifyDataSetChanged();
    }

    @Override
    public void onInvalidated() {
        this.clear();
        notifyDataSetInvalidated();
    }

}