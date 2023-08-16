package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.ui.adapter.ApiHistoryDialogAdapter;
import com.github.tvbox.osc.util.HawkConfig;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.orhanobut.hawk.Hawk;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述
 *
 * @author pj567
 * @since 2020/12/27
 */
public class ApiDialog extends BaseDialog {
    private TextView tvAddress;
    private EditText inputApi;


    public ApiDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_api);
        
        tvAddress = findViewById(R.id.tvAddress);
        inputApi = findViewById(R.id.input);
        //内置网络接口在此处添加
        inputApi.setText(Hawk.get(HawkConfig.API_URL, ""));
        findViewById(R.id.inputSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newApi = inputApi.getText().toString().trim();
                if (!newApi.isEmpty()) {
                    ArrayList<String> history = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<String>());
                    if (!history.contains(newApi))
                        history.add(0, newApi);
                    if (history.size() > 30)
                        history.remove(30);
                    Hawk.put(HawkConfig.SUBSCRIPTIONS, history);
                    listener.onchange(newApi);
                    dismiss();
                }
            }
        });
        findViewById(R.id.apiHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> history = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<String>());
                if (history.isEmpty())
                    return;
                String current = Hawk.get(HawkConfig.API_URL, "");
                int idx = 0;
                if (history.contains(current))
                    idx = history.indexOf(current);
                ApiHistoryDialog dialog = new ApiHistoryDialog(getContext());
                dialog.setTip("历史配置列表");
                dialog.setAdapter(new ApiHistoryDialogAdapter.SelectDialogInterface() {
                    @Override
                    public void click(String value) {
                        inputApi.setText(value);
                        listener.onchange(value);
                        dialog.dismiss();
                    }

                    @Override
                    public void del(String value, ArrayList<String> data) {
                        Hawk.put(HawkConfig.SUBSCRIPTIONS, data);
                    }
                }, history, idx);
                dialog.show();
            }
        });
        findViewById(R.id.storagePermission).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (XXPermissions.isGranted(getContext(), Permission.Group.STORAGE)) {
                    Toast.makeText(getContext(), "已获得存储权限", Toast.LENGTH_SHORT).show();
                } else {
                    XXPermissions.with(getContext())
                            .permission(Permission.Group.STORAGE)
                            .request(new OnPermissionCallback() {
                                @Override
                                public void onGranted(List<String> permissions, boolean all) {
                                    if (all) {
                                        Toast.makeText(getContext(), "已获得存储权限", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onDenied(List<String> permissions, boolean never) {
                                    if (never) {
                                        Toast.makeText(getContext(), "获取存储权限失败,请在系统设置中开启", Toast.LENGTH_SHORT).show();
                                        XXPermissions.startPermissionActivity((Activity) getContext(), permissions);
                                    } else {
                                        Toast.makeText(getContext(), "获取存储权限失败", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

    }

    public void setOnListener(OnListener listener) {
        this.listener = listener;
    }

    OnListener listener = null;

    public interface OnListener {
        void onchange(String api);
    }
}
