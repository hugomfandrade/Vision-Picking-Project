package org.gtp.cocacolaproject.view;

import android.app.Activity;
import android.content.Context;

import org.gtp.cocacolaproject.data.Order;

public interface MainActivityListener {

    Context getApplicationContext();

    Activity getActivity();

    void displayToastMessage(String message);

    void displayMessage(String message);

    void hideMessage();

    void showButton(String text);

    void hideButton();

    void showOrder(Order order);

    void hideOrder();

    void setRecognitionTimerVisibilityState(boolean isVisible);

    void setRecognitionTimerState(boolean enable);

    void showRecognition(String message);
}
