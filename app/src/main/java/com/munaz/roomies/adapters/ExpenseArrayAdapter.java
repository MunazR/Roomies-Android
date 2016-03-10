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
import com.munaz.model.Expense;
import com.munaz.model.User;
import com.munaz.roomies.R;

/**
 * Used to display expenses in a list view
 */
public class ExpenseArrayAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final Expense[] expenses;
    private final User[] users;

    public ExpenseArrayAdapter(Context context, Expense[] expenses, User[] users, String[] expenseTitles) {
        super(context, R.layout.list_expense, expenseTitles);

        this.context = context;
        this.expenses = expenses;
        this.users = users;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Expense selectedExpense = expenses[position];
        User selectedUser = null;

        for (User user : users) {
            if (selectedExpense.expensedBy.equals(user.id)) {
                selectedUser = user;
                break;
            }
        }

        View rowView = inflater.inflate(R.layout.list_expense, parent, false);
        TextView titleTextView = (TextView) rowView.findViewById(R.id.expense_title);
        TextView amountTextView = (TextView) rowView.findViewById(R.id.expense_amount);
        final ImageView imageView = (ImageView) rowView.findViewById(R.id.expensed_by_picture);
        titleTextView.setText(selectedExpense.title);
        amountTextView.setText("$" + selectedExpense.amount);

        if (selectedUser != null) {
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
