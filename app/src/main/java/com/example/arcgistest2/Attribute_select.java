package com.example.arcgistest2;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class Attribute_select extends AppCompatActivity {
    private List<String> field_value;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.attribute_select);

        Button return_button = findViewById(R.id.return_button);
        return_button.setOnClickListener(v -> finish());
        List<String> field_Name = read_firstName();
        Spinner line_spinner = findViewById(R.id.line_spinner);
        ArrayAdapter<String> fieldsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, field_Name);
        fieldsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        line_spinner.setAdapter(fieldsAdapter);
        Spinner value_spinner = findViewById(R.id.value_spinner);
        line_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                field_value = readAndSortValuesSkippingFirstLine(position);
                ArrayAdapter<String> valuesAdapter = new ArrayAdapter<>(Attribute_select.this, android.R.layout.simple_spinner_item, field_value);
                valuesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                value_spinner.setAdapter(valuesAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        Button sumbit_button=findViewById(R.id.submit_button);
        sumbit_button.setOnClickListener(v -> {
            String selectedFieldName = line_spinner.getSelectedItem().toString();
            String selectedFieldValue = value_spinner.getSelectedItem().toString();
            Intent intent = new Intent();
            intent.putExtra("fieldName", selectedFieldName);
            intent.putExtra("fieldValue", selectedFieldValue);
            setResult(RESULT_OK, intent);
            finish();
        });

        Button inputButton=findViewById(R.id.input_button);
        inputButton.setOnClickListener(v -> {
            Intent intent = new Intent(Attribute_select.this, Input_select.class);
            startActivityForResult(intent, 1);
        });
    }
    public List<String> read_firstName() {
        Resources res = getResources();
        InputStream is = res.openRawResource(R.raw.data);
        List<String> namesList = new ArrayList<>(); // 使用ArrayList以支持删除操作
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String firstLine = reader.readLine();
            if (firstLine != null) {
                String[] namesArray = firstLine.split(",");
                for (String name : namesArray) {
                    namesList.add(name.trim()); // 将分割后的字符串添加到列表中
                }
                // 删除列表中的"FID"元素，如果存在
                namesList.remove("FID");
            } else {
                throw new Exception("文件是空的");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("读取文件时发生错误：" + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return namesList;
    }
    public List<String> readAndSortValuesSkippingFirstLine(int position) {
        List<String> valuesList = new ArrayList<>();
        //在活动中
        Resources res = getResources();
        InputStream is = null;
        try {
            is = res.openRawResource(R.raw.data);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            // 标记是否正在读取第一行
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] values = line.split(",");
                if (position+1 < values.length) {
                    String value = values[position+1].trim();
                    if (!value.isEmpty()) {
                        valuesList.add(value);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 使用LinkedHashSet来去除重复的值
        LinkedHashSet<String> uniqueValues = new LinkedHashSet<>(valuesList);
        // 将LinkedHashSet转换为List以便排序
        List<String> sortedValues = new ArrayList<>(uniqueValues);

        // 自定义比较器
        Collections.sort(sortedValues, (o1, o2) -> {
            // 检查是否为数字
            boolean isNumeric1 = o1.matches("\\d+");
            boolean isNumeric2 = o2.matches("\\d+");

            // 如果两个字符串都是数字
            if (isNumeric1 && isNumeric2) {
                return Integer.compare(Integer.parseInt(o1), Integer.parseInt(o2));
            }
            // 如果两个字符串都不是数字
            else if (!isNumeric1 && !isNumeric2) {
                return o1.compareTo(o2);
            }
            // 如果第一个字符串是数字，第二个不是
            else if (isNumeric1) {
                return -1; // 数字应该排在前面
            }
            // 如果第一个字符串不是数字，第二个是
            else {
                return 1; // 字符串应该排在后面
            }
        });

        return sortedValues;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1) {
            String fieldName = data.getStringExtra("fieldName");
            String fieldValue = data.getStringExtra("fieldValue");
            Intent intent = new Intent();
            intent.putExtra("fieldName", fieldName);
            intent.putExtra("fieldValue", fieldValue);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}

