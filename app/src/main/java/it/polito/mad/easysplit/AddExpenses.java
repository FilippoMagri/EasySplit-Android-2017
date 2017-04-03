package it.polito.mad.easysplit;

import android.app.DatePickerDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.dummy.DummyGroupModel;

public class AddExpenses extends AppCompatActivity implements View.OnClickListener {
    Toolbar toolbar;
    ImageView checkImgView;
    private EditText dateEditText;
    private DatePickerDialog datePickerDialog;
    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GroupModel gm = DummyGroupModel.getInstance();
        gm.getMembers();
        setTitle(gm.getName());
        setContentView(R.layout.activity_add_expenses);
        dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ITALY);
        findViewsById();
        setDateTimeField();
        Calendar newDate = Calendar.getInstance();
        dateEditText.setText(dateFormatter.format(newDate.getTime()));
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        checkImgView = (ImageView) findViewById(R.id.img_check);

        toolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_left_black_36dp);
        setSupportActionBar(toolbar);
        //Setting the Listener On The Button To Come Back To the Previous Activity
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        //Setting the Listener On the Check Image View On Top Right
        checkImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private void findViewsById() {
        dateEditText = (EditText) findViewById(R.id.dateEditText);
        dateEditText.setInputType(InputType.TYPE_NULL);
        dateEditText.requestFocus();
    }

    private void setDateTimeField() {
        dateEditText.setOnClickListener(this);

        Calendar newCalendar = Calendar.getInstance();
        datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                dateEditText.setText(dateFormatter.format(newDate.getTime()));
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onClick(View view) {
        if(view == dateEditText) {
            datePickerDialog.show();
        }
    }

}
