package com.example.arcgistest2.ui;

import android.content.Context;
import android.view.View;

public class UIManager {
    private final Context context;
    private final View.OnClickListener clickListener;

    public UIManager(Context context, View.OnClickListener clickListener) {
        this.context = context;
        this.clickListener = clickListener;
    }
} 