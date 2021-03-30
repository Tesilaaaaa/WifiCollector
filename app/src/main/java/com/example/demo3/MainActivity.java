package com.example.demo3;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.view.LayoutInflater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1000;
    private final int UPDATE_UI_REQUEST_CODE = 1024;
    private TextView mCurrConnTV;   // 显示当前所连WiFi信息的控件
    private TextView mScanResultTV;    // 显示WiFi扫描结果的控件
    private StringBuffer mCurrConnStr;  // 暂存当前所连WiFi信息的字符串
    private StringBuffer mScanResultStr;    // 暂存WiFi扫描结果的字符串
    private Button wbutton;
    private WifiManager mWifiManager;   // 调用WiFi各种API的对象
    private Timer mTimer;   // 启动定时任务的对象
    private final int SAMPLE_RATE = 2000; // 采样周期，以毫秒为单位
    private RadioButton train,test;//训练和测试的选择
    private RadioGroup gp;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_UI_REQUEST_CODE) {
                updateUI();
            }
        }
    };

    public String trainOrTestMode=null;
    class TheWifi{
        public double level;
        public String mac;
    }

    class LibWifi extends TheWifi {
        public double[] levelonce=new double[5];

    }
    String[] permissions=new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CHANGE_WIFI_STATE,Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION };//用于存储需要申请的危险权限
    List<String> mPermissions=new ArrayList<>();//用于存储未申请的危险权限
    //下面的函数用于动态申请危险权限，需要在主函数中调用initPermission函数才能申请动态权限
    private  void initPermission(){
        mPermissions.clear();

        for(int i=0;i<permissions.length;i++){
            if(ContextCompat.checkSelfPermission(this,permissions[i])!=PackageManager.PERMISSION_GRANTED)
                mPermissions.add(permissions[i]);
        }

        if(mPermissions.size()>0){
            ActivityCompat.requestPermissions(this,permissions,1);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gp=findViewById(R.id.radiogp);
        train=findViewById(R.id.radioButton);
        test=findViewById(R.id.radioButton3);
        RadioGroup.OnCheckedChangeListener mChangeRadio = new
                RadioGroup.OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int i) {
                        if (i==train.getId()){
                            trainOrTestMode = "Train";
                        } else if (i==test.getId()){
                            trainOrTestMode = "Test";
                        }
                    }
                };
        gp.setOnCheckedChangeListener(mChangeRadio);
        initPermission();
        wbutton=findViewById(R.id.button);
        mCurrConnTV = findViewById(R.id.connected_wifi_info_tv);
        mScanResultTV = findViewById(R.id.scan_results_info_tv);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        getLocationAccessPermission();  // 先获取位置权限
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                scanWifi();
                mHandler.sendEmptyMessage(UPDATE_UI_REQUEST_CODE);
            }
        }, 0, SAMPLE_RATE); // 立即执行任务，每隔2000ms执行一次WiFi扫描的任务
        // 扫描周期不能太快，WiFi扫描所有的AP需要一定时间
        wbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                wbutton.setText("Scanning..");
                write(view);
//                wbutton.setText("WRITE");
            }
        });
    }

    /**
     * 增加开启位置权限功能，以适应Android 6.0及以上的版本
     */
    private void getLocationAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
        }
    }


    public StringBuffer scanWifi() {
        // 如果WiFi未打开，先打开WiFi
        if (!mWifiManager.isWifiEnabled())
            mWifiManager.setWifiEnabled(true);
        // 开始扫描WiFi
        mWifiManager.startScan();
        // 获取并保存当前所连WiFi信息
        mCurrConnStr = new StringBuffer();
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if(wifiInfo.getBSSID()==null)
            mCurrConnStr.append("未连接WiFi");
        else{
            mCurrConnStr.append("SSID: ").append(wifiInfo.getSSID()).append("\n");
            mCurrConnStr.append("MAC: ").append(wifiInfo.getBSSID()).append("\n");
            mCurrConnStr.append("信号强度(dBm): ").append(wifiInfo.getRssi()).append("\n");
            mCurrConnStr.append("速率: ").append(wifiInfo.getLinkSpeed()).append(" ").append(WifiInfo.LINK_SPEED_UNITS);
        }

        // 获取并保存WiFi扫描结果
        mScanResultStr = new StringBuffer();
        List<ScanResult> scanResults = mWifiManager.getScanResults();
        mScanResultStr.append("SSID   MAC   信号强度(dBm)").append("\n");
        for (ScanResult sr : scanResults) {
            mScanResultStr.append(sr.SSID).append("  ");
            mScanResultStr.append(sr.BSSID).append("  ");
            mScanResultStr.append(sr.level).append("\n");
        }
    return mScanResultStr;
    }

    private void updateUI() {
        mCurrConnTV.setText(mCurrConnStr);
        mScanResultTV.setText(mScanResultStr);
    }

    StringBuffer wifiScanRes=new StringBuffer();//记录过程中产生的wifi扫描结果


    public void write(View v){
        TheWifi[] wifi=new TheWifi[5*50];
        EditText xText,yText;//点位的坐标
        xText=findViewById(R.id.editTextTextPersonName3);
        yText=findViewById(R.id.editTextTextPersonName7);
        String xstring,ystring;
        xstring=xText.getText().toString();
        ystring=yText.getText().toString();
        double x,y;
        try{
            x=Double.parseDouble(xstring);
            y=Double.parseDouble(ystring);
        }
        catch (Exception e){
            AlertDialog textTips = new AlertDialog.Builder(MainActivity.this)
                    .setMessage("请输入点位坐标！")
                    .create();
            textTips.show();
            wbutton.setText("WRITE");
            return;
        }
        if(trainOrTestMode==null){
            AlertDialog textTips = new AlertDialog.Builder(MainActivity.this)
                    .setMessage("请选择模式！")
                    .create();
            textTips.show();
            wbutton.setText("WRITE");
            return;
        }
        final int[] num = {0};
        final int[] i = {0};
        for (int m=0;m<250;m++)
            wifi[m]=new TheWifi();
        int demo=0;
        while (true){
            mWifiManager.startScan();
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            String scanRes = new String();
            for (ScanResult sr : scanResults) {

                wifi[num[0]*50+i[0]].level=sr.level;
                wifi[num[0]*50+i[0]].mac=sr.BSSID;
                i[0]++;
                if (i[0] >= 50)
                    break;
            }
            i[0] = 0;
            num[0]++;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(num[0]>=5){
                demo=1;
                break;
            }
        }
        if(demo==1)
            wbutton.setText("WRITE");
        LibWifi[] libWifi=new LibWifi[100];
        for (int d=0;d<100;d++){
            libWifi[d]=new LibWifi();
        }
        int[] libnum2=new int[100];//存储每个wifi出现了几次，也用作存储每次测量wifi强度的索引
        int libnum=0;// 存储libwifi非null边界的索引
        for(int num1=0;num1<250;num1++){
            if(num1<50){//前30组数据为第一次测量的WiFi强度，不可能有同一个mac的情况
                if(wifi[num1].mac!=null){
                    libWifi[libnum].mac=wifi[num1].mac;
                    libWifi[libnum].levelonce[0]=wifi[num1].level;
                    libnum2[libnum]++;
                    libnum++;
                }
            }
            else{//都是后来测的数据，mac会有很多与第一组重复，这里需要用字典将其鉴别
                int mode=0;//判定wifi库是否存在匹配mac的状态值
                for (int j = 0; j < 100; j++){
                    if(wifi[num1].mac!=null){
                        if(wifi[num1].mac.equals(libWifi[j].mac)){
                            libWifi[j].levelonce[libnum2[j]]=wifi[num1].level;
                            libnum2[j]++;
                            mode=1;
                        }
                        else if(mode==0 && j==99){//不匹配，需要将新mac加入库中
                            libWifi[libnum].mac=wifi[num1].mac;
                            libWifi[libnum].levelonce[libnum2[libnum]]=wifi[num1].level;
                            libnum2[libnum]++;
                            libnum++;
                        }
                    }
                    else {
                        break;
                    }
                }
            }
        }
        for(int j=0;j<libnum;j++){
            double sum=0;
            for(int n=0;n<5;n++){
                sum+=libWifi[j].levelonce[n];//对wifi字典内的每次测量的强度求和
            }
            libWifi[j].level=sum/libnum2[j];
        }
        try {
           down(x,y,libWifi,libnum);
        } catch (IOException e) {
            e.printStackTrace();
        }
        AlertDialog textTips = new AlertDialog.Builder(MainActivity.this)
                .setMessage(trainOrTestMode+" ("+xstring+","+ystring+")wifi信息记录完成！")
                .create();
        textTips.show();
    }

    //获取系统时间，构建文件名
    public String getDate(){
        Long time=System.currentTimeMillis();
        Date date = new Date(time);
        TimeZone tz = TimeZone.getTimeZone("Asia/Beijing");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'");
        df.setTimeZone(tz);
        String nowAsIOS = df.format(date);
        return nowAsIOS;
    }
    public String getTime(){
        Long time=System.currentTimeMillis();
        Date date = new Date(time);
        TimeZone tz = TimeZone.getTimeZone("Asia/Beijing");
        String nowAsIOS=Integer.toString(date.getHours())+":"+Integer.toString(date.getMinutes())+":"+Integer.toString(date.getSeconds());
        return nowAsIOS;
    }




    public void down(double x,double y,LibWifi[] libWifi,int libnum) throws IOException {
        String path =Environment.getExternalStorageDirectory().getAbsolutePath() +"/WifiScanner";
        File Folder=new File(path);
        if (!Folder.exists())
            Folder.mkdirs();
        File file=null;
        String nowAsIOS=getDate();
        String nowTime=getTime();
        file=new File(path+"/"+trainOrTestMode+nowAsIOS+".txt");
        if (!file.exists())
            file.createNewFile();
        FileWriter fos = new FileWriter (file,true);
        String data=x+" "+y+" "+libnum+" "+nowAsIOS+"-"+nowTime+"\n";
        fos.write(data);
        fos.flush();
        for(int i=0;i<libnum;i++){
            data=libWifi[i].mac+" "+libWifi[i].level+" | ";
            for(int j=0;j<5;j++){
                data=data+libWifi[i].levelonce[j]+" ";
            }
            data=data+"\n";
            fos.write(data);
            fos.flush();
        }
        data="\n";
        fos.write(data);
        fos.close();
    }
}