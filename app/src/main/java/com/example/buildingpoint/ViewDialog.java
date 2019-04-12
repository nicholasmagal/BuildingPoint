package com.example.buildingpoint;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class ViewDialog {

    public void showDialog(Activity activity, String Name, String Department, String Address){
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.layout_dialog);

        //Setting the textView up
        TextView name=(TextView) dialog.findViewById(R.id.name_holder);
        TextView dep=(TextView) dialog.findViewById(R.id.department_holder);
        TextView address=(TextView) dialog.findViewById(R.id.address_holder);

        name.setText(Name);
        dep.setText(Department);
        address.setText(Address);


        //Stick it on the bottem and resume the darkened screen
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.BOTTOM;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);

        dialog.show();

    }
}