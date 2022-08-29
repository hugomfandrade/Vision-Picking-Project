package org.gtp.cocacolaproject.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public final class NetworkBroadcastReceiverUtils {

    @SuppressWarnings("unused")
    private static final String TAG = NetworkBroadcastReceiverUtils.class.getSimpleName();

    public static BroadcastReceiver register(Context context, final INetworkBroadcastReceiver iNetworkBroadcastReceiver) {

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(context);

                iNetworkBroadcastReceiver.setNetworkAvailable(isNetworkAvailable);
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(broadcastReceiver, intentFilter);

        return broadcastReceiver;
    }

    public static void unregister(Context context, BroadcastReceiver broadcastReceiver) {
        context.unregisterReceiver(broadcastReceiver);
    }

    public interface INetworkBroadcastReceiver {

        void setNetworkAvailable(boolean isNetworkAvailable);
    }

}
