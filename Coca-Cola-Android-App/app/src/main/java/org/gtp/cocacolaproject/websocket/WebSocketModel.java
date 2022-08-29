package org.gtp.cocacolaproject.websocket;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import org.gtp.cocacolaproject.common.MessageBase;
import org.gtp.cocacolaproject.presenter.State;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketModel {

    private final static String TAG = WebSocketModel.class.getSimpleName();

    private final Context mContext;

    private ReplyHandler mReplyHandler = null;
    private Messenger mReplyMessage = null;
    private Messenger mRequestMessengerRef = null;

    public boolean isConnected;

    private Set<WebSocketModelListener> mListeners = new HashSet<WebSocketModelListener>();

    public WebSocketModel(Context context) {
        mContext = context;

        mReplyHandler = new ReplyHandler(this);
        mReplyMessage = new Messenger(mReplyHandler);

        // Bind to the Service.
        bindService();
    }

    public void addListener(WebSocketModelListener listener) {

        mListeners.add(listener);
    }

    public void removeListener(WebSocketModelListener listener) {

        mListeners.remove(listener);
    }

    public void onDestroy(boolean isChangingConfigurations) {
        if (isChangingConfigurations)
            Log.d(TAG,
                    "just a configuration change - unbindService() not called");
        else {
            // Unbind from the Services only if onDestroy() is not
            // triggered by a runtime configuration change.
            unbindService();
            stopService();
            mReplyHandler.shutdown();
        }
    }

    private void bindService() {
        if (mRequestMessengerRef == null) {
            mContext.bindService(WebSocketService.makeIntent(mContext), mServiceConnection, Context.BIND_AUTO_CREATE);
        }

    }

    private void unbindService() {
        if (mRequestMessengerRef != null && mContext != null) {
            unregisterCallback();
            mContext.unbindService(mServiceConnection);
            mRequestMessengerRef = null;
        }
    }

    private void stopService() {
        if (mContext != null) {
            mContext.stopService(WebSocketService.makeIntent(mContext));
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mRequestMessengerRef = new Messenger(binder);
            registerCallback();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mRequestMessengerRef = null;
        }
    };

    private void registerCallback() {

        MessageBase requestMessage = MessageBase.makeMessage(MessageBase.OperationType.REGISTER_CALLBACK.ordinal(),
                mReplyMessage);

        try {
            mRequestMessengerRef.send(requestMessage.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending message to Service.", e);
        }
    }

    private void unregisterCallback() {

        MessageBase requestMessage = MessageBase.makeMessage(MessageBase.OperationType.UNREGISTER_CALLBACK.ordinal(),
                mReplyMessage);

        try {
            mRequestMessengerRef.send(requestMessage.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending message to Service.", e);
        }
    }

    public void sendRecognition(String recognition) {

        MessageBase requestMessage = MessageBase.makeMessage(MessageBase.OperationType.BROADCAST_RECOGNITION.ordinal(),
                mReplyMessage);
        requestMessage.setString(recognition);
        try {
            mRequestMessengerRef.send(requestMessage.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending message to Service.", e);
        }
    }

    private boolean areObjectsSet() {
        if (mRequestMessengerRef == null) {
            Log.e(TAG, "mRequestMessengerRef is null when requesting");
            return false;
        }
        if (mReplyMessage == null) {
            Log.e(TAG, "replyMessage is null when requesting");
            return false;
        }
        return true;
    }

    public boolean connect(String url) {

        if (!areObjectsSet())
            return false;

        MessageBase requestMessage
                = MessageBase.makeMessage(MessageBase.OperationType.CONNECT.ordinal(), mReplyMessage);
        requestMessage.setUrl(url);

        try {
            mRequestMessengerRef.send(requestMessage.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending message back to Activity.", e);
            return false;
        }
        return true;
    }

    public boolean sendJsonObject(JSONObject jsonObject) {

        if (!areObjectsSet())
            return false;

        MessageBase requestMessage
                = MessageBase.makeMessage(MessageBase.OperationType.SEND_JSON_OBJECT.ordinal(), mReplyMessage);
        requestMessage.setJsonObject(jsonObject);

        try {
            mRequestMessengerRef.send(requestMessage.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending message back to Activity.", e);
            return false;
        }
        return true;

    }

    private void onOutput(String message) {
        if (message == null) return;

        for (WebSocketModelListener l : mListeners) {
            l.onWebSocketMessage(message);
        }
    }

    private void onErrorOutput(String message) {
        if (message == null) return;

        for (WebSocketModelListener l : mListeners) {
            l.onWebSocketMessage(message);
        }
    }

    private void onClose() {
        Log.e(TAG, "onClose");

        isConnected = false;

        for (WebSocketModelListener l : mListeners) {
            l.onWebSocketClose();
        }
    }

    private void onOpen() {
        isConnected = true;

        for (WebSocketModelListener l : mListeners) {
            l.onWebSocketOpen();
        }
    }

    private void onFailure(String message) {
        if (message.contains(WebSocketConstants.connectionResetMessage)) {
            isConnected = false;
        }

        for (WebSocketModelListener l : mListeners) {
            l.onWebSocketErrorMessage(message);
        }
    }

    private static class ReplyHandler extends android.os.Handler {

        private WeakReference<WebSocketModel> mModel;
        private ExecutorService mExecutorService;

        ReplyHandler(WebSocketModel service) {
            mModel = new WeakReference<WebSocketModel>(service);
            mExecutorService = Executors.newCachedThreadPool();
        }

        public void handleMessage(Message message){
            super.handleMessage(message);

            if (mModel == null || mModel.get() == null) // Do not handle incoming request
                return;

            final MessageBase requestMessage = MessageBase.makeMessage(message);
            final int requestCode = requestMessage.getRequestCode();
            final int requestResult = requestMessage.getRequestResult();

            final boolean wasSuccessful = requestResult == MessageBase.REQUEST_RESULT_SUCCESS;

            if (requestCode == MessageBase.OperationType.ON_OUTPUT.ordinal()) {
                mModel.get().onOutput(requestMessage.getString());
            }
            else if (requestCode == MessageBase.OperationType.ON_FAILURE.ordinal()) {
                mModel.get().onFailure(requestMessage.getString());
            }
            else if (requestCode == MessageBase.OperationType.ON_CLOSE.ordinal()) {
                Log.e(TAG, "onClose");
                mModel.get().onClose();
            }
            else if (requestCode == MessageBase.OperationType.ON_OPEN.ordinal()) {
                mModel.get().onOpen();
            }
        }
        void shutdown() {
            mExecutorService.shutdown();
        }
    }
}
