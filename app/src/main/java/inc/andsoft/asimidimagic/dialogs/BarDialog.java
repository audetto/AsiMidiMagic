package inc.andsoft.asimidimagic.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import inc.andsoft.asimidimagic.R;

public class BarDialog extends DialogFragment {

    public interface BarDialogListener {
        void onDialogPositiveClick(int periods, @ColorInt int color, int width);
    }

    // Use this instance of the interface to deliver action events
    private BarDialogListener myListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.dialog_bar, null);
        builder.setView(view);
        builder.setTitle("add bars");

        builder.setPositiveButton(android.R.string.ok, (DialogInterface dialog, int id) -> {
            TextView textPeriod = view.findViewById(R.id.text_period);
            TextView textColor = view.findViewById(R.id.text_color);
            TextView textWidth = view.findViewById(R.id.text_width);

            try {
                int period = Integer.valueOf(textPeriod.getText().toString());
                int color = Color.parseColor(textColor.getText().toString());
                int width = Integer.valueOf(textWidth.getText().toString());

                myListener.onDialogPositiveClick(period, color, width);
            } catch (Exception e) {
                Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
            }

        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            myListener = (BarDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

}
