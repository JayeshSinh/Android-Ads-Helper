package com.example.app.ads.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.CountDownTimer
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.DrawableCompat
import com.example.app.ads.helper.demo.blurBitmap
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView


/**
 * @author Akshay Harsoda
 * @since 24 Nov 2021
 *
 * NativeAdvancedModel.kt - Simple class which has load and handle your multiple size Native Advanced AD data
 * @param mContext this is a reference to your activity or fragment context
 */
class NativeAdvancedModelHelper(private val mContext: Activity) : AdMobAdsListener {

    private val TAG = "Admob_${javaClass.simpleName}"

    companion object {
        val getNativeAd: NativeAd?
            get() {
                return NativeAdvancedHelper.mNativeAd
            }

        fun destroy() {
            NativeAdvancedHelper.destroy()
        }

        internal fun removeListener() {
            NativeAdvancedHelper.removeListener()
        }
    }

    private var mCloseTimer: AdsCloseTimer? = null

    private var mSize: NativeAdsSize = NativeAdsSize.Medium
    private var mLayout: FrameLayout = FrameLayout(mContext)
    private var mCustomAdView: View? = null
    private var mIsNeedLayoutShow: Boolean = true
    private var mIsAddVideoOptions: Boolean = false
    private var mIsAdLoaded: (isNeedToRemoveCloseButton: Boolean) -> Unit = {}
    private var mOnClickAdClose: () -> Unit = {}

    /**
     * Call this method when you need to load your Native Advanced AD
     * you need to call this method only once in any activity or fragment
     *
     * this method will load your Native Advanced AD with 4 different size like [NativeAdsSize.Medium], [NativeAdsSize.Big], [NativeAdsSize.FullScreen]
     * for Native Advanced AD Size @see [NativeAdsSize] once
     *
     * @param fSize it indicate your Ad Size
     * @param fLayout FrameLayout for add NativeAd View
     * @param isNeedLayoutShow [by Default value = true] pass false if you do not need to show AD at a time when it's loaded successfully
     * @param isAddVideoOptions [by Default value = true] pass false if you don't need to add video option
     * @param isAdLoaded lambda function call when ad isLoaded
     * @param onClickAdClose lambda function call when user click close button of ad
     */
    fun loadNativeAdvancedAd(
        @NonNull fSize: NativeAdsSize,
        @NonNull fLayout: FrameLayout,
        fCustomAdView: View? = null,
        isNeedLayoutShow: Boolean = true,
        isAddVideoOptions: Boolean = true,
        isAdLoaded: (isNeedToRemoveCloseButton: Boolean) -> Unit = {},
        onClickAdClose: () -> Unit = {}
    ) {

        /*if (isAppInTesting) {
            val isTestDevice = AdRequest.Builder().build().isTestDevice(fLayout.context)
            Log.e(TAG, "loadNativeAdvancedAd: isTestDevice::${isTestDevice}")
            if (!isTestDevice) {
                return
            }
        }*/

        Log.i(TAG, "loadAd: ")
        mSize = fSize
        mLayout = fLayout
        mCustomAdView = fCustomAdView
        mIsNeedLayoutShow = isNeedLayoutShow
        mIsAddVideoOptions = isAddVideoOptions
        mIsAdLoaded = isAdLoaded
        mOnClickAdClose = onClickAdClose

        mCloseTimer?.cancel()

        mCloseTimer = AdsCloseTimer(
            millisInFuture = 3000,
            countDownInterval = 1000,
            onFinish = {
                onClickAdClose.invoke()
            }
        )
        mCloseTimer?.start()

        NativeAdvancedHelper.loadNativeAdvancedAd(
            fContext = mContext,
            isAddVideoOptions = isAddVideoOptions,
            fSize = fSize,
            fListener = this,
        )
    }

    @SuppressLint("InflateParams")
    private fun loadAdWithPerfectLayout(
        @NonNull fSize: NativeAdsSize,
        @NonNull fLayout: FrameLayout,
        @NonNull nativeAd: NativeAd,
        fCustomAdView: View? = null,
        isNeedLayoutShow: Boolean = true,
        isAdLoaded: (isNeedToRemoveCloseButton: Boolean) -> Unit = {},
        onClickAdClose: () -> Unit
    ) {

        mCloseTimer?.cancel()
        mCloseTimer = null

        val adView = when (fSize) {

            NativeAdsSize.Big -> {
                mContext.inflater.inflate(
                    com.example.app.ads.helper.R.layout.layout_google_native_ad_big,
                    null
                ) as NativeAdView
            }

            NativeAdsSize.Medium -> {
                mContext.inflater.inflate(
                    com.example.app.ads.helper.R.layout.layout_google_native_ad_medium,
                    null
                ) as NativeAdView
            }

            NativeAdsSize.FullScreen -> {
                if (nativeAd.starRating != null && nativeAd.price != null && nativeAd.store != null) {
                    mContext.inflater.inflate(
                        com.example.app.ads.helper.R.layout.layout_google_native_ad_exit_full_screen_app_store,
                        null
                    ) as ConstraintLayout
                } else {
                    mContext.inflater.inflate(
                        com.example.app.ads.helper.R.layout.layout_google_native_ad_exit_full_screen_website,
                        null
                    ) as NativeAdView
                }
            }

            NativeAdsSize.Custom -> {
                fCustomAdView
                    ?: mContext.inflater.inflate(
                        com.example.app.ads.helper.R.layout.layout_google_native_ad_big,
                        null
                    ) as NativeAdView
            }
        }

        val value = TypedValue()
        mContext.theme.resolveAttribute(com.example.app.ads.helper.R.attr.native_ads_main_color, value, true)

        val unwrappedDrawable = AppCompatResources.getDrawable(mContext, com.example.app.ads.helper.R.drawable.native_ad_button)
        val wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable!!)
        DrawableCompat.setTint(wrappedDrawable, value.data)

        if (fSize == NativeAdsSize.FullScreen) {
            if (nativeAd.starRating != null && nativeAd.price != null && nativeAd.store != null) {
//                No Need To Update Button
            } else {
//                adView.findViewById<Button>(R.id.ad_call_to_action).background = wrappedDrawable
                adView.findViewById<TextView>(R.id.ad_call_to_action).background = wrappedDrawable
            }
        } else {
//            adView.findViewById<Button>(R.id.ad_call_to_action).background = wrappedDrawable
            adView.findViewById<TextView>(R.id.ad_call_to_action).background = wrappedDrawable
        }

        when (fSize) {
            NativeAdsSize.FullScreen -> {
                populateFullScreenNativeAdView(
                    nativeAd,
                    adView.findViewById(R.id.native_ad_view),
                    onClickAdClose
                )
            }

            NativeAdsSize.Custom -> {
                if (fCustomAdView != null) {
                    populateNativeAdView(
                        nativeAd,
                        adView.findViewById(R.id.native_ad_view),
                    )
                } else {
                    populateNativeAdView(nativeAd, adView as NativeAdView)
                }
            }

            else -> {
                populateNativeAdView(nativeAd, adView as NativeAdView)
            }
        }

        fLayout.removeAllViews()
        fLayout.addView(adView)
        fLayout.visibility = if (isNeedLayoutShow) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (fSize == NativeAdsSize.FullScreen && nativeAd.starRating != null && nativeAd.price != null && nativeAd.store != null) {
            isAdLoaded.invoke(true)
        } else {
            isAdLoaded.invoke(false)
        }
    }

    private fun populateFullScreenNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
        onClickAdClose: () -> Unit
    ) {
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.imageView = adView.findViewById(R.id.iv_bg_main_image)

        adView.mediaView?.let { fView ->
            fView.gone
            if (nativeAd.mediaContent != null) {
                nativeAd.mediaContent?.let { fData ->
                    Log.e(TAG, "populateFullScreenNativeAdView: Set Media View")
                    fView.setMediaContent(fData)
                    fView.visible
                }
            } else {
                getNativeAd?.let { fNativeAd ->
                    populateFullScreenNativeAdView(fNativeAd, adView, onClickAdClose)
                }
            }
        }

        adView.imageView?.let { fView ->
            if (nativeAd.images.size > 0) {
                nativeAd.images[0]?.drawable?.let { fData ->
                    fView.visible

                    val bitmap: Bitmap = Bitmap.createBitmap(fData.intrinsicWidth, fData.intrinsicHeight, Bitmap.Config.ARGB_8888)

                    val canvas = Canvas(bitmap)
                    fData.setBounds(0, 0, canvas.width, canvas.height)
                    fData.draw(canvas)

                    blurBitmap(mContext, bitmap)?.let {
                        (fView as ImageView).setImageBitmap(it)
                    }

                }
            }
        }

        adView.advertiserView?.let { fView ->
            fView.gone
            nativeAd.advertiser?.let { fData ->
                (fView as TextView).text = fData
                fView.visible
            }
        }

        adView.bodyView?.let { fView ->
            fView.gone
            nativeAd.body?.let { fData ->
                (fView as TextView).text = fData
                fView.visible
            }
        }

        adView.headlineView?.let { fView ->
            fView.gone
            nativeAd.headline?.let { fData ->
                (fView as TextView).text = fData
                fView.visible
            }
        }

        adView.priceView?.let { fView ->
            fView.gone
            nativeAd.price?.let { fData ->
                (fView as TextView).text = fData
                fView.visible
            }
        }

        adView.storeView?.let { fView ->
            with(fView as TextView) {
                this.gone
                nativeAd.store?.let { fData ->
                    this.text = fData
                    this.isSelected = true
                    this.visible
                    if (fData.equals("Google Play", false)) {
                        (adView.findViewById(R.id.iv_play_logo) as View?)?.visible
                    } else {
                        (adView.findViewById(R.id.iv_play_logo) as View?)?.gone
                    }
                }
            }
        }

        adView.starRatingView?.let { fView ->
            fView.gone
            (adView.findViewById(R.id.txt_rating) as TextView?)?.gone

            nativeAd.starRating?.let { fData ->
                (fView as RatingBar).rating = fData.toFloat()
                fView.visible

                (adView.findViewById(R.id.txt_rating) as TextView?)?.let { txtRating ->
                    txtRating.text = fData.toFloat().toString()
                    txtRating.visible
                }
            }
        }

        adView.iconView?.let { fView ->
            fView.gone

            when {
                nativeAd.icon != null -> {
                    nativeAd.icon!!.drawable?.let { fData ->
                        (fView as ImageView).setImageDrawable(fData)
                        fView.visible
                    }
                }
                nativeAd.images.size > 0 -> {
                    nativeAd.images[0]?.drawable?.let { fData ->
                        (fView as ImageView).setImageDrawable(fData)
                        fView.visible
                    }
                }
                else -> {
                    fView.gone
                }
            }
        }

        adView.callToActionView?.let { fView ->
            fView.gone
            nativeAd.callToAction?.let { fData ->
                (fView as TextView).text = getCamelCaseString(fData)
                fView.isSelected = true
                fView.visible
            }
        }

        if (adView.storeView?.visibility == View.GONE && adView.priceView?.visibility == View.GONE) {
            (adView.findViewById(R.id.cl_ad_price_store) as View?)?.gone
        }

        (adView.findViewById(R.id.ad_call_to_close) as TextView?)?.let {
            it.setOnClickListener {
                onClickAdClose.invoke()
            }
        }

        adView.setNativeAd(nativeAd)
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

        adView.mediaView?.let { fView ->
            fView.gone
            if (nativeAd.mediaContent != null) {
                nativeAd.mediaContent?.let { fData ->
                    Log.e(TAG, "populateNativeAdView: Set Media View")
                    fView.setMediaContent(fData)
                    fView.visible
                }
            } else {
                getNativeAd?.let { fNativeAd ->
                    populateNativeAdView(fNativeAd, adView)
                }
            }
        }

        adView.advertiserView?.let { fView ->
            fView.gone

            nativeAd.advertiser?.let { fData ->
                (fView as TextView).text = fData
                fView.visible
            }
        }

        adView.bodyView?.let { fView ->
            fView.gone
            nativeAd.body?.let { fData ->
                (fView as TextView).text = fData
                fView.visible
            }
        }

        adView.headlineView?.let { fView ->
            fView.gone
            nativeAd.headline?.let { fData ->
                (fView as TextView).text = fData
                fView.visible
            }
        }

        adView.priceView?.let { fView ->
            fView.gone
            nativeAd.price?.let { fData ->
                (fView as TextView).text = fData
                fView.visible
            }
        }

        adView.storeView?.let { fView ->
            with(fView as TextView) {
                this.gone
                nativeAd.store?.let { fData ->
                    this.text = fData
                    this.visible
                }
            }
        }

        adView.starRatingView?.let { fView ->
            fView.gone
            nativeAd.starRating?.let { fData ->
                (fView as RatingBar).rating = fData.toFloat()
                fView.visible
            }
        }

        adView.iconView?.let { fView ->
            fView.gone

            when {
                nativeAd.icon != null -> {
                    nativeAd.icon!!.drawable?.let { fData ->
                        (fView as ImageView).setImageDrawable(fData)
                        fView.visible
                    }
                }
                nativeAd.images.size > 0 -> {
                    nativeAd.images[0]?.drawable?.let { fData ->
                        (fView as ImageView).setImageDrawable(fData)
                        fView.visible
                    }
                }
                else -> {
                    fView.gone
                }
            }
        }

        adView.callToActionView?.let { fView ->
            fView.gone
            nativeAd.callToAction?.let { fData ->
                (fView as Button).text = getCamelCaseString(fData)
                fView.isSelected = true
                fView.visible
            }
        }

        adView.setNativeAd(nativeAd)
    }

    private fun getCamelCaseString(text: String): String {

        val words: Array<String> = text.split(" ").toTypedArray()

        val builder = StringBuilder()
        for (i in words.indices) {
            var word: String = words[i]
            word = if (word.isEmpty()) word else Character.toUpperCase(word[0])
                .toString() + word.substring(1).lowercase()
            builder.append(word)
            if (i != (words.size - 1)) {
                builder.append(" ")
            }
        }
        return builder.toString()
    }

    override fun onAdClosed(isShowFullScreenAd: Boolean, needToLoadNewAd: Boolean) {
        super.onAdClosed(isShowFullScreenAd, needToLoadNewAd)
        Log.i(TAG, "onAdClosed: ")

        mLayout.removeAllViews()

        loadNativeAdvancedAd(
            fSize = mSize,
            fLayout = mLayout,
            fCustomAdView = mCustomAdView,
            isNeedLayoutShow = mIsNeedLayoutShow,
            isAddVideoOptions = mIsAddVideoOptions,
            isAdLoaded = mIsAdLoaded,
            onClickAdClose = mOnClickAdClose
        )
    }

    override fun onNativeAdLoaded(nativeAd: NativeAd) {
        super.onNativeAdLoaded(nativeAd)

        Log.e(TAG, "onNativeAdLoaded: ")

        loadAdWithPerfectLayout(
            fSize = mSize,
            fLayout = mLayout,
            nativeAd = nativeAd,
            fCustomAdView = mCustomAdView,
            isNeedLayoutShow = mIsNeedLayoutShow,
            isAdLoaded = mIsAdLoaded,
            onClickAdClose = mOnClickAdClose
        )
    }

    inner class AdsCloseTimer(
        private val millisInFuture: Long,
        countDownInterval: Long,
        private val onFinish: () -> Unit
    ) : CountDownTimer(millisInFuture, countDownInterval) {
        override fun onTick(millisUntilFinished: Long) {

        }

        override fun onFinish() {
            onFinish.invoke()
        }
    }
}