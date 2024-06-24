package com.equativ.videoheaderadsample

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.equativ.displaysdk.ad.banner.SASVideoHeaderAd
import com.equativ.displaysdk.exception.SASException
import com.equativ.displaysdk.model.SASAdInfo
import com.equativ.displaysdk.model.SASAdPlacement
import com.equativ.kotlinsample.viewholder.BannerViewHolder
import com.equativ.kotlinsample.viewholder.ListItemViewHolder
import com.equativ.videoheaderadsample.databinding.ListBannerHolderBinding
import com.equativ.videoheaderadsample.databinding.ListItemBinding
import com.equativ.videoheaderadsample.databinding.VideoHeaderAdActivityBinding

/**
 * This sample shows how to display a banner as a Video Header Ad over a recycler view.
 *
 * This integration relies on the SASVideoHeaderAd which is available in the project.
 * Check its source code for more details or to adapt it for your app.
 */
class VideoHeaderAdActivity: AppCompatActivity(), SASVideoHeaderAd.VideoHeaderAdListener {

    private val binding by lazy { VideoHeaderAdActivityBinding.inflate(layoutInflater) }

    // Definition of the SASVideoHeaderAd.
    private val videoHeaderAd by lazy {
        SASVideoHeaderAd(this).also {
            // Set this activity as listener. This is optional.
            it.videoHeaderAdListener = this
        }
    }

    // Implementation of RecyclerView.Adapter class, used by the RecycleView.
    private val recyclerViewAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val viewTypeContent = 0
        private val viewTypeAd = 1

        override fun getItemViewType(position: Int): Int {
            // The video header ad must be the first cell
            return if (position == 0) { viewTypeAd } else { viewTypeContent }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)

            return if (viewType == viewTypeAd) {
                // Create banner view holder
                BannerViewHolder(ListBannerHolderBinding.inflate(inflater, parent, false))
            } else {
                // Create content view holder
                ListItemViewHolder(ListItemBinding.inflate(inflater, parent, false))
            }
        }

        override fun getItemCount(): Int = 25

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ListItemViewHolder) {
                holder.setIndex(position)
            } else if (holder is BannerViewHolder) {
                // Add the video header ad to the view holder.
                //
                // The video header ad will automatically resize its size when the user scroll (by calling `OnParentViewScrolled`
                // as you will see below) and will automatically be removed from its table view and stuck over it when
                // its size reach the minimum ratio (check the `SASVideoHeaderAd` class for more info).
                //
                // Note that you don't need to handle the case where no ad can be found or where the ad loading fails
                // for any reason: in this case the SASVideoHeaderAd will automatically collapse and will not be
                // visible by the user anymore.
                holder.binding.bannerContainer.addView(videoHeaderAd)
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            if (holder is BannerViewHolder) {
                holder.binding.bannerContainer.removeView(videoHeaderAd)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Setup swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            // This swipe to refresh does nothing, it is just here to show that we can use a swipeToRefreshLayout while having a SASVideoHeaderAd
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Setup recyclerview
        binding.recyclerView.adapter = recyclerViewAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        // Add the SASVideoHeaderAd's stickTotopContainerView where you want to display it. Here, in the
        // stickToTopContainer of the layout, which is on top of the screen, above the recycler view.
        binding.headerAdStickToTopContainer.addView(videoHeaderAd.stickToTopContainerView)

        // The video header ad must know the current scroll state of the recycler view:
        // It is forwarded for each scroll event.
        binding.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                videoHeaderAd.onParentViewScrolled(dx, dy)
            }
        })

        loadBanner()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Once you are done using your SASVideoHeaderAd instance, make sure to call
        // `onDestroy` to let it release its resources.
        videoHeaderAd.onDestroy()
    }

    private fun loadBanner() {
        // Create the ad placement.
        // Warning: Check your placements carefully: the ad on this placement must use the Video Header-Ad dedicated template
        // otherwise the ad might not be displayed properly (typically the ad will not have a close button or might expand
        // when clicked for videos).
        val adPlacement = SASAdPlacement(
            507206,
            1579908,
            15048,
            "header01"
        )

        // If you are an inventory reseller, you must provide a Supply Chain Object.
        // More info here: https://help.smartadserver.com/s/article/Sellers-json-and-SupplyChain-Object
        // adPlacement.supplyChainObjectString = "1.0,1!exchange1.com,1234,1,publisher,publisher.com"

        // Load the placement using the `loadAd` method.
        videoHeaderAd.loadAd(adPlacement)
    }

    /// SASVideoHeaderAd.VideoHeaderAdListener implementation

    /**
     * The SASVideoHeaderAd provided in this sample allows you to set a listener similar to the listener available on a
     * regular banner view.
     *
     * Implementing it is totally optional as the ad will automatically handle its display, including when the ad
     * cannot be loaded (it will disappear automatically).
     */
    override fun onVideoHeaderAdLoaded(adInfo: SASAdInfo) {
        Log.d(TAG, "onVideoHeaderAdLoaded: $adInfo")
    }

    override fun onVideoHeaderAdFailedToLoad(exception: SASException) {
        Log.d(TAG, "onVideoHeaderAdFailedToLoad: $exception")
    }

    override fun onVideoHeaderAdClicked() {
        Log.d(TAG, "onVideoHeaderAdClicked")
    }

    override fun onVideoHeaderAdClosed() {
        Log.d(TAG, "onVideoHeaderAdClosed")
    }

    companion object {
        private const val TAG = "VideoHeaderAdActivity"
    }
}