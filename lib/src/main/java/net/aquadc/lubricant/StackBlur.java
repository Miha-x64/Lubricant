package net.aquadc.lubricant;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.AnyThread;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.nio.IntBuffer;
import java.util.Arrays;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * This is copied from a <a href="https://stackoverflow.com/a/10028267/3050249">StackOverflow answer</a>
 * and optimized for less memory consumption, better locality, and less GC pressure.
 * <p>{@code int[] dv} division memoization was removed: on my load it had 700k elements and gave no speedup.</p>
 * <p>This class is <strong>not</strong> thread-safe: it reuses some buffers.</p>
 *
 * @author Mario Klingemann <mario@quasimondo.com> (Stack Blur Algorithm)
 * @author Yahel Bouaziz <yahel at kayenko.com> (Android port)
 * @author vir us <a href="https://stackoverflow.com/a/33002383/3050249">from StackOverflow</a> (Alpha Channel Support)
 * @author Mike Gorünov (Optimizations)
 */
public final class StackBlur {

    private int[] pix;
    private int[] rgb;
    private int[] vmin;
    private int[] stack;
    private IntBuffer pixBuf;

    @WorkerThread public StackBlur() { // for main thread, use static .stackBlur()
    }

    private static final StackBlur mainThreadInstance = new StackBlur();

    /**
     * Blur RGB channels
     * <p/>
     * If you are using this algorithm in your code please add
     * the following line:
     * Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
     */
    public void blurRgb(@NonNull Bitmap bitmap, @IntRange(from = 1) int radius) {
        check(bitmap, radius, false);

        int div = radius + radius + 1;
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        prepareBuffers(w, h, div);
        int[] pix = this.pix, rgb = this.rgb, vmin = this.vmin, stack = this.stack;

        int rsum, gsum, bsum, x, y, i, p, yp, yi = 0, yw = 0;
        bitmap.copyPixelsToBuffer(pixBuf);
        pixBuf.rewind();

        int ptr, start;
        int sir, rsir, gsir, bsir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;
        int wm = w - 1, hm = h - 1;

        int divsum = (div + 1) >> 1;
        divsum *= divsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                stack[i + radius] = p = pix[yi + min(wm, max(i, 0))];
                rsir = (p & 0xFF0000) >> 16;
                gsir = (p & 0x00FF00) >> 8;
                bsir = (p & 0x0000FF);

                rbs = r1 - abs(i);
                rsum += rsir * rbs;
                gsum += gsir * rbs;
                bsum += bsir * rbs;
                if (i > 0) {
                    rinsum += rsir;
                    ginsum += gsir;
                    binsum += bsir;
                } else {
                    routsum += rsir;
                    goutsum += gsir;
                    boutsum += bsir;
                }
            }
            ptr = radius;

            for (x = 0; x < w; x++) {
                rgb[yi] = ((rsum / divsum) << 16) |
                    ((gsum / divsum) << 8) |
                    (bsum / divsum);

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                start = ptr - radius + div;
                sir = stack[start % div];

                routsum -= (sir & 0xFF0000) >> 16;
                goutsum -= (sir & 0x00FF00) >> 8;
                boutsum -= (sir & 0x0000FF);

                if (y == 0)
                    vmin[x] = min(x + radius + 1, wm);

                stack[start % div] = p = pix[yw + vmin[x]];
                rsir = (p & 0xFF0000) >> 16;
                gsir = (p & 0x00FF00) >> 8;
                bsir = (p & 0x0000FF);

                rinsum += rsir;
                ginsum += gsir;
                binsum += bsir;

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                ptr = (ptr + 1) % div;
                sir = stack[ptr % div];
                rsir = (sir & 0xFF0000) >> 16;
                gsir = (sir & 0x00FF00) >> 8;
                bsir = (sir & 0x0000FF);

                routsum += rsir;
                goutsum += gsir;
                boutsum += bsir;

                rinsum -= rsir;
                ginsum -= gsir;
                binsum -= bsir;

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = max(0, yp) + x;

                int c = stack[i + radius] = rgb[yi];

                rsir = (c & 0xFF0000) >> 16;
                gsir = (c & 0x00FF00) >> 8;
                bsir = (c & 0x0000FF);

                rbs = r1 - abs(i);

                rsum += rsir * rbs;
                gsum += gsir * rbs;
                bsum += bsir * rbs;

                if (i > 0) {
                    rinsum += rsir;
                    ginsum += gsir;
                    binsum += bsir;
                } else {
                    routsum += rsir;
                    goutsum += gsir;
                    boutsum += bsir;
                }

                if (i < hm) yp += w;
            }
            yi = x;
            ptr = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xFF000000 & pix[yi] )
                pix[yi] = (0xFF000000 & pix[yi]) |
                    ((rsum / divsum) << 16) |
                    ((gsum / divsum) << 8) |
                    (bsum / divsum);

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                start = ptr - radius + div;
                sir = stack[start % div];

                routsum -= (sir & 0xFF0000) >> 16;
                goutsum -= (sir & 0x00FF00) >> 8;
                boutsum -= (sir & 0x0000FF);

                if (x == 0) vmin[y] = min(y + r1, hm) * w;
                p = x + vmin[y];

                int c = stack[start % div] = rgb[p];

                rinsum += (c & 0xFF0000) >> 16;
                ginsum += (c & 0x00FF00) >> 8;
                binsum += (c & 0x0000FF);

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                ptr = (ptr + 1) % div;
                sir = stack[ptr];

                rsir = (sir & 0xFF0000) >> 16;
                gsir = (sir & 0x00FF00) >> 8;
                bsir = (sir & 0x0000FF);

                routsum += rsir;
                goutsum += gsir;
                boutsum += bsir;

                rinsum -= rsir;
                ginsum -= gsir;
                binsum -= bsir;

                yi += w;
            }
        }

        bitmap.copyPixelsFromBuffer(pixBuf);
        pixBuf.rewind();
    }

    /**
     * Blur ARGB channels
     * <p/>
     * If you are using this algorithm in your code please add
     * the following line:
     * Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
     */
    public void blurArgb(@NonNull Bitmap bitmap, @IntRange(from=1) int radius) {
        check(bitmap, radius, true);

        int div = radius + radius + 1;
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        prepareBuffers(w, h, div);
        int[] pix = this.pix, rgb = this.rgb, vmin = this.vmin, stack = this.stack;

        int rsum, gsum, bsum, asum, x, y, i, p, yp, yi = 0, yw = 0;

        bitmap.copyPixelsToBuffer(pixBuf);
        pixBuf.rewind();

        int ptr, start;
        int sir, asir, rsir, gsir, bsir;
        int rbs;
        int r1 = radius + 1;
        int aoutsum, routsum, goutsum, boutsum;
        int ainsum, rinsum, ginsum, binsum;
        int wm = w - 1, hm = h - 1;

        int divsum = (div + 1) >> 1;
        divsum *= divsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = ainsum = routsum = goutsum = boutsum = aoutsum = rsum = gsum = bsum = asum = 0;
            for (i = -radius; i <= radius; i++) {
                stack[i + radius] = p = pix[yi + min(wm, max(i, 0))];
                asir = (p & 0xFF000000) >>> 24;
                rsir = (p & 0xFF0000) >> 16;
                gsir = (p & 0x00FF00) >> 8;
                bsir = (p & 0x0000FF);

                rbs = r1 - abs(i);
                asum += asir * rbs;
                rsum += rsir * rbs;
                gsum += gsir * rbs;
                bsum += bsir * rbs;
                if (i > 0) {
                    ainsum += asir;
                    rinsum += rsir;
                    ginsum += gsir;
                    binsum += bsir;
                } else {
                    aoutsum += asir;
                    routsum += rsir;
                    goutsum += gsir;
                    boutsum += bsir;
                }
            }
            ptr = radius;

            for (x = 0; x < w; x++) {
                rgb[yi] = ((asum / divsum)) << 24 |
                    ((rsum / divsum) << 16) |
                    ((gsum / divsum) << 8) |
                    (bsum / divsum);

                asum -= aoutsum;
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                start = ptr - radius + div;
                sir = stack[start % div];

                aoutsum -= (sir & 0xFF000000) >>> 24;
                routsum -= (sir & 0xFF0000) >> 16;
                goutsum -= (sir & 0x00FF00) >> 8;
                boutsum -= (sir & 0x0000FF);

                if (y == 0)
                    vmin[x] = min(x + radius + 1, wm);

                stack[start % div] = p = pix[yw + vmin[x]];

                asir = (p & 0xFF000000) >>> 24;
                rsir = (p & 0xFF0000) >> 16;
                gsir = (p & 0x00FF00) >> 8;
                bsir = (p & 0x0000FF);

                ainsum += asir;
                rinsum += rsir;
                ginsum += gsir;
                binsum += bsir;

                asum += ainsum;
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                ptr = (ptr + 1) % div;
                sir = stack[ptr % div];
                asir = (sir & 0xFF000000) >>> 24;
                rsir = (sir & 0xFF0000) >> 16;
                gsir = (sir & 0x00FF00) >> 8;
                bsir = (sir & 0x0000FF);

                aoutsum += asir;
                routsum += rsir;
                goutsum += gsir;
                boutsum += bsir;

                ainsum -= asir;
                rinsum -= rsir;
                ginsum -= gsir;
                binsum -= bsir;

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = ainsum = routsum = goutsum = boutsum = aoutsum = rsum = gsum = bsum = asum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = max(0, yp) + x;

                int c = stack[i + radius] = rgb[yi];

                asir = (c & 0xFF000000) >>> 24;
                rsir = (c & 0xFF0000) >> 16;
                gsir = (c & 0x00FF00) >> 8;
                bsir = (c & 0x0000FF);

                rbs = r1 - abs(i);

                asum += asir * rbs;
                rsum += rsir * rbs;
                gsum += gsir * rbs;
                bsum += bsir * rbs;

                if (i > 0) {
                    ainsum += asir;
                    rinsum += rsir;
                    ginsum += gsir;
                    binsum += bsir;
                } else {
                    aoutsum += asir;
                    routsum += rsir;
                    goutsum += gsir;
                    boutsum += bsir;
                }

                if (i < hm) yp += w;
            }
            yi = x;
            ptr = radius;
            for (y = 0; y < h; y++) {
                pix[yi] = ((asum / divsum) << 24) |
                    ((rsum / divsum) << 16) |
                    ((gsum / divsum) << 8) |
                    (bsum / divsum);

                asum -= aoutsum;
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                start = ptr - radius + div;
                sir = stack[start % div];

                aoutsum -= (sir & 0xFF000000) >>> 24;
                routsum -= (sir & 0xFF0000) >> 16;
                goutsum -= (sir & 0x00FF00) >> 8;
                boutsum -= (sir & 0x0000FF);

                if (x == 0) vmin[y] = min(y + r1, hm) * w;
                p = x + vmin[y];

                int c = stack[start % div] = rgb[p];

                ainsum += (c & 0xFF000000) >>> 24;
                rinsum += (c & 0xFF0000) >> 16;
                ginsum += (c & 0x00FF00) >> 8;
                binsum += (c & 0x0000FF);

                asum += ainsum;
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                ptr = (ptr + 1) % div;
                sir = stack[ptr];

                asir = (sir & 0xFF000000) >>> 24;
                rsir = (sir & 0xFF0000) >> 16;
                gsir = (sir & 0x00FF00) >> 8;
                bsir = (sir & 0x0000FF);

                aoutsum += asir;
                routsum += rsir;
                goutsum += gsir;
                boutsum += bsir;

                ainsum -= asir;
                rinsum -= rsir;
                ginsum -= gsir;
                binsum -= bsir;

                yi += w;
            }
        }

        bitmap.copyPixelsFromBuffer(pixBuf);
        pixBuf.rewind();
    }

    private static void check(Bitmap bitmap, int radius, boolean alpha) {
        if (radius < 1)
            throw new IllegalArgumentException("radius < 1: " + radius);
        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888)
            throw new UnsupportedOperationException("ARGB_8888 bitmap required, got " + bitmap.getConfig());
        if (alpha && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !bitmap.isPremultiplied())
            throw new UnsupportedOperationException("premultiplied bitmap required");
    }
    private void prepareBuffers(int w, int h, int div) {
        int wh = w * h;
        if (pix == null || pix.length < wh) {
            pix = new int[wh];
            pixBuf = IntBuffer.wrap(pix);
            rgb = new int[wh];
        } else {
            // Bitmap#getPixels will fill pix[]
            Arrays.fill(rgb, 0);
        }

        if (vmin == null || vmin.length < Math.max(w, h)) vmin = new int[Math.max(w, h)];
        else Arrays.fill(vmin, 0);

        if (stack == null || stack.length < div) stack = new int[div];
        else Arrays.fill(stack, 0);
    }

    public void trimMemory() {
        pix = null;
        pixBuf = null;
        rgb = null;
        vmin = null;
        stack = null;
    }

    @SuppressLint("WrongThread") @AnyThread public static StackBlur stackBlur() {
        return Looper.myLooper() == Looper.getMainLooper() ? mainThreadInstance : new StackBlur();
    }
    public static void trimMainMemory() {
        Looper mainLooper = Looper.getMainLooper();
        if (Looper.myLooper() == mainLooper) {
            mainThreadInstance.trimMemory();
        } else {
            new Handler(mainLooper).post(mainThreadInstance::trimMemory);
        }
    }

}
