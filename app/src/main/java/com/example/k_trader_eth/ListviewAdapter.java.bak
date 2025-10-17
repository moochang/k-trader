package com.example.k_trader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by 김무창 on 2017-12-20.
 */

public class ListviewAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private ArrayList<Listviewitem> data;
    private int layout;

    public ListviewAdapter(Context context, int layout, ArrayList<Listviewitem> data){
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.data = data;
        this.layout = layout;
    }

    @Override
    public int getCount(){return data.size();}

    @Override
    public String getItem(int position){return data.get(position).getName();}

    @Override
    public long getItemId(int position){return position;}

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        if(convertView == null){
            convertView = inflater.inflate(layout,parent,false);
        }
        Listviewitem listviewitem = data.get(position);
//        ImageView icon = (ImageView)convertView.findViewById(R.id.imageview);
//        icon.setImageResource(listviewitem.getIcon());
        TextView name = (TextView)convertView.findViewById(R.id.textview);
        name.setText(listviewitem.getName());

        convertView.setBackgroundColor(listviewitem.getColor());

        return convertView;
    }
}
