package org.gtp.cocacolaproject.websocket;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import java.util.List;

public class FindLocalIPAddressReceiver extends ResultReceiver {

    public static final int RESULT_CODE_OK = 1100;
    public static final int RESULT_CODE_ERROR = 666;
    public static final String PARAM_EXCEPTION = "exception";
    public static final String PARAM_RESULT = "result";

    private ResultReceiverCallBack  mReceiver;

    public FindLocalIPAddressReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(ResultReceiverCallBack receiver) {
        mReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {

        if (mReceiver != null) {
            if (resultCode == RESULT_CODE_OK){
                mReceiver.onSuccess(resultData.getStringArrayList(PARAM_RESULT));
            } else {
                mReceiver.onError(resultData.getString(PARAM_EXCEPTION));

            }
        }
    }

    public interface ResultReceiverCallBack {
        void onSuccess(List<String> localIPAddressList);
        void onError(String errorMessage);
    }

}
