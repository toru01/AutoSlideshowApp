package com.android.techacademy5slide.autoslideshowapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
 /*
 ToDo : スライドさせる画像は、Android端末に保存されているGallery画像を表示させてください（つまり、ContentProviderの利用）
 ToDo : 画面には画像と3つのボタン（進む、戻る、再生/停止）を配置してください
 ToDo : 進むボタンで1つ先の画像を表示し、戻るボタンで1つ前の画像を表示します
 ToDo : 最後の画像の表示時に、進むボタンをタップすると、最初の画像が表示されるようにしてください
 ToDo : 最初の画像の表示時に、戻るボタンをタップすると、最後の画像が表示されるようにしてください
 ToDo : 再生ボタンをタップすると自動送りが始まり、2秒毎にスライドさせてください
 ToDo : 自動送りの間は、進むボタンと戻るボタンはタップ不可にしてください
 ToDo : 再生ボタンをタップすると停止ボタンになり、停止ボタンをタップすると再生ボタンにしてください
 ToDo : 停止ボタンをタップすると自動送りが止まり、進むボタンと戻るボタンをタップ可能にしてください
 ToDo : ユーザがパーミッションの利用を「拒否」した場合にも、アプリの強制終了やエラーが発生しない
 */

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    Timer mTimer;
    TextView textView;

    Handler mHandler = new Handler();

    Button mNextButton;
    Button mBackButton;
    Button mStartButton;

    Boolean PermissionEnabled;

    Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        mNextButton = (Button) findViewById(R.id.next);
        mBackButton = (Button) findViewById(R.id.back);
        mStartButton = (Button) findViewById(R.id.start);

        // パーミッションの許可状態を確認する
        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfo();
                PermissionEnabled=true;
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
                PermissionEnabled=false;
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo();
            PermissionEnabled=true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo();
                    PermissionEnabled=true;
                    Log.d("ANDROID", "許可された");
                } else {
                    PermissionEnabled=false;
                    Log.d("ANDROID", "許可されなかった");
                    //requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
                    // 許可されていないので許可ダイアログを表示する
                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        // パーミッションが必要であることを明示するアプリケーション独自のUIを表示
                        new AlertDialog.Builder(this)
                                .setTitle("パーミッションの許可")
                                .setMessage("このアプリで画像を表示するにはパーミッションが必要です。権限から「ストレージ」パーミッションをONにしてください。")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                                PERMISSIONS_REQUEST_CODE);
                                        openSettings();
                                    }
                                })
                                .create()
                                .show();
                        return;
                    }
                }
                break;
            default:
                break;
        }
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        //Fragmentの場合はgetContext().getPackageName()
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void getContentsInfo() {
        // 画像の情報を取得する
        ContentResolver resolver = getContentResolver();
        cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );
        if(cursor.moveToFirst()) {
            showContentsInfo();
        }

    }

    private void showContentsInfo() {
        // indexからIDを取得し、そのIDから画像のURIを取得する
        int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        Long id = cursor.getLong(fieldIndex);
        Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        ImageView imageVIew = (ImageView) findViewById(R.id.imageView);
        imageVIew.setImageURI(imageUri);
        Log.d("ANDROID", "URI : " + imageUri.toString());
        textView.setText("Slide : "+(cursor.getPosition()+1)+"/"+cursor.getCount());
    }

    @Override
    protected void onStart() {
        super.onStart();


        Log.d("Android", "onStart");

        //前の画像表示
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionEnabled == true) {
                    if (cursor.getPosition() != 0) {
                        cursor.moveToPrevious();
                        showContentsInfo();
                    } else {
                        cursor.moveToLast();
                        showContentsInfo();

                    }
                }else{
                    // パーミッションが必要であることを明示するアプリケーション独自のUIを表示
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("パーミッションの許可")
                            .setMessage("このアプリで画像を表示するにはパーミッションが必要です。権限から「ストレージ」パーミッションをONにしてください。")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                            PERMISSIONS_REQUEST_CODE);
                                    openSettings();
                                }
                            })
                            .create()
                            .show();
                }
            }
        });

        //次の画像表示
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionEnabled == true) {
                    if (cursor.getPosition() < cursor.getCount() - 1) {
                        cursor.moveToNext();
                        showContentsInfo();
                    } else {
                        cursor.moveToFirst();
                        showContentsInfo();
                    }
                }
                else{
                        // パーミッションが必要であることを明示するアプリケーション独自のUIを表示
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("パーミッションの許可")
                                .setMessage("このアプリで画像を表示するにはパーミッションが必要です。権限から「ストレージ」パーミッションをONにしてください。")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                                PERMISSIONS_REQUEST_CODE);
                                        openSettings();
                                    }
                                })
                                .create()
                                .show();
                }
            }
        });

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionEnabled == true) {
                    //ボタンのDisable化
                    mNextButton.setEnabled(false);
                    mBackButton.setEnabled(false);
                    mStartButton.setText("停止");
                    if (mTimer == null) {
                        mTimer = new Timer();

                        mTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                //mTimerSec += 0.1;
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (cursor.getPosition() < cursor.getCount() - 1) {
                                            cursor.moveToNext();
                                            showContentsInfo();
                                        } else {
                                            cursor.moveToFirst();
                                            showContentsInfo();
                                        }
                                    }
                                });
                            }
                        }, 2000, 2000);
                    } else {
                        mTimer.cancel();
                        mTimer = null;
                        //ボタンのEnable化
                        mNextButton.setEnabled(true);
                        mBackButton.setEnabled(true);
                        mStartButton.setText("再生");
                    }
                }else{
                    // パーミッションが必要であることを明示するアプリケーション独自のUIを表示
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("パーミッションの許可")
                            .setMessage("このアプリで画像を表示するにはパーミッションが必要です。権限から「ストレージ」パーミッションをONにしてください。")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                            PERMISSIONS_REQUEST_CODE);
                                    openSettings();
                                }
                            })
                            .create()
                            .show();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Android", "onPause");


    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Android", "onStop");
        if(cursor!=null) {
            cursor.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Android", "onDestroy");
        if(cursor!=null) {
            cursor.close();
        }
    }
}
