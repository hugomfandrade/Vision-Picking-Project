package org.gtp.cocacolaproject.presenter;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.gtp.cocacolaproject.common.BundleBuilder;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class StateImpl {

    protected final String TAG = getClass().getSimpleName();

    protected final StateImplListener mListener;

    private final boolean isMainThread;

    protected final MHandler mHandler;

    public StateImpl(StateImplListener listener) {
        mListener = listener;

        isMainThread = Looper.getMainLooper().getThread() == Thread.currentThread();

        mHandler = new MHandler(this);
    }

    public abstract void start();

    public abstract void abort();

    public abstract void progress(Transition transition, String message);

    protected final void onProgress(Transition transition) {
        mHandler.onProgress(transition);
    }

    protected final void onProgress(Transition transition, String message) {
        mHandler.onProgress(transition, message);
    }

    public boolean onButtonClicked() {
        return false;
    }

    public static class MHandler extends Handler {

        private static final String MESSAGE = "message";

        private final WeakReference<StateImpl> mTaskRef;
        private final ExecutorService mExecutorService;

        MHandler(StateImpl state) {
            super(Looper.getMainLooper());
            mTaskRef = new WeakReference<StateImpl>(state);
            mExecutorService = Executors.newCachedThreadPool();
        }

        void onProgress(Transition transition) {
            Message message = obtainMessage();
            message.obj = transition;
            message.sendToTarget();
        }

        void onProgress(Transition transition, String str) {
            Message message = obtainMessage();
            message.obj = transition;
            message.setData(BundleBuilder.instance().putString(MESSAGE, str).create());
            message.sendToTarget();
        }

        @Override
        public void handleMessage(Message message){
            final Message m = Message.obtain(message);

            if (mTaskRef.get() != null) {
                if (mTaskRef.get().isMainThread) {
                    // Run on main thread
                    String str = m.getData() == null ? null : m.getData().getString(MESSAGE);
                    mTaskRef.get().progress((Transition) m.obj, str);
                }
                else {
                    // Run on another thread
                    mExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mTaskRef.get() != null) {
                                String str = m.getData() == null ? null : m.getData().getString(MESSAGE);
                                mTaskRef.get().progress((Transition) m.obj, str);
                            }
                        }
                    });
                }
            }
        }

        public void shutdown() {
            mExecutorService.shutdown();
        }
    }
}
