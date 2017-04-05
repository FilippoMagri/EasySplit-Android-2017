package it.polito.mad.easysplit;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;

import java.sql.ParameterMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.dummy.DummyGroupModel;

public class AddExpenses_checkBox extends AppCompatActivity {

    private ImageView checkImgView;
    GroupModel gm;
    private List<PersonModel> list_pm;
    private Map<String ,Boolean> map_results_check_box = new HashMap<String ,Boolean>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Select partecipants");
        setContentView(R.layout.activity_add_expenses_check_box);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //add checkboxes Dinamically
        gm = new DummyGroupModel();
        list_pm = gm.getMembers();
        LinearLayout ll = (LinearLayout)findViewById(R.id.linearLayoutCheckBox);
        for(int i = 0; i < list_pm.size(); i++) {
            if (i<list_pm.size()) {
                CheckBox cb = new CheckBox(this);
                cb.setText(list_pm.get(i).getIdentifier());
                cb.setId(i);
                cb.setChecked(true);
                cb.setWidth(LinearLayoutCompat.LayoutParams.MATCH_PARENT);
                cb.setGravity(Gravity.END);
                cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                cb.setTag(i);
                Resources r = getApplicationContext().getResources();
                int px = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        16,
                        r.getDisplayMetrics()
                );

                LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT);
                checkParams.setMargins(px, px, px, px);
                ll.addView(cb, checkParams);
            }
        }

        //Add Listener to button
        checkImgView = (ImageView) findViewById(R.id.img_confirm_checkbox);
        //Setting the Listener On the Check Image View On Top Right
        checkImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),AddExpenses.class);
                Bundle b = new Bundle();

                for (int i=0;i<list_pm.size();i++) {
                    CheckBox cb = (CheckBox) findViewById(i);
                    map_results_check_box.put(list_pm.get(i).getIdentifier(),Boolean.valueOf(cb.isChecked()));
                    b.putBoolean(list_pm.get(i).getIdentifier(),Boolean.valueOf(cb.isChecked()));
                }
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                intent.putExtras(b);
                startActivity(intent);
            }
        });

    }
}
