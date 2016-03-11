package com.munaz.roomies.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.munaz.model.PantryItem;
import com.munaz.roomies.R;

/**
 * Used to display pantry items in a list view
 */
public class PantryArrayAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final PantryItem[] pantryItems;

    public PantryArrayAdapter(Context context, PantryItem[] pantryItems, String[] pantryItemTitles) {
        super(context, R.layout.list_pantry, pantryItemTitles);

        this.context = context;
        this.pantryItems = pantryItems;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        PantryItem selectedPantryItem = pantryItems[position];

        View rowView = inflater.inflate(R.layout.list_pantry, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.pantry_title);
        textView.setText(selectedPantryItem.title);

        return rowView;
    }
}
