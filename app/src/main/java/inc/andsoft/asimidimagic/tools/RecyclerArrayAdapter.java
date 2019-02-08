package inc.andsoft.asimidimagic.tools;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class RecyclerArrayAdapter<T> extends RecyclerView.Adapter<RecyclerArrayAdapter.MultiViewHolder> {
    private List<T> myItems;
    private int myResource;

    static class MultiViewHolder extends RecyclerView.ViewHolder {
        MultiViewHolder(View view) {
            super(view);
        }
    }

    public RecyclerArrayAdapter(@LayoutRes int resource) {
        myResource = resource;
    }

    public void setItems(List<T> items) {
        myItems = items;
    }

    public void clear() {
        if (myItems != null) {
            myItems.clear();
        }
    }

    @NonNull
    @Override
    public MultiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(myResource, parent, false);

        return new MultiViewHolder(view);
    }

    abstract public void populateView(@NonNull View itemView, int position, @NonNull T data);

    @Override
    public void onBindViewHolder(@NonNull MultiViewHolder holder, int position) {
        T data = myItems.get(position);

        populateView(holder.itemView, position, data);
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
