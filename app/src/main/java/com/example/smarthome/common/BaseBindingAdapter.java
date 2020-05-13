package com.example.smarthome.common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.BR;
import com.example.smarthome.MainActivity;
import com.example.smarthome.R;
import com.example.smarthome.ui.device.DetailDeviceFragment;
import com.example.smarthome.ui.device.model.Device;

import java.util.List;
import java.util.Objects;

public class BaseBindingAdapter<T> extends RecyclerView.Adapter<BaseBindingAdapter.ViewHolder> {
    private List<T> data;
    private LayoutInflater inflater;
    private @LayoutRes
    int resId;
    private OnItemClickListener<T> onItemClickListener;
    private Context mContext;

    public void setOnItemClickListener(OnItemClickListener<T> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public BaseBindingAdapter(Context context, @LayoutRes int resId) {
        inflater = LayoutInflater.from(context);
        this.resId = resId;
        mContext = context;
    }

    public void setData(List<T> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public List<T> getData() {
        return data;
    }

    @NonNull
    @Override
    public BaseBindingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(DataBindingUtil.inflate(inflater, resId, parent, false));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull BaseBindingAdapter.ViewHolder holder, int position) {
        T item = data.get(position);
        holder.binding.setVariable(BR.item_device, item);
        holder.binding.executePendingBindings();
        holder.itemView.setOnClickListener(v -> {
            onItemClickListener.onItemClick(item);
        });

        if (item instanceof Device) {
            Device device = (Device) item;
            try {
                if (Double.parseDouble(device.getNO()) > Double.parseDouble(device.getNG())) {
                    holder.itemView.findViewById(R.id.imgWarning).setVisibility(View.VISIBLE);
                    holder.itemView.findViewById(R.id.imgWarning).setAnimation(DetailDeviceFragment.createFlashingAnimation());
                    createNotification(device.getNO(), device.getId());
                } else {
                    holder.itemView.findViewById(R.id.imgWarning).setVisibility(View.GONE);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ViewDataBinding binding;

        public ViewHolder(ViewDataBinding inflate) {
            super(inflate.getRoot());
            this.binding = inflate;
        }
    }

    private void createNotification(String ng, String idDevice) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(Objects.requireNonNull(mContext).getApplicationContext(), "notify_001");
        Intent ii = new Intent(mContext.getApplicationContext(), MainActivity.class);
        ii.putExtra("menuFragment", "DetailDeviceFragment");
        ii.putExtra("idDevice", idDevice);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, ii, 0);

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText("Nhiệt độ đo được: " + ng + " tại thiết bị: " + idDevice);
        bigText.setBigContentTitle("Nhiệt độ vượt ngưỡng !");
        bigText.setSummaryText("Cảnh báo");

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.drawable.ic_warning_red);
        mBuilder.setContentTitle(mContext.getString(R.string.app_name));
        mBuilder.setContentText("Nhiệt độ vượt ngưỡng !");
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setStyle(bigText);

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

// === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "Your_channel_id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
            mBuilder.setChannelId(channelId);
        }

        if (mNotificationManager != null) {
            mNotificationManager.notify(0, mBuilder.build());
        }
    }

    public interface OnItemClickListener<T> {
        void onItemClick(T item);
    }
}

