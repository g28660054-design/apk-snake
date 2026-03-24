package com.exple.mpp;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.EditText;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
  WebView wv; ValueCallback<Uri[]> filePathCallback;
  static final int FILE_CHOOSER_CODE=1,PERM_CODE=2;
  static final String APP_FOLDER="MyAppek";

  @Override protected void onCreate(Bundle s) {
    super.onCreate(s);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    wv=new WebView(this); setContentView(wv);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    requestRuntimePerms();
    WebSettings ws=wv.getSettings();
    ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setDatabaseEnabled(true);
    ws.setAllowFileAccess(true);ws.setAllowFileAccessFromFileURLs(true);
    ws.setAllowUniversalAccessFromFileURLs(true);ws.setCacheMode(WebSettings.LOAD_DEFAULT);
    ws.setMediaPlaybackRequiresUserGesture(false);ws.setBuiltInZoomControls(false);
    ws.setDisplayZoomControls(false);ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    wv.addJavascriptInterface(new Object(){
      @JavascriptInterface public void saveFile(final String fn,final String b64,final String mt){
        try{saveToDownloads(fn,Base64.decode(b64,Base64.DEFAULT));}catch(Exception e){
          runOnUiThread(()->Toast.makeText(MainActivity.this,"Ошибка: "+e.getMessage(),Toast.LENGTH_LONG).show());}
      }
    },"Android");
    wv.setDownloadListener((url,ua,cd,mt,cl)->{
      String fn=URLUtil.guessFileName(url,cd,mt);if(fn==null||fn.isEmpty())fn="file";
      if(url.startsWith("blob:")){final String f=fn,m=mt;
        wv.post(()->wv.evaluateJavascript("(function(){fetch('"+url+"').then(r=>r.blob()).then(b=>{var rd=new FileReader();rd.onloadend=function(){Android.saveFile('"+f+"',rd.result.split(',')[1],'"+m+"');};rd.readAsDataURL(b);})})()",null));return;}
      if(url.startsWith("data:")){try{String d=url.contains(",")? url.split(",",2)[1]:"";saveToDownloads(fn,Base64.decode(d,Base64.DEFAULT));}catch(Exception e){Toast.makeText(this,"Ошибка: "+e.getMessage(),Toast.LENGTH_LONG).show();}return;}
      try{DownloadManager.Request req=new DownloadManager.Request(Uri.parse(url));
        req.setMimeType(mt);req.addRequestHeader("User-Agent",ua);req.setTitle(fn);
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,APP_FOLDER+"/"+fn);
        ((DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(req);
        Toast.makeText(this,"Скачивается: "+fn,Toast.LENGTH_SHORT).show();
      }catch(Exception e){Toast.makeText(this,"Ошибка: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    });
    wv.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){
        String url=req.getUrl().toString();
        if(url.startsWith("file:")||url.startsWith("javascript:"))return false;
        try{startActivity(new Intent(Intent.ACTION_VIEW,req.getUrl()));}catch(Exception ignored){}
        return true;
      }
      @Override public void onReceivedError(WebView v,int c,String d,String u){}
    });
    wv.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onJsAlert(WebView v,String u,String m,final JsResult r){
        new AlertDialog.Builder(MainActivity.this).setMessage(m).setCancelable(false).setPositiveButton("OK",(d,w)->r.confirm()).show();return true;}
      @Override public boolean onJsConfirm(WebView v,String u,String m,final JsResult r){
        new AlertDialog.Builder(MainActivity.this).setMessage(m).setCancelable(false).setPositiveButton("OK",(d,w)->r.confirm()).setNegativeButton("Отмена",(d,w)->r.cancel()).show();return true;}
      @Override public boolean onJsPrompt(WebView v,String u,String m,String def,final JsPromptResult r){
        final EditText et=new EditText(MainActivity.this);et.setText(def);
        new AlertDialog.Builder(MainActivity.this).setMessage(m).setView(et).setCancelable(false).setPositiveButton("OK",(d,w)->r.confirm(et.getText().toString())).setNegativeButton("Отмена",(d,w)->r.cancel()).show();return true;}
      @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams p){
        filePathCallback=cb;startActivityForResult(Intent.createChooser(p.createIntent(),"Выбери файл"),FILE_CHOOSER_CODE);return true;}
    });
    if(s!=null)wv.restoreState(s);else wv.loadUrl("file:///android_asset/splash.html");
  }
  void saveToDownloads(String fn,byte[] data){
    try{File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),APP_FOLDER);
      if(!dir.exists())dir.mkdirs();File out=new File(dir,fn);
      FileOutputStream fos=new FileOutputStream(out);fos.write(data);fos.close();
      runOnUiThread(()->Toast.makeText(this," Сохранено: Downloads/"+APP_FOLDER+"/"+fn,Toast.LENGTH_LONG).show());
    }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Ошибка: "+e.getMessage(),Toast.LENGTH_LONG).show());}
  }
  void requestRuntimePerms(){
    if(Build.VERSION.SDK_INT<23)return;
    List<String> need=new ArrayList<>();
    String[] cand=Build.VERSION.SDK_INT>=33
      ?new String[]{android.Manifest.permission.CAMERA,android.Manifest.permission.RECORD_AUDIO,"android.permission.READ_MEDIA_IMAGES","android.permission.READ_MEDIA_VIDEO","android.permission.READ_MEDIA_AUDIO"}
      :new String[]{android.Manifest.permission.CAMERA,android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    for(String p:cand)if(checkSelfPermission(p)!=android.content.pm.PackageManager.PERMISSION_GRANTED)need.add(p);
    if(!need.isEmpty())requestPermissions(need.toArray(new String[0]),PERM_CODE);
  }
  @Override protected void onActivityResult(int req,int res,Intent data){
    if(req==FILE_CHOOSER_CODE){Uri[] r=null;if(res==Activity.RESULT_OK&&data!=null)r=new Uri[]{data.getData()};if(filePathCallback!=null)filePathCallback.onReceiveValue(r);filePathCallback=null;}
  }
  @Override protected void onSaveInstanceState(Bundle o){super.onSaveInstanceState(o);wv.saveState(o);}
  @Override public void onBackPressed(){if(wv.canGoBack())wv.goBack();else super.onBackPressed();}
}
