package org.gtp.cocacolaproject.data;

public class Order {

    private String orderID;
    private int taskOrder;
    private String taskID;
    private String destinationName;
    private float[] destinationCoordinates;
    private String productSku;
    private String productDescription;
    private float productWeight;
    private float packageCount;
    private float distance;

    public Order(String orderID,
                 int taskOrder,
                 String taskID,
                 String destinationName,
                 float[] destinationCoordinates,
                 String productSku,
                 String productDescription,
                 float productWeight,
                 float packageCount,
                 float distance) {
        this.orderID = orderID;
        this.taskOrder = taskOrder;
        this.taskID = taskID;
        this.destinationName = destinationName;
        this.destinationCoordinates = destinationCoordinates;
        this.productSku = productSku;
        this.productDescription = productDescription;
        this.productWeight = productWeight;
        this.packageCount = packageCount;
        this.distance = distance;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public void setTaskOrder(int taskOrder) {
        this.taskOrder = taskOrder;
    }

    public void setTaskID(String taskID) {
        this.taskID = taskID;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public void setDestinationCoordinates(float[] destinationCoordinates) {
        this.destinationCoordinates = destinationCoordinates;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public void setProductWeight(float productWeight) {
        this.productWeight = productWeight;
    }

    public void setPackageCount(float packageCount) {
        this.packageCount = packageCount;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public static float[] stringToCoordinates(String coordinatesStr) {
        if (coordinatesStr == null) {
            return new float[0];
        }
        else {
            if (coordinatesStr.contains("(") && coordinatesStr.contains(")") && coordinatesStr.contains(",")) {
                coordinatesStr = coordinatesStr.substring(coordinatesStr.indexOf("(") + 1, coordinatesStr.lastIndexOf(")"));
                String[] s = coordinatesStr.split(",");
                float[] coordinates = new float[s.length];

                for (int i = 0 ; i < s.length ; i++) {
                    try {
                        coordinates[i] = Float.parseFloat(s[i]);
                    }
                    catch (NumberFormatException e) {

                    }
                }
                return coordinates;
            }
            else {
                return new float[0];
            }
        }
    }

    public static String stringToCoordinates(float[] coordinates) {
        if (coordinates == null) {
            return null;
        }
        else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("(");

            for (int i = 0 ; i < coordinates.length ; i++) {
                if (i != 0) {
                    stringBuilder.append(",");
                }
                stringBuilder.append(Float.toString(coordinates[i]));
            }
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    public String getOrderID() {
        return orderID;
    }

    public int getTaskOrder() {
        return taskOrder;
    }

    public String getTaskID() {
        return taskID;
    }

    public float[] getDestinationCoordinates() {
        return destinationCoordinates;
    }

    public String getProductSku() {
        return productSku;
    }

    public float getProductWeight() {
        return productWeight;
    }

    public float getPackageCount() {
        return packageCount;
    }

    public float getDistance() {
        return distance;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public static class Entry {
        public static final String ORDER_ID = "order-id";
        public static final String TASK_ORDER = "task-order";
        public static final String TASK_ID = "task-id";
        public static final String DESTINATION_NAME = "destination-name";
        public static final String DESTINATION_COORDINATES = "destination-coordinates";
        public static final String PRODUCT_SKU = "product-sku";
        public static final String PRODUCT_DESCRIPTION = "product-description";
        public static final String PRODUCT_WEIGHT = "product-weight";
        public static final String PACKAGE_COUNT = "package-count";
        public static final String DISTANCE = "distance";
    }
}
