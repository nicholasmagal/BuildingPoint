package com.example.buildingpoint;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;


public class ViewDialog {


    public void showDialog(Activity activity, String Name, String Department, String Address) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.layout_dialog);

        //Setting the textView up
        TextView name = dialog.findViewById(R.id.name_holder);
        TextView dep = dialog.findViewById(R.id.department_holder);
        TextView address = dialog.findViewById(R.id.address_holder);
        Button exit = dialog.findViewById(R.id.exit_button);


        name.setText(Name);
        name.setTextColor(Color.WHITE);
        dep.setText(Department);
        dep.setTextColor(Color.WHITE);
        address.setText(Address);
        address.setTextColor(Color.WHITE);


        //Stick it on the bottem and resume the darkened screen
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.BOTTOM;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);

        //Setting the dialog to be the entire horiztal screen

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;


        //Showing the dialog
        dialog.show();

        //Setting the backgroundcolor
        //int color=ContextCompat.getColor(ViewDialog.this,R.color.myOrange);


       // dialog.getWindow().setBackgroundDrawableResoure(color);
        //setting the horizontal portion
        dialog.getWindow().setAttributes(lp);
        //Click handler for exiting


        //Setting up the button to exit the dilog
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

    }


}