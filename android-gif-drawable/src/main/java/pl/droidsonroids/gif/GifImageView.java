package pl.droidsonroids.gif;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.glidebitmappool.internal.BitmapPool;
import com.glidebitmappool.internal.LruBitmapPool;

import pl.droidsonroids.gif.utils.BitmapCacheUtils;

/**
 * An {@link ImageView} which tries treating background and src as {@link GifDrawable}
 *
 * @author koral--
 */
public class GifImageView extends ImageView {

    private boolean isNotGif;
    /*=================自定义-begin=====================*/
    //圆角大小，默认为10

    private Paint mPaint;

    // 3x3 矩阵，主要用于缩小放大
    private Matrix mMatrix;

    //渲染图像，使用图像为绘制图形着色
    private BitmapShader mBitmapShader;

    /**
     * 圆形模式
     */
    public static final int MODE_CIRCLE = 1;
    /**
     * 普通模式
     */
    private static final int MODE_NONE = 0;
    /**
     * 圆角模式
     */
    public static final int MODE_ROUND = 2;
    private int currMode = MODE_ROUND;


    private static final String TAG = "CustomFramlayout";
    private Bitmap mBitmap;
    private int size;
    private Context context;

    /**
     * 圆角半径
     */
    private float radius;
    private final float default_radius = 5;   //默认圆角角度,单位:dp

    private RectF rectF = new RectF();
    private Matrix matrix;

    private Paint strokePaint;
    private RectF rectFStroke;

    private boolean mFreezesAnimation;
    private float stroke_width;
    private boolean gif_is_use_border;

    private int lastBitmapWidth;
    private int lastBitmapHeight;


    /**
     * A corresponding superclass constructor wrapper.
     *
     * @param context
     * @see ImageView#ImageView(Context)
     */
    public GifImageView(Context context) {
        super(context);
        this.context = context;
        initViews(null);
    }

    /**
     * Like equivalent from superclass but also try to interpret src and background
     * attributes as {@link GifDrawable}.
     *
     * @param context
     * @param attrs
     * @see ImageView#ImageView(Context, AttributeSet)
     */
    public GifImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        postInit(GifViewUtils.initImageView(this, attrs, 0, 0));
        initViews(attrs);
    }

    /**
     * Like equivalent from superclass but also try to interpret src and background
     * attributes as GIFs.
     *
     * @param context
     * @param attrs
     * @param defStyle
     * @see ImageView#ImageView(Context, AttributeSet, int)
     */
    public GifImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        postInit(GifViewUtils.initImageView(this, attrs, defStyle, 0));
        initViews(attrs);
    }

    /**
     * Like equivalent from superclass but also try to interpret src and background
     * attributes as GIFs.
     *
     * @param context
     * @param attrs
     * @param defStyle
     * @param defStyleRes
     * @see ImageView#ImageView(Context, AttributeSet, int, int)
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public GifImageView(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        this.context = context;
        postInit(GifViewUtils.initImageView(this, attrs, defStyle, defStyleRes));
        initViews(attrs);
    }


    //西直门马爷uid:5bcb425fb2f6546f677e62b8
    private void initViews(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.GifImageView);
            currMode = typedArray.getInt(R.styleable.GifImageView_gif_mode, MODE_CIRCLE);
            radius = typedArray.getDimensionPixelOffset(R.styleable.GifImageView_gif_radius, dp2px(context, 0));

            stroke_width = typedArray.getDimensionPixelOffset(R.styleable.GifImageView_gif_stroke_width, 0);
            gif_is_use_border=typedArray.getBoolean(R.styleable.GifImageView_gif_is_use_border,false);
            if(stroke_width>0){
                int stroke_color = typedArray.getColor(R.styleable.GifImageView_gif_stroke_color, Color.WHITE);
                strokePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setStrokeWidth(stroke_width);
                strokePaint.setColor(stroke_color);
                rectFStroke = new RectF();
            }

            typedArray.recycle();
        }

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMatrix = new Matrix();

//        setScaleType(ScaleType.CENTER_CROP);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /*if(!isNotGif){
            super.onMeasure(widthMeasureSpec,heightMeasureSpec);
            return;
        }
        *//*
         * 当模式为圆形模式的时候，我们强制让宽高一致
         *//*
        if (currMode == MODE_CIRCLE) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int result = Math.min(getMeasuredHeight(), getMeasuredWidth());
            setMeasuredDimension(result, result);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }*/

        /*================================*/
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (currMode == MODE_CIRCLE) {
            size = Math.min(getMeasuredWidth(), getMeasuredHeight());
            if (size > 0) {
                setMeasuredDimension(size, size);
            }
        }

        /*================================*/
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(gif_is_use_border){
            super.onDraw(canvas);
            return;
        }

        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }
        /*Bitmap bitmap = drawableToBitamp(getDrawable());
        mBitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float scale = 1.0f;
        if (!(bitmap.getWidth() == getWidth() && bitmap.getHeight() == getHeight())){
            // 如果图片的宽或者高与view的宽高不匹配，计算出需要缩放的比例；缩放后的图片的宽高，一定要大于我们view的宽高；所以我们这里取大值；
            scale = Math.max(getWidth() * 1.0f / bitmap.getWidth(),
                    getHeight() * 1.0f / bitmap.getHeight());
        }
        // shader的变换矩阵，我们这里主要用于放大或者缩小
        mMatrix.setScale(scale, scale);
        // 设置变换矩阵
        mBitmapShader.setLocalMatrix(mMatrix);
        // 设置shader
        mPaint.setShader(mBitmapShader);
        switch (currMode){
            case MODE_CIRCLE:
                canvas.drawOval(new RectF(0,0,getWidth(),getHeight()),mPaint);
                break;
            case MODE_ROUND:
                canvas.drawRoundRect(new RectF(0,0,getWidth(),getHeight()), currRound, currRound,mPaint);
                break;
        }*/

        /*============================================*/
        float width = getWidth();
        float height = getHeight();

//        Log.i(TAG,"drawable="+drawable.toString()+"\t,hashCode:"+drawable.hashCode());

        mBitmap = drawable2bitmap(drawable, this);

        BitmapShader bitmapShader = new BitmapShader(mBitmap, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);

        int bitmapWidth=mBitmap.getWidth();
        int bitmapHeight = mBitmap.getHeight();

        if(matrix==null || lastBitmapWidth!=bitmapWidth || lastBitmapHeight!=bitmapHeight){
            matrix = buildMatrix(width, height,bitmapWidth,bitmapHeight);
        }

        lastBitmapWidth=bitmapWidth;
        lastBitmapHeight=bitmapHeight;

        bitmapShader.setLocalMatrix(matrix);

        mPaint.setShader(bitmapShader);
        if (currMode == MODE_CIRCLE) {
            canvas.drawCircle(size / 2, size / 2, size / 2, mPaint);
            if(strokePaint!=null){
                canvas.drawCircle(width/2, width/2, width/2-stroke_width/2, strokePaint);
            }

        } else {
            rectF.set(0, 0, width, height);
            canvas.drawRoundRect(rectF, radius, radius, mPaint);
            if(strokePaint!=null){
                if(rectFStroke==null){
                    rectFStroke=new RectF();
                }
                rectFStroke.set(rectF.left+stroke_width/2,rectF.top+stroke_width/2,rectF.right-stroke_width/2,rectF.bottom-stroke_width/2);
                canvas.drawRoundRect(rectFStroke, radius, radius, strokePaint);
            }
        }


        /*============================================*/
    }

    /**
     * 将dip或dp值转换为px值，保证尺寸大小不变
     */
    private int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }

    private Matrix buildMatrix(float width, float height,int bitmapWidth,int bitmapHeight) {
        Log.i(TAG,"构建buildMatrix--->buildMatrix()");
        Matrix matrix = new Matrix();
        float scale = 1;
        float dx = 0;
        float dy = 0;

        if (!(bitmapWidth == width && bitmapHeight == height)){
            // 如果图片的宽或者高与view的宽高不匹配，计算出需要缩放的比例；缩放后的图片的宽高，一定要大于我们view的宽高；所以我们这里取大值；
            scale = Math.max(width * 1.0f / bitmapWidth,height * 1.0f / bitmapHeight);
        }

        if(getScaleType()==ScaleType.CENTER_CROP){
            if(bitmapWidth<width){
                dx=Math.abs(width-bitmapWidth)/2;
            }else if (bitmapWidth>width){
                dx=-Math.abs(width-bitmapWidth)/2;
            }

            if(bitmapHeight<height){
                dy=Math.abs(height-bitmapHeight)/2;
            }else if(bitmapHeight>height){
                dy=-Math.abs(height-bitmapHeight)/2;
            }
            //图片宽度:144,高度:256,控件宽度:222.0,高度:222.0,dx:39.0,dy:-17.0,scale:1.5416666
//            Log.i(TAG,"图片宽度:"+bitmapWidth+",高度:"+bitmapHeight+",控件宽度:"+width+",高度:"+height+",dx:"+dx+",dy:"+dy+",scale:"+scale);

            matrix.setTranslate(dx, dy);
            matrix.postScale(scale, scale,width/2,height/2);
        }else {
            matrix.postScale(scale, scale);
        }
        return matrix;
    }



    private void postInit(GifViewUtils.GifImageViewAttributes result) {
        mFreezesAnimation = result.freezesAnimation;
        if (result.mSourceResId > 0) {
            super.setImageResource(result.mSourceResId);
        }
        if (result.mBackgroundResId > 0) {
            super.setBackgroundResource(result.mBackgroundResId);
        }
    }

    /**
     * Sets the content of this GifImageView to the specified Uri.
     * If uri destination is not a GIF then {@link ImageView#setImageURI(Uri)}
     * is called as fallback.
     * For supported URI schemes see: {@link android.content.ContentResolver#openAssetFileDescriptor(Uri, String)}.
     *
     * @param uri The Uri of an image
     */
    @Override
    public void setImageURI(Uri uri) {
        if (!GifViewUtils.setGifImageUri(this, uri)) {
            super.setImageURI(uri);
        }
    }

    @Override
    public void setImageResource(int resId) {
        if (!GifViewUtils.setResource(this, true, resId)) {
            super.setImageResource(resId);
        }
    }

    public void setImageResource2(int resId) {
        super.setImageResource(resId);
    }

    @Override
    public void setBackgroundResource(int resId) {
        if (!GifViewUtils.setResource(this, false, resId)) {
            super.setBackgroundResource(resId);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Drawable source = mFreezesAnimation ? getDrawable() : null;
        Drawable background = mFreezesAnimation ? getBackground() : null;
        return new GifViewSavedState(super.onSaveInstanceState(), source, background);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof GifViewSavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        GifViewSavedState ss = (GifViewSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        ss.restoreState(getDrawable(), 0);
        ss.restoreState(getBackground(), 1);
    }

    /**
     * Sets whether animation position is saved in {@link #onSaveInstanceState()} and restored
     * in {@link #onRestoreInstanceState(Parcelable)}
     *
     * @param freezesAnimation whether animation position is saved
     */
    public void setFreezesAnimation(boolean freezesAnimation) {
        mFreezesAnimation = freezesAnimation;
    }

    public void setNotGif(boolean notGif) {
        isNotGif = notGif;
    }

    public static Bitmap drawable2bitmap(Drawable drawable, View view) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) drawable;
            return bd.getBitmap();
        }
        // 当设置不为图片，为颜色时，获取的drawable宽高会有问题，所有当为颜色时候获取控件的宽高
        int w = drawable.getIntrinsicWidth() <= 0 ? view.getWidth() : drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight() <= 0 ? view.getHeight() : drawable.getIntrinsicHeight();

        Bitmap bitmap = BitmapCacheUtils.getInstance().getBitmapPool().getDirty(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setHasAlpha(true);


        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);

        BitmapCacheUtils.getInstance().getBitmapPool().put(bitmap);

        return bitmap;
    }
}
