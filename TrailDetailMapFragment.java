package org.TrailHUB;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;


import org.adapters.TrailMapInfoAdapter;
import org.constants.AppUtility;
import org.constants.Constant;
import org.model.BaseModel;
import org.model.OrganizationTrailModel;
import org.others.MyGcmListenerService;
import org.serverrequest.MyCustomRequest;
import org.serverrequest.MySingleton;
import org.serverrequest.NetworkResponseListener;
import org.serverrequest.ResponseErrorListener;
import org.serverrequest.ResponseSuccessListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.kml.KmlLayer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 
 * This class have methods to render kml file on
 * Google map ,filter trail on basis of apply filter,
 * reflection of trail status change ,redirection of
 * trail alert ,info alert and trustee invitation screen
 * on click of respective icon click ,for making all trail
 * favourate ,have option of filter trail and have option to
 * disable and enable extra pins.
 */
public class TrailDetailMapFragment extends Fragment implements GoogleMap.OnInfoWindowClickListener{

    GoogleMap map;                                                                 //Google map object reference
    TrailDetail activity;                                                          // Reference of parent activity.
    HashMap<String,KmlLayer> kmlLayerMap = new HashMap<String,KmlLayer>();                       //It stores trailId and kml layer object pair
    HashMap<String, OrganizationTrailModel.pins> pinsMap = new HashMap<String, OrganizationTrailModel.pins>();      //It stores pins marker id and pin object pair
    HashMap<String, OrganizationTrailModel.records> trailMap = new HashMap<String, OrganizationTrailModel.records>(); //It stores trail markerId and marker object pair
    HashMap<String, Marker> trailMarkerMap = new HashMap<String, Marker>();       //It stores trailid and marker object pair.
    TrailMapInfoAdapter trailMapInfoAdapter;                                     //It will be use to set info window on marker.


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {

        View view = inflater.inflate(R.layout.search_organization_result_mapview ,container ,false);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(activity);
        manager.registerReceiver(notificationReceiver, new IntentFilter(MyGcmListenerService.class.getSimpleName()));

        return view;
    }


    /*
    * It is a receiver which is called
     * whenever trail status of any trail
     * showing at current moment on map is changed
     * */
    BroadcastReceiver notificationReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {

           updateTrail(intent);

        }
    };

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(notificationReceiver);
        super.onDestroy();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (TrailDetail)activity;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        map = ((SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
       try {
           map.setMyLocationEnabled(true);
       }
       catch (SecurityException e){

       }
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnInfoWindowClickListener(this);

       showOrganization();
       prepareExtraPins((HashMap<String, String>) getActivity().getIntent().getSerializableExtra(Constant.O_FILTER_SELECTION_TAG));
       addTrailPin();
       renderKml();
    }

    @Override
    public void onInfoWindowClick(Marker marker)
    {
        OrganizationTrailModel.pins pin = pinsMap.get(marker.getId());
        OrganizationTrailModel.records trailRecord = trailMap.get(marker.getId());

        if(pin != null && (pin.website != null && !pin.website.equalsIgnoreCase("")))
            AppUtility.redirectToLink(activity, pin.website);


        if(trailRecord != null)
            extractLinkFromString(trailRecord.trailNote);
    }


    // Extract link from trail note
    private void extractLinkFromString(String trailNote){

        Matcher m = Patterns.WEB_URL.matcher(trailNote);
        String url="";
        if(m.find())
        {
            url = m.group();
            AppUtility.redirectToLink(activity ,url);

        }



    }

    /*
    * It is called when we need
    * to mall all favourate
    * @params position shows position of trail
    * in record
    * */
    public void makeCallToMakeTrailFavorite(int position)
    {
        if(AppUtility.isNetworkAvailable(getActivity()))
        {
            AppUtility.showProgress(getActivity() ,getString(R.string.pls_wait));
            String url = Constant.BASE_URL+Constant.API_NAME.SET_FAVOURITE_TRAIL.getKey();


            MyCustomRequest<BaseModel> request = new MyCustomRequest<BaseModel>(url , Request.Method.POST ,BaseModel.class ,getParameterForMakeTrailFavourate(position)
                    ,new ResponseSuccessListener<BaseModel>(networkResponseListener ,Constant.MAKE_TRAIL_FAV) ,new ResponseErrorListener(networkResponseListener,Constant.MAKE_TRAIL_FAV));
            MySingleton.getInstance(getActivity()).addToRequestQueue(request);
        }

    }

    /*
    * It prepares map having key value
    * to send on server making trails
    * favourate.
    * */
    private Map<String ,String> getParameterForMakeTrailFavourate(int position)
    {
        SharedPreferences pref = getActivity().getSharedPreferences(Constant.SHARED_PREF, Context.MODE_PRIVATE);
        OrganizationTrailModel trailModel = ((TrailDetail)getActivity()).getModelFromIntent();

         Map<String ,String> map = new HashMap<String ,String>();



         map.put(Constant.COMMON_API_KEYS.CHECK_FAVOURITE.getKey(), Constant.FAV_STATUS.FAV.getKey());
        map.put(Constant.COMMON_API_KEYS.ORGANIZATION_ID.getKey(), trailModel.organizationId);
         map.put(Constant.COMMON_API_KEYS.USER_ID.getKey()
                 , pref.getString(Constant.PF_USER_ID, ""));
        map.put(Constant.SEARCH_ORG_API_KEYS.ACTIVITY_ID.getKey()
                , trailModel.activityId);
        map.put("trailIds"
                , getAllTrailId(trailModel));

        map.put(Constant.COMMON_API_KEYS.AUTH_TOKEN.getKey()
                , pref.getString(Constant.AUTH_TOKEN, ""));

        return map;
    }


    /*
    * It returns id of all trails
    * separeted with comma
    * @params trailModel is OrganizationTrailModel class
    * object having data.
    * */
    private String getAllTrailId(OrganizationTrailModel trailModel)
    {

        String ids = "";
        try
        {
            List<OrganizationTrailModel.records> records = trailModel.records;
            for(int index=0;index<records.size();index++)
            {
                OrganizationTrailModel.records obj = records.get(index);

                if(index == 0)
                {
                    ids = ids+obj.trailId;
                }
                else
                {
                    ids =  ids+","+obj.trailId;
                }

            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return ids;
    }


    //Here network response will be received.
    NetworkResponseListener networkResponseListener = new NetworkResponseListener() {
        @Override
        public void onNetworkResponseSuccess(Object successResponse, String tag) {
            //On successful response of server request
            //call will be received here.

             if(tag.equalsIgnoreCase(Constant.MAKE_TRAIL_FAV))
            {
                AppUtility.hideProgress();
                performOperation((BaseModel) successResponse);

            }

        }

        @Override
        public void onNetworkResponseFailure(VolleyError volleyError, String tag)
        {
            //On unsuccessful response of server request with reason
            //call will be received here.
            AppUtility.hideProgress();

        }
    };


    /*
    * After getting result from server,
    * it is used to perform suitable operation
    * like to show to user
    * */
    private void performOperation(BaseModel model)
    {
        if(model != null)
        {

            if(model.status.equalsIgnoreCase(Constant.SUCCESS))
            {
                AppUtility.showToast(getActivity() ,"",model.message);

                OrganizationTrailModel trailModel = ((TrailDetail)getActivity()).getModelFromIntent();
                AppUtility.makeAllTrailFavourate(trailModel.records);
            }
            else if(model.status.equalsIgnoreCase(Constant.ERROR))
            {
                AppUtility.showToast(getActivity() ,getString(R.string.error_tit),model.message);

            }
            else if(model.status.equalsIgnoreCase(Constant.UNAUTHORIZE))
            {
                AppUtility.showToast(getActivity() ,getString(R.string.error_tit),model.message);
            }
        }
    }

  /*
   * Here trail lsit having kml file
  * is sent to a task render to render kml
   * on Google map.*/
  private void renderKml()
  {
       ArrayList<OrganizationTrailModel.records> fileList = prepareFileList();
       if(fileList.size()==0)
       {

           (activity).noFileDialog();

       }
       else
       {
           if(kmlLayerMap.size()>0)
           {
               kmlLayerMap.clear();
           }

           new RenderTask(activity ,fileList ,true).execute("");
       }

      return;



  }



    /*
    * It reflects status change of trail
    * by changing color of trail
    * @params intent contains information of trail
    * which status has been changed
    * and task to re-render those trail is
    * started*/
    private void updateTrail(Intent intent)
    {
        OrganizationTrailModel model = ((TrailDetail)getActivity()).getModelFromIntent();
        String trailId = intent.getStringExtra("id");
        String oid = intent.getStringExtra("oid");
        String trailStatus = intent.getStringExtra("status");

        if(!model.organizationId.equalsIgnoreCase(oid))
        return;

        String traiIdArray[] = trailId.split(",");
        String statusArray[] = trailStatus.split(",");

        onNotificationUpdateKmlHashMap(trailId);

        try {

            ArrayList<OrganizationTrailModel.records> fileList = new ArrayList<OrganizationTrailModel.records>();
            for(int index0=0;index0<traiIdArray.length;index0++)
            {
                String id = traiIdArray[index0];
                String status = statusArray[index0];

                for (int index = 0; index < model.records.size(); index++) {
                    OrganizationTrailModel.records trailObject = model.records.get(index);
                    if (trailObject.trailId.equalsIgnoreCase(id)) {
                        trailObject.trailStatus = status;
                        fileList.add(trailObject);

                        Marker marker;
                        if((marker =trailMarkerMap.get(trailObject.trailId)) != null)
                        {
                           trailMap.remove(marker.getId());
                           marker.remove();
                           trailMarkerMap.remove(trailObject.trailId);
                           addTrailMarker(trailObject);

                        }

                    }
                }

            }

            if(fileList.size()>0)
            {
                new RenderTask(activity ,fileList ,false).execute("");
            }
        }
        catch (Exception e){
          e.printStackTrace();
        }

    }

    /*
    * It remove kml from Google map
    * status of which has been changed.*/
    private void  onNotificationUpdateKmlHashMap(String id)
    {

        String traiIdArray[] = id.split(",");
        for(int index=0;index<traiIdArray.length;index++)
        {
            KmlLayer kmlLayer = kmlLayerMap.get(traiIdArray[index]);
            if(kmlLayer == null)
                return;
            kmlLayer.removeLayerFromMap();
            kmlLayerMap.remove(id);

        }


    }

    /*
    * Here trail having kml/kmz file is
    * filtered from all trails
    * */
    private ArrayList<OrganizationTrailModel.records> prepareFileList()
    {
        ArrayList<OrganizationTrailModel.records> fileList = new ArrayList<OrganizationTrailModel.records>();
        OrganizationTrailModel model = ((TrailDetail)getActivity()).getModelFromIntent();
        File file;
        ArrayList<OrganizationTrailModel.records> filterList = (ArrayList<OrganizationTrailModel.records>)getArguments().getSerializable(Constant.FILTER_LIST);


        for(int index=0;index<filterList.size();index++)
        {

            OrganizationTrailModel.records trailObject = filterList.get(index);


            if(trailObject.fileUrl != null && !trailObject.fileUrl.equalsIgnoreCase("")) {
                file = new File(activity.getExternalCacheDir(),
                        Constant.KML_FOLDER + "/" + model.organizationId + "/" +trailObject.kmlModifiedDateTime.replace(":" ,"-")+"_"+trailObject.fileUrl);

                if (file.exists()) {
                    if (trailObject.fileUrl.endsWith(".kml"))
                    {

                        fileList.add(trailObject);


                    } else if (trailObject.fileUrl.endsWith(".kmz"))
                    {

                        fileList.add(trailObject);
                    }

                }

            }

        }

        return fileList;
    }


    /*
    * This class is used to parse kml
    * file and render its data to Google map.
    * A loop will called to render all kml file
    * on Google map*/
    private class RenderTask extends AsyncTask<String ,KmlLayer ,ArrayList<KmlLayer>>
    {
        Context ctx;
        ArrayList<OrganizationTrailModel.records> fileList;
        boolean isShowProgress = false;
        int i=0;
        public RenderTask(Context ctx ,ArrayList<OrganizationTrailModel.records> fileList ,boolean isShowProgress)
        {
            this.ctx = ctx;
            this.fileList = fileList;
            this.isShowProgress = isShowProgress;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(isShowProgress)
            AppUtility.showProgress(ctx, getString(R.string.pls_wait));
        }


        @Override
        protected ArrayList<KmlLayer> doInBackground(String... strings) {


            try
            {
                 ArrayList<KmlLayer> layList = new ArrayList<KmlLayer>();
                for(int index=0;index<fileList.size();index++)
                {

                        i = index;
                        OrganizationTrailModel.records trailObject = fileList.get(index);
                        KmlLayer layer = TrailDetailMapFragment.this.setDataOnMap(trailObject);
                        layer.setPosition(index);
                        publishProgress(layer);


                }

                return layList;

            }
            catch (Exception e)
            {
                e.printStackTrace();

            }

            return null;
        }


        @Override
        protected void onProgressUpdate(KmlLayer... layer) {
            super.onProgressUpdate(layer);

            try {

                KmlLayer kmlLayer = layer[0];
				
				//kml data is stored
				kmlLayer.storeDataOfKml(map, ctx);

                      if(kmlLayer == null)
                          return;

                    OrganizationTrailModel.records trailObject = fileList.get(kmlLayer.getPosition());


                    int colorFlag=0;

                    if(trailObject.trailStatus.equalsIgnoreCase("O"))
                    {
                        colorFlag=0;

                    }
                    else if(trailObject.trailStatus.equalsIgnoreCase("CA"))
                    {
                        colorFlag=1;

                    }
                    else if(trailObject.trailStatus.equalsIgnoreCase("C"))
                    {
                        colorFlag=2;

                    }

					//kml is added to map
					// and colorFlag is passed
					//to set color of trail
					//according to status.
                    kmlLayer.addLayerToMap(colorFlag);
                    kmlLayerMap.put(trailObject.trailId, kmlLayer);

                if(i == fileList.size()-1 && isShowProgress)
                {

                    showOrganization();
                  }


            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                Toast.makeText(ctx,ex.getMessage(),Toast.LENGTH_SHORT).show();
            }

        }


        @Override
        protected void onPostExecute(ArrayList<KmlLayer> s) {
            super.onPostExecute(s);
            AppUtility.hideProgress();

        }

    }





    /*
    * For each trail having kml/kmz file
    * it is called and file path is sent to
    * method of parsing those file that is in
    * kmlibrary and KmlLayer object is return.
    * */
    private KmlLayer setDataOnMap(OrganizationTrailModel.records trailObject)
    {
        OrganizationTrailModel model = ((TrailDetail)getActivity()).getModelFromIntent();
        File file = null;
        InputStream is=null;
        FileOutputStream os = null;

        try{
            File tempFileDirectory = new File(activity.getExternalCacheDir(),
                    Constant.KML_FOLDER);
            File tempFile = new File(tempFileDirectory,
                    "temp.txt");

            String fileUrl = trailObject.kmlModifiedDateTime.replace(":","-")+"_"+trailObject.fileUrl;

            if(fileUrl != null && !fileUrl.equalsIgnoreCase(""))
            {
                file = new File(activity.getExternalCacheDir(),
                        Constant.KML_FOLDER+"/"+model.organizationId+"/"+fileUrl);


                if (file.exists())
                {
					
                    if(fileUrl.endsWith(".kml"))
                    {
						//if file is available with 
					//.kml extansion it is read
					//here
                        is = new FileInputStream(file);
                    }
                    else if(fileUrl.endsWith(".kmz"))
                    {
						//if file is available with 
					//.kmz extansion it is read
					//here like a zip file read.

                        ZipEntry ze;

                        try {
                            if (!tempFile.exists()){
                                tempFile.createNewFile();
                            }
                            else{
                                tempFile.delete();
                                tempFile.createNewFile();
                            }
                            os = new FileOutputStream(tempFile);

                            is = new FileInputStream(file);
                            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));

                            while ((ze = zis.getNextEntry()) != null) {

                                byte[] buffer = new byte[1024];
                                int count;
                                while ((count = zis.read(buffer)) != -1) {

                                    os.write(buffer, 0, count);

                                }

                                is = new FileInputStream(tempFile);

                            }
                        }
                        catch (Exception e1)
                        {
                            e1.printStackTrace();
                        }

                    }

                    try {

                        if (is != null)
                        {

                            KmlLayer layer = new KmlLayer(is, activity /*ctx*/);

                            return layer;
                        }
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }

                }
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /*
    * It is called when any filter is apply
    * on trails
    * @params hashMap holds information about
    * filter that has been apply.
    * */
    public void filterTrail(HashMap<String ,String> hashMap)
    {

        OrganizationTrailModel model = getModelFromArgument();
        List<OrganizationTrailModel.records> statusFilterList =null;
        List<OrganizationTrailModel.records> difficultyFilterList=null;
        List<OrganizationTrailModel.records> favFilterList= null;

        if(hashMap == null)
            return;

        List<OrganizationTrailModel.records> originalList = model.records;

        if(!hashMap.get(Constant.O_IS_FILTER_ON ).equalsIgnoreCase("0"))
        {
            if (hashMap.get(Constant.O_IS_FAV_SEL).equalsIgnoreCase("1"))
            {
                favFilterList = filterOnBasisOfFavourate(true ,originalList);
            }

            if (!hashMap.get(Constant.O_STATUS_SEL_VAL).equalsIgnoreCase(""))
            {
                if (favFilterList == null)
                    statusFilterList = filterOnBasisOfStatus(hashMap.get(Constant.O_STATUS_SEL_VAL), originalList);
                else
                    statusFilterList = filterOnBasisOfStatus(hashMap.get(Constant.O_STATUS_SEL_VAL), favFilterList);
            }
            else
            {
                statusFilterList = favFilterList;
            }

            if (!hashMap.get(Constant.T_DIFF_SEL_VAL).equalsIgnoreCase(""))
            {
                if(statusFilterList == null)
                    difficultyFilterList = filterOnBasisOfDiff(hashMap.get(Constant.T_DIFF_SEL_VAL) ,originalList);
                else
                    difficultyFilterList = filterOnBasisOfDiff(hashMap.get(Constant.T_DIFF_SEL_VAL) ,statusFilterList);
            }
            else
            {
                difficultyFilterList = statusFilterList;
            }


            if(difficultyFilterList.size()!=0)
            {

                ArrayList<OrganizationTrailModel.records> filterList = (ArrayList<OrganizationTrailModel.records>)getArguments()
                        .getSerializable(Constant.FILTER_LIST);
                filterList.clear();
                filterList.addAll(difficultyFilterList);
                prepareExtraPins(hashMap);
                addTrailPin();
                renderKml();

            }
            else{

                ((TrailDetail)getActivity()).showNoResultDialog();
                return;
            }

        }
        else
        {
            ArrayList<OrganizationTrailModel.records> filterList = (ArrayList<OrganizationTrailModel.records>)getArguments()
                    .getSerializable(Constant.FILTER_LIST);
            filterList.clear();
            filterList.addAll(originalList);
            prepareExtraPins(hashMap);
            addTrailPin();
            renderKml();

        }

    }


    /*
    * It return list of all trail that
    * is favourate to user.
    */
    public List<OrganizationTrailModel.records> filterOnBasisOfFavourate(boolean isToShowFavourate ,List<OrganizationTrailModel.records> originalList)
    {

        List<OrganizationTrailModel.records> filterList = new ArrayList<OrganizationTrailModel.records>();

            if(isToShowFavourate)
            {
                for (int index = 0; index < originalList.size(); index++)
                {
                    OrganizationTrailModel.records obj = originalList.get(index);
                    if (obj.isFavourite.equalsIgnoreCase(Constant.FAV_STATUS.FAV.getKey()))
                    {
                        filterList.add(obj);
                    }

                }

            }
            else
            {

            }


        return filterList;
    }


    /*
    * It filters trail according to status
    * that has been selected in filter and
    * return list of those
    * */
    public List<OrganizationTrailModel.records> filterOnBasisOfStatus(String ids
            ,List<OrganizationTrailModel.records> originalList)
    {
        //  TrailDetailListAdapter adapter = getAdapter();
        ArrayList<String> selectItem = new ArrayList<String>();
        if(ids.contains(" "))
        {
            String[] selectedValuesArray  = ids.split(" ");
            // selectItem.addAll(selectedValuesArray);
            for(int index=0;index<selectedValuesArray.length;index++)
            {
                selectItem.add(selectedValuesArray[index]);
            }
        }
        else
        {
            selectItem.add(ids);

        }

        if(selectItem.size()==0)
            return null;


        List<OrganizationTrailModel.records> filterList = new ArrayList<OrganizationTrailModel.records>();
        for (int index = 0; index < originalList.size(); index++)
        {
            OrganizationTrailModel.records obj = originalList.get(index);
            if (selectItem.contains(obj.trailStatus))
            {
                filterList.add(obj);
            }

        }

        return filterList;
    }


    /*
    * It return list of those trail
    * that have those difficulty level
    * which is selected in filters.
    * */
    public List<OrganizationTrailModel.records> filterOnBasisOfDiff(String ids
            ,List<OrganizationTrailModel.records> originalList){


        ArrayList<String> selectItem = new ArrayList<String>();
        if(ids.contains(" "))
        {
            String[] selectedValuesArray  = ids.split(" ");
           
            for(int index=0;index<selectedValuesArray.length;index++)
            {
                selectItem.add(selectedValuesArray[index]);
            }
        }
        else
        {
            selectItem.add(ids);

        }

        if(selectItem.size()==0)
            return null;


        List<OrganizationTrailModel.records> filterList = new ArrayList<OrganizationTrailModel.records>();
        for (int index = 0; index < originalList.size(); index++)
        {
            OrganizationTrailModel.records obj = originalList.get(index);
            if (selectItem.contains(obj.trailDifficultyId))
            {
                filterList.add(obj);
            }

        }

        return filterList;

    }

    /*
    * This method is used to show
    * Navigation ,Service and Special Pins
    * that has been selected in Show option
    * @params hashMap holds information of
    * selected pins in show option.*/
    private void prepareExtraPins(HashMap<String ,String> hashMap)
    {
        List<OrganizationTrailModel.pins> pinsList = getModelFromArgument().pins;

        if(pinsMap.size()>0) {
            pinsMap.clear();
            map.clear();
        }
        if(hashMap.get(Constant.O_IS_SHOW_FILTER_ON).equalsIgnoreCase("1"))
        {

            if(pinsList != null && pinsList.size()>0)
            {
                for(int index=0;index<pinsList.size();index++)
                {

                     prepareMarkerObject(pinsList.get(index) ,hashMap);

                }

                if(pinsMap.size()>0)
                {
                    if(trailMapInfoAdapter == null)
                        trailMapInfoAdapter = new TrailMapInfoAdapter(activity);
                    map.setInfoWindowAdapter(trailMapInfoAdapter);
                    trailMapInfoAdapter.setPinsMap(pinsMap);
                }

            }

        }
        else
        {


        }

    }



    /*
    * It add marker ie pin
    * on Google map and store pin's
    * info window information in
    * a hash map pinMap.
    * */
    private void prepareMarkerObject(OrganizationTrailModel.pins obj ,HashMap<String ,String> hashMap) {
        boolean flag = false;

        if (obj.pinType.equalsIgnoreCase("1"))
        {
            if (hashMap.get(Constant.O_ACCOM_SEL_VAL).equalsIgnoreCase("1"))
            {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("2"))
        {
            if (hashMap.get(Constant.O_DIFFICULTY_SEL_VAL).equalsIgnoreCase("1"))
            {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("3"))
        {
            if (hashMap.get(Constant.O_FEATURES_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("4"))
        {
            if (hashMap.get(Constant.O_RESTAURANTS_SEL_VAL).equalsIgnoreCase("1"))
            {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("5"))
        {
            if (hashMap.get(Constant.O_FUEL_GAS_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("6"))
        {
            if (hashMap.get(Constant.O_INTERSECTIIONS_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("7"))
        {
            if (hashMap.get(Constant.O_PARKING_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("8"))
        {
            if (hashMap.get(Constant.O_SERVICE_REPAIR_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("9"))
        {
            if (hashMap.get(Constant.O_SCENIC_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("10"))
        {
            if (hashMap.get(Constant.O_DIRECTION_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("11"))
        {
            if (hashMap.get(Constant.O_TRAILHEADS_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("12"))
        {
            if (hashMap.get(Constant.O_RESTAURANTS_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("13"))
        {
            if (hashMap.get(Constant.O_BAR_PUB_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }
        }
        else if (obj.pinType.equalsIgnoreCase("14"))
        {

            if (hashMap.get(Constant.O_BREWERIES_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }

        }
        else if (obj.pinType.equalsIgnoreCase("15"))
        {

            if (hashMap.get(Constant.O_DONAT_SEL_VAL).equalsIgnoreCase("1")) {
                flag=true;
            }

        }

        if (flag)
        {
            Marker marker = map.addMarker(getMarkerOptionObject(obj));
            pinsMap.put(marker.getId() ,obj);

        }
    }

    /*
    * It return marker option object
    * for pins to be added on Google map
    * @params pinObject contains information
    * about particular pin.
    * */
    private MarkerOptions getMarkerOptionObject(OrganizationTrailModel.pins pinObject)
    {
        MarkerOptions markerOptions = null;
        try
        {
            markerOptions =  new MarkerOptions()
                    .position(new LatLng(Double.parseDouble(pinObject.lat), Double.parseDouble(pinObject.lon)))
                    .icon(getExtraPinsImage(pinObject));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return markerOptions;
    }

    /*
    * It gives images of different pins
    * according to its type.
    * @return BitmapDescriptor of image of
    * Service and Special pins
    * */
    private BitmapDescriptor getExtraPinsImage(OrganizationTrailModel.pins pinObject){

        int imageId;

        if(pinObject.pinType.equalsIgnoreCase("1"))
        {
            imageId = R.drawable.accomdation_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("2"))
        {
            imageId = getDifficultyPin(pinObject.img);
        }
        else if(pinObject.pinType.equalsIgnoreCase("3"))
        {
            imageId = R.drawable.feature_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("4"))
        {
            imageId = R.drawable.restaurants_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("5"))
        {
            imageId = R.drawable.fuel_gas_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("6"))
        {
            imageId = R.drawable.intersection;
        }
        else if(pinObject.pinType.equalsIgnoreCase("7"))
        {
            imageId = R.drawable.parking_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("8"))
        {
            imageId = R.drawable.service_repairs_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("9"))
        {
            imageId = R.drawable.scenic_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("10"))
        {
            imageId = getDirectionPin(pinObject.img);
        }
        else if(pinObject.pinType.equalsIgnoreCase("11"))
        {
            imageId = R.drawable.trail_heads_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("12"))
        {
            imageId = R.drawable.restaurants_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("13"))
        {
            imageId = R.drawable.bars_pubs_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("14"))
        {
            imageId = R.drawable.breweries_pin;
        }
        else if(pinObject.pinType.equalsIgnoreCase("15"))
        {
            imageId = R.drawable.donate_pin;
        }
        else
        {
            imageId = R.drawable.other;
        }

        return BitmapDescriptorFactory.fromResource(imageId);
    }


    /*
    * It returns difficulty pins
    * id according to its level.
    * */
    private int getDifficultyPin(String difficultyLevel)
    {
        String level = "";
        int imageId = R.drawable.p_beginer;

        try
        {
            level = (difficultyLevel.split("_"))[1];

        }catch (Exception e)
        {
            return imageId;
        }



        if(level.equalsIgnoreCase("1"))
        {
            imageId = R.drawable.p_beginer;
        }
        else if(level.equalsIgnoreCase("2"))
        {
            imageId = R.drawable.p_intermediate;
        }
        else if(level.equalsIgnoreCase("3"))
        {
            imageId = R.drawable.p_expert;
        }
        else if(level.equalsIgnoreCase("4"))
        {
            imageId = R.drawable.p_expert_only;
        }
        else if(level.equalsIgnoreCase("5"))
        {
            imageId = R.drawable.p_terrain_park;
        }

        return imageId;
    }

    /*
    * It returns direction pins
    * id according to direction type.
    * */
    private int getDirectionPin(String directionType)
    {
        int imageId = R.drawable.e;
        String direction="";
        try
        {
            direction = (directionType.split("_"))[1];
        }
        catch (Exception e)
        {
            return imageId;
        }


        if(direction.equalsIgnoreCase("e"))
        {
            imageId = R.drawable.e;
        }
        else if(direction.equalsIgnoreCase("w"))
        {
            imageId = R.drawable.w;
        }
        else if(direction.equalsIgnoreCase("n"))
        {
            imageId = R.drawable.n;
        }
        else if(direction.equalsIgnoreCase("s"))
        {
            imageId = R.drawable.s;
        }
        else if(direction.equalsIgnoreCase("ne"))
        {
            imageId = R.drawable.ne;
        }
        else if(direction.equalsIgnoreCase("nw"))
        {
            imageId = R.drawable.nw;
        }
        else if(direction.equalsIgnoreCase("se"))
        {
            imageId = R.drawable.se;
        }
        else if(direction.equalsIgnoreCase("sw"))
        {
            imageId = R.drawable.sw;
        }

        return imageId;
    }


    /*
    * It return OrganizationTrailModel
    * stored in intent
    * */
    private OrganizationTrailModel getModelFromArgument()
    {
        OrganizationTrailModel model = (OrganizationTrailModel)getArguments().getSerializable(OrganizationTrailModel.class.getSimpleName());

        return model;
    }

    /*
    * It display organization pins
    * on Google map.*/
    private void showOrganization()
    {
        try {
            OrganizationTrailModel model = getModelFromArgument();
            LatLng latlng = new LatLng(Double.parseDouble(model.latitude) ,Double.parseDouble(model.longitude));

            Marker marker = map.addMarker(new MarkerOptions()
                    .title(model.organizationName)
                    .position(latlng)
                    .icon(AppUtility.getBimapDiscriptor(model.organizationStatus)));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latlng)      // Sets the center of the map to Mountain View
                    .zoom(12)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder

            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        }
        catch (Exception e)
        {

        }

    }

    /*
    * This method checks all trail
    * that is to be shown on Google
    * map have lat-lng for trail marker
    * and kml/kmz file corresponding to
    * it.if it has then information is
    * passed to  set marker pin on map
     **/
    private void addTrailPin()
    {

        ArrayList<OrganizationTrailModel.records> filterList = (ArrayList<OrganizationTrailModel.records>)getArguments()
                .getSerializable(Constant.FILTER_LIST);

        if(trailMap.size()>0)
            trailMap.clear();

        if(trailMarkerMap.size()>0)
            trailMarkerMap.clear();

        for(int index=0;index<filterList.size();index++)
        {
            OrganizationTrailModel.records trailObject = filterList.get(index);

            if(trailObject.trailLattitude != null && !trailObject.trailLattitude.equalsIgnoreCase(""))
            {

                if(trailObject.fileUrl != null && !trailObject.fileUrl.equalsIgnoreCase(""))
                addTrailMarker(trailObject);
            }

        }
        if(trailMapInfoAdapter == null)
        {
            trailMapInfoAdapter = new TrailMapInfoAdapter(activity);
            trailMapInfoAdapter.setPinsMap(pinsMap);
            map.setInfoWindowAdapter(trailMapInfoAdapter);
        }
        trailMapInfoAdapter.setTrailPinMap(trailMap);
        trailMapInfoAdapter.setMeasureUnit(getModelFromArgument().openMeasure);
    }


    /*
    * This method sets trail marker
    * on Google map and store
    * info window information of each
    * trail marker in trailMarkerMap.
    *
    **/
    private void addTrailMarker(OrganizationTrailModel.records trail)
    {

        MarkerOptions markerOptions = null;
        try
        {
            markerOptions =  new MarkerOptions()
                    .position(new LatLng(Double.parseDouble(trail.trailLattitude), Double.parseDouble(trail.trailLongitude)))
                    .icon(BitmapDescriptorFactory.fromResource(getTrailPin(trail.trailStatus)));
            Marker marker = map.addMarker(markerOptions);
            trailMarkerMap.put(trail.trailId ,marker);
            trailMap.put(marker.getId() ,trail);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
    * This mmethod return id
    * of trail pin according to
    * status of trail.
    * */
    private int getTrailPin(String trailStatus)
    {
        int imageId = R.drawable.t_open_pin;

        if(trailStatus.equalsIgnoreCase(Constant.TRAIL_STATUS.OPEN.getKey()))
        {
            imageId = R.drawable.t_open_pin;
        }
        else if(trailStatus.equalsIgnoreCase(Constant.TRAIL_STATUS.CAUTION.getKey()))
        {
            imageId = R.drawable.t_caution_pin;
        }
        else if(trailStatus.equalsIgnoreCase(Constant.TRAIL_STATUS.CLOSE.getKey()))
        {
            imageId = R.drawable.t_closed_pin;
        }

        return imageId;
    }



}
