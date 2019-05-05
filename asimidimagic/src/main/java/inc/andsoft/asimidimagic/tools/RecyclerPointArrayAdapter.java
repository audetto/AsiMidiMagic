package inc.andsoft.asimidimagic.tools;

import android.graphics.Point;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import inc.andsoft.asimidimagic.R;


public class RecyclerPointArrayAdapter extends RecyclerView.Adapter<RecyclerPointArrayAdapter.PointViewHolder> {
    private List<Point> myItems;
    private int myResource;

    static class PointViewHolder extends RecyclerView.ViewHolder {
        CheckBox mySelectedBox;
        TextView myNumberX;
        TextView myNumberY;

        TextWatcher myWatcherX;
        TextWatcher myWatcherY;

        PointViewHolder(View view) {
            super(view);
            mySelectedBox = itemView.findViewById(R.id.check_selected);
            myNumberX = itemView.findViewById(R.id.number_x);
            myNumberY = itemView.findViewById(R.id.number_y);
        }

        void populate(int position, @NonNull Point data) {
            mySelectedBox.setText(String.valueOf(position));

            myNumberX.removeTextChangedListener(myWatcherX);
            myNumberX.setText(String.valueOf(data.x));
            myWatcherX = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int x = Integer.valueOf(s.toString());
                        data.set(x, data.y);
                    } catch (NumberFormatException e) {

                    }
                }
            };
            myNumberX.addTextChangedListener(myWatcherX);

            myNumberY.removeTextChangedListener(myWatcherY);
            myNumberY.setText(String.valueOf(data.y));
            myWatcherY = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int y = Integer.valueOf(s.toString());
                        data.set(data.x, y);
                    } catch (NumberFormatException e) {

                    }
                }
            };
            myNumberY.addTextChangedListener(myWatcherY);
        }
    }

    public RecyclerPointArrayAdapter(@LayoutRes int resource) {
        myResource = resource;
    }

    public void setItems(List<Point> items) {
        myItems = items;
    }

    public List<Point> getItems() { return myItems; }

    public void clear() {
        if (myItems != null) {
            myItems.clear();
        }
    }

    @NonNull
    @Override
    public PointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(myResource, parent, false);

        return new PointViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PointViewHolder holder, int position) {
        Point data = myItems.get(position);

        holder.populate(position, data);
    }

    @Override
    public int getItemCount() {
        if (myItems == null) {
            return 0;
        } else {
            return myItems.size();
        }
    }

}
