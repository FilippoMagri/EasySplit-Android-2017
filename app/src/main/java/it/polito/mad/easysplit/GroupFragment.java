package it.polito.mad.easysplit;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import it.polito.mad.easysplit.models.GroupModel;


import static it.polito.mad.easysplit.R.layout.fragment_group;

/**
 * A placeholder fragment containing a simple view.
 */
public class GroupFragment extends ListFragment {

    public GroupFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);



                String[] values =new String[] { "Flat", "Trip in France", "Clara's birthday",
                "Festival Budapest" };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
               R.layout.fragment_group, R.id.label, values);
        setListAdapter(adapter);


    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO implement some logic
    }
}