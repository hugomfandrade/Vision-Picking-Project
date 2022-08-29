package org.gtp.cocacolaproject.websocket;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import org.gtp.cocacolaproject.common.AsynchronousForCycleStatus;
import org.gtp.cocacolaproject.common.BundleBuilder;
import org.gtp.cocacolaproject.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FindLocalIPAddressService extends Service {

    private static final String TAG = FindLocalIPAddressService.class.getSimpleName();

    private static final String INTENT_EXTRA_RESULT_RECEIVER = "intent_extra_result_receiver";
    private boolean isThreadAlive = true;

    public static void startService(Context context, FindLocalIPAddressReceiver.ResultReceiverCallBack resultReceiverCallBack){
        FindLocalIPAddressReceiver bankResultReceiver = new FindLocalIPAddressReceiver(new Handler(context.getMainLooper()));
        bankResultReceiver.setReceiver(resultReceiverCallBack);

        context.startService(new Intent(context, FindLocalIPAddressService.class)
                .putExtra(INTENT_EXTRA_RESULT_RECEIVER, bankResultReceiver));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {

        final ResultReceiver resultReceiver = intent.getParcelableExtra(INTENT_EXTRA_RESULT_RECEIVER);

        if (resultReceiver == null) return super.onStartCommand(intent, flags, startId);

        final String localIPAddress = NetworkUtils.getIpAddress(this);

        if (localIPAddress == null) {

            resultReceiver.send(FindLocalIPAddressReceiver.RESULT_CODE_ERROR,
                    BundleBuilder.instance()
                            .putString(FindLocalIPAddressReceiver.PARAM_EXCEPTION, "Could not find local IP address")
                            .create());

            return super.onStartCommand(intent, flags, startId);
        }

        //Log.d(TAG, "localIPAddress = " + localIPAddress);

        final String subNet = localIPAddress.substring(0, localIPAddress.lastIndexOf("."));
        Log.d(TAG, "subNet = " + subNet);

        final List<String> localIpAddresses = new ArrayList<String>();

        final int DEFAULT_THREAD_COUNT = 255;
        final int DEFAULT_QUEUE_LENGTH = 255;
        final ThreadPoolExecutor executorService = new ThreadPoolExecutor(
                DEFAULT_THREAD_COUNT,
                DEFAULT_THREAD_COUNT,
                2,
                TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(DEFAULT_QUEUE_LENGTH),
                new ThreadPoolExecutor.CallerRunsPolicy());

        final AsynchronousForCycleStatus n = new AsynchronousForCycleStatus(255);
        final Object syncObj = new Object();
        final Object anotherSyncObj = new Object();

        Handler handler = new Handler();
        Thread t = new Thread();
        t.run();

        for (int ipSuffix = 1 ; ipSuffix <= 255 ; ipSuffix++) {

            /*String host = subNet + "." + ipSuffix;
            Log.e(TAG, "host::" + host);


            if (!NetworkUtils.isReachable(host, WebSocketConstants.TIMEOUT)) {
                n.operationCompleted();
                continue;
            }

            if (!NetworkUtils.isPortOpen(host, WebSocketConstants.PORT, WebSocketConstants.TIMEOUT)) {
                n.operationCompleted();
                continue;
            }

            n.operationCompleted();

            localIpAddresses.add(host);

            if (n.isFinished()) {

                for (String ipAddress : localIpAddresses) {
                    Log.e(TAG, "successfully fetched ip = " + ipAddress);
                }

                resultReceiver.send(FindLocalIPAddressReceiver.RESULT_CODE_OK,
                        BundleBuilder.instance()
                                .putStringList(FindLocalIPAddressReceiver.PARAM_RESULT, localIpAddresses)
                                .create());
            }/**/




            IsReachableAndPortIsOpenTask
                    .instance(WebSocketConstants.PORT, WebSocketConstants.TIMEOUT)
                    .setOnIPAddressListener(new IsReachableAndPortIsOpenTask.OnIPAddressListener() {
                        @Override
                        public void onIsReachableAndOpen(String ip, boolean isReachableAndOpen) {

                            synchronized (syncObj) {
                                //Log.e(TAG, "ip = " + ip + " , " + isReachableAndOpen + " , " + n.toString());

                                n.operationCompleted();

                                if (isReachableAndOpen) {
                                    localIpAddresses.add(ip);
                                }

                                if (n.isFinished()) {

                                    for (String ipAddress : localIpAddresses) {
                                        //Log.e(TAG, "successfully fetched ip = " + ipAddress);
                                    }

                                    resultReceiver.send(FindLocalIPAddressReceiver.RESULT_CODE_OK,
                                            BundleBuilder.instance()
                                                    .putStringList(FindLocalIPAddressReceiver.PARAM_RESULT, localIpAddresses)
                                                    .create());

                                    stopSelf();
                                    /*isThreadAlive = false;

                                    synchronized (anotherSyncObj) {
                                        anotherSyncObj.notify();
                                    }/**/
                                }

                            }
                        }
                    })
                    .executeOnExecutor(executorService, subNet + "." + ipSuffix); /**/
        }

        /*synchronized (anotherSyncObj) {
            try {
                anotherSyncObj.wait(WebSocketConstants.TIMEOUT * 5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.e(TAG, "END!!!");/**/

        /*isThreadAlive = true;
        while (isThreadAlive) {
            if (Thread.interrupted()) {
                return;
            }
        }/**/
        return super.onStartCommand(intent, flags, startId);
    }

    private class MThread extends Thread {

        private final String localIPAddress;
        private final ResultReceiver resultReceiver;

        public MThread(String localIPAddress, ResultReceiver resultReceiver) {
            super();
            this.localIPAddress = localIPAddress;
            this.resultReceiver = resultReceiver;

        }

        @Override
        public void run() {
            super.run();


        }
    }

    private static class IsReachableAndPortIsOpenTask extends AsyncTask<String, Void, Boolean> {

        private String host;
        private final int port;
        private final int timeout;
        private OnIPAddressListener ipAddressListener;

        public static IsReachableAndPortIsOpenTask instance(int port, int timeout) {
            return new IsReachableAndPortIsOpenTask(port, timeout);
        }

        public IsReachableAndPortIsOpenTask setOnIPAddressListener(OnIPAddressListener ipAddressListener) {
            this.ipAddressListener = ipAddressListener;
            return this;
        }

        private IsReachableAndPortIsOpenTask(int port, int timeout) {
            this.port = port;
            this.timeout = timeout;
        }

        @Override
        protected Boolean doInBackground(String... hosts) {
            //Log.e(TAG, "doInBackground = " + host);
            this.host = hosts[0];


            if (!NetworkUtils.isReachable(host, timeout)) {
                //Log.e(TAG, "doInBackground (e) = " + host);
                return false;
            }

            if (!NetworkUtils.isPortOpen(host, port, timeout)) {
                //Log.e(TAG, "doInBackground (por) = " + host);
                return false;
            }

            //Log.e(TAG, "doInBackground (yes) = " + host);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean isReachableAndOpen) {
            //Log.e(TAG, "onPostExecute = " + host);
            if (ipAddressListener != null)
                ipAddressListener.onIsReachableAndOpen(host, isReachableAndOpen);
        }

        public interface OnIPAddressListener {
            void onIsReachableAndOpen(String ip, boolean isReachableAndOpen);
        }
    }
}
