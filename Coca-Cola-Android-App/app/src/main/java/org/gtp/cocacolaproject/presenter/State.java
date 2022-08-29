package org.gtp.cocacolaproject.presenter;

public enum State {

    INITIAL {
        @Override State nextState() { return WIFI_CONNECTED;}
    },
    WIFI_CONNECTED{
        @Override State nextState() { return WEB_SOCKET_CONNECTED;}
    },
    WEB_SOCKET_CONNECTED{
        @Override State nextState() { return AUTHENTICATED;}
    },
    AUTHENTICATED{
        @Override State nextState() { return RECEIVED_ORDERS;}
    },
    RECEIVED_ORDERS{
        @Override State nextState() { return SHOW_ORDER;}
    },
    SHOW_ORDER{
        @Override State nextState() { return RECOGNIZE_PRODUCT;}
    },
    RECOGNIZE_PRODUCT{
        @Override State nextState() { return SHOW_WEIGHT;}
    },
    SHOW_WEIGHT{
        @Override State nextState() { return RECEIVED_ORDERS;}
    },
    FINALLY{
        @Override State nextState() { return null;}
    };

    abstract State nextState();
}
