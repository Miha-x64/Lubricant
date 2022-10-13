
Yet another dynamic blur library.

### Motivation?

Most libraries use RenderScript which shows poor performance to me. Also, most of them use `onPreDraw` as invalidation listener catching all invalidations across the window.


### Name?

In order to apply a post-effect, you need to lubricate Android SDK heavily. Also, [in Russian, “blurry” and “lubricated” is the same word, as well as “unclear” or “smooth”](https://www.multitran.com/m.exe?s=%D1%81%D0%BC%D0%B0%D0%B7%D0%B0%D0%BD%D0%BD%D1%8B%D0%B9&l1=2&l2=1).

### Architecture

#### Layer 1: static blur

`StackBlur` class provides `blurRgb` and `blurArgb` methods for applying blur effect to an `ARGB_8888` bitmap. For `blurArgb` it must be premultiplied which is almost always true.

The Stack Blur Algorithm by Mario Klingemann ported by Yahel Bouaziz found [on StackOverflow](https://stackoverflow.com/a/10028267/3050249) is used and improved for better speed and less memory consumption.

#### Layer 2: DynamicBlur

This class is used to hold a bitmap, and manage downscaling, drawing, and blurring.

#### Layer 3: ViewBlurDrawable, PostEffectView

A ready-to-use drawable taking image from PostEffectView.

Just drop a `PostEffectRecyclerView` into layout and set `rv.blurDrawable(…)` as a background for some overlaid view. See [MainLayout](app/src/main/java/net/aquadc/lubricant/demo/MainLayout.kt) for examples.


#### Blur in Vector Drawables

Under construction.

#### Blur for VideoView?

Check out [VidEffects library](https://github.com/krazykira/VidEffects/) which allows you to apply a shader on a video. This is not enough for quality backdrop effect, so this challenge remains to me.