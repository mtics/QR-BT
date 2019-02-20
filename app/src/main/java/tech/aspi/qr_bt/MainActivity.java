package tech.aspi.qr_bt;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import tech.aspi.util.JsonUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button scanBtn;

    private TextView tvScanFormat, tvScanContent;

    private LinearLayout llSearch;

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private JsonUtil jsonUtil = new JsonUtil();

    private final static int SEARCH_CODE = 0x123;

    private static final String TAG = "MainActivity";

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        scanBtn = (Button) findViewById(R.id.scan_button);

        tvScanFormat = (TextView) findViewById(R.id.tvScanFormat);

        tvScanContent = (TextView) findViewById(R.id.tvScanContent);

        llSearch = (LinearLayout) findViewById(R.id.llSearch);

        scanBtn.setOnClickListener(this);

    }

    public void onClick(View v) {

        llSearch.setVisibility(View.GONE);

        IntentIntegrator integrator = new IntentIntegrator(this);

        integrator.setPrompt("Scan a QRcode");

        integrator.setOrientationLocked(false);

        integrator.initiateScan();

//        Use this for more customization

//        IntentIntegrator integrator = new IntentIntegrator(this);

//        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);

//        integrator.setPrompt("Scan a barcode");

//        integrator.setCameraId(0);  // Use a specific camera of the device

//        integrator.setBeepEnabled(false);

//        integrator.setBarcodeImageEnabled(true);

//        integrator.initiateScan();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        // 若识别到二维码中的内容
        if (result != null) {

            // 若内容为空
            if (result.getContents() == null) {

                llSearch.setVisibility(View.GONE);

                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();

            } else {

                llSearch.setVisibility(View.VISIBLE);

                tvScanContent.setText(result.getContents());

                tvScanFormat.setText(result.getFormatName());

                try {
                    // 将内容转换为JSON格式，以获取MAC地址
                    JSONObject jsonObject = jsonUtil.strToJson(result.getContents());

                    String macAddr = jsonObject.get("MAC").toString();

                    if (BluetoothAdapter.checkBluetoothAddress(macAddr)){
                        //若蓝牙未打开则打开蓝牙
                        if (!mBluetoothAdapter.isEnabled()) {
                            mBluetoothAdapter.enable();
                        }

                        // 根据MAC地址创建BluetoothDevice
                        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(macAddr);

                        // 蓝牙连接，返回值为boolean
                        bluetoothDevice.createBond();
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

        } else {
            // 若未识别到二维码
            super.onActivityResult(requestCode, resultCode, data);

        }

    }

    /**
     * 判断蓝牙是否开启
     */
    private void init() {
        // 判断手机是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }
        // 判断是否打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            //弹出对话框提示用户是后打开
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, SEARCH_CODE);
        } else {
            // 不做提示，强行打开
            mBluetoothAdapter.enable();
        }
        startDiscovery();
        Log.e(TAG, "startDiscovery: 开启蓝牙");
    }

    /**
     * 注册异步搜索蓝牙设备的广播
     */
    private void startDiscovery() {
        // 找到设备的广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        // 注册广播
        registerReceiver(receiver, filter);
        // 搜索完成的广播
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        // 注册广播
        registerReceiver(receiver, filter1);
        Log.e(TAG, "startDiscovery: 注册广播");
    }

    /**
     * 广播接收器
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 收到的广播类型
            String action = intent.getAction();
            // 发现设备的广播
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 从intent中获取设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 没配对
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

                    // 搜索完成
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    // 关闭进度条
                    progressDialog.dismiss();
                    Log.e(TAG, "onReceive: 搜索完成");
                }
            }
        }
    };


        private ProgressDialog progressDialog;

        /**
         * 获取本机蓝牙地址
         */
        private String getBluetoothAddress() {
            try {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                Field field = bluetoothAdapter.getClass().getDeclaredField("mService");
                // 参数值为true，禁用访问控制检查
                field.setAccessible(true);
                Object bluetoothManagerService = field.get(bluetoothAdapter);
                if (bluetoothManagerService == null) {
                    return null;
                }
                Method method = bluetoothManagerService.getClass().getMethod("getAddress");
                Object address = method.invoke(bluetoothManagerService);
                if (address != null && address instanceof String) {
                    return (String) address;
                } else {
                    return null;
                }
                //抛一个总异常省的一堆代码...
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

}