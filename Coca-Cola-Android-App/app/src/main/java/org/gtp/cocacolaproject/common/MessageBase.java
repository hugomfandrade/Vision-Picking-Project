package org.gtp.cocacolaproject.common;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MessageBase {

    private static final String LOCAL_IP_ADDRESS_LIST = "localIpAddresses";
    private static final String JSON_OBJECT = "jsonObject";
    private static final String JSON_ARRAY = "jsonArray";

    private static final String URL       = "url";
    private static final String MESSAGE       = "message";
    private static final String ERROR_MESSAGE       = "ERROR_MESSAGE";

    // Data Extras Key
    public static final int REQUEST_RESULT_FAILURE = 0;
    public static final int REQUEST_RESULT_SUCCESS = 1;

    public enum OperationType {
        @SuppressWarnings("unused") OPERATION_UNKNOWN,

        START,
        CONNECT,
        SEND_JSON_OBJECT,
        REGISTER_CALLBACK,
        UNREGISTER_CALLBACK,
        ON_OUTPUT,
        ON_CLOSE,
        ON_OPEN,
        ON_FAILURE,
        BROADCAST_RECOGNITION
    }

    /**
     * Message object
     */
    private Message mMessage;

    /**
     * Private constructor. Initializes Message
     */
    private MessageBase(Message message) {
        mMessage = message;
    }

    /**
     * Factory Method
     */
    public static MessageBase makeMessage(Message message) {
        return new MessageBase(Message.obtain(message));
    }

    /**
     * Factory Method
     */
    public static MessageBase makeMessage(int requestCode, int requestResult) {
        // Create a RequestMessage that holds a reference to a Message
        // created via the Message.obtain() factory method.
        MessageBase requestMessage = new MessageBase(Message.obtain());
        requestMessage.setData(new Bundle());
        requestMessage.setRequestCode(requestCode);
        requestMessage.setRequestResult(requestResult);

        // Return the message to the caller.
        return requestMessage;
    }

    /**
     * Factory Method
     */
    public static MessageBase makeMessage(int requestCode, Messenger messenger) {
        // Create a RequestMessage that holds a reference to a Message
        // created via the Message.obtain() factory method.
        MessageBase requestMessage = new MessageBase(Message.obtain());
        requestMessage.setData(new Bundle());
        requestMessage.setRequestCode(requestCode);
        requestMessage.setMessenger(messenger);

        // Return the message to the caller.
        return requestMessage;
    }

    public Message getMessage() {
        return mMessage;
    }

    /**
     * Sets provided Bundle as the data of the underlying Message
     * @param data - the Bundle to set
     */
    public void setData(Bundle data) {
        mMessage.setData(data);
    }

    /**
     * Accessor method that sets the result code
     * @param resultCode - the code tooset
     */
    public void setRequestCode(int resultCode) {
        mMessage.what = resultCode;
    }

    /**
     * Accessor method that returns the result code of the message, which
     * can be used to check if the download succeeded.
     */
    public int getRequestCode() {
        return mMessage.what;
    }

    /**
     * Accessor method that sets the result code
     * @param requestResult - the code to set
     */
    public void setRequestResult(int requestResult) {
        mMessage.arg1 = requestResult;
    }

    /**
     * Accessor method that returns the result code of the message, which
     * can be used to check if the download succeeded.
     */
    public int getRequestResult() {
        return mMessage.arg1;
    }

    /**
     * Accessor method that sets Messenger of the Message
     */
    public void setMessenger(Messenger messenger) {
        mMessage.replyTo = messenger;
    }

    /**
     * Accessor method that returns Messenger of the Message.
     */
    public Messenger getMessenger() {
        return mMessage.replyTo;
    }

    public void setIpAddressList(ArrayList<String> localIpAddresses) {
        mMessage.getData().putStringArrayList(LOCAL_IP_ADDRESS_LIST, localIpAddresses);
    }

    public List<String> getIpAddressList() {
        return mMessage.getData().getStringArrayList(LOCAL_IP_ADDRESS_LIST);
    }

    public void setJsonObject(JSONObject jsonObject) {
        mMessage.getData().putString(JSON_OBJECT, jsonObject.toString());
    }


    public JSONObject getJsonObject() {
        try {
            return new JSONObject(mMessage.getData().getString(JSON_OBJECT));
        } catch (JSONException e) {
            return null;
        }
    }

    public void setJsonArray(JSONArray jsonArray) {
        mMessage.getData().putString(JSON_ARRAY, jsonArray.toString());
    }


    public JSONArray getJsonArray() {
        try {
            return new JSONArray(mMessage.getData().getString(JSON_ARRAY));
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     *
     */
    public void setUrl(String url) {
        mMessage.getData().putString(URL, url);
    }

    /**
     *
     */
    public String getUrl() {
        return mMessage.getData().getString(URL);
    }

    /**
     *
     */
    public void setString(String message) {
        mMessage.getData().putString(MESSAGE, message);
    }

    /**
     *
     */
    public String getString() {
        return mMessage.getData().getString(MESSAGE);
    }

    /**
     *
     */
    public void setErrorMessage(String errorMessage) {
        mMessage.getData().putString(ERROR_MESSAGE, errorMessage);
    }

    /**
     *
     */
    public String getErrorMessage() {
        return mMessage.getData().getString(ERROR_MESSAGE);
    }
}
