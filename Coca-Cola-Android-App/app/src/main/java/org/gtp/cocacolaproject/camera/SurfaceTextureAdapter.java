package org.gtp.cocacolaproject.camera;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

public class SurfaceTextureAdapter implements TextureView.SurfaceTextureListener {

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
