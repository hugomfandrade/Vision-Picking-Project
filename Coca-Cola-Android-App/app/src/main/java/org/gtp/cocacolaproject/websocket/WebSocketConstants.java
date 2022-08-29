package org.gtp.cocacolaproject.websocket;

public interface WebSocketConstants {
    int TIMEOUT = 1000;
    int PORT = 9090;

    String connectionResetMessage = "Connection reset";

    String OPERATION_TYPE = "OperationType";
    String OPERATION_RESULT = "OperationResult";
    String OPERATION_RESULT_OK = "Ok";
    String OPERATION_RESULT_NO = "No";
    String REPORT_RECOGNITION_RESULT = "Report-Recognition-Result";
    String REPORT_PACKAGE_VOLUME = "Report-Package-Volume";
    String ORDER = "Order";

    enum OperationType {

        AUTHENTICATE {
            @Override
            public String shortName() {
                return "Authenticate";
            }
        },
        REQUEST_ORDER_LIST {
            @Override
            public String shortName() {
                return "Request-Order-List";
            }
        },
        ORDER_LIST {
            @Override
            public String shortName() {
                return "Order-List";
            }
        },
        REQUEST_DESTINATION_CONFIRMATION {
            @Override
            public String shortName() {
                return "Request-Destination-Confirmation";
            }
        },
        DESTINATION_CONFIRMATION {
            @Override
            public String shortName() {
                return "Destination-Confirmation";
            }
        },
        REQUEST_PACKAGE_VOLUME_CONFIRMATION {
            @Override
            public String shortName() {
                return "Request-Package-Volume-Confirmation";
            }
        },
        PACKAGE_VOLUME_CONFIRMATION {
            @Override
            public String shortName() {
                return "Package-Volume-Confirmation";
            }
        },
        REPORT_PACKAGE_VOLUME {
            @Override
            public String shortName() {
                return "Report-Package-Volume";
            }
        },
        REPORT_RECOGNITION {
            @Override
            public String shortName() {
                return "Report-Recognition";
            }
        };

        public abstract String shortName();
    }
}
