package org.gtp.cocacolaproject.presenter.state;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImpl;
import org.gtp.cocacolaproject.presenter.StateImplListener;
import org.gtp.cocacolaproject.presenter.Transition;
import org.gtp.cocacolaproject.wifi.WifiConstants;

import java.util.List;

public class InitialState extends StateImpl {

    private final static int NETWORK_TYPE_WEP = 1;
    private final static int NETWORK_TYPE_WPA = 2;
    private final static int NETWORK_TYPE_OPEN = 3;

    private final State state = State.INITIAL;

    private final Context mContext;

    private InitialStateThread mInitialStateThread;

    /**
     * enable wifi, connect to wifi
     *
     * @param context context of the application
     * @param listener listener to report progress
     */
    public InitialState(Context context, StateImplListener listener) {
        super(listener);

        mContext = context;

        onProgress(Transition.INITIAL);
    }

    @Override
    public void start() {
        Log.d(TAG, "start of " + state);
        mInitialStateThread = new InitialStateThread();
        mInitialStateThread.start();
    }

    @Override
    public void abort() {
        if (mInitialStateThread != null) {
            mInitialStateThread.interrupt();
            mInitialStateThread = null;
        }
        if (mHandler != null){
            mHandler.shutdown();
        }
    }

    @Override
    public void progress(Transition transition, String message) {
        Log.e(TAG, "progress of " + state + " = " + transition);
        mListener.onProgress(state, transition);
    }

    private class InitialStateThread extends Thread {

        @SuppressWarnings({"UnnecessaryLocalVariable", "ConstantConditions"})
        @Override
        public void run() {
            Log.e(TAG, "start of " + state);

            onProgress(Transition.RUNNING);

            WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            if (wifiManager == null) {
                onProgress(Transition.FAILED);
                return;
            }

            Log.e(TAG, "enable wifi");
            // enable wifi
            boolean isWifiEnabled = wifiManager.isWifiEnabled();

            if (!isWifiEnabled) {
                Log.e(TAG, "wifi is not enabled");
                wifiManager.setWifiEnabled(true);
            }

            // wifi is connected?
            ConnectivityManager connManager = (ConnectivityManager) mContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connManager == null) {
                onProgress(Transition.FAILED);
                return;
            }

            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWifi.isConnected()) {
                Log.e(TAG, "wifi is connected");
                // Do whatever
                onProgress(Transition.SUCCESSFUL);
                return;
            }

            Log.e(TAG, "connect to wifi");
            // connect to wifi saved in Constants
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + WifiConstants.networkSSID + "\"";   // Please note the quotes. String should contain ssid in quotes

            int networkType = NETWORK_TYPE_WPA;
            switch (networkType) {
                case NETWORK_TYPE_WEP:
                    //Then, for WEP network you need to do this:
                    conf.wepKeys[0] = "\"" + WifiConstants.networkPass + "\"";
                    conf.wepTxKeyIndex = 0;
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    break;
                case NETWORK_TYPE_WPA:
                    //For WPA network you need to add passphrase like this:
                    conf.preSharedKey = "\""+ WifiConstants.networkPass +"\"";
                    break;
                case NETWORK_TYPE_OPEN:
                    //For Open network you need to do this:
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    break;

                default: return;
            }

            //Then, you need to add it to Android wifi manager settings:
            wifiManager.addNetwork(conf);

            // And finally, you might need to enable it, so Android connects to it:
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + WifiConstants.networkSSID + "\"")) {
                    Log.e(TAG, "wifi connect to network");
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    break;
                }
            }

            SupplicantState supplicantState;
            do {
                supplicantState = getSupplicantState(wifiManager);
                Log.e(TAG, "wifi is connecting = " + supplicantState);

                if (supplicantState != SupplicantState.ASSOCIATED) {
                    break;
                }

                if (supplicantState != SupplicantState.ASSOCIATING) {
                    Log.e(TAG, "wifi did not connect");
                    onProgress(Transition.FAILED);
                    return;
                }
            } while (supplicantState != SupplicantState.ASSOCIATED);

            Log.e(TAG, "wifi is finally connected");
            // Do whatever
            onProgress(Transition.SUCCESSFUL);
        }

        private SupplicantState getSupplicantState(WifiManager wifiManager) {
            return wifiManager.getConnectionInfo().getSupplicantState();
        }
    }
}
