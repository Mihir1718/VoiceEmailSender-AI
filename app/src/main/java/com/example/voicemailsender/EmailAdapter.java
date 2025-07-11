package com.example.voicemailsender;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class EmailAdapter extends ArrayAdapter<String> {

    public EmailAdapter(Context context, List<String> subjects) {
        super(context, 0, subjects);
    }

    @SuppressLint("ViewHolder")
    @Override
     public View getView(int position, View convertView, ViewGroup parent) {
        String subject = getItem(position);

        View view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        TextView tv1 = view.findViewById(android.R.id.text1);
        TextView tv2 = view.findViewById(android.R.id.text2);

        tv1.setText(subject);
        tv2.setText("Tap to hear full email");  // optional subtitle

        return view;
    }

}
