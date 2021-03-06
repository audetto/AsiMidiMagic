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

public class BeatDialog extends DialogFragment {

    public interface BeatDialogListener {
        void onDialogPositiveClick(int count, @ColorInt int color, int width);
    }

    // Use this instance of the interface to deliver action events
    private BeatDialogListener myListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.dialog_beats, null);
        builder.setView(view);
        builder.setTitle(R.string.add_beat);

        TextView textCount = view.findViewById(R.id.text_count);
        TextView textColor = view.findViewById(R.id.text_color);
        TextView textWidth = view.findViewById(R.id.text_width);

        Bundle args = getArguments();
        if (args != null) {
            String suggested_color = args.getString("color");
            textColor.setText(suggested_color);

            int suggested_width = args.getInt("width");
            textWidth.setText(String.valueOf(suggested_width));
        }

        builder.setPositiveButton(android.R.string.ok, (DialogInterface dialog, int id) -> {
            try {
                int count = Integer.valueOf(textCount.getText().toString());
                int color = Color.parseColor(textColor.getText().toString());
                int width = Integer.valueOf(textWidth.getText().toString());

                myListener.onDialogPositiveClick(count, color, width);
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
            myListener = (BeatDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

}
