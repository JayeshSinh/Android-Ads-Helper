@file:Suppress("unused")

package com.example.app.ads.helper

import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import com.example.app.ads.helper.dialogs.FullScreenNativeAdDialog
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * @author Akshay Harsoda
 * @since 05 Aug 2021
 *
 * InterstitialAdHelper.kt - Simple object which has load and handle your Interstitial AD data
 */
object InterstitialAdHelper {

    private val TAG = "Admob_${javaClass.simpleName}"

    private var mInterstitialAdMob: InterstitialAd? = null

    private var mIsAdMobAdLoaded = false
    private var mIsAnyAdShow = false

    private var mListener: AdMobAdsListener? = null

    private var mIsShowFullScreenNativeAd: Boolean = true

    internal fun loadAd(@NonNull fContext: Context, @NonNull fListener: AdMobAdsListener) {

        var lInterstitialAd: InterstitialAd?

        InterstitialAd.load(
            fContext,
            admob_interstitial_ad_id ?: fContext.getStringRes(R.string.admob_interstitial_ad_id),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.i(TAG, "onAdLoaded: ")

                    lInterstitialAd = interstitialAd
                    fListener.onInterstitialAdLoaded(interstitialAd = interstitialAd)

                    lInterstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent()
                                Log.i(TAG, "onAdDismissedFullScreenContent: ")
                                lInterstitialAd = null
                                isAnyAdShowing = false
                                isInterstitialAdShow = false
                                fListener.onAdClosed(needToLoadNewAd = true)
                            }

                            override fun onAdShowedFullScreenContent() {
                                super.onAdShowedFullScreenContent()
                                Log.i(TAG, "onAdShowedFullScreenContent: ")
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                super.onAdFailedToShowFullScreenContent(adError)
                                Log.i(
                                    TAG,
                                    "onAdFailedToShowFullScreenContent: \nErrorMessage::${adError.message}\nErrorCode::${adError.code}"
                                )
                            }

                        }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.i(
                        TAG,
                        "onAdFailedToLoad: Ad failed to load -> \nresponseInfo::${adError.responseInfo}\nErrorCode::${adError.code}\nErrorMessage::${adError.message}"
                    )
                    if (developerNeedLoadAdsOnce) {
                        loadAdsOnce = false
                    }
                    lInterstitialAd = null
                    fListener.onAdFailed()
                }
            }
        )
    }

    /**
     * Call this method when you need to load your Interstitial AD
     * you need to call this method only once in any activity or fragment
     *
     * @param fContext this is a reference to your activity context
     */
    fun loadInterstitialAd(
        @NonNull fContext: Context,
        fIsShowFullScreenNativeAd: Boolean = true,
        onAdLoaded: () -> Unit = {}
    ) {

        /*if (isAppInTesting) {
            val isTestDevice = AdRequest.Builder().build().isTestDevice(fContext)
            Log.e(TAG, "loadNativeAdvancedAd: isTestDevice::${isTestDevice}")
            if (!isTestDevice) {
                return
            }
        }*/

        this.mIsShowFullScreenNativeAd = fIsShowFullScreenNativeAd

        if (loadAdsOnce) {
            if (mInterstitialAdMob == null) {

                loadAd(fContext, object : AdMobAdsListener {
                    override fun onAdLoaded() {
                        mIsAdMobAdLoaded = true
                    }

                    override fun onInterstitialAdLoaded(interstitialAd: InterstitialAd) {
                        super.onInterstitialAdLoaded(interstitialAd)
                        mIsAdMobAdLoaded = true
                        mInterstitialAdMob = interstitialAd
                        onAdLoaded.invoke()
                    }

                    override fun onAdFailed() {
                        mIsAdMobAdLoaded = false
                    }

                    override fun onAdClosed(isShowFullScreenAd: Boolean, needToLoadNewAd: Boolean) {
                        mIsAdMobAdLoaded = false
                        mIsAnyAdShow = false
                        mInterstitialAdMob?.fullScreenContentCallback = null
                        mInterstitialAdMob = null
                        mListener?.onAdClosed(needToLoadNewAd = needToLoadNewAd)
                    }

                })
            } else {
                onAdLoaded.invoke()
            }
        } else {
            Log.i(TAG, "developerNeedLoadAdsOnce = true")
        }
    }

    /**
     * Call this method when you need to show Interstitial AD
     * also this method call our offline native dialog AD [FullScreenNativeAdDialog] when Interstitial Ad fails and give call bake on same way
     *
     * Use of this Method
     * activity.isShowInterstitialAd {[your code which has run after AD show or if AD fails to show]}
     *
     * @param onAdClosed this is a call back of your ad close, it will call also if your ad was not showing to the user
     */
    fun FragmentActivity.isShowInterstitialAd(
        isBackAds: Boolean = false,
        @NonNull onAdClosed: (isShowFullScreenAd: Boolean) -> Unit
    ) {
        mListener = object : AdMobAdsListener {
            override fun onAdClosed(isShowFullScreenAd: Boolean, needToLoadNewAd: Boolean) {
                Log.i(TAG, "onAdClosed: ")
                isInterstitialAdShow = false
                mIsAnyAdShow = false
                onAdClosed.invoke(isShowFullScreenAd)

                if (!needToLoadNewAd) {
                    Log.i(TAG, "Interstitial AdMob null")
                } else if (!isBackAds) {
                    Log.e(TAG, "onAdClosed: Load New Ad")
                    loadInterstitialAd(this@isShowInterstitialAd)
                }
            }
        }

        if (isBlockInterstitialAd) {
            mInterstitialAdMob = null
        }

        mIsAnyAdShow = if (!isInterstitialAdShow && isNeedToShowAds && !mIsAnyAdShow) {
            if (mIsAdMobAdLoaded && mInterstitialAdMob != null) {
                if (!isAnyAdShowing) {
                    isAnyAdShowing = true
                    isAnyAdOpen = true
                    isInterstitialAdShow = true
                    mInterstitialAdMob?.show(this)
                    Log.i(TAG, "isShowInterstitialAd: Show Interstitial Ad")
                    true
                } else {
                    false
                }
            } else {
                if (mIsShowFullScreenNativeAd && NativeAdvancedModelHelper.getNativeAd != null && isOnline && !this.isFinishing) {
                    if (!isAnyAdShowing) {
                        isAnyAdShowing = true
                        Log.i(TAG, "isShowInterstitialAd: Try To Open Dialog...")
                        FullScreenNativeAdDialog(this) {
                            mIsAnyAdShow = false
                            mListener?.onAdClosed(isShowFullScreenAd = true, needToLoadNewAd = true)
                        }.showFullScreenNativeAdDialog(true)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        } else {
            false
        }

        if (!mIsAnyAdShow) {
            mListener?.onAdClosed(false, needToLoadNewAd = false)
        }
    }
}