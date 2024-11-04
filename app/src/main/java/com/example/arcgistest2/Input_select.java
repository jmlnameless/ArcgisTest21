package com.example.arcgistest2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class Input_select extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_select);

        Button select_button= findViewById(R.id.select_button);
        Button ret_button= findViewById(R.id.return_button1);
        select_button.setOnClickListener(v -> finish());
        ret_button.setOnClickListener(v -> {
            // 创建一个Intent来启动首页Activity
            Intent intent = new Intent(Input_select.this, Main.class);
            // 可选：添加flags来清除任务栈，这样用户点击返回时不会回到当前Activity
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            // 启动首页Activity
            startActivity(intent);
            // 结束当前Activity
            finish();
        });

        Button sumit_button = findViewById(R.id.submit_button1);
        sumit_button.setOnClickListener(v -> {
            EditText e1 = findViewById(R.id.editTextText2);
            EditText e2 = findViewById(R.id.editTextText3);
            String selectedFieldName = e1.getText().toString().trim().toUpperCase();
            String selectedFieldValue = e2.getText().toString().trim();
            if(selectedFieldName.isEmpty() | selectedFieldValue.isEmpty()){
                showAlertDialog();

            }else{
                Intent intent = new Intent();
                intent.putExtra("fieldName", selectedFieldName);
                intent.putExtra("fieldValue", selectedFieldValue);
                setResult(RESULT_OK, intent);
                finish();
            }
        });



    }
    private void showAlertDialog() {
        // 在 UI 线程中执行
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            builder.setMessage("列名或列值不能为空");
            builder.setPositiveButton("确定", null);
            try {
                builder.show();
            } catch (WindowManager.BadTokenException e) {
                Log.e("AlertDialogError", "Unable to show AlertDialog", e);
            }
        });
    }
}


