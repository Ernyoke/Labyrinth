package com.sapientia.ernyoke.labyrinth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


public class MainMenu extends Activity implements View.OnClickListener{

    private Button mediumBtn;
    private Button easyBtn;
    private Button hardBtn;

    public enum DIFFICULTY{EASY, MEDIUM, HARD};
    private DIFFICULTY currentDiff;
    public static final String DIFF_ID = "DIFFICULTY";
    public static final int REQ_CODE = 1;
    public static final int NEXT_DIFF = 1;
    public static final int EXIT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        easyBtn = (Button) findViewById(R.id.easyBtn);
        mediumBtn = (Button) findViewById(R.id.mediumBtn);
        hardBtn = (Button) findViewById(R.id.hardBtn);

        easyBtn.setOnClickListener(this);
        mediumBtn.setOnClickListener(this);
        hardBtn.setOnClickListener(this);


    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), LabyrinthActivity.class);
        Bundle bundle = new Bundle();
        switch (view.getId()) {
            case R.id.easyBtn: {
                bundle.putSerializable(DIFF_ID, DIFFICULTY.EASY);
                currentDiff = DIFFICULTY.EASY;
                break;
            }
            case R.id.mediumBtn: {
                bundle.putSerializable(DIFF_ID, DIFFICULTY.MEDIUM);
                currentDiff = DIFFICULTY.MEDIUM;
                break;
            }
            case R.id.hardBtn: {
                bundle.putSerializable(DIFF_ID, DIFFICULTY.HARD);
                currentDiff = DIFFICULTY.HARD;
                break;
            }
        }
        intent.putExtras(bundle);
        setUpPlayground(intent);
    }

    private void setUpPlayground(Intent intent) {
        this.startActivityForResult(intent, REQ_CODE);
    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent intent = new Intent(this, LabyrinthActivity.class);
        Bundle bundle = new Bundle();
        if(requestCode == REQ_CODE) {
            if(resultCode == NEXT_DIFF) {
                switch (currentDiff) {
                    case EASY: {
                        currentDiff = DIFFICULTY.MEDIUM;
                        break;
                    }
                    case MEDIUM: {
                        currentDiff = DIFFICULTY.HARD;
                        break;
                    }
                    case HARD: {
                        currentDiff = DIFFICULTY.EASY;
                        break;
                    }
                }
                bundle.putSerializable(DIFF_ID,currentDiff);
                intent.putExtras(bundle);
                setUpPlayground(intent);
            }
        }
    }
}
