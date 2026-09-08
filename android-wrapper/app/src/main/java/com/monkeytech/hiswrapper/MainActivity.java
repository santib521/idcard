package com.monkeytech.hiswrapper;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final String DEFAULT_HIS="https://his-uat.easehospital.com/web/login";
    private static final String FLOATING_URL="https://santib521.github.io/idcard/index.html";
    private static final String PREFS="mkt_his_wrapper";
    private static final int SLOTS=6,CAMERA_REQUEST=10,FILE_REQUEST=11;
    private WebView his,floating;
    private PermissionRequest cameraRequest;
    private ValueCallback<Uri[]> fileCallback;
    private boolean expanded;

    @Override public void onCreate(Bundle state){
        super.onCreate(state); ensureDefaults();
        FrameLayout root=new FrameLayout(this);
        his=new WebView(this); root.addView(his,new FrameLayout.LayoutParams(-1,-1));
        floating=new WebView(this); floating.setBackgroundColor(Color.TRANSPARENT);
        root.addView(floating,miniParams()); setContentView(root);
        setupHis(); setupFloating();
        if(state==null) his.loadUrl(defaultUrl()); else his.restoreState(state);
        floating.loadUrl(FLOATING_URL);
    }

    private void setupHis(){
        common(his,false);
        his.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                String host=r.getUrl().getHost();
                if(host!=null&&allowedHosts().contains(host.toLowerCase())) return false;
                try{startActivity(new Intent(Intent.ACTION_VIEW,r.getUrl()));}catch(Exception ignored){}
                return true;
            }
        });
        his.setWebChromeClient(chrome(false));
    }

    private void setupFloating(){
        common(floating,true);
        floating.addJavascriptInterface(new Bridge(),"MktWrapperHost");
        floating.setOnLongClickListener(v->{showSetup();return true;});
        floating.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                return !"santib521.github.io".equalsIgnoreCase(r.getUrl().getHost());
            }
            @Override public void onPageFinished(WebView v,String url){
                // สังเกต Mini/Full ที่หน้าเดิมสร้างไว้เท่านั้น ไม่แก้ index.html หรือ Function อ่านบัตร
                v.evaluateJavascript("(function(){var w=document.getElementById('mktFloatingWidget');if(!w)return;function r(){MktWrapperHost.setExpanded(w.classList.contains('mkt-full-mode'));}new MutationObserver(r).observe(w,{attributes:true,attributeFilter:['class']});r();})();",null);
            }
        });
        floating.setWebChromeClient(chrome(true));
    }

    private void common(WebView w,boolean localAgent){
        WebSettings s=w.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true); s.setSupportZoom(false); s.setAllowFileAccess(false); s.setAllowContentAccess(true);
        if(localAgent)s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(w,true);
    }

    private FrameLayout.LayoutParams miniParams(){
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(330),dp(92));
        p.gravity=Gravity.END|Gravity.BOTTOM; p.setMargins(0,0,dp(2),dp(2)); return p;
    }
    private FrameLayout.LayoutParams fullParams(){
        int width=Math.min(getResources().getDisplayMetrics().widthPixels,dp(450));
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(width,-1); p.gravity=Gravity.END|Gravity.BOTTOM; return p;
    }
    private void resize(boolean full){
        if(expanded==full)return; expanded=full;
        floating.setLayoutParams(full?fullParams():miniParams()); floating.bringToFront();
    }
    private class Bridge{
        @JavascriptInterface public void setExpanded(boolean full){runOnUiThread(()->resize(full));}
    }

    private void showSetup(){
        SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
        EditText[] fields=new EditText[SLOTS]; RadioButton[] radios=new RadioButton[SLOTS];
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(12),dp(8),dp(12),dp(8));
        int selected=p.getInt("default_slot",0);
        for(int i=0;i<SLOTS;i++){
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            radios[i]=new RadioButton(this); radios[i].setChecked(i==selected);
            final int index=i; radios[i].setOnClickListener(v->{for(int x=0;x<SLOTS;x++)radios[x].setChecked(x==index);});
            row.addView(radios[i],new LinearLayout.LayoutParams(dp(48),dp(56)));
            fields[i]=new EditText(this); fields[i].setSingleLine(); fields[i].setText(p.getString("url_"+i,i==0?DEFAULT_HIS:""));
            fields[i].setHint("https://hospital.example.com/web/login"); fields[i].setTextSize(13);
            row.addView(fields[i],new LinearLayout.LayoutParams(0,dp(58),1)); list.addView(row);
        }
        ScrollView scroll=new ScrollView(this); scroll.addView(list);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("ตั้งค่า HIS URL")
                .setMessage("เพิ่มได้สูงสุด 6 URL และเลือก Default").setView(scroll)
                .setNegativeButton("ยกเลิก",null).setPositiveButton("บันทึกและเปิด",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            int def=-1; SharedPreferences.Editor e=p.edit();
            for(int i=0;i<SLOTS;i++){
                String value=fields[i].getText().toString().trim();
                if(!value.isEmpty()&&!validHttps(value)){fields[i].setError("ต้องเป็น https:// URL");return;}
                e.putString("url_"+i,value); if(radios[i].isChecked())def=i;
            }
            if(def<0||fields[def].getText().toString().trim().isEmpty()){
                Toast.makeText(this,"กรุณาเลือก Default ที่มี URL",Toast.LENGTH_LONG).show();return;
            }
            e.putInt("default_slot",def).apply(); his.loadUrl(fields[def].getText().toString().trim()); d.dismiss();
        })); d.show();
    }

    private boolean validHttps(String value){
        try{Uri u=Uri.parse(value);return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null;}catch(Exception e){return false;}
    }
    private void ensureDefaults(){
        SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
        if(!p.contains("url_0"))p.edit().putString("url_0",DEFAULT_HIS).putInt("default_slot",0).apply();
    }
    private String defaultUrl(){
        SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);String s=p.getString("url_"+p.getInt("default_slot",0),DEFAULT_HIS);
        return s==null||s.trim().isEmpty()?DEFAULT_HIS:s.trim();
    }
    private Set<String> allowedHosts(){
        SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);Set<String> result=new HashSet<>();
        for(int i=0;i<SLOTS;i++){String s=p.getString("url_"+i,"");if(s!=null){String h=Uri.parse(s).getHost();if(h!=null)result.add(h.toLowerCase());}}
        return result;
    }

    private WebChromeClient chrome(boolean permitCamera){
        return new WebChromeClient(){
            @Override public void onPermissionRequest(PermissionRequest r){
                if(!permitCamera){r.deny();return;}
                if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)r.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                else{cameraRequest=r;requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST);}
            }
            @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> callback,FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;
                try{startActivityForResult(params.createIntent(),FILE_REQUEST);return true;}catch(Exception e){fileCallback=null;return false;}
            }
        };
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){
        super.onRequestPermissionsResult(code,permissions,results);
        if(code==CAMERA_REQUEST&&cameraRequest!=null){
            if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)cameraRequest.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});else cameraRequest.deny();cameraRequest=null;
        }
    }
    @Override protected void onActivityResult(int code,int result,Intent data){
        super.onActivityResult(code,result,data);
        if(code==FILE_REQUEST&&fileCallback!=null){fileCallback.onReceiveValue(result==RESULT_OK?WebChromeClient.FileChooserParams.parseResult(result,data):null);fileCallback=null;}
    }
    @Override public void onBackPressed(){
        if(expanded)floating.evaluateJavascript("if(window.minimizeWidget){minimizeWidget();}",null);
        else if(his.canGoBack())his.goBack();else super.onBackPressed();
    }
    @Override protected void onSaveInstanceState(Bundle out){his.saveState(out);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){if(floating!=null)floating.destroy();if(his!=null)his.destroy();super.onDestroy();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
