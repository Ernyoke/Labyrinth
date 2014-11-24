package com.sapientia.ernyoke.labyrinth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;


public class MainMenu extends Activity implements View.OnClickListener{

    private Button mediumBtn;
    private Button easyBtn;
    private Button hardBtn;
    private Button netBtn;

    private ProgressBar rolling;

    public enum DIFFICULTY{EASY, MEDIUM, HARD, NET};
    private DIFFICULTY currentDiff;

    private static final String URL_LEVELS = "http://ms.sapientia.ro:8009/LabyrinthService/labyrinths/levels";
    private static final String URL_SELECTED_LEVEL = "http://ms.sapientia.ro:8009/LabyrinthService/labyrinths/labyrinth/level";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        easyBtn = (Button) findViewById(R.id.easyBtn);
        mediumBtn = (Button) findViewById(R.id.mediumBtn);
        hardBtn = (Button) findViewById(R.id.hardBtn);
        netBtn = (Button) findViewById(R.id.netBtn);

        easyBtn.setOnClickListener(this);
        mediumBtn.setOnClickListener(this);
        hardBtn.setOnClickListener(this);
        netBtn.setOnClickListener(this);

        rolling = (ProgressBar) findViewById(R.id.rolling);
        rolling.setVisibility(View.INVISIBLE);
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
            Intent settingsIntent = new Intent(MainMenu.this, Settings.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        final Intent intent = new Intent(view.getContext(), LabyrinthActivity.class);
        final Bundle bundle = new Bundle();
        switch (view.getId()) {
            case R.id.easyBtn: {
                bundle.putSerializable(Constants.DIFF_ID, DIFFICULTY.EASY);
                currentDiff = DIFFICULTY.EASY;
                intent.putExtras(bundle);
                setUpPlayground(intent);
                break;
            }
            case R.id.mediumBtn: {
                bundle.putSerializable(Constants.DIFF_ID, DIFFICULTY.MEDIUM);
                currentDiff = DIFFICULTY.MEDIUM;
                intent.putExtras(bundle);
                setUpPlayground(intent);
                break;
            }
            case R.id.hardBtn: {
                bundle.putSerializable(Constants.DIFF_ID, DIFFICULTY.HARD);
                currentDiff = DIFFICULTY.HARD;
                intent.putExtras(bundle);
                setUpPlayground(intent);
                break;
            }

            case R.id.netBtn: {
                if (isNetworkAvailable()) {
                    currentDiff = DIFFICULTY.NET;
                    bundle.putSerializable(Constants.DIFF_ID, DIFFICULTY.NET);
                    HttpGetter REST_levels = new HttpGetter(rolling);
                    try {
                        String result = REST_levels.execute(URL_LEVELS).get();
//                        Log.d(Constants.TAG, result);
                        if(result != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setTitle("Pick a level:");
                            final String[] levels = result.split("#");
                            builder.setItems(levels, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    HttpGetter REST_lab = new HttpGetter(rolling);
                                    try {
                                        String labStr = REST_lab.execute(URL_SELECTED_LEVEL + i).get();
                                        if (labStr != null) {
                                            bundle.putString(Constants.LAB, labStr);
                                            bundle.putInt(Constants.NET_LEVEL, i);
                                            bundle.putInt(Constants.FINAL_NET_LEVEL, levels.length);
                                            intent.putExtras(bundle);
                                            setUpPlayground(intent);
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            builder.show();
                        }
                        else {
                            Toast.makeText(this, R.string.network_error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                else {
                    Toast.makeText(this, getString(R.string.no_network), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void setUpPlayground(Intent intent) {
        this.startActivityForResult(intent, Constants.REQ_CODE);
    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent intent = new Intent(this, LabyrinthActivity.class);
        Bundle bundle = new Bundle();
        if(requestCode == Constants.REQ_CODE) {
            if(resultCode == Constants.NEXT_DIFF) {
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
                bundle.putSerializable(Constants.DIFF_ID,currentDiff);
                intent.putExtras(bundle);
                setUpPlayground(intent);
            }
            else {
                if(resultCode == Constants.NEXT_NET_LEVEL) {
                    //
                }
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
