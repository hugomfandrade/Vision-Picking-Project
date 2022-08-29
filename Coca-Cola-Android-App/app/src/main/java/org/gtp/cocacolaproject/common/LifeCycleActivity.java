package org.gtp.cocacolaproject.common;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public abstract class LifeCycleActivity extends Activity {

    protected final String TAG = getClass().getSimpleName();

    @Override
    protected synchronized void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected synchronized void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected synchronized void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected synchronized void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected synchronized void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
