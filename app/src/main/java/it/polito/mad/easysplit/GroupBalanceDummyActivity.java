package it.polito.mad.easysplit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import it.polito.mad.easysplit.layout.MemberListFragment;
import it.polito.mad.easysplit.models.PersonModel;

public class GroupBalanceDummyActivity extends AppCompatActivity
        implements MemberListFragment.OnListFragmentInteractionListener
{
    @Override
    public void onListFragmentInteraction(PersonModel person) {
        /// TODO Do something with the person?
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_group_balance_dummy);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
}
