package org.gtp.cocacolaproject.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;
import android.widget.Toast;


/**
 * Support methods used to implement the API permission code model.
 */
public final class PermissionUtils {

    /**
     * String used in logging output.
     */
    @SuppressWarnings("unused")
    private static final String TAG = PermissionUtils.class.getSimpleName();

    /**
     * Ensure this class is only used as a utility.
     */
    private PermissionUtils() {
        throw new AssertionError();
    }

    /**
     * RequestListener ID used in permission request calls.
     */
    private static final int REQUEST_CAMERA = 1;

    public static void requestPermission(Activity activity, String permission) {
        final boolean locationPermission = hasGrantedPermission(activity, permission);;

        // Permission has not been granted.
        if (!locationPermission) {
            PermissionUtils.requestPermission(
                    activity,
                    permission,
                    (ViewGroup) activity.findViewById(android.R.id.content));
        }
    }

    public interface OnRequestPermissionsResultCallback {
        void onRequestPermissionsResult(String permissionType, boolean wasPermissionGranted);
    }

    public static boolean hasGrantedPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Helper method to be called from the activity's
     * onRequestPermissionResults() hook method which is called when a
     * permissions request has been completed.
     *
     * @return returns true if the permission is handled; {@code false} if not.
     */
    @SuppressWarnings({"UnusedReturnValue", "UnusedParameters"})
    public static boolean onRequestPermissionsResult(
            Activity activity,
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults,
            OnRequestPermissionsResultCallback callback) {

        if (requestCode == PermissionUtils.REQUEST_CAMERA) {

            ViewGroup layout = (ViewGroup) activity.findViewById(android.R.id.content);
            assert layout != null;

            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Show granted replay message.
                //Snackbar.make(layout, R.string.permission_available_fine_location, Snackbar.LENGTH_SHORT).show();

                if (callback != null)
                    callback.onRequestPermissionsResult(
                            Manifest.permission.CAMERA,
                            true);
            }
            else {
                // Show denied replay message.
                //Snackbar.make(layout, R.string.permissions_not_granted, Snackbar.LENGTH_SHORT).show();

                if (callback != null)
                    callback.onRequestPermissionsResult(
                            Manifest.permission.CAMERA,
                            false);
            }
            // Signal that we have handled the permissions.
            return true;
        } else {
            // Signal that we did not handle the permissions.
            return false;
        }
    }



    /**
     * Requests the fine location permission.
     * If the permission has been denied previously, a SnackBar
     * will prompt the user to grant the permission, otherwise
     * it is requested directly.
     */
    private static void requestPermission(final Activity activity,
                                          final String permission,
                                          final ViewGroup layout) {

        boolean shouldRequestPermission =
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);

        // Request anyway;
        shouldRequestPermission = false;

        if (shouldRequestPermission) {
            // Provide an additional rationale to the user if the permission
            // was not granted and the user would benefit from additional
            // context for the use of the permission. For example if the user
            // has previously denied the permission.
            Toast.makeText(activity, "Camera permission is required.", Toast.LENGTH_LONG).show();

        } else {
            // Permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission
                                    .CAMERA},
                    REQUEST_CAMERA);
        }
    }
}

