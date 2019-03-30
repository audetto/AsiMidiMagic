package inc.andsoft.asimidimagic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import inc.andsoft.asimidimagic.activities.CommonMidiPassActivity;
import inc.andsoft.asimidimagic.activities.ReceiverState;

class CommandReceiverState implements ReceiverState {
    MidiReceiver myReceiver;
    MidiReceiver myInputPort;

    @Override
    public MidiReceiver getReceiver() {
        return myReceiver;
    }

    @Override
    public void close() throws IOException {
    }
}

public class CommandActivity extends CommonMidiPassActivity<CommandReceiverState> {
    TextView myTextLog;
    boolean myRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AutoCompleteTextView text = findViewById(R.id.text_command);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sysex_messages, android.R.layout.simple_dropdown_item_1line);
        text.setAdapter(adapter);

        Button send = findViewById(R.id.button_send);
        send.setOnClickListener(v -> sendMessage(text.getText().toString()));

        Button clear = findViewById(R.id.button_clear);
        clear.setOnClickListener(v -> myTextLog.setText(""));

        Button save = findViewById(R.id.button_save);
        save.setOnClickListener(v -> {
            String payload = myTextLog.getText().toString();

            File folder = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS);

            File storage = new File(folder, "logger.txt");
            try {
                try (FileWriter fw = new FileWriter(storage)) {
                    fw.append(payload);
                }
            } catch (IOException io) {
                myTextLog.append(io.getLocalizedMessage());
                myTextLog.append("\n");
            }
            myTextLog.append(storage.getAbsolutePath());
            myTextLog.append("\n");
        });

        myTextLog = findViewById(R.id.text_logging);
        myTextLog.setMovementMethod(new ScrollingMovementMethod());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    protected CommandReceiverState getReceiverState(MidiInputPort inputPort) {
        CommandReceiverState state = new CommandReceiverState();

        state.myReceiver = new MidiReceiver() {
            @Override
            public void onSend(byte[] data, int offset, int count, long timestamp) throws IOException {
                if (myRunning) {
                    StringBuilder sb = new StringBuilder();

                    sb.append("In:  ");
                    for (int i = 0; i < count; i++) {
                        byte b = data[offset + i];
                        sb.append(String.format("%02X ", b));
                    }
                    sb.append("\n");

                    String text = sb.toString();
                    runOnUiThread(() -> myTextLog.append(text));
                }
            }
        };
        state.myInputPort = inputPort;

        return state;
    }

    @Override
    protected @LayoutRes int getLayoutID() {
        return R.layout.activity_command;
    }

    @Override
    protected void setRunning(boolean value) {
        myRunning = value;
    }

    private void sendMessage(String command) {
        try {
            String[] commands = command.split(" ");

            int n = commands.length;

            byte[] buffer = new byte[n];

            for (int i = 0; i < n; ++i) {
                int v = Integer.parseInt(commands[i], 16);
                buffer[i] = (byte) v;
            }

            myReceiverState.myInputPort.send(buffer, 0, buffer.length);
            myTextLog.append("Out: ");
            myTextLog.append(command);
            myTextLog.append("\n");
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
