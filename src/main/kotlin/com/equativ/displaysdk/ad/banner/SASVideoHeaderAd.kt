package com.equativ.displaysdk.ad.banner

import android.content.Context
import android.widget.FrameLayout
import com.equativ.displaysdk.exception.SASException
import com.equativ.displaysdk.model.SASAdInfo
import com.equativ.displaysdk.model.SASAdPlacement

class SASVideoHeaderAd(private val context: Context) : FrameLayout(context), SASBannerView.BannerListener {

    /**
     * Listener of SASVideoHeaderAd.
     */
    interface VideoHeaderAdListener {
        /**
         * Called when the video header ad successfully loads an ad.
         */
        fun onVideoHeaderAdLoaded(adInfo: SASAdInfo)

        /**
         * Called when the video header ad fails to load an ad.
         *
         * Note that the ad will automatically collapsed in this case.
         */
        fun onVideoHeaderAdFailedToLoad(exception: SASException)

        /**
         * Called when the ad inside the cell is clicked by the user.
         */
        fun onVideoHeaderAdClicked()

        /**
         * Called when the ad cell is closed (when the user click on the top-right close button).
         */
        fun onVideoHeaderAdClosed()
    }

    /**
     * The view which will act as the parent of the SASBannerView after it has reached its minimum ratio,
     * when the user scrolls the recycler view.
     *
     * This view has to be manually added to your activity layout, otherwise the ad will disappear when the user scrolls.
     */
    val stickToTopContainerView = FrameLayout(context).apply { layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT) }

    /**
     * The listener of the SASVideoHeaderAd.
     */
    var videoHeaderAdListener: VideoHeaderAdListener? = null

    /**
     * The internal SASBannerAd instance, manipulated by this SASVideoHeaderAd object to use it as a video header format.
     */
    private val bannerView = SASBannerView(context).also { it.bannerListener = this }

    private var scrollY = 0f
    private var isStuckToTop = false
    private var isClosed = false

    init {
        // Setup view with the maximum size, either the current SASVideoHeaderAd but also the SASBannerView
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, maxSize())
        addView(bannerView, LayoutParams.MATCH_PARENT, maxSize())
    }

    /**
     * Loads the ad.
     *
     * @param adPlacement The SASAdPlacement instance used to load the ad.
     */
    fun loadAd(adPlacement: SASAdPlacement) {
        // Delegate the load to the SASBannerView
        bannerView.loadAd(adPlacement)
    }

    /**
     * Must be called when the recycler view in which this SASVideoHeaderAd is set has scroll.
     * The SASVideoHeaderAd then can update its view to compute the video header ad behavior.
     */
    fun onParentViewScrolled(dx: Int, dy: Int) {
        // Quick exit if the ad is already closed.
        if (isClosed) {
            return
        }

        // Update the overall scrollY value
        scrollY += dy

        // Get the initialHeight of the ad
        val initialHeight = maxSize()

        // Compute the possible new size
        val possibleNewBannerHeight = initialHeight - scrollY

        if (possibleNewBannerHeight > minSize()) {
            // If the resulting size is bigger than the minimum size, we unstick the ad if necessary…
            unstickToTopIfNecessary()

            // …then we update the size of the SASBannerView to fit with the computed size.
            val layoutParams = bannerView.layoutParams
            layoutParams.height = possibleNewBannerHeight.toInt()
            bannerView.layoutParams = layoutParams
            bannerView.y = scrollY
        } else {
            // Else, if the resulting size is smaller than the minimum size, we stick the ad if necessary.
            stickToTopIfNecessary()
        }
    }

    /**
     * Closes the ad by removing it from the view hierarchy and killing the internal SASBannerView lifecycle.
     */
    fun closeAd() {
        // Quick exit if the ad is already closed
        if (isClosed) {
            return
        }

        // Update the flag
        isClosed = true

        // Remove the bannerView from the view it is in.
        if (isStuckToTop) {
            stickToTopContainerView.removeView(bannerView)
        } else {
            removeView(bannerView)
        }

        // Destroy the SASBannerView instance
        bannerView.onDestroy()

        // update the current SASVideoHeaderAd size to 0
        val lParams = layoutParams
        lParams.height = 0
        layoutParams = lParams

        // Call the listener, if any
        videoHeaderAdListener?.onVideoHeaderAdClosed()
    }

    /**
     * Util method that return the maximum size of the ad, based on the MAX_RATIO value and the width of the device.
     */
    private fun maxSize() = (context.resources.displayMetrics.widthPixels / MAX_RATIO).toInt()

    /**
     * Util method that return the minimum size of the ad, based on the MIN_RATIO value and the width of the device.
     */
    private fun minSize() = (context.resources.displayMetrics.widthPixels / MIN_RATIO).toInt()

    /**
     * Destroy the SASVideoHeaderAd object, and clean its properties.
     */
    fun onDestroy() {
        bannerView.onDestroy()
    }

    /**
     * Stick the internal SASBannerView to the top of the screen, if necessary.
     */
    private fun stickToTopIfNecessary() {
        // If the ad is already stuck to the top, we do nothing.
        if (!isStuckToTop) {
            isStuckToTop = true

            // Update the SASVideoHeaderAd size to the minSize
            val layoutParams = bannerView.layoutParams
            layoutParams.height = minSize()

            // Update the SASBannerView Y value to 0, to have it at the right place in the stickToTopContainer.
            bannerView.layoutParams = layoutParams
            bannerView.y = 0f

            // Remove the SASBannerView from the SASVideoHeaderAd actual view
            removeView(bannerView)

            // Add the SASBannerView to the stickToTopContainerView
            stickToTopContainerView.addView(bannerView)
        }
    }

    /**
     * Unstick the internal SASBannerView from the top of the screen, if necessary.
     */
    private fun unstickToTopIfNecessary() {
        // If the ad is not stuck to the top, we do nothing.
        if (isStuckToTop) {
            isStuckToTop = false

            // Remove the SASBannerView from the stickToTopContainerView
            stickToTopContainerView.removeView(bannerView)

            // Add the SASBannerView back to the SASVideoHeaderAd actual view.
            addView(bannerView)
        }
    }

    /// Banner view listener implementation

    override fun onBannerAdClicked() {
        // Forwarding the banner listener call to the video header ad listener.
        videoHeaderAdListener?.onVideoHeaderAdClicked()
    }

    override fun onBannerAdFailedToLoad(exception: SASException) {
        // Forwarding the banner listener call to the video header ad listener.
        videoHeaderAdListener?.onVideoHeaderAdFailedToLoad(exception)

        // The ad must be closed if no ad can be loaded.
        closeAd()
    }

    override fun onBannerAdLoaded(adInfo: SASAdInfo) {
        // Forwarding the banner listener call to the video header ad listener.
        videoHeaderAdListener?.onVideoHeaderAdLoaded(adInfo)
    }

    override fun onBannerAdRequestClose() {
        // Actually close the ad.
        closeAd()
    }

    companion object {
        /**
         * Maximum ratio of the ad. The ad will never grow higher than necessary to achieve this ratio.
         *
         * Note that we recommend a value of 16:9 as it is the typical video ratio, however always check that it works
         * well for your app and that it does not make navigation too cumbersome for the user.
         */
        const val MAX_RATIO = 16f / 9f

        /**
         * Minimum ratio of the ad before it is removed from the recycler view and stuck over it.
         *
         * When the ratio of the ad grows back over this minimum ratio again, the ad will be unstuck and added back
         * to the first cell of the recycler view.
         */
        const val MIN_RATIO = 32f / 9f
    }

}