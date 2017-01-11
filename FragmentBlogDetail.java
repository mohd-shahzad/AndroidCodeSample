package com.example.utility;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
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
import com.rtmedia.Comment.FragmentComment;
import com.rtmedia.Constant.AppConstant;
import com.rtmedia.CustomViews.CustomTextView;
import com.rtmedia.Home.HomeActivity;
import com.rtmedia.Managers.APIManager;
import com.rtmedia.Model.BlogDetailModel;
import com.rtmedia.Model.GetCommentModel;
import com.rtmedia.Model.PageViewCountModel;
import com.rtmedia.Model.SavedUnsavedArticlesModel;
import com.rtmedia.Observer.BlogDetailObserver;
import com.rtmedia.Observer.PageCountBlogObserver;
import com.rtmedia.Observer.SavedUnsavedBlogObserver;
import com.rtmedia.R;
import com.rtmedia.Register.FragmentSignIn;
import com.rtmedia.Share.SharingActivity;
import com.rtmedia.Utility.HandleGetToken;
import com.rtmedia.Utility.NavigateController;
import com.rtmedia.Utility.OnRedirectionCallback;
import com.rtmedia.Utility.OnTokenCallback;
import com.rtmedia.Utility.Utility;
import com.rtmedia.ViewPager.ImageSliderAdapter;

import java.util.Observable;
import java.util.Observer;

/**
 * 
 * This class shows blog detail and
 * have methods to show comment ,to show
 * number of user who have viewed this blog,
 * to bookmark this blog ,to show full screen
 * ads ,to show bottom ads if applicable
 */
public class FragmentBlogDetail extends Fragment implements Observer {
    View view;                           //Reference of view of fragment.
    String BlogId ="";                    //To store id of blog.
    ImageView desc_saved;                //Reference to make bookmark to this blog option.
    ImageView desc_share;                //Reference to share this blog option.
    ImageView desc_comment;             //Reference to go to comment to this blog option.

    ImageView desc_featured_image;        // Reference of featured image
    CustomTextView desc_date;             //To blog date.
    WebView webView_detail;              //Reference to webview to show blog detail.
    CustomTextView desc_title;          //To show description title of author.
    CustomTextView author_title;        //To show title of author.
    CustomTextView author_desc;        //To show description of author.
    ImageView author_image;            //To hold image of author of blog.

    FrameLayout progressBarContainer; // Container of progress bar
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if(view == null) {
            view = inflater.inflate(R.layout.fragment_blog_detail, container, false);
            setFullScreenAds();
            NavigateController.refresh(getActivity(), FragmentBlogDetail.this);

            progressBarContainer = (FrameLayout) view.findViewById(R.id.progressBarContainer);
            progressBarContainer.setVisibility(View.VISIBLE);

            Bundle bundleModel = this.getArguments();
            if (bundleModel.containsKey(AppConstant.BUNDLE_ARGUMENT_KEYS.BlogId.getKey()))
                BlogId = bundleModel.getString(AppConstant.BUNDLE_ARGUMENT_KEYS.BlogId.getKey(), "");

			
			//It checks for device token for GCM
            OnTokenCallback onTokenCallback = new OnTokenCallback() {
                
				
				@Override
                public void onSuccessCall(boolean value) {
					//if user has got device token
				//here it will be informed.
					
                    makeCallToBlogDetailAPI();
                }

                @Override
                public void onErrorCall(boolean value) {
					//if user has not got device token
				//here it will be informed.
				
                    progressBarContainer.setVisibility(View.GONE);
                }
            };
            HandleGetToken.getInstance().getToken(getActivity(), onTokenCallback, true);

            desc_featured_image = (ImageView) view.findViewById(R.id.desc_featured_image);
            desc_date = (CustomTextView) view.findViewById(R.id.desc_date);
            desc_title = (CustomTextView) view.findViewById(R.id.desc_title);
            author_title = (CustomTextView) view.findViewById(R.id.author_title);
            author_desc = (CustomTextView) view.findViewById(R.id.author_desc);
            author_image = (ImageView) view.findViewById(R.id.author_image);

            webView_detail = (WebView) view.findViewById(R.id.webView_detail);
            webView_detail.setBackgroundColor(Color.TRANSPARENT);
            desc_saved = (ImageView) view.findViewById(R.id.desc_saved);
            desc_saved.setOnClickListener(new View.OnClickListener() {
				
				//make blog detail bookmark 
				//callback will be received here
				
                @Override
                public void onClick(View v) {
                    if(AppConstant.AUTH_TOKEN.length()>0) {
                        if (blogDetailModel != null) {
                            if (blogDetailModel.is_save_article.equalsIgnoreCase("0")) {
                                blogDetailModel.is_save_article = "1";
                                desc_saved.setImageResource(R.drawable.detail_screen_bookmark_selected_icon);
                            } else {
                                blogDetailModel.is_save_article = "0";
                                desc_saved.setImageResource(R.drawable.detail_screen_bookmark_unselected_icon);
                            }
                            OnTokenCallback onTokenCallback = new OnTokenCallback() {
                                @Override
                                public void onSuccessCall(boolean value) {
                                    makeCallToSaveUnsaveArticlesDetailAPI();
                                }

                                @Override
                                public void onErrorCall(boolean value) {

                                }
                            };
                            HandleGetToken.getInstance().getToken(getActivity(), onTokenCallback, true);
                        }
                    }
                    else
                        Utility.showSnackBar(getActivity(), getResources().getString(R.string.msg_login_to_save_article), R.color.red_failed, -1);
                }
            });

            desc_share = (ImageView) view.findViewById(R.id.desc_share);
            desc_share.setOnClickListener(new View.OnClickListener() {
				
				//share blog detail  
				//callback will be received here
				
                @Override
                public void onClick(View v) {
                    if (blogDetailModel != null) {
                        try {
                            View view = getView();
                            ImageView freatureImageView = (ImageView) view.findViewById(R.id.desc_featured_image);
                            Bitmap bitmap = null;
                            if (((BitmapDrawable) freatureImageView.getDrawable()) != null)
                                bitmap = ((BitmapDrawable) freatureImageView.getDrawable()).getBitmap();
                            else {
                                ImageView desc_featured_place_holder = (ImageView) view.findViewById(R.id.desc_featured_place_holder);
                                bitmap = ((BitmapDrawable) desc_featured_place_holder.getDrawable()).getBitmap();
                            }
                            Intent intent = new Intent(getActivity(), SharingActivity.class);
                            intent.putExtra("title", blogDetailModel.result.title);
                            intent.putExtra("short_description", blogDetailModel.result.short_description +"\n"+ AppConstant.AppLink);
                            intent.putExtra("url", blogDetailModel.result.featured_image);
                            String imagePath = null;
                            if ((imagePath = Utility.getFilePath(bitmap)) != null)
                                intent.putExtra("image", imagePath);
                            getActivity().startActivityForResult(intent, AppConstant.REQUEST_CODE);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            desc_comment = (ImageView) view.findViewById(R.id.desc_comment);

            desc_comment.setOnClickListener(new View.OnClickListener() {
				
				//comment to blog detail  
				//callback will be received here
				
                @Override
                public void onClick(View v) {
                    redirect_to_comment();
                }
            });
        }
        addObserver();
        setPageTitle();
        return view;
    }

    /*
    * This method redirects user to
    * comment's screen on particular
    * blog.
    * */
    public void redirect_to_comment()
    {
        if (blogDetailModel != null) {
            GetCommentModel commentModel = new GetCommentModel();
            for (int i = 0; i < blogDetailModel.result.comment.size(); i++) {
                try {
                    GetCommentModel.News newsObj = commentModel.new News();

                    newsObj.comment_id = blogDetailModel.result.comment.get(i).comment_id;
                    newsObj.comment_author = blogDetailModel.result.comment.get(i).comment_author;
                    newsObj.publish_date = blogDetailModel.result.comment.get(i).publish_date;
                    newsObj.comment_content = blogDetailModel.result.comment.get(i).comment_content;
                    commentModel.result.add(newsObj);
                } catch (Exception e) {
                }
            }

			//Comment fragment object is prepared
			//and added to framelayout 
			//previous comment to this
			//blog is shown
            Fragment fragment = new FragmentComment();
            Bundle bundle = new Bundle();
            bundle.putParcelable(AppConstant.BUNDLE_ARGUMENT_KEYS.BlogCommentModel.getKey(), commentModel);
            bundle.putString(AppConstant.BUNDLE_ARGUMENT_KEYS.PostId.getKey(), BlogId);
            fragment.setArguments(bundle);
            NavigateController.replace_fragment_inner(fragment, getActivity(), getActivity());

        }
    }

    /*
    * This method shows a full screen
    * ads when first time user
    * visit to a particular blog page.
    * */
    public void setFullScreenAds()
    {
        ((HomeActivity)getActivity()).setFragmentRestartListener(new HomeActivity.FragmentRestartListener() {
            @Override
            public void onRefresh() {
                if(blogDetailModel!=null && blogDetailModel.big_advertiesment!=null && blogDetailModel.big_advertiesment.length()>0)
                    ((HomeActivity)getActivity()).showAdvertisement(blogDetailModel.big_advertiesment,blogDetailModel.big_advertiesment_url);
            }
        });
    }

    /*
    * It makes call to server
    * for making any mark bookmark
    * */
    private void makeCallToSaveUnsaveArticlesDetailAPI() {
        // checking Internet connection
        if (!Utility.isNetworkConnected(getActivity())) {
            Utility.showSnackBar(getActivity(), getActivity().getResources().getString(R.string.internet_require), R.color.red_failed, -1);
        } else {
            SavedUnsavedArticlesModel model = getSaveUnsaveArticlesStatus();
            APIManager.getSharedInstance().makeSaveUnsaveArticlesDetailAPIRequest(getActivity(), model);
        }
    }

    /*
    *  It return a SavedUnsavedArticlesModel object
     *  having value to be send on server as parameter
     *  for making blog bookmark.
     *  */
    private SavedUnsavedArticlesModel getSaveUnsaveArticlesStatus()
    {
        SavedUnsavedArticlesModel model = new SavedUnsavedArticlesModel();
        model.auth_token = AppConstant.AUTH_TOKEN;
        model.post_type = ""+AppConstant.DETAIL_SELECTION.BLOG.getKey();
        model.post_id = ""+BlogId;
        model.time_stamp = ""+Utility.getCurrentTimeStamp();
        model.is_save = ""+blogDetailModel.is_save_article;
        model.device_token = AppConstant.DEVICE_TOKEN;
        model.language = AppConstant.language_selection_keys.getKey();
        model.device_type = AppConstant.DEVICE_TYPE_ANDROID;
        return model;
    }

    /*
    * It makes call to server for
    * getting blog detail
    * */
    private void makeCallToBlogDetailAPI() {
        // checking Internet connection
        if (!Utility.isNetworkConnected(getActivity())) {
            Utility.showSnackBar(getActivity(), getActivity().getResources().getString(R.string.internet_require), R.color.red_failed, -1);
        } else {
            BlogDetailModel model = getBlogDetailStatus();
            APIManager.getSharedInstance().makeBlogDetailAPIRequest(getActivity(), model);
        }
    }


    /*
    *  It return a BlogDetailModel object
     *  having value to be send on server as parameter
     *  for getting blog detail.
     *  */
    private BlogDetailModel getBlogDetailStatus()
    {
        BlogDetailModel model = new BlogDetailModel();
        model.auth_token = AppConstant.AUTH_TOKEN;//"decd96fe41073c478ac0cf154a0b3ae1";
        model.id = ""+BlogId;
        model.device_type = AppConstant.DEVICE_TYPE_ANDROID;
        model.device_token = AppConstant.DEVICE_TOKEN;
        model.language = AppConstant.language_selection_keys.getKey();
        return model;
    }

    /*
    * It makes call to server for
    * updating no. of user who
    * have viewed this blog.
    * */
    private void makeCallToPageViewCountAPI() {
        // checking Internet connection
        if (!Utility.isNetworkConnected(getActivity())) {
            Utility.showSnackBar(getActivity(), getActivity().getResources().getString(R.string.internet_require), R.color.red_failed, -1);
        } else {
            PageViewCountModel model = getPageViewCountStatus();
            APIManager.getSharedInstance().makePageViewCountAPIRequest(getActivity(), model);
        }
    }

    /*
    *  It return a PageViewCountModel object
     *  having value to be send on server as parameter
     *  for updating viewer count of blog to server.
     *  */
    private PageViewCountModel getPageViewCountStatus()
    {
        PageViewCountModel model = new PageViewCountModel();
        model.auth_token = ""+ AppConstant.AUTH_TOKEN;
        model.id = ""+BlogId;
        model.device_type = AppConstant.DEVICE_TYPE_ANDROID;
        model.device_token= AppConstant.DEVICE_TOKEN;
        model.post_type= AppConstant.DETAIL_SELECTION.BLOG.getKey();
        model.call_back = AppConstant.DETAIL_SELECTION.CALLBACK_BLOG.getKey();
        return model;
    }

    /*
    * It adds all the observer
     * that has been used in this class.
    * */
    public void addObserver()
    {
        BlogDetailObserver.getSharedInstance().addObserver(FragmentBlogDetail.this);
        SavedUnsavedBlogObserver.getSharedInstance().addObserver(FragmentBlogDetail.this);
        PageCountBlogObserver.getSharedInstance().addObserver(FragmentBlogDetail.this);
    }

    /*
    * It removes all the observer
     * that has been added in this class
     * when we close this screen.
    * */
    public void deleteObserver()
    {
        BlogDetailObserver.getSharedInstance().deleteObserver(FragmentBlogDetail.this);
        SavedUnsavedBlogObserver.getSharedInstance().deleteObserver(FragmentBlogDetail.this);
        PageCountBlogObserver.getSharedInstance().deleteObserver(FragmentBlogDetail.this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        deleteObserver();
    }


    /*
    * It sets title on toolbar*/
    public void setPageTitle()
    {
        CustomTextView toolbar_title = (CustomTextView)getActivity().findViewById(R.id.toolbar_title);
        toolbar_title.setText(R.string.page_blog_detail);
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

        /*set layout above of advertisement or footer*/
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        if(blogDetailModel.advertisement.size()>0) {
            p.addRule(RelativeLayout.ABOVE, R.id.advertisement);
            advertisement_cross_layout.setVisibility(View.VISIBLE);
        }
        else {
            p.addRule(RelativeLayout.ABOVE, R.id.footer);
            advertisement_cross_layout.setVisibility(View.GONE);
        }

        ScrollView scroll_view = (ScrollView)view.findViewById(R.id.scroll_view);
        scroll_view.setLayoutParams(p);

		//It check advertisement is available or not
		//if available all images is downloaded
		//and displayed
        for (int i = 0; i < blogDetailModel.advertisement.size(); i++) {
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

			
            ImageLoader.getInstance().displayImage(blogDetailModel.advertisement.get(i).image, imageView, null, new SimpleImageLoadingListener() {
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
                     // your code
                     try {
                         Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(Utility.getAdvertisementUrl(blogDetailModel.advertisement.get(v.getId()).url)));
                         getActivity().startActivity(intent);
                     } catch (ActivityNotFoundException e) {
                     }
                     catch (Exception e) {
                     }
                 }
             }
            );
        }
        advertisement.setAutoStart(true);
        advertisement.setFlipInterval(4000);
        advertisement.startFlipping();
    }

    BlogDetailModel blogDetailModel; // BlogDetailModel object reference


    /*
    * Here response of every call to
    * server is received and appropriate
    * action is performed.*/
    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof BlogDetailObserver) {
            progressBarContainer.setVisibility(View.GONE);
            if(data==null)
                return;
            makeCallToPageViewCountAPI();
            blogDetailModel = (BlogDetailModel) data;
            advertisement();
            if(blogDetailModel.status.equalsIgnoreCase(AppConstant.STATUS_SUCCESS))
            {
				//feature image is set here
                Utility.setImageInView(null, desc_featured_image, blogDetailModel.result.featured_image.trim(), getActivity());
				
				//description about author 
				//of blog is set here
                desc_date.setVisibility(View.VISIBLE);
                desc_date.setText(blogDetailModel.result.publish_date);
                desc_title.setText(blogDetailModel.result.title);
                author_title.setText(blogDetailModel.result.author_name);
                author_desc.setText(blogDetailModel.result.author_description);
                Utility.setImageInView(null, author_image, blogDetailModel.result.author_image, getActivity());
				
                webView_detail.loadData(Utility.getWebViewData(blogDetailModel.result.long_description), "text/html; charset=utf-8", "UTF-8");
                webView_detail.setBackgroundColor(Color.TRANSPARENT);
                webView_detail.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                if(blogDetailModel.is_save_article.equalsIgnoreCase("0"))
                    desc_saved.setImageResource(R.drawable.detail_screen_bookmark_unselected_icon);
                else
                    desc_saved.setImageResource(R.drawable.detail_screen_bookmark_selected_icon);

                CustomTextView badge = (CustomTextView)view.findViewById(R.id.badge);
                RelativeLayout badgeLayout = (RelativeLayout)view.findViewById(R.id.badgeLayout);
				
				//viewer of blog count is checked
                //and set here. 				
                if((blogDetailModel.result.total_view_count.length()<=0) || blogDetailModel.result.total_view_count.equalsIgnoreCase("0")){
                    badge.setText("0");
                    badgeLayout.setVisibility(View.GONE);
                }
                else {
                    badge.setText(blogDetailModel.result.total_view_count);
                    badgeLayout.setVisibility(View.VISIBLE);
                }

				//Here gallery images is set to view
                RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.grid_image);
                CustomTextView label_image = (CustomTextView) view.findViewById(R.id.label_image);
                if(blogDetailModel.result.images.size()<=0) {
                    recyclerView.setVisibility(View.GONE);
                    label_image.setVisibility(View.GONE);
                }
                else {
                    recyclerView.setVisibility(View.VISIBLE);
                    label_image.setVisibility(View.VISIBLE);

                    recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
                    recyclerView.setAdapter(new ImageSliderAdapter(getActivity(), blogDetailModel.result.images,null,null,null, getActivity()));
                }

                ScrollView scroll_view = (ScrollView)view.findViewById(R.id.scroll_view);
                scroll_view.fullScroll(View.FOCUS_UP);
            }
            else
                Utility.showSnackBar(getActivity(), blogDetailModel.message, R.color.red_failed, -1);
        }
        if (observable instanceof SavedUnsavedBlogObserver) {
            if(data==null)
                return;
            SavedUnsavedArticlesModel model = (SavedUnsavedArticlesModel) data;
            if(model.status.equalsIgnoreCase(AppConstant.STATUS_SUCCESS)) {

                /*-------------------Update Save Article COunt in Menu--------------------*/
                AppConstant.SAVED_ARTICLE_COUNT = model.saved_article_count;
                ((HomeActivity)getActivity()).callBack_SavedArticleCount();
            }
        }
        if (observable instanceof PageCountBlogObserver) {
            if(data==null)
                return;
            PageViewCountModel pageViewCountModel = (PageViewCountModel)data;
            if(pageViewCountModel.status.equalsIgnoreCase(com.rtmedia.Constant.AppConstant.STATUS_SUCCESS))
            {

            }
        }
    }
}
