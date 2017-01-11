package com.example.utility;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.ViewFlipper;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.rtmedia.Constant.AppConstant;
import com.rtmedia.CustomViews.CustomTextView;
import com.rtmedia.Home.HomeActivity;
import com.rtmedia.InTouchTvNews.AdapterInTouchRecycler;
import com.rtmedia.Managers.APIManager;
import com.rtmedia.Model.BlogModel;
import com.rtmedia.Model.PressReleaseModel;
import com.rtmedia.News.AdapterNewsRecycler;
import com.rtmedia.Observer.BlogObserver;
import com.rtmedia.R;
import com.rtmedia.RTMediaNews.AdapterRTMediaRecycler;
import com.rtmedia.Utility.HandleGetToken;
import com.rtmedia.Utility.NavigateController;
import com.rtmedia.Utility.OnRedirectionCallback;
import com.rtmedia.Utility.OnTokenCallback;
import com.rtmedia.Utility.Utility;

import java.util.Observable;
import java.util.Observer;

/**
 * This class is used to
 * show blog list
 */
public class FragmentBlogs extends Fragment implements Observer{

    int page_number = 1;   //page counter ,it is use for pagination                            
    int page_size = 10;    //show number of blag in single page call.

    private RecyclerView recyclerView;                 //Reference to recyclerView for blog list.
    private RecyclerView.LayoutManager layoutManager;  //Reference to layoutManager for recyclerView
    MasonryAdapter adapter;                            //Reference of adapter of blog
    SwipeRefreshLayout pullToRefresh;                  //Refrence of pull to refresh layout

    View view;                                         //Reference of view of fragment
    RecyclerView mRecyclerView;                         
    OnRedirectionCallback onRedirectionCallback;       //Reference to listener to get callback of login status 
    OnRedirectionCallback onRedirectionCallbackVip;    //Reference to listener to get callback for about membership  

    FrameLayout progressBarContainer;                  // Container layout for progress bar

    public AppConstant.FONT_SIZE_BUTTON font_size_button; //It is local font size of page content.

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if(view==null || font_size_button!= AppConstant.font_size_button) {
            setFont();
            view = inflater.inflate(R.layout.fragment_blogs, container, false);
            setFullScreenAds();

            mRecyclerView = (RecyclerView) view.findViewById(R.id.masonry_grid);

            mRecyclerView.setHasFixedSize(true);
            layoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(layoutManager);

            pullToRefresh = (SwipeRefreshLayout) view.findViewById(R.id.pullToRefresh);

			//when user pull down page 
			//call is recieve here.
            pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {

				      //Checking network connectivity
                    if (Utility.isNetworkConnected(getActivity())) {
                        if (adapter != null) {

                            MasonryAdapter adapterNews = ((MasonryAdapter) adapter);
                            adapterNews.setAppendLocation(0);
							
							//Check blog list has data or not
							//if has data we will send "up" direction
							//otherwise "down" 
							
                            if (adapterNews.blogModel.result.size() > 0)
                                makeCallToBlogAPI("up", adapter.getItem(0).getId());
                            else
                                makeCallToBlogAPI("down", "");
                        }
                    } else {
                        hideRfreshIcon();
                    }


                }
            });

			//Here call back will be recevied
			//when it s checked whether user is login or not
            onRedirectionCallback = new OnRedirectionCallback() {
                @Override
                public void onSuccessCall(boolean value, int position) {
                    if (AppConstant.AUTH_TOKEN.length() > 0) {
                        if (((MasonryAdapter) adapter).blogModel.result.get(position).post_restriction.subscription.free_plan.equalsIgnoreCase("1"))// &&  AppConstant.USER_SUBSCRIPTION_PLAN.equalsIgnoreCase(AppConstant.USER_SUBSCRIPTION_PLAN_SELECTION.free_plan.getKey()))
                        {
                            ((MasonryAdapter) adapter).redirect_to_detail(position);
                        } else if (((MasonryAdapter) adapter).blogModel.result.get(position).post_restriction.subscription.soft_copy.equalsIgnoreCase("1") && (AppConstant.USER_SUBSCRIPTION_PLAN.equalsIgnoreCase(AppConstant.USER_SUBSCRIPTION_PLAN_SELECTION.soft_copy.getKey()) || AppConstant.USER_SUBSCRIPTION_PLAN.equalsIgnoreCase(AppConstant.USER_SUBSCRIPTION_PLAN_SELECTION.hard_copy.getKey()))) {
                            ((MasonryAdapter) adapter).redirect_to_detail(position);
                        } else if (((MasonryAdapter) adapter).blogModel.result.get(position).post_restriction.subscription.hard_copy.equalsIgnoreCase("1") && AppConstant.USER_SUBSCRIPTION_PLAN.equalsIgnoreCase(AppConstant.USER_SUBSCRIPTION_PLAN_SELECTION.hard_copy.getKey())) {
                            ((MasonryAdapter) adapter).redirect_to_detail(position);
                        } else {
                            ((MasonryAdapter) adapter).redirect_to_vip(position);
                        }
                    }
                }

                @Override
                public void onErrorCall(boolean value) {

                }

                @Override
                public int describeContents() {
                    return 0;
                }

                @Override
                public void writeToParcel(Parcel dest, int flags) {

                }
            };

             //Here call back will be recevied
			//After checking member ship of user
		    onRedirectionCallbackVip = new OnRedirectionCallback() {
                @Override
                public void onSuccessCall(boolean value, int position) {
                    if (((MasonryAdapter) adapter).blogModel.result.get(position).post_restriction.subscription.free_plan.equalsIgnoreCase("1"))// &&  AppConstant.USER_SUBSCRIPTION_PLAN.equalsIgnoreCase(AppConstant.USER_SUBSCRIPTION_PLAN_SELECTION.free_plan.getKey()))
                    {
                        ((MasonryAdapter) adapter).redirect_to_detail(position);
                    } else if (((MasonryAdapter) adapter).blogModel.result.get(position).post_restriction.subscription.soft_copy.equalsIgnoreCase("1") && (AppConstant.USER_SUBSCRIPTION_PLAN.equalsIgnoreCase(AppConstant.USER_SUBSCRIPTION_PLAN_SELECTION.soft_copy.getKey()) || AppConstant.USER_SUBSCRIPTION_PLAN.equalsIgnoreCase(AppConstant.USER_SUBSCRIPTION_PLAN_SELECTION.hard_copy.getKey()))) {
                        ((MasonryAdapter) adapter).redirect_to_detail(position);
                    } else if (((MasonryAdapter) adapter).blogModel.result.get(position).post_restriction.subscription.hard_copy.equalsIgnoreCase("1") && AppConstant.USER_SUBSCRIPTION_PLAN.equalsIgnoreCase(AppConstant.USER_SUBSCRIPTION_PLAN_SELECTION.hard_copy.getKey())) {
                        ((MasonryAdapter) adapter).redirect_to_detail(position);
                    } else {
                        ((MasonryAdapter) adapter).redirect_to_vip(position);
                    }
                }

                @Override
                public void onErrorCall(boolean value) {

                }

                @Override
                public int describeContents() {
                    return 0;
                }

                @Override
                public void writeToParcel(Parcel dest, int flags) {

                }
            };

            if (blogModel == null) {

			//It checks for device token for GCM
                OnTokenCallback onTokenCallback = new OnTokenCallback() {
                    @Override
                    public void onSuccessCall(boolean value) {
					 //if user has got device token
				    //here it will be informed.
						
                        makeCallToBlogAPI("down", "");
                    }

                    @Override
                    public void onErrorCall(boolean value) {
						//if user has not got device token
				//here it will be informed.
						
                        progressBarContainer.setVisibility(View.GONE);
                    }
                };
                HandleGetToken.getInstance().getToken(getActivity(), onTokenCallback, true);
                progressBarContainer = (FrameLayout) view.findViewById(R.id.progressBarContainer);
                progressBarContainer.setVisibility(View.VISIBLE);
            }
            else{
                mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
                SpacesItemDecoration decoration = new SpacesItemDecoration(2);
                mRecyclerView.addItemDecoration(decoration);

                setAdapter(blogModel);
                progressBarContainer = (FrameLayout) view.findViewById(R.id.progressBarContainer);
                progressBarContainer.setVisibility(View.GONE);
            }
        }

        NavigateController.refresh(getActivity(), FragmentBlogs.this);
        setPageTitle();
        addObserver();
        return view;
    }

    public void setFont()
    {
        font_size_button = AppConstant.font_size_button;
    }

	//
    public void setFullScreenAds()
    {
        ((HomeActivity)getActivity()).setFragmentRestartListener(new HomeActivity.FragmentRestartListener() {
            @Override
            public void onRefresh() {
                if(blogModel!=null && blogModel.big_advertiesment!=null && blogModel.big_advertiesment.length()>0)
                    ((HomeActivity)getActivity()).showAdvertisement(blogModel.big_advertiesment,blogModel.big_advertiesment_url);
            }
        });
    }

    /*
    * It makes call to server
    * for fetching blog list
    * */
    private void makeCallToBlogAPI(String direction ,String lastId) {
        // checking Internet connection
        if (!Utility.isNetworkConnected(getActivity())) {
            Utility.showSnackBar(getActivity(), getActivity().getResources().getString(R.string.internet_require), R.color.red_failed, -1);
        } else {
            BlogModel model = getBlogStatus(direction ,lastId);
            APIManager.getSharedInstance().makeBlogAPIRequest(getActivity(), model);
        }
    }

    /*
   *  It return a BlogModel object
    *  having value to be send on server as parameter
    *  for getting blog list.
    *  */
    private BlogModel getBlogStatus(String direction ,String lastId)
    {
        BlogModel model = new BlogModel();
        model.device_token = AppConstant.DEVICE_TOKEN;
        model.device_type = ""+ AppConstant.DEVICE_TYPE_ANDROID;
        model.language = ""+ AppConstant.language_selection_keys.getKey();
        model.page_size = ""+page_size;
        model.last_id = lastId;
        model.direction = direction;
        if(direction.equalsIgnoreCase("up"))
            model.page_number = "";
        else
            model.page_number = ""+page_number;

        model.auth_token = AppConstant.AUTH_TOKEN;
        return model;
    }

    /*
    * It adds all the observer
     * that has been used in this class.
    * */
    public void addObserver()
    {
        BlogObserver.getSharedInstance().addObserver(FragmentBlogs.this);
    }

    /*
   * It removes all the observer
    * that has been added in this class
    * when we close this screen.
   * */
    public void deleteObserver()
    {
        BlogObserver.getSharedInstance().deleteObserver(FragmentBlogs.this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        deleteObserver();
    }

    /*
   * It sets title on toolbar
   * */
    public void setPageTitle()
    {
        CustomTextView toolbar_title = (CustomTextView)getActivity().findViewById(R.id.toolbar_title);
        toolbar_title.setText(R.string.page_blogs);
    }

    /*
       * If bottom ads is available for
       * any blog then it shows those
       * ads on bottom in flipper
       * and register listener for
       * getting call of each ads
       * click event.*/
    public void advertisement() {
        final ViewFlipper advertisement = (ViewFlipper) view.findViewById(R.id.advertisement);

        ImageView advertisement_croos = (ImageView) view.findViewById(R.id.advertisement_croos);
        final FrameLayout advertisement_cross_layout =(FrameLayout)view.findViewById(R.id.advertisement_cross_layout);

        advertisement_croos.setOnClickListener(new View.OnClickListener() {
			
			
            @Override
            public void onClick(View v) {
                advertisement.setVisibility(View.GONE);
                advertisement_cross_layout.setVisibility(View.GONE);
            }
        });

		//It check advertisement is available or not
		//if available all images is downloaded
		//and displayed
        for (int i = 0; i < blogModel.advertisement.size(); i++) {
            advertisement.setVisibility(View.VISIBLE);
            final ImageView imageView = new ImageView(getActivity());
            RelativeLayout relativeLayout = new RelativeLayout(getActivity());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            lp.gravity = Gravity.CENTER;
            imageView.setLayoutParams(lp);

            imageView.setAdjustViewBounds(true);
            imageView.setClickable(true);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            layoutParams.gravity = Gravity.CENTER;
            final ProgressBar progressBar = new ProgressBar(getActivity(),null,android.R.attr.progressBarStyleSmall);
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setLayoutParams(layoutParams);

            relativeLayout.addView(imageView);
            relativeLayout.addView(progressBar);

            imageView.setId(i);
            advertisement.addView(relativeLayout);

            ImageLoader.getInstance().displayImage(blogModel.advertisement.get(i).image, imageView, null, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    super.onLoadingFailed(imageUri, view, failReason);
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    super.onLoadingComplete(imageUri, view, loadedImage);
                    progressBar.setVisibility(View.GONE);
                    imageView.setImageBitmap(loadedImage);
                }

            });

            imageView.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     
                     try {
                         Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(Utility.getAdvertisementUrl(blogModel.advertisement.get(v.getId()).url)));
                         getActivity().startActivity(intent);
                     } catch (ActivityNotFoundException e) {
                     }
                     catch (Exception e) {
                     }
                 }
             }
            );
        }

        if(advertisement.getChildCount()>0) {
            advertisement_cross_layout.setVisibility(View.VISIBLE);
        }
        else {
            advertisement_cross_layout.setVisibility(View.GONE);
        }
        advertisement.setAutoStart(true);
        advertisement.setFlipInterval(4000);
        advertisement.startFlipping();
    }

    BlogModel blogModel; //Reference of blog model object.


    /*
    * Here response of every call to
    * server is received and appropriate
    * action is performed.
    * */
    @Override
    public void update(Observable observable, Object data) {

        hideRfreshIcon();
        if (observable instanceof BlogObserver) {
            progressBarContainer.setVisibility(View.GONE);
            if(data==null)
                return;
            blogModel = (BlogModel) data;
            mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            SpacesItemDecoration decoration = new SpacesItemDecoration(2);
            mRecyclerView.addItemDecoration(decoration);

            if(adapter == null)
            {
                advertisement();
                setAdapter(blogModel);

            }
            else
            {
                MasonryAdapter masonryAdapter = (MasonryAdapter)adapter;

                if(((BlogModel)data).result.size()>0) {
                    if (((BlogModel) data).direction.equalsIgnoreCase("up")) {
                        masonryAdapter.blogModel.result.addAll(0, blogModel.result);
                    } else if (((BlogModel) data).direction.equalsIgnoreCase("down")) {
                        masonryAdapter.blogModel.result.addAll(blogModel.result);
                        hideBottomProgressBar();
                        if (((BlogModel) data).response_pagination.gettotal_record_count().equalsIgnoreCase("" + (masonryAdapter.getItemCount() - 1))) {

                            masonryAdapter.setIsMoreItem(false);
                        }

                    }
                    adapter.notifyDataSetChanged();
                }

            }


            this.recyclerView.getWidth();

            if(blogModel.status.equalsIgnoreCase(AppConstant.STATUS_FAILURE)) {
                Utility.showSnackBar(getActivity(), blogModel.message, R.color.red_failed, -1);
                if (adapter == null)
                {
                    setAdapter(blogModel);
                }
            }
        }
    }


    /*
    * It sets blogs into list
    * */
    public void setAdapter(final BlogModel blogModel) {

        adapter = new MasonryAdapter(getActivity(), blogModel, onRedirectionCallback,onRedirectionCallbackVip, getActivity(), new MasonryAdapter.GetMoreData() {
            @Override
            public void getData()
            {
                if (((MasonryAdapter) adapter).isMoreItem && ((MasonryAdapter)adapter).blogModel.result.size()>0)
                {
                    ProgressBar progressbarbottom = (ProgressBar) view.findViewById(R.id.progressbarbottom);
                    progressbarbottom.setVisibility(View.VISIBLE);
                    page_number++;
                    makeCallToBlogAPI("down", ((MasonryAdapter) adapter).getItem(((MasonryAdapter) adapter).getItemCount() - 2).getId());
                }
                else if (((MasonryAdapter) adapter).isMoreItem)
                {
                    ((MasonryAdapter) adapter).setIsMoreItem(false);

                    recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });

        mRecyclerView.setAdapter(adapter);

        if(blogModel.response_pagination.gettotal_record_count()
                .equalsIgnoreCase((((MasonryAdapter) adapter).getItemCount() - 1) + ""))
            ((MasonryAdapter) adapter).setIsMoreItem(false);
        else
            ((MasonryAdapter) adapter).setIsMoreItem(true);


    }

    /*
    * It hides pull to
    * refresh icon
    * */
    private void hideRfreshIcon()
    {
        if(pullToRefresh.isRefreshing())
            pullToRefresh.setRefreshing(false);

    }

    /*
    * It hides bottom progress
    * bar icon.
    * */
    private void hideBottomProgressBar()
    {

        ProgressBar progressbarbottom = (ProgressBar) view.findViewById(R.id.progressbarbottom);
        progressbarbottom.setVisibility(View.GONE);
    }

}
