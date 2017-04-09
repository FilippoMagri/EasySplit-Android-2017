package it.polito.mad.easysplit;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import it.polito.mad.easysplit.layout.ExpenseListFragment;
import it.polito.mad.easysplit.layout.MemberListFragment;
import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.dummy.DummyGroupModel;


public class GroupDetailsActivity extends AppCompatActivity implements MemberListFragment.OnListFragmentInteractionListener {
    /// TODO The activity should refer to a single specific group (not in the global state)
    public GroupModel getGroup() {
        return ((MyApplication) getApplication()).getGroupModel();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MyApplication app = (MyApplication) getApplicationContext();
        GroupModel dm = app.getGroupModel(); //new DummyGroupModel();
        if (dm == null) {
            dm = new DummyGroupModel();
            app.setGroupModel(dm);
        }

        setTitle(dm.getName());

        LocalPagerAdapter pagerAdapter = new LocalPagerAdapter(getSupportFragmentManager());
        ViewPager pager = (ViewPager) findViewById(R.id.group_details_pager);
        pager.setAdapter(pagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(pager);
    }

    private class LocalPagerAdapter extends FragmentPagerAdapter {
        private LocalPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            /// TODO Set the specific group through arguments in a Bundle
            if (i == 0)
                return new ExpenseListFragment();
            else if (i == 1)
                return new MemberListFragment();
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0)
                return getString(R.string.tab_name_expenses);
            else if (position == 1)
                return getString(R.string.tab_name_balance);
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    public void onListFragmentInteraction(PersonModel person) {
        // Called when a person is clicked in the balance view.
        // Do nothing (for now)
        /// TODO Do something?
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
