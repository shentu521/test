package valley.api;

/**
 * Created by shawn on 2018/10/12.
 */
import android.view.SurfaceView;

public class hvideo_t {

    public static final int RENDER_TYPE_FULL = 0 ;
    public static final int RENDER_TYPE_ADAPTIVE =  1 ;
    public static final int RENDER_TYPE_CROP = 2 ;
    public static final int RENDER_TYPE_AUTO = 3 ;

    public SurfaceView view = null;
    public int rendertype  = RENDER_TYPE_FULL;
}
