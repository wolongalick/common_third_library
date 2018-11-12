package pl.droidsonroids.gif.utils;

import com.glidebitmappool.internal.BitmapPool;
import com.glidebitmappool.internal.LruBitmapPool;

/**
 * 功能:
 * 作者: 崔兴旺
 * 日期: 2018/11/9
 * 备注:
 */
public class BitmapCacheUtils {
    private static BitmapCacheUtils instance = null;

    private BitmapPool bitmapPool=new LruBitmapPool(33177600);

    private BitmapCacheUtils(){
    }

    public static BitmapCacheUtils getInstance() {
        if (instance == null) {
            synchronized (BitmapCacheUtils.class) {
                if (instance == null) {
                    instance = new BitmapCacheUtils();
                }
            }
        }
        return instance;
    }

    public BitmapPool getBitmapPool() {
        return bitmapPool;
    }
}
