package it.polito.mad.easysplit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CreationGroup extends AppCompatActivity {
private String ListGroup ="listgroup.txt";

    private File mFile =null;
private EditText nameGroup = null;
    private ImageButton valid = null;
    private String finalName = null;
    private Button mread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


       setContentView(R.layout.activity_creation_group);
        // On crée un fichier qui correspond à l'emplacement extérieur
        mFile = new File(Environment.getExternalStorageDirectory().getPath() + "/Android/data/ " + getPackageName() + "/files/" + ListGroup);



       ImageButton back = (ImageButton) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CreationGroup.this, Group.class);
                startActivity(intent);




        valid = (ImageButton) findViewById(R.id.valid);
        valid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    // Flux interne
                    FileOutputStream output = openFileOutput(ListGroup, MODE_PRIVATE);


                   nameGroup = (EditText) findViewById(R.id.nameGroup);
                    finalName = nameGroup.getText().toString();
                    output.write(finalName.getBytes());
                    // On écrit dans le flux interne
                    //output.write(nameGroup.getText().toString());

                    if (output != null)
                        output.close();


                    // Si le fichier est lisible et qu'on peut écrire dedans
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                            && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
                        // On crée un nouveau fichier. Si le fichier existe déjà, il ne sera pas créé
                        mFile.createNewFile();
                        output = new FileOutputStream(mFile);

                        //output.write(nameGroup.getBytes());
                        output.write(finalName.getBytes());

                        if (output != null)
                            output.close();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }});}}

   /* mRead = (Button) findViewById(R.id.read);
    mRead.setOnClickListener(new View.OnClickListener() {

        public void onClick(View pView) {
            try {
                FileInputStream input = openFileInput(PRENOM);
                int value;
                // On utilise un StringBuffer pour construire la chaîne au fur et à mesure
                StringBuffer lu = new StringBuffer();
                // On lit les caractères les uns après les autres
                while((value = input.read()) != -1) {
                    // On écrit dans le fichier le caractère lu
                    lu.append((char)value);
                }
                Toast.makeText(MainActivity.this, "Interne : " + lu.toString(), Toast.LENGTH_SHORT).show();
                if(input != null)
                    input.close();

                if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    lu = new StringBuffer();
                    input = new FileInputStream(mFile);
                    while((value = input.read()) != -1)
                        lu.append((char)value);

                    Toast.makeText(MainActivity.this, "Externe : " + lu.toString(), Toast.LENGTH_SHORT).show();
                    if(input != null)
                        input.close();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
                Intent intent = new Intent(CreationGroup.this, Group.class);
                startActivity(intent);

            }

        });

    }

    private void addPreferencesFromResource(int preferenceResId) {
    }
}*/
