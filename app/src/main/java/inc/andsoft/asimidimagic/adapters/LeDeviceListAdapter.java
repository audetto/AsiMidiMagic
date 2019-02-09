package inc.andsoft.asimidimagic.adapters;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import inc.andsoft.asimidimagic.R;

// Adapter for holding devices found through scanning.
public class LeDeviceListAdapter extends RecyclerView.Adapter<LeDeviceListAdapter.LeViewHolder> {
    private List<Map.Entry<BluetoothDevice, Boolean>> myDevices;

    static class LeViewHolder extends RecyclerView.ViewHolder {

        LeViewHolder(View view) {
            super(view);
        }

        void setValues(BluetoothDevice device, Boolean selected,
                       CompoundButton.OnCheckedChangeListener listener) {
            Switch name = itemView.findViewById(R.id.device_name);
            TextView address = itemView.findViewById(R.id.device_address);

            String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                name.setText(deviceName);
            else
                name.setText(R.string.unknown_device);
            address.setText(device.getAddress());

            name.setChecked(selected);
            name.setOnCheckedChangeListener(listener);
        }
    }

    public LeDeviceListAdapter() {
        myDevices = new ArrayList<>();
    }

    public void addDevice(BluetoothDevice device) {
        boolean present = myDevices.stream().anyMatch(x -> x.getKey().equals(device));
        if (!present) {
            Map.Entry<BluetoothDevice, Boolean> value = new AbstractMap.SimpleEntry<>(device, false);
            myDevices.add(value);
            notifyDataSetChanged();
        }
    }

    private void setDevice(BluetoothDevice device, boolean selected) {
        Optional<Map.Entry<BluetoothDevice, Boolean>> value =
                myDevices.stream().filter(x -> x.getKey().equals(device)).findFirst();
        value.ifPresent(x -> x.setValue(selected));
        // this is called as part of an event listener,
        // so the view is automatically update
    }

    public void clear() {
        myDevices.clear();
    }

    public BluetoothDevice[] getSelectedDevices() {
        BluetoothDevice[] devices = myDevices.stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toArray(BluetoothDevice[]::new);
        return devices;
    }

    @NonNull
    @Override
    public LeDeviceListAdapter.LeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_ble_scan, parent, false);

        return new LeDeviceListAdapter.LeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeDeviceListAdapter.LeViewHolder holder, int position) {
        Map.Entry<BluetoothDevice, Boolean> data = myDevices.get(position);
        BluetoothDevice device = data.getKey();

        holder.setValues(device, data.getValue(),
                (CompoundButton buttonView, boolean isChecked) -> setDevice(device, isChecked));
    }

    @Override
    public int getItemCount() {
        return myDevices.size();
    }

}
