package com.munaz.roomies.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.munaz.api.Server;
import com.munaz.model.Chore;
import com.munaz.model.User;
import com.munaz.roomies.R;

/**
 * Used to display chores in a list view
 */
public class ChoreArrayAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final Chore[] chores;
    private final User[] users;

    public ChoreArrayAdapter(Context context, Chore[] chores, User[] users, String[] choreTitles) {
        super(context, R.layout.list_roommate, choreTitles);

        this.context = context;
        this.chores = chores;
        this.users = users;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Chore selectedChore = chores[position];
        User selectedUser = null;

        for (User user : users) {
            if (selectedChore.assignedTo.equals(user.id)) {
                selectedUser = user;
                break;
            }
        }

        View rowView = inflater.inflate(R.layout.list_chore, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.chore_title);
        final ImageView imageView = (ImageView) rowView.findViewById(R.id.assigned_to_picture);
        textView.setText(selectedChore.title);

        if (selectedChore != null) {

            ImageRequest request = new ImageRequest(selectedUser.profilePictureUrl,
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            imageView.setImageBitmap(response);
                        }
                    }, 0, 0, null,
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            imageView.setImageResource(R.drawable.com_facebook_profile_picture_blank_portrait);
                        }
                    });

            Server.getInstance(context).addToRequestQueue(request);
        }

        return rowView;
    }
}
