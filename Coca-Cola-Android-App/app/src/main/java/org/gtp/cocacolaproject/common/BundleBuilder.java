package org.gtp.cocacolaproject.common;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public final class BundleBuilder {

    private final Bundle mBundle;

    public static BundleBuilder instance() {
        return new BundleBuilder();
    }

    private BundleBuilder() {
        mBundle = new Bundle();
    }

    public Bundle create() {
        return mBundle;
    }

    public BundleBuilder putString(String key, String value) {
        mBundle.putString(key, value);
        return this;
    }

    public BundleBuilder putStringArrayList(String key, ArrayList<String> value) {
        mBundle.putStringArrayList(key, value);
        return this;
    }

    public BundleBuilder putStringList(String key, List<String> value) {
        mBundle.putStringArrayList(key, new ArrayList<String>(value));
        return this;
    }
}
