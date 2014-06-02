/**
 * CLASS: CommandGroupAdapter
 *   This is an ArrayAdapter used for the list of troubleshooting scripts
 */

package com.cjcornell.samplebluetooth;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CommandGroupAdapter extends ArrayAdapter<ItemGroup> {
    private final Context context;
    private final List<ItemGroup> commandGroups;
    
    /** Constructor */
    public CommandGroupAdapter(Context context, final List<ItemGroup> objects) {
        super(context, R.layout.command_group_item, objects);
        this.context = context;
        this.commandGroups = objects;
    }
    
    /** 
     * The ViewHolder class will define what is in the view. In this case,
     * the view consists of two TextViews.
     */
    static class ViewHolder {
        TextView firstLine;
        TextView secondLine;
    }
    
    /**
     * Get the specified view
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.command_group_item, parent, false);
            ViewHolder vh = new ViewHolder();
            vh.firstLine = (TextView)convertView.findViewById(R.id.firstLine);
            vh.secondLine = (TextView)convertView.findViewById(R.id.secondLine);
            convertView.setTag(vh);
        }

        ItemGroup item = commandGroups.get(position);
        ViewHolder vh = (ViewHolder)convertView.getTag();
        vh.firstLine.setText(item.getName());
        vh.secondLine.setText(item.getDescription());

        return convertView;
    }
}
