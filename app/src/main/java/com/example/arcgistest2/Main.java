package com.example.arcgistest2;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.documentfile.provider.DocumentFile;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureCollectionTable;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.example.arcgistest2.base.BaseMapActivity;
import com.example.arcgistest2.editor.FeatureEditor;
import com.example.arcgistest2.layer.LayerManager;
import com.example.arcgistest2.legend.LegendManager;
import com.example.arcgistest2.ui.UIManager;
import com.example.arcgistest2.utils.FileManager;
import com.example.arcgistest2.utils.PermissionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Main extends BaseMapActivity {

    private static final String TAG = "MainActivity";
    private MapView mMapView; // 地图视图组件
    private FeatureLayer mFeatureLayer; // 要素图层
    private ListView layerListView;//图层列表
    private Button layerListButton;
    private Button tuli;

    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;

    // 添加图层管关变量
    private ArcGISMap map;
    private List<FeatureLayer> featureLayers = new ArrayList<>();
    private ArrayAdapter<String> layerListAdapter;
    private List<String> layerNames = new ArrayList<>();

    private FeatureEditor featureEditor;
    private boolean isDrawing = false;

    // 在类的开头添加常量
    private static final int PICK_SHAPEFILE_REQUEST = 1001;

    // 在类的开头添加权限请求码
    private static final int PERMISSION_REQUEST_CODE = 1000;

    // 在类的开头添加权限相关常量
    private static final String[] REQUIRED_PERMISSIONS_BELOW_33 = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final String[] REQUIRED_PERMISSIONS_33_AND_ABOVE = {
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // 在 Main 类中添加成员变量
    private com.example.arcgistest2.location.LocationManager locationManager;
    private static final int PICK_GPS_FILE = 1002;
    private static final int PICK_LEGEND_IMAGE = 1003;

    // 加成员变量
    private FeatureLayer locationLayer;
    private FeatureCollectionTable locationTable;

    // 添加成员变量
    private LegendManager legendManager;

    private UIManager uiManager;
    private PermissionManager permissionManager;
    private FileManager fileManager;

    // 添加成员变量
    private boolean isMapQueryEnabled = false;

    // 在类的开头添加成员变量
    private Map<Integer, View> expandableLayouts;

    // 管理器
    private LayerManager layerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializePermissionManager();
        super.onCreate(savedInstanceState);
        checkPermissions();
    }

    private void initializePermissionManager() {
        permissionManager = new PermissionManager(this, PERMISSION_REQUEST_CODE, 
            new PermissionManager.PermissionCallback() {
                @Override
                public void onPermissionsGranted() {
                    initializeApp();
                }

                @Override
                public void onPermissionsDenied() {
                    showSettingsDialog();
                }
            });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.index;
    }

    @Override
    protected int getMapViewId() {
        return R.id.mapView;
    }

    @Override
    protected void setupUI() {
        try {
            // 设置基本控件
            setupMapControls();
            
            // 设置图层列表
            setupLayerList();
            
            // 设置所有按钮的点击监听器
            setupButtons();
            
            // 设置地图触摸监听器
            setupMapTouchListener();
            
            Log.d(TAG, "UI setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI: " + e.getMessage());
        }
    }

    private void setupButtons() {
        // 地图控制按钮
        setButtonClickListener(R.id.magnify);
        setButtonClickListener(R.id.shrink);
        
        // 图层管理按钮
        setButtonClickListener(R.id.Load_layer);
        setButtonClickListener(R.id.tucengliebiao);
        setButtonClickListener(R.id.tuli);
        setButtonClickListener(R.id.btn_export);
        setButtonClickListener(R.id.btn_zoom_to_layer);
        
        // GPS相关按钮
        setButtonClickListener(R.id.GPS_1);
        setButtonClickListener(R.id.GPS_2);
        
        // 查询按钮
        setButtonClickListener(R.id.tuchashuxing);
        setButtonClickListener(R.id.shuxingchaxun);
        
        // 要素编辑按钮
        setButtonClickListener(R.id.yaosumoban);
        setButtonClickListener(R.id.yaosubianjigongju);
        
        // 图例按钮
        setButtonClickListener(R.id.btn_add_legend);
        
        // 功能区展开/折叠按钮
        setButtonClickListener(R.id.action1);
        setButtonClickListener(R.id.action2);
        setButtonClickListener(R.id.action3);
        setButtonClickListener(R.id.action4);
    }

    private void setButtonClickListener(int buttonId) {
        View button = findViewById(buttonId);
        if (button != null) {
            button.setOnClickListener(this::handleButtonClick);
            button.setClickable(true);
            button.setEnabled(true);
            Log.d(TAG, "Button listener set for: " + buttonId);
        } else {
            Log.e(TAG, "Button not found: " + buttonId);
        }
    }

    private void setupLayerList() {
        // 初始化图层列表适配器
        layerListAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_list_item_multiple_choice, 
            layerNames);
        
        ListView listView = findViewById(R.id.layerListView);
        if (listView != null) {
            listView.setAdapter(layerListAdapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            
            // 添加图层择监听器
            listView.setOnItemClickListener((parent, view, position, id) -> {
                if (position < featureLayers.size()) {
                    FeatureLayer layer = featureLayers.get(position);
                    layer.setVisible(listView.isItemChecked(position));
                }
            });
            Log.d(TAG, "Layer list setup completed");
        } else {
            Log.e(TAG, "Layer list view not found");
        }
    }

    @Override
    protected void initializeManagers() {
        try {
            if (map == null || mMapView == null) {
                Log.e(TAG, "Map or MapView is null");
                return;
            }

            if (map.getLoadStatus() != LoadStatus.LOADED) {
                Log.e(TAG, "Map is not loaded yet");
                return;
            }

            // 初始化所有管理器
            layerManager = new LayerManager(this, mMapView, map);
            Log.d(TAG, "LayerManager initialized successfully");

            featureEditor = new FeatureEditor(mMapView, this);
            Log.d(TAG, "FeatureEditor initialized");

            legendManager = new LegendManager(this);
            Log.d(TAG, "LegendManager initialized");

            uiManager = new UIManager(this, this::handleButtonClick);
            Log.d(TAG, "UIManager initialized");

            permissionManager = new PermissionManager(this, PERMISSION_REQUEST_CODE, 
                new PermissionManager.PermissionCallback() {
                    @Override
                    public void onPermissionsGranted() {
                        // 权限已授予，不需要再次初始化app
                        Log.d(TAG, "Permissions granted");
                    }

                    @Override
                    public void onPermissionsDenied() {
                        showSettingsDialog();
                    }
                });
            Log.d(TAG, "PermissionManager initialized");

            locationManager = new com.example.arcgistest2.location.LocationManager(this, mMapView);
            Log.d(TAG, "LocationManager initialized");

            Log.d(TAG, "All managers initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing managers: " + e.getMessage());
            Toast.makeText(this, "初始化管理器失败: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void handleButtonClick(View view) {
        if (view == null) return;
        
        Log.d(TAG, "Button clicked: " + view.getId());
        
        try {
            int id = view.getId();
            
            // 首先检查是否是展开/折叠按钮
            if (id == R.id.action1 || id == R.id.action2 || 
                id == R.id.action3 || id == R.id.action4) {
                View expandableLayout = expandableLayouts.get(id);
                if (expandableLayout != null) {
                    toggleExpandableLayout(expandableLayout);
                    Log.d(TAG, "Toggle expandable layout for button: " + id);
                }
                animateButton(view);
                return;
            }
            
            // 处理其他按钮点击事件
            if (id == R.id.magnify) {
                double scale = mMapView.getMapScale();
                mMapView.setViewpointScaleAsync(scale * 0.5);
                Log.d(TAG, "Magnify button clicked");
            } else if (id == R.id.shrink) {
                double scale = mMapView.getMapScale();
                mMapView.setViewpointScaleAsync(scale * 2.0);
                Log.d(TAG, "Shrink button clicked");
            } else if (id == R.id.Load_layer) {
                openFilePicker();
                Log.d(TAG, "Load layer button clicked");
            } else if (id == R.id.GPS_1) {
                startLocationTracking();
                Log.d(TAG, "GPS tracking button clicked");
            } else if (id == R.id.GPS_2) {
                openGPSFilePicker();
                Log.d(TAG, "GPS import button clicked");
            } else if (id == R.id.tucengliebiao) {
                handleLayerList();
                Log.d(TAG, "Layer list button clicked");
            } else if (id == R.id.tuli) {
                handleLegend();
                Log.d(TAG, "Legend button clicked");
            } else if (id == R.id.btn_export) {
                exportSelectedLayers();
                Log.d(TAG, "Export button clicked");
            } else if (id == R.id.tuchashuxing) {
                enableMapQuery();
                Log.d(TAG, "Map query button clicked");
            } else if (id == R.id.shuxingchaxun) {
                showAttributeQueryDialog();
                Log.d(TAG, "Attribute query button clicked");
            } else if (id == R.id.yaosumoban) {
                showFeatureTemplateMenu(view);
                Log.d(TAG, "Feature template button clicked");
            } else if (id == R.id.yaosubianjigongju) {
                showFeatureEditToolMenu(view);
                Log.d(TAG, "Feature edit tool button clicked");
            } else if (id == R.id.btn_add_legend) {
                openLegendImagePicker();
                Log.d(TAG, "Add legend button clicked");
            } else if (id == R.id.btn_zoom_to_layer) {
                zoomToSelectedLayers();
                Log.d(TAG, "Zoom to layer button clicked");
            } else {
                Log.w(TAG, "Unhandled button click: " + view.getId());
            }
            
            // 加按钮动画效果
            animateButton(view);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling button click: " + e.getMessage());
            Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 添加权限检查方法
    private void checkPermissions() {
        String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
            REQUIRED_PERMISSIONS_33_AND_ABOVE : REQUIRED_PERMISSIONS_BELOW_33;
        permissionManager.checkAndRequestPermissions(permissions);
    }

    private void initializeApp() {
        try {
            // 设置布局
            setContentView(R.layout.index);
            
            // 初始化地图视图
            mMapView = findViewById(R.id.mapView);
            if (mMapView == null) {
                throw new IllegalStateException("MapView not found in layout");
            }
            
            // 初始化地图
            map = new ArcGISMap(SpatialReferences.getWgs84());
            mMapView.setMap(map);
            
            // 等待地图加载完成
            map.addDoneLoadingListener(() -> {
                if (map.getLoadStatus() == LoadStatus.LOADED) {
                    Log.d(TAG, "Map loaded successfully");
                    // 地图加载完成后初始化管理器
                    initializeManagers();
                    // 初始化UI组件
                    setupUI();
                    // 设置导航抽屉
                    setupNavigationDrawer();
                } else {
                    Log.e(TAG, "Map failed to load: " + map.getLoadError().getMessage());
                }
            });
            map.loadAsync();
            
            Log.d(TAG, "App initialization started");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing app: " + e.getMessage());
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showSettingsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("权限被拒绝")
            .setMessage("请在设置中手动授予所需权限，否则应用将无法正常作。")
            .setPositiveButton("去设置", (dialogInterface, i) -> {
                dialogInterface.dismiss();
                // 开应用设置页面
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("退出", (dialogInterface, i) -> {
                dialogInterface.dismiss();
                finish();
            })
            .setCancelable(false)
            .create();
        
        dialog.show();
    }

    private void animateButton(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f, 1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
    }

    private void setupMapControls() {
        MaterialButton magnifyButton = findViewById(R.id.magnify);
        MaterialButton shrinkButton = findViewById(R.id.shrink);

        magnifyButton.setOnClickListener(v -> {
            animateButton(v);
            double scale = mMapView.getMapScale();
            mMapView.setViewpointScaleAsync(scale * 0.5);
        });

        shrinkButton.setOnClickListener(v -> {
            animateButton(v);
            double scale = mMapView.getMapScale();
            mMapView.setViewpointScaleAsync(scale * 2.0);
        });
    }

    private void setupNavigationDrawer() {
        // 移除这些按钮的click listener设置，因为它们已经在setupButtons中设置过了
        View expandableLayout1 = findViewById(R.id.expandable_layout_1);
        View expandableLayout2 = findViewById(R.id.expandable_layout_2);
        View expandableLayout3 = findViewById(R.id.expandable_layout_3);
        View expandableLayout4 = findViewById(R.id.expandable_layout_4);

        // 存储布局引用，供后续使用
        expandableLayouts = new HashMap<>();
        expandableLayouts.put(R.id.action1, expandableLayout1);
        expandableLayouts.put(R.id.action2, expandableLayout2);
        expandableLayouts.put(R.id.action3, expandableLayout3);
        expandableLayouts.put(R.id.action4, expandableLayout4);
    }

    private void toggleExpandableLayout(View layout) {
        if (layout == null) return;
        
        // 关闭其他展开的布局
        for (View otherLayout : expandableLayouts.values()) {
            if (otherLayout != layout && otherLayout.getVisibility() == View.VISIBLE) {
                collapseView(otherLayout);
            }
        }
        
        // 切换当前布局的可见性
        if (layout.getVisibility() == View.VISIBLE) {
            collapseView(layout);
        } else {
            expandView(layout);
        }
    }

    private void collapseView(View view) {
        if (view == null) return;
        
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    private void expandView(View view) {
        if (view == null) return;
        
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    // ... 必要的回调方法

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 允许选择多个文件
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivityForResult(
                Intent.createChooser(intent, "选择Shapefile相关文件(.shp, .shx, .dbf, .prj)"),
                PICK_SHAPEFILE_REQUEST
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case PICK_SHAPEFILE_REQUEST:
                    handleMultipleFileSelection(data);
                    break;
                case PICK_GPS_FILE:
                    handleGPSFile(data.getData());
                    break;
                case PICK_LEGEND_IMAGE:
                    handleLegendImage(data.getData());
                    break;
                case 2296:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            initializeApp();
                        } else {
                            showSettingsDialog();
                        }
                    }
                    break;
            }
        }
    }

    private void handleMultipleFileSelection(Intent data) {
        try {
            // 检查LayerManager是否已初始化
            if (layerManager == null) {
                Log.e(TAG, "LayerManager is not initialized");
                Toast.makeText(this, "LayerManager未初始化，请稍后重试", Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查地图状态
            if (map == null || map.getLoadStatus() != LoadStatus.LOADED) {
                Log.e(TAG, "Map is not ready");
                Toast.makeText(this, "地图未准备就绪，请稍后重试", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建临时目录
            File tempDir = new File(getExternalFilesDir(null), "shapefiles");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            String shpFileName = null;
            Map<String, Uri> selectedFiles = new HashMap<>();

            // 处理多个文件
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    String fileName = getFileName(uri);
                    String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
                    selectedFiles.put(ext, uri);
                    
                    if (ext.equals(".shp")) {
                        shpFileName = fileName.substring(0, fileName.lastIndexOf("."));
                    }
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                String fileName = getFileName(uri);
                String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
                selectedFiles.put(ext, uri);
                
                if (ext.equals(".shp")) {
                    shpFileName = fileName.substring(0, fileName.lastIndexOf("."));
                }
            }

            // 检查是否选择了.shp文件
            if (shpFileName == null) {
                Toast.makeText(this, "请选择.shp文件", Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查必需的文件是否都已选择
            String[] requiredExts = {".shp", ".shx", ".dbf", ".prj"};
            for (String ext : requiredExts) {
                if (!selectedFiles.containsKey(ext)) {
                    Toast.makeText(this, "缺少必需的文件: " + ext, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 复制所有选中的文件
            for (Map.Entry<String, Uri> entry : selectedFiles.entrySet()) {
                File destFile = new File(tempDir, shpFileName + entry.getKey());
                copyUriToFile(entry.getValue(), destFile);
            }

            // 加载图层
            String shpFilePath = new File(tempDir, shpFileName + ".shp").getAbsolutePath();
            layerManager.loadShapefileLayer(shpFilePath, new LayerManager.LayerLoadCallback() {
                @Override
                public void onSuccess(FeatureLayer layer) {
                    featureLayers.add(layer);
                    layerNames.add(layer.getName());
                    runOnUiThread(() -> {
                        layerListAdapter.notifyDataSetChanged();
                        ListView listView = findViewById(R.id.layerListView);
                        listView.setItemChecked(layerNames.size() - 1, true);
                        layerManager.zoomToLayer(layer);
                        Toast.makeText(Main.this, "图层加载成功", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Layer load error: " + error);
                    runOnUiThread(() -> 
                        Toast.makeText(Main.this, "加载图层失败: " + error, 
                            Toast.LENGTH_SHORT).show()
                    );
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "处理文件失败: " + e.getMessage());
            Toast.makeText(this, "处理文件失败: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void copyUriToFile(Uri uri, File destFile) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(destFile)) {
            if (inputStream == null) {
                throw new IOException("Cannot open input stream");
            }
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
        }
    }

    // 添加文件复制方法
    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile))) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }
            bos.flush();
        }
    }

    // 添加检查Shapefile相关文件的方法
    private boolean checkShapefileFiles(String basePath) {
        String[] extensions = {".shp", ".shx", ".dbf", ".prj"};
        for (String ext : extensions) {
            File file = new File(basePath + ext);
            if (!file.exists()) {
                Log.e(TAG, "Missing file: " + basePath + ext);
                return false;
            }
        }
        return true;
    }

    // 改进获取真实文件路径的方法
    private String getRealPathFromUri(Uri uri) {
        try {
            Log.d(TAG, "Getting real path for URI: " + uri.toString());
            String path = null;

            // 处理 "content://" URI
            if ("content".equals(uri.getScheme())) {
                String[] projection = {MediaStore.MediaColumns.DATA};
                try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        path = cursor.getString(columnIndex);
                        Log.d(TAG, "Path from content resolver: " + path);
                    }
                }
            }
            
            // 处理 "file://" URI
            if (path == null && "file".equals(uri.getScheme())) {
                path = uri.getPath();
                Log.d(TAG, "Path from file URI: " + path);
            }

            // 如果还是获取不到路径，尝试使用DocumentFile
            if (path == null) {
                DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
                if (documentFile != null) {
                    // 获取文件的显示名称
                    String fileName = documentFile.getName();
                    // 创建临时目录
                    File tempDir = new File(getFilesDir(), "temp_shapefiles");
                    if (!tempDir.exists()) {
                        tempDir.mkdirs();
                    }
                    // 创建临时文件
                    File tempFile = new File(tempDir, fileName);
                    // 复制文件内容
                    try (InputStream is = getContentResolver().openInputStream(uri);
                         FileOutputStream fos = new FileOutputStream(tempFile)) {
                        if (is == null) throw new IOException("Cannot open input stream");
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                    path = tempFile.getAbsolutePath();
                    Log.d(TAG, "Path from document file: " + path);
                }
            }

            if (path != null) {
                File file = new File(path);
                if (!file.exists()) {
                    Log.e(TAG, "File does not exist: " + path);
                    return null;
                }
                Log.d(TAG, "File exists at path: " + path);
            } else {
                Log.e(TAG, "Could not resolve path for URI");
            }

            return path;
        } catch (Exception e) {
            Log.e(TAG, "Error getting real path: " + e.getMessage(), e);
            return null;
        }
    }

    // 修改 handleGPSFile 方法
    private void handleGPSFile(Uri uri) {
        try {
            String filePath = getRealPathFromUri(uri);
            if (filePath != null && filePath.toLowerCase().endsWith(".shp")) {
                layerManager.loadShapefileLayer(filePath, new LayerManager.LayerLoadCallback() {
                    @Override
                    public void onSuccess(FeatureLayer layer) {
                        // 设置GPS点的样式
                        SimpleMarkerSymbol gpsSymbol = new SimpleMarkerSymbol(
                            SimpleMarkerSymbol.Style.CIRCLE, 
                            Color.RED, 
                            8);
                        gpsSymbol.setOutline(new SimpleLineSymbol(
                            SimpleLineSymbol.Style.SOLID,
                            Color.WHITE,
                            1));
                        layer.setRenderer(new SimpleRenderer(gpsSymbol));

                        // 添加到图层列表
                        featureLayers.add(layer);
                        layerNames.add("GPS点: " + layer.getName());
                        runOnUiThread(() -> {
                            layerListAdapter.notifyDataSetChanged();
                            ListView listView = findViewById(R.id.layerListView);
                            listView.setItemChecked(layerNames.size() - 1, true);
                            layerManager.zoomToLayer(layer);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> 
                            Toast.makeText(Main.this, "加载GPS数据失败: " + error, 
                                Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            } else {
                Toast.makeText(this, "请选择.shp文件", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "处理GPS数据失败: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    // 添加处理图例图的方法
    private void handleLegendImage(Uri uri) {
        try {
            // 保存图例图片
            File legendFile = legendManager.saveLegendImage(uri);
            
            // 显示图例
            ImageView legendImage = findViewById(R.id.legend_image);
            if (legendImage != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(legendFile.getAbsolutePath());
                legendImage.setImageBitmap(bitmap);
                legendImage.setVisibility(View.VISIBLE);
            }
            
            Toast.makeText(this, "图例添加成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "图例添加失败: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    // 添加位置追踪方法
    private void startLocationTracking() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != 
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                PERMISSION_REQUEST_CODE);
            return;
        }

        // 确保 locationManager 已经初始化
        if (locationManager == null) {
            locationManager = new com.example.arcgistest2.location.LocationManager(this, mapView);
        }

        // 创建位置回调
        com.example.arcgistest2.location.LocationManager.LocationCallback callback = 
            new com.example.arcgistest2.location.LocationManager.LocationCallback() {
                @Override
                public void onLocationReceived(Point point) {
                    runOnUiThread(() -> {
                        // 创建位置层（如果不存在）
                        if (locationLayer == null) {
                            createLocationLayer();
                        }

                        // 添加位置点
                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("TITLE", "当前位置");
                        Feature feature = locationTable.createFeature(attributes, point);
                        locationTable.addFeatureAsync(feature);

                        // 缩放到当前位置
                        mMapView.setViewpointCenterAsync(point, 50000);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> 
                        Toast.makeText(Main.this, error, Toast.LENGTH_SHORT).show()
                    );
                }
            };

        // 启动位置更新
        locationManager.startLocationUpdates(callback);
    }

    // 添加创建位置图层的方法
    private void createLocationLayer() {
        try {
            // 创建字段列表
            List<Field> fields = new ArrayList<>();
            fields.add(Field.createString("TITLE", "标题", 50));
            
            // 创建要素表
            locationTable = new FeatureCollectionTable(fields, 
                GeometryType.POINT, 
                SpatialReferences.getWgs84());
            
            // 创建要素图层
            locationLayer = new FeatureLayer(locationTable);
            
            // 设置号
            SimpleMarkerSymbol locationSymbol = new SimpleMarkerSymbol(
                SimpleMarkerSymbol.Style.CIRCLE, 
                Color.RED, 
                12);
            locationSymbol.setOutline(new SimpleLineSymbol(
                SimpleLineSymbol.Style.SOLID,
                Color.WHITE,
                2));
            locationLayer.setRenderer(new SimpleRenderer(locationSymbol));
            
            // 添加图层到地图
            map.getOperationalLayers().add(locationLayer);
            
        } catch (Exception e) {
            Log.e(TAG, "创建位置图层失败: " + e.getMessage());
            Toast.makeText(this, "创建位置图层失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 加缺失的方法
    private void openGPSFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                Intent.createChooser(intent, "选择GPS数据文件"),
                PICK_GPS_FILE
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "请安装件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLayerList() {
        View layerListContainer = findViewById(R.id.layer_list_container);
        View legendContainer = findViewById(R.id.legend_container);
        if (layerListContainer.getVisibility() == View.VISIBLE) {
            layerListContainer.setVisibility(View.GONE);
        } else {
            legendContainer.setVisibility(View.GONE);
            layerListContainer.setVisibility(View.VISIBLE);
            updateLayerList();
        }
    }

    private void handleLegend() {
        View legendContainer = findViewById(R.id.legend_container);
        View layerListContainer = findViewById(R.id.layer_list_container);
        if (legendContainer.getVisibility() == View.VISIBLE) {
            legendContainer.setVisibility(View.GONE);
        } else {
            layerListContainer.setVisibility(View.GONE);
            legendContainer.setVisibility(View.VISIBLE);
        }
    }

    private void exportSelectedLayers() {
        try {
            // 创建导出目录
            File exportDir = new File(getExternalFilesDir(null), "exported_layers");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // 导出每个选中图层
            ListView listView = findViewById(R.id.layerListView);
            boolean hasExported = false;
            for (int i = 0; i < featureLayers.size(); i++) {
                if (listView.isItemChecked(i)) {
                    FeatureLayer layer = featureLayers.get(i);
                    String fileName = layer.getName() + ".shp";
                    File outputFile = new File(exportDir, fileName);
                    layerManager.exportLayerToShapefile(layer, outputFile.getAbsolutePath());
                    hasExported = true;
                }
            }

            if (hasExported) {
                Toast.makeText(this, "图层导出成功：" + exportDir.getAbsolutePath(), 
                    Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "请先选择要导出的图层", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "导出图层失败: " + e.getMessage());
            Toast.makeText(this, "导图层失败: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void enableMapQuery() {
        // 启用地图查询模式
        isMapQueryEnabled = true;
        Toast.makeText(this, "请点击地图要素进行查询", Toast.LENGTH_SHORT).show();
    }

    private void showAttributeQueryDialog() {
        View attributeTableContainer = findViewById(R.id.attribute_table_container);
        if (attributeTableContainer.getVisibility() == View.VISIBLE) {
            attributeTableContainer.setVisibility(View.GONE);
        } else {
            showAttributeTable();
            attributeTableContainer.setVisibility(View.VISIBLE);
        }
    }

    private void showAttributeTable() {
        TableLayout attributeTable = findViewById(R.id.attribute_table);
        attributeTable.removeAllViews();

        // 获取选中的图层
        ListView layerListView = findViewById(R.id.layerListView);
        final FeatureLayer selectedLayer = getSelectedLayer(layerListView);

        if (selectedLayer == null) {
            Toast.makeText(this, "请先选择一个图层", Toast.LENGTH_SHORT).show();
            return;
        }

        // 查询图层的所有要素
        QueryParameters query = new QueryParameters();
        query.setWhereClause("1=1");
        
        ListenableFuture<FeatureQueryResult> future = selectedLayer.getFeatureTable().queryFeaturesAsync(query);
        future.addDoneListener(() -> {
            try {
                FeatureQueryResult result = future.get();
                runOnUiThread(() -> {
                    try {
                        // 创建表头
                        TableRow headerRow = new TableRow(this);
                        for (Field field : result.getFields()) {
                            TextView headerText = new TextView(this);
                            headerText.setText(field.getName());
                            headerText.setPadding(10, 10, 10, 10);
                            headerText.setBackgroundColor(Color.LTGRAY);
                            headerRow.addView(headerText);
                        }
                        attributeTable.addView(headerRow);

                        // 添加数据行
                        for (Feature feature : result) {
                            TableRow row = new TableRow(this);
                            row.setClickable(true);
                            Map<String, Object> attributes = feature.getAttributes();
                            
                            for (Field field : result.getFields()) {
                                TextView cellText = new TextView(this);
                                cellText.setText(String.valueOf(attributes.get(field.getName())));
                                cellText.setPadding(10, 10, 10, 10);
                                row.addView(cellText);
                            }

                            // 设置行点击事件
                            row.setOnClickListener(v -> {
                                selectedLayer.clearSelection();
                                selectedLayer.selectFeature(feature);
                                
                                // 缩放到要素
                                if (feature.getGeometry() != null) {
                                    Envelope extent = feature.getGeometry().getExtent();
                                    Envelope bufferedExtent = new Envelope(
                                        extent.getXMin() - extent.getWidth() * 0.1,
                                        extent.getYMin() - extent.getHeight() * 0.1,
                                        extent.getXMax() + extent.getWidth() * 0.1,
                                        extent.getYMax() + extent.getHeight() * 0.1,
                                        extent.getSpatialReference()
                                    );
                                    mMapView.setViewpointGeometryAsync(bufferedExtent, 1.0);
                                }
                            });

                            attributeTable.addView(row);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "创属性表失败: " + e.getMessage());
                        Toast.makeText(this, "建属性表失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "查询要素失败: " + e.getMessage());
                runOnUiThread(() -> 
                    Toast.makeText(this, "查询要素失败", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private FeatureLayer getSelectedLayer(ListView layerListView) {
        for (int i = 0; i < featureLayers.size(); i++) {
            if (layerListView.isItemChecked(i)) {
                return featureLayers.get(i);
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            mapView.dispose();
        }
        if (layerManager != null) {
            layerManager.dispose();
        }
        if (locationManager != null) {
            locationManager.stopLocationUpdates();
        }
        super.onDestroy();
    }

    // 添加素模板菜单显示方法
    private void showFeatureTemplateMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("点要素");
        popup.getMenu().add("线要素");
        popup.getMenu().add("面要素");
        
        popup.setOnMenuItemClickListener(item -> {
            GeometryType geometryType;
            switch (item.getTitle().toString()) {
                case "点要素":
                    geometryType = GeometryType.POINT;
                    break;
                case "线要素":
                    geometryType = GeometryType.POLYLINE;
                    break;
                case "面要素":
                    geometryType = GeometryType.POLYGON;
                    break;
                default:
                    return false;
            }
            featureEditor.setCurrentGeometryType(geometryType);
            Toast.makeText(this, "已选择" + item.getTitle() + "模板\n请点击要素编辑工具开始绘", 
                Toast.LENGTH_LONG).show();
            Log.d(TAG, "Selected geometry type: " + geometryType);
            return true;
        });
        
        popup.show();
    }

    // 添加要素编辑工具菜单显示方法
    private void showFeatureEditToolMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("开始绘制");
        popup.getMenu().add("完成绘制");
        popup.getMenu().add("取消绘制");
        popup.getMenu().add("保存");
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getTitle().toString()) {
                case "开绘制":
                    if (featureEditor.getCurrentGeometryType() != null) {
                        isDrawing = true;
                        featureEditor.startEditing();
                        Toast.makeText(this, "开始绘制，点击地图添加节点\n" +
                            (featureEditor.getCurrentGeometryType() != GeometryType.POINT ? 
                            "双击完成绘制，长按取消" : ""), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "请先选择要素类型", Toast.LENGTH_SHORT).show();
                    }
                    break;
                
                case "完成绘制":
                    if (isDrawing) {
                        featureEditor.completeDrawing();
                        isDrawing = false;
                        Toast.makeText(this, "制完成", Toast.LENGTH_SHORT).show();
                    }
                    break;
                
                case "取消绘制":
                    if (isDrawing) {
                        featureEditor.cancelDrawing();
                        isDrawing = false;
                        Toast.makeText(this, "已取消绘制", Toast.LENGTH_SHORT).show();
                    }
                    break;
                
                case "保存":
                    String fileName = "编辑要素_" + System.currentTimeMillis();
                    featureEditor.saveAsShapefile(fileName);
                    Toast.makeText(this, "已保存为: " + fileName + ".shp", 
                        Toast.LENGTH_SHORT).show();
                    break;
            }
            return true;
        });
        
        popup.show();
    }

    // 添加图例图片选择器方法
    private void openLegendImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, 
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
            startActivityForResult(intent, PICK_LEGEND_IMAGE);
        } catch (android.content.ActivityNotFoundException ex) {
            // 如果没有图片选择器，尝试使用文件择器
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            try {
                startActivityForResult(
                    Intent.createChooser(intent, "选择图例图片"),
                    PICK_LEGEND_IMAGE
                );
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, "没有可用的图片选择器", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 添加地图点击事件处理
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isDrawing) {
                // 如果正在绘制，添加点
                android.graphics.Point screenPoint = new android.graphics.Point(
                    Math.round(event.getX()),
                    Math.round(event.getY()));
                Point mapPoint = mMapView.screenToLocation(screenPoint);
                featureEditor.addPoint(mapPoint);
                return true;
            } else if (isMapQueryEnabled) {
                // 如果正在进行地图查询，执行查询
                android.graphics.Point screenPoint = new android.graphics.Point(
                    Math.round(event.getX()),
                    Math.round(event.getY()));
                identifyFeature(screenPoint);
                isMapQueryEnabled = false; // 查询完成后禁用查询模式
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    // 添加要素识别方法
    private void identifyFeature(android.graphics.Point screenPoint) {
        // 获取当前可见的层
        List<FeatureLayer> visibleLayers = new ArrayList<>();
        for (FeatureLayer layer : featureLayers) {
            if (layer.isVisible()) {
                visibleLayers.add(layer);
            }
        }
        
        if (visibleLayers.isEmpty()) {
            Toast.makeText(this, "没有可查询的层", Toast.LENGTH_SHORT).show();
            return;
        }

        // 设置查询容差
        double tolerance = 10;
        double mapTolerance = tolerance * mMapView.getUnitsPerDensityIndependentPixel();
        
        // 遍历可见图层进行查询
        for (FeatureLayer layer : visibleLayers) {
            Point mapPoint = mMapView.screenToLocation(screenPoint);
            Envelope envelope = new Envelope(
                mapPoint.getX() - mapTolerance,
                mapPoint.getY() - mapTolerance,
                mapPoint.getX() + mapTolerance,
                mapPoint.getY() + mapTolerance,
                mapPoint.getSpatialReference()
            );

            QueryParameters query = new QueryParameters();
            query.setGeometry(envelope);
            query.setSpatialRelationship(QueryParameters.SpatialRelationship.INTERSECTS);

            final ListenableFuture<FeatureQueryResult> future = layer.selectFeaturesAsync(query, 
                FeatureLayer.SelectionMode.NEW);
            
            future.addDoneListener(() -> {
                try {
                    FeatureQueryResult result = future.get();
                    if (result.iterator().hasNext()) {
                        // 显示属性信息
                        Feature feature = result.iterator().next();
                        showFeatureAttributes(feature, layer);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "查询要素失败: " + e.getMessage());
                }
            });
        }
    }

    // 添加显示要素属性的方法
    private void showFeatureAttributes(Feature feature, FeatureLayer layer) {
        runOnUiThread(() -> {
            try {
                // 清空并显示属性表容器
                TableLayout attributeTable = findViewById(R.id.attribute_table);
                View attributeTableContainer = findViewById(R.id.attribute_table_container);
                attributeTableContainer.setVisibility(View.VISIBLE);
                attributeTable.removeAllViews();

                // 创建表头
                TableRow headerRow = new TableRow(this);
                headerRow.setBackgroundColor(Color.LTGRAY);
                Map<String, Object> attributes = feature.getAttributes();
                
                for (Field field : layer.getFeatureTable().getFields()) {
                    TextView headerText = new TextView(this);
                    headerText.setText(field.getAlias()); // 使用字段别名
                    headerText.setPadding(10, 10, 10, 10);
                    headerText.setTextColor(Color.BLACK);
                    headerRow.addView(headerText);
                }
                attributeTable.addView(headerRow);

                // 创建数据行
                TableRow dataRow = new TableRow(this);
                dataRow.setBackgroundColor(Color.WHITE);
                for (Field field : layer.getFeatureTable().getFields()) {
                    TextView cellText = new TextView(this);
                    Object value = attributes.get(field.getName());
                    cellText.setText(value != null ? value.toString() : "");
                    cellText.setPadding(10, 10, 10, 10);
                    cellText.setTextColor(Color.BLACK);
                    dataRow.addView(cellText);
                }
                attributeTable.addView(dataRow);

                // 缩放到选中要素
                if (feature.getGeometry() != null) {
                    Envelope extent = feature.getGeometry().getExtent();
                    Envelope bufferedExtent = new Envelope(
                        extent.getXMin() - extent.getWidth() * 0.1,
                        extent.getYMin() - extent.getHeight() * 0.1,
                        extent.getXMax() + extent.getWidth() * 0.1,
                        extent.getYMax() + extent.getHeight() * 0.1,
                        extent.getSpatialReference()
                    );
                    mMapView.setViewpointGeometryAsync(bufferedExtent, 1.0);
                }

                // 确保属性表可见
                View expandableLayout2 = findViewById(R.id.expandable_layout_2);
                expandableLayout2.setVisibility(View.VISIBLE);
                attributeTableContainer.setVisibility(View.VISIBLE);

            } catch (Exception e) {
                Log.e(TAG, "显示要素属性失败: " + e.getMessage());
                Toast.makeText(this, "显示素属性失败: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void zoomToSelectedLayers() {
        ListView listView = findViewById(R.id.layerListView);
        List<FeatureLayer> selectedLayers = new ArrayList<>();
        
        for (int i = 0; i < featureLayers.size(); i++) {
            if (listView.isItemChecked(i)) {
                selectedLayers.add(featureLayers.get(i));
            }
        }

        if (selectedLayers.isEmpty()) {
            Toast.makeText(this, "请先选择要缩放的图层", Toast.LENGTH_SHORT).show();
            return;
        }

        // 计算所有选中图层总范围
        Envelope totalExtent = null;
        for (FeatureLayer layer : selectedLayers) {
            if (layer.getFullExtent() != null) {
                if (totalExtent == null) {
                    totalExtent = layer.getFullExtent();
                } else {
                    totalExtent = GeometryEngine.union(totalExtent, 
                        layer.getFullExtent()).getExtent();
                }
            }
        }

        if (totalExtent != null) {
            // 加缓冲区
            Envelope bufferedExtent = new Envelope(
                totalExtent.getXMin() - totalExtent.getWidth() * 0.1,
                totalExtent.getYMin() - totalExtent.getHeight() * 0.1,
                totalExtent.getXMax() + totalExtent.getWidth() * 0.1,
                totalExtent.getYMax() + totalExtent.getHeight() * 0.1,
                totalExtent.getSpatialReference()
            );
            mMapView.setViewpointGeometryAsync(bufferedExtent, 1.0);
        }
    }

    private void updateLayerList() {
        try {
            ListView listView = findViewById(R.id.layerListView);
            if (listView == null) {
                Log.e(TAG, "Layer list view not found");
                return;
            }

            // 清并重新设置适配器
            layerNames.clear();
            for (FeatureLayer layer : featureLayers) {
                layerNames.add(layer.getName());
            }
            
            // 如果适配器为空，创建新的适配器
            if (layerListAdapter == null) {
                layerListAdapter = new ArrayAdapter<>(this, 
                    android.R.layout.simple_list_item_multiple_choice, 
                    layerNames);
                listView.setAdapter(layerListAdapter);
            } else {
                layerListAdapter.notifyDataSetChanged();
            }

            // 更新选中状态
            for (int i = 0; i < featureLayers.size(); i++) {
                FeatureLayer layer = featureLayers.get(i);
                listView.setItemChecked(i, layer.isVisible());
            }

            Log.d(TAG, "Layer list updated with " + layerNames.size() + " layers");
        } catch (Exception e) {
            Log.e(TAG, "Error updating layer list: " + e.getMessage());
            Toast.makeText(this, "更新图层列表失败: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void setupMapTouchListener() {
        if (mMapView == null) {
            Log.e(TAG, "MapView is null");
            return;
        }
        
        // 创建自定义的摸监听器
        DefaultMapViewOnTouchListener mapTouchListener = new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.d(TAG, "Single tap up detected");
                // 取点击位置的屏幕坐标
                android.graphics.Point screenPoint = new android.graphics.Point(
                    Math.round(e.getX()),
                    Math.round(e.getY()));
                
                // 将屏幕坐标转换为地图坐标
                Point mapPoint = mMapView.screenToLocation(screenPoint);
                
                if (mapPoint != null) {
                    Log.d(TAG, "Map point: " + mapPoint.getX() + ", " + mapPoint.getY());
                    if (isDrawing) {
                        Log.d(TAG, "Drawing mode active, handling click");
                        // 如果正在绘制，添点
                        handleDrawingClick(mapPoint);
                        return true;
                    } else if (isMapQueryEnabled) {
                        Log.d(TAG, "Query mode active, handling click");
                        // 如果正在进行地图查询
                        identifyFeature(screenPoint);
                        isMapQueryEnabled = false;
                        return true;
                    }
                }
                return super.onSingleTapUp(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.d(TAG, "Double tap detected");
                if (isDrawing && featureEditor.getCurrentGeometryType() != GeometryType.POINT) {
                    // 双击完成绘制对于线和面要素）
                    featureEditor.completeDrawing();
                    isDrawing = false;
                    Toast.makeText(Main.this, "绘制完成", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return super.onDoubleTap(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Log.d(TAG, "Long press detected");
                if (isDrawing && featureEditor.getCurrentGeometryType() != GeometryType.POINT) {
                    // 长按取消绘制
                    featureEditor.cancelDrawing();
                    isDrawing = false;
                    Toast.makeText(Main.this, "已取消绘制", Toast.LENGTH_SHORT).show();
                }
                super.onLongPress(e);
            }
        };

        // 设置触摸监听器
        mMapView.setOnTouchListener(mapTouchListener);
        Log.d(TAG, "Map touch listener set up");
    }

    private void handleDrawingClick(Point mapPoint) {
        try {
            if (featureEditor == null) {
                Log.e(TAG, "FeatureEditor is null");
                return;
            }

            GeometryType currentType = featureEditor.getCurrentGeometryType();
            if (currentType == null) {
                Log.e(TAG, "No geometry type selected");
                Toast.makeText(this, "请先选择要素类型", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Handling drawing click for geometry type: " + currentType);
            
            switch (currentType) {
                case POINT:
                    // 对于点要素，直接添加并完成绘制
                    featureEditor.addPoint(mapPoint);
                    featureEditor.completeDrawing();
                    isDrawing = false;
                    Toast.makeText(this, "点要素添加成功", Toast.LENGTH_SHORT).show();
                    break;
                    
                case POLYLINE:
                case POLYGON:
                    // 对于线和面要素，添加点到当前图形
                    featureEditor.addPoint(mapPoint);
                    Toast.makeText(this, "节点已添加，双击完成绘", Toast.LENGTH_SHORT).show();
                    break;
                    
                default:
                    Log.w(TAG, "Unsupported geometry type: " + currentType);
                    Toast.makeText(this, "不支持的几何类型", Toast.LENGTH_SHORT).show();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling drawing click: " + e.getMessage());
            Toast.makeText(this, "绘制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isDrawing = false;
        }
    }

    // 添加获取文件名方法
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "unknown";
    }

    private void copyFileFromUri(Uri sourceUri, File destFile) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destFile)) {
            if (inputStream == null) {
                throw new IOException("无法读取文件");
            }
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    private Uri findRelatedFile(Uri originalUri, String targetFileName) {
        try {
            String originalPath = getRealPathFromUri(originalUri);
            if (originalPath != null) {
                File originalFile = new File(originalPath);
                File parentDir = originalFile.getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    File targetFile = new File(parentDir, targetFileName);
                    if (targetFile.exists()) {
                        return Uri.fromFile(targetFile);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "查找相关文件失败: " + e.getMessage());
        }
        return null;
    }

    // 添加复制相关文件的方法
    private boolean copyRelatedShapefiles(Uri originalUri, String basePath) {
        try {
            String originalFileName = getFileName(originalUri);
            String baseFileName = originalFileName.substring(0, originalFileName.length() - 4);
            String[] extensions = {".shp", ".shx", ".dbf", ".prj"};
            
            File tempDir = new File(getFilesDir(), "temp_shapefiles");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            for (String ext : extensions) {
                Uri relatedUri = findRelatedFile(originalUri, baseFileName + ext);
                if (relatedUri != null) {
                    File destFile = new File(tempDir, baseFileName + ext);
                    copyFileFromUri(relatedUri, destFile);
                } else {
                    Log.e(TAG, "Missing related file: " + baseFileName + ext);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error copying related files: " + e.getMessage());
            return false;
        }
    }
}

