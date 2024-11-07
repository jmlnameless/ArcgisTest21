package com.example.arcgistest2.ui;

import android.app.Activity;
import android.view.View;
import android.widget.ListView;
import com.google.android.material.button.MaterialButton;
import com.example.arcgistest2.R;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;
import com.google.android.material.textfield.TextInputEditText;

public class UIManager {
    private final Activity activity;
    private final View.OnClickListener buttonClickListener;

    public UIManager(Activity activity, View.OnClickListener buttonClickListener) {
        this.activity = activity;
        this.buttonClickListener = buttonClickListener;
    }

    public void setupUI() {
        setupMapControls();
        setupNavigationDrawer();
        setupLayerManagement();
        setupAttributeQuery();
        setupFeatureEdit();
        setupGPSData();
    }

    private void setupMapControls() {
        MaterialButton magnifyButton = activity.findViewById(R.id.magnify);
        MaterialButton shrinkButton = activity.findViewById(R.id.shrink);
        
        magnifyButton.setOnClickListener(buttonClickListener);
        shrinkButton.setOnClickListener(buttonClickListener);
    }

    public void setupNavigationDrawer() {
        MaterialButton action1 = activity.findViewById(R.id.action1);
        MaterialButton action2 = activity.findViewById(R.id.action2);
        MaterialButton action3 = activity.findViewById(R.id.action3);
        MaterialButton action4 = activity.findViewById(R.id.action4);

        View expandableLayout1 = activity.findViewById(R.id.expandable_layout_1);
        View expandableLayout2 = activity.findViewById(R.id.expandable_layout_2);
        View expandableLayout3 = activity.findViewById(R.id.expandable_layout_3);
        View expandableLayout4 = activity.findViewById(R.id.expandable_layout_4);

        action1.setOnClickListener(v -> {
            buttonClickListener.onClick(v);
            if (expandableLayout1.getVisibility() == View.VISIBLE) {
                expandableLayout1.setVisibility(View.GONE);
            } else {
                expandableLayout1.setVisibility(View.VISIBLE);
                expandableLayout2.setVisibility(View.GONE);
                expandableLayout3.setVisibility(View.GONE);
                expandableLayout4.setVisibility(View.GONE);
            }
        });

        action2.setOnClickListener(v -> {
            buttonClickListener.onClick(v);
            if (expandableLayout2.getVisibility() == View.VISIBLE) {
                expandableLayout2.setVisibility(View.GONE);
            } else {
                expandableLayout1.setVisibility(View.GONE);
                expandableLayout2.setVisibility(View.VISIBLE);
                expandableLayout3.setVisibility(View.GONE);
                expandableLayout4.setVisibility(View.GONE);
            }
        });

        action3.setOnClickListener(v -> {
            buttonClickListener.onClick(v);
            if (expandableLayout3.getVisibility() == View.VISIBLE) {
                expandableLayout3.setVisibility(View.GONE);
            } else {
                expandableLayout1.setVisibility(View.GONE);
                expandableLayout2.setVisibility(View.GONE);
                expandableLayout3.setVisibility(View.VISIBLE);
                expandableLayout4.setVisibility(View.GONE);
            }
        });

        action4.setOnClickListener(v -> {
            buttonClickListener.onClick(v);
            if (expandableLayout4.getVisibility() == View.VISIBLE) {
                expandableLayout4.setVisibility(View.GONE);
            } else {
                expandableLayout1.setVisibility(View.GONE);
                expandableLayout2.setVisibility(View.GONE);
                expandableLayout3.setVisibility(View.GONE);
                expandableLayout4.setVisibility(View.VISIBLE);
            }
        });
    }

    public void setupLayerManagement() {
        MaterialButton layerListButton = activity.findViewById(R.id.tucengliebiao);
        MaterialButton legendButton = activity.findViewById(R.id.tuli);
        View layerListContainer = activity.findViewById(R.id.layer_list_container);
        View legendContainer = activity.findViewById(R.id.legend_container);
        View expandableLayout1 = activity.findViewById(R.id.expandable_layout_1);

        // 设置图层列表按钮点击事件
        layerListButton.setOnClickListener(v -> {
            buttonClickListener.onClick(v);
            // 确保父容器可见
            expandableLayout1.setVisibility(View.VISIBLE);
            // 切换图层列表容器的可见性
            if (layerListContainer.getVisibility() == View.VISIBLE) {
                layerListContainer.setVisibility(View.GONE);
            } else {
                legendContainer.setVisibility(View.GONE);
                layerListContainer.setVisibility(View.VISIBLE);
            }
        });

        // 设置图例按钮点击事件
        legendButton.setOnClickListener(v -> {
            buttonClickListener.onClick(v);
            // 确保父容器可见
            expandableLayout1.setVisibility(View.VISIBLE);
            // 切换图例容器的可见性
            if (legendContainer.getVisibility() == View.VISIBLE) {
                legendContainer.setVisibility(View.GONE);
            } else {
                layerListContainer.setVisibility(View.GONE);
                legendContainer.setVisibility(View.VISIBLE);
            }
        });

        // 确保初始状态为隐藏
        layerListContainer.setVisibility(View.GONE);
        legendContainer.setVisibility(View.GONE);
    }

    public void setupAttributeQuery() {
        MaterialButton attributeQueryButton = activity.findViewById(R.id.shuxingchaxun);
        MaterialButton mapQueryButton = activity.findViewById(R.id.tuchashuxing);
        View attributeTableContainer = activity.findViewById(R.id.attribute_table_container);
        TextInputEditText searchInput = activity.findViewById(R.id.search_input);

        attributeQueryButton.setOnClickListener(v -> {
            buttonClickListener.onClick(v);
            if (attributeTableContainer.getVisibility() == View.VISIBLE) {
                attributeTableContainer.setVisibility(View.GONE);
            } else {
                attributeTableContainer.setVisibility(View.VISIBLE);
            }
        });

        mapQueryButton.setOnClickListener(buttonClickListener);

        // 添加搜索功能
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    // 触发搜索回调
                    if (buttonClickListener instanceof SearchCallback) {
                        ((SearchCallback) buttonClickListener).onSearch(s.toString());
                    }
                }
            });
        }
    }

    public void setupFeatureEdit() {
        MaterialButton templateButton = activity.findViewById(R.id.yaosumoban);
        MaterialButton editToolButton = activity.findViewById(R.id.yaosubianjigongju);

        templateButton.setOnClickListener(buttonClickListener);
        editToolButton.setOnClickListener(buttonClickListener);
    }

    public void setupGPSData() {
        MaterialButton gps1Button = activity.findViewById(R.id.GPS_1);
        MaterialButton gps2Button = activity.findViewById(R.id.GPS_2);

        gps1Button.setOnClickListener(buttonClickListener);
        gps2Button.setOnClickListener(buttonClickListener);
    }

    // 搜索回调接口
    public interface SearchCallback {
        void onSearch(String query);
    }
} 