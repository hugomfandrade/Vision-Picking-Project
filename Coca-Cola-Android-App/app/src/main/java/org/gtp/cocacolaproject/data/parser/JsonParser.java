package org.gtp.cocacolaproject.data.parser;

import android.util.Log;

import org.gtp.cocacolaproject.data.LoginData;
import org.gtp.cocacolaproject.data.Order;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Parses the Json data returned from the Mobile Service Client API
 * and returns the objects that contain this data.
 */
public class JsonParser {

    /**
     * Used for logging purposes.
     */
    private final String TAG = getClass().getSimpleName();


    public LoginData parseLoginData(JSONObject jsonObject) {
        return new LoginData(
                getJsonPrimitive(jsonObject, LoginData.Entry.USERNAME, null),
                getJsonPrimitive(jsonObject, LoginData.Entry.PASSWORD, null));
    }

    public ArrayList<Order> parseOrders(JSONArray result) {

        if (result == null) {
            return new ArrayList<Order>();
        }

        ArrayList<Order> orders = new ArrayList<Order>();

        for (int i = 0 ; i < result.length() ; i++) {
            try {
                orders.add(parseOrder(result.getJSONObject(i)));
            } catch (JSONException e) {
                Log.e(TAG, "Exception caught when parsing Order data: " + e.getMessage());
            }
        }
        return orders;
    }

    public Order parseOrder(JSONObject jsonObject) {

        return new Order(
                getJsonPrimitive(jsonObject, Order.Entry.ORDER_ID, null),
                getJsonPrimitive(jsonObject, Order.Entry.TASK_ORDER, 0),
                getJsonPrimitive(jsonObject, Order.Entry.TASK_ID, null),
                getJsonPrimitive(jsonObject, Order.Entry.DESTINATION_NAME, null),
                Order.stringToCoordinates(getJsonPrimitive(jsonObject, Order.Entry.DESTINATION_COORDINATES, null)),
                getJsonPrimitive(jsonObject, Order.Entry.PRODUCT_SKU, null),
                getJsonPrimitive(jsonObject, Order.Entry.PRODUCT_DESCRIPTION, null),
                getJsonPrimitive(jsonObject, Order.Entry.PRODUCT_WEIGHT, 0f),
                getJsonPrimitive(jsonObject, Order.Entry.PACKAGE_COUNT, 0f),
                getJsonPrimitive(jsonObject, Order.Entry.DISTANCE, 0f));

    }

    public JSONObject format(Order order) {
        JSONObject jsonObject = new JSONObject();

        if (order != null) {
            try {
                jsonObject.put(Order.Entry.ORDER_ID, order.getOrderID());
                jsonObject.put(Order.Entry.TASK_ORDER, order.getTaskOrder());
                jsonObject.put(Order.Entry.TASK_ID, order.getTaskID());
                jsonObject.put(Order.Entry.DESTINATION_NAME, order.getDestinationName());
                jsonObject.put(Order.Entry.ORDER_ID, order.getOrderID());
                jsonObject.put(Order.Entry.DESTINATION_COORDINATES, Order.stringToCoordinates(order.getDestinationCoordinates()));
                jsonObject.put(Order.Entry.PRODUCT_SKU, order.getProductSku());
                jsonObject.put(Order.Entry.PRODUCT_DESCRIPTION, order.getProductDescription());
                jsonObject.put(Order.Entry.PRODUCT_WEIGHT, order.getProductWeight());
                jsonObject.put(Order.Entry.PACKAGE_COUNT, order.getPackageCount());
                jsonObject.put(Order.Entry.DISTANCE, order.getDistance());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }

    private int getJsonPrimitive(JSONObject jsonObject, String jsonMemberName, int defaultValue) {
        try {
            return (int) jsonObject.getInt(jsonMemberName);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private float getJsonPrimitive(JSONObject jsonObject, String jsonMemberName, float defaultValue) {
        try {
            return (float) jsonObject.getDouble(jsonMemberName);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getJsonPrimitive(JSONObject jsonObject, String jsonMemberName, String defaultValue) {
        try {
            return jsonObject.getString(jsonMemberName);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean getJsonPrimitive(JSONObject jsonObject, String jsonMemberName, boolean defaultValue) {
        try {
            return jsonObject.getBoolean(jsonMemberName);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private JSONArray getJsonArray(JSONObject jsonObject, String jsonMemberName, JSONArray defaultValue) {
        try {
            return jsonObject.getJSONArray(jsonMemberName);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private JSONObject getJsonObject(JSONObject jsonObject, String jsonMemberName, JSONObject defaultValue) {
        try {
            return jsonObject.getJSONObject(jsonMemberName);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
