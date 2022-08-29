package org.gtp.cocacolaproject.websocket;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import org.gtp.cocacolaproject.common.MessageBase;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class WebSocketService extends Service implements EchoWebSocketListener.IEchoWebSocketListener {

    private static final String TAG = WebSocketService.class.getSimpleName();

    private Messenger mRequestMessenger = null;
    private RequestHandler mRequestHandler = null;
    private Messenger mReplyTo;

    private OkHttpClient mOkHttpClient;
    private WebSocket mWebSocket;

    public static Intent makeIntent(Context context) {
        return new Intent(context, WebSocketService.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mRequestHandler = new RequestHandler(this);
        mRequestMessenger = new Messenger(mRequestHandler);
        mOkHttpClient = new OkHttpClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mRequestMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mRequestHandler.shutdown();
    }

    private void unregister(Messenger messenger) {
        if (messenger == mReplyTo) {
            mReplyTo = null;
        }
    }

    private void register(Messenger messenger) {
        mReplyTo = messenger;
    }

    private void connect(final String url, final Messenger replyTo, final int requestCode) {

        Request request = new Request.Builder().url(url).build();
        EchoWebSocketListener listener = new EchoWebSocketListener(this);
        mWebSocket = mOkHttpClient.newWebSocket(request, listener);
    }

    private void broadcastRecognition(String messenger) {
        if (mWebSocket != null) {
            mWebSocket.send(messenger);
        }
    }

    private void sendJsonObject(JSONObject jsonObject, Messenger messenger, int requestCode) {
        if (mWebSocket != null) {
            mWebSocket.send(jsonObject == null? null : jsonObject.toString());
        }
    }

    @Override
    public void onOutput(String message) {

        MessageBase requestMessage = MessageBase
                .makeMessage(MessageBase.OperationType.ON_OUTPUT.ordinal(), MessageBase.REQUEST_RESULT_FAILURE);
        requestMessage.setString(message);

        sendRequestMessage(mReplyTo, requestMessage);
    }

    @Override
    public void onClose(String message) {
        Log.e(TAG, "onClose");

        MessageBase requestMessage = MessageBase
                .makeMessage(MessageBase.OperationType.ON_CLOSE.ordinal(), MessageBase.REQUEST_RESULT_FAILURE);
        requestMessage.setString(message);

        sendRequestMessage(mReplyTo, requestMessage);
    }

    @Override
    public void onOpen(String message) {

        MessageBase requestMessage = MessageBase
                .makeMessage(MessageBase.OperationType.ON_OPEN.ordinal(), MessageBase.REQUEST_RESULT_FAILURE);
        requestMessage.setString(message);

        sendRequestMessage(mReplyTo, requestMessage);
    }

    @Override
    public void onFailure(String message) {

        MessageBase requestMessage = MessageBase
                .makeMessage(MessageBase.OperationType.ON_FAILURE.ordinal(), MessageBase.REQUEST_RESULT_FAILURE);
        requestMessage.setString(message);

        sendRequestMessage(mReplyTo, requestMessage);
    }

    private void sendErrorMessage(Messenger replyTo, int requestCode, String errorMessage) {
        MessageBase requestMessage = MessageBase
                .makeMessage(requestCode, MessageBase.REQUEST_RESULT_FAILURE);
        requestMessage.setErrorMessage(errorMessage);

        sendRequestMessage(replyTo, requestMessage);
    }

    private void sendRequestMessage(Messenger replyTo, MessageBase requestMessage) {
        try {
            replyTo.send(requestMessage.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending message back to Activity.", e);
        }
    }

    private static class RequestHandler extends Handler {

        @SuppressWarnings("unused")
        private static final String TAG = RequestHandler.class.getSimpleName();

        private WeakReference<WebSocketService> mService;
        private ExecutorService mExecutorService;
        private boolean isAsync = true;

        RequestHandler(WebSocketService service) {
            mService = new WeakReference<WebSocketService>(service);
            mExecutorService = Executors.newCachedThreadPool();
        }

        public void handleMessage(Message message){
            final MessageBase requestMessage = MessageBase.makeMessage(message);
            final Messenger messenger = requestMessage.getMessenger();

            final int requestCode = requestMessage.getRequestCode();
            final Runnable sendDataToHub;

            if (requestCode == MessageBase.OperationType.CONNECT.ordinal()) {
                sendDataToHub = new Runnable() {
                    @Override
                    public void run() {
                        mService.get().connect(
                                requestMessage.getUrl(),
                                messenger,
                                requestCode);
                    }
                };
            }
            else if (requestCode == MessageBase.OperationType.SEND_JSON_OBJECT.ordinal()) {
                sendDataToHub = new Runnable() {
                    @Override
                    public void run() {
                        mService.get().sendJsonObject(
                                requestMessage.getJsonObject(),
                                messenger,
                                requestCode);
                    }
                };
            }
            else if (requestCode == MessageBase.OperationType.UNREGISTER_CALLBACK.ordinal()) {
                sendDataToHub = new Runnable() {
                    @Override
                    public void run() {
                        mService.get().unregister(messenger);
                    }
                };
            }
            else if (requestCode == MessageBase.OperationType.REGISTER_CALLBACK.ordinal()) {
                sendDataToHub = new Runnable() {
                    @Override
                    public void run() {
                        mService.get().register(messenger);
                    }
                };
            }
            else if (requestCode == MessageBase.OperationType.BROADCAST_RECOGNITION.ordinal()) {
                sendDataToHub = new Runnable() {
                    @Override
                    public void run() {
                        mService.get().broadcastRecognition(requestMessage.getString());
                    }
                };
            }
            else {
                return;
            }

            if (isAsync)
                mExecutorService.execute(sendDataToHub);
            else
                sendDataToHub.run();
        }

        void shutdown() {
            mExecutorService.shutdown();
        }
    }

}
