package com.vachel.editor.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.vachel.editor.R;
import com.vachel.editor.util.Utils;

public class ProgressDialog extends Dialog implements LifecycleObserver {

    public ProgressDialog(@NonNull Context context) {
        super(context, R.style.ProgressDialog);
        initDialog(context, "");
    }


    public ProgressDialog(@NonNull Context context, String description) {
        super(context, R.style.ProgressDialog);
        initDialog(context, description);
    }

    public ProgressDialog bindLifeCycle(@NonNull final LifecycleOwner owner){
        owner.getLifecycle().addObserver(this);
        setOnDismissListener(dialog -> owner.getLifecycle().removeObserver(ProgressDialog.this));
        return this;
    }

    public void initDialog(Context context, String description) {
        OnKeyListener onKeyListener = (arg0, arg1, arg2) -> true;
        LayoutInflater mInflater = LayoutInflater.from(context);
        View convertView = mInflater.inflate(R.layout.progress_dialog_view, null);
        ProgressBar progressBar = convertView.findViewById(R.id.refreshing);
        TextView text = convertView.findViewById(R.id.text);
        if (!description.equals("")) {
            text.setPadding(Utils.dip2px(context,8),0,0,0);
            convertView.setBackgroundResource(R.drawable.white_rectangle);
        }else{
            convertView.setBackground(null);
            ViewGroup.LayoutParams params = progressBar.getLayoutParams();
            params.height =Utils.dip2px(context,32);
            params.width =Utils.dip2px(context,32);
            progressBar.setLayoutParams(params);
        }
        text.setText(description);
        setContentView(convertView);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        setOnKeyListener(onKeyListener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            this.create();
       }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onStop() {
        dismiss();
    }

}
