package com.tencent.doh.demo.demo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.tencent.doh.demo.R;
import com.tencent.doh.ui.activity.DohWebViewActivity;

public class MainActivity extends DohWebViewActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle("main");
        Button demoBtn = (Button) findViewById(R.id.demo);
        Button medBtn = (Button) findViewById(R.id.med);
        demoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(MainActivity.this, DemoActivity.class);
                startActivity(it);
            }
        });
        medBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(MainActivity.this, MedActivity.class);
                startActivity(it);
            }
        });
        initIpSettingUI();
    }

    private void initIpSettingUI(){
        final View inputRoot = findViewById(R.id.input_root);

        Switch resSwitch = (Switch) findViewById(R.id.use_remote_res_switch);
        resSwitch.setChecked(SharedPreferencesUtils.getUseRemoteRes(this));
        resSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferencesUtils.saveUseRemoteRes(getApplicationContext(),isChecked);
                if (isChecked)
                    inputRoot.setVisibility(View.VISIBLE);
                else
                    inputRoot.setVisibility(View.INVISIBLE);
            }
        });
        if (!SharedPreferencesUtils.getUseRemoteRes(this))
            inputRoot.setVisibility(View.INVISIBLE);
        final EditText  ipEdit = (EditText) findViewById(R.id.ip_edit_id);
        final EditText  nameEdit = (EditText) findViewById(R.id.name_edit_id);

        ipEdit.setText(SharedPreferencesUtils.getIp(getApplicationContext()));
        nameEdit.setText(SharedPreferencesUtils.getName(getApplicationContext()));

        Button bt = (Button) findViewById(R.id.ok);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ipEdit.getText().toString().trim();
                String name = nameEdit.getText().toString().trim();

                SharedPreferencesUtils.saveIp(getApplicationContext(),ip);
                SharedPreferencesUtils.saveName(getApplicationContext(),name);
                Toast.makeText(getApplicationContext(),"修改成功",Toast.LENGTH_SHORT).show();
            }
        });
    }



}
