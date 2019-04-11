package com.example.buildingpoint;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Html;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class BuildingDialog extends AppCompatDialogFragment {
    private EditText editTextUsername;
    private EditText editTextPassword;
    String Name;
    String Department;
    String Address;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null){
            Name = getArguments().getString("Name","");
            Department = getArguments().getString("Department");
            Address = getArguments().getString("Address");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog,null);

        builder.setView(view)
                .setMessage(Html.fromHtml("<b>"+"Building:" + "</b>" + Name + "\n" + "<b>" + "Department: " + "</b>" +Department + "\n" + "<b>" + "Address:  "+"</b>"+ Address+"\n" ))
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        return builder.create();
    }
}

