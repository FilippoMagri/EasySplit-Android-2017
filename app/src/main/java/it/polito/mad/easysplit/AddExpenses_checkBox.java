package it.polito.mad.easysplit;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.mad.easysplit.models.dummy.DummyGroupIdentity;

public class AddExpenses_checkBox extends AppCompatActivity {

    private ImageView checkImgView;
    GroupState gm;
    private List<PersonState> list_pm;
    private Map<String ,Boolean> map_results_check_box = new HashMap<String ,Boolean>();
    private ArrayList<String> payerGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setTitle("Select partecipants");
        setContentView(R.layout.activity_add_expenses_check_box);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        populateWithInformationReceived();
        addActionToButtonTopBar();
    }

    public void populateWithInformationReceived() {
        Intent receivedIntent = getIntent();

        //Otherwise: take the bundle, take elements sent by AddExpenses (managing JsonFormat)
        Bundle b = receivedIntent.getExtras();
        String info_received_json_format= (String) b.get("group_info");
        gm = DummyGroupIdentity.fromJSONstatic(info_received_json_format);
        list_pm = gm.getMembers();
        setTitle(gm.getName());

        //add checkboxes Dynamically To The Screen
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
    }

    public void addActionToButtonTopBar (){
        checkImgView = (ImageView) findViewById(R.id.img_confirm_checkbox);
        checkImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),AddExpenses.class);
                Bundle b = new Bundle();
                //Add to the bundle all information about witch people are engaged into the cost
                payerGroup = new ArrayList<String>();
                for (int i=0;i<list_pm.size();i++) {
                    CheckBox cb = (CheckBox) findViewById(i);
                    if (cb.isChecked()) {
                        payerGroup.add(list_pm.get(i).getIdentifier());
                    }
                }
                b.putSerializable("payerGroup",payerGroup);
                //Custom Message if we want
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                intent.putExtras(b);
                intent.putExtra("Uniqid","From_Activity_AddExpenses_checkBox");
                startActivity(intent);
            }
        });
    }
}
