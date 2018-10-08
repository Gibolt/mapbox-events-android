package com.mapbox.android.core.location;

import android.content.Intent;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class BackgroundLocationEngine extends ForegroundLocationEngine implements IntentHandler {
  private final BroadcastReceiverProxy broadcastReceiverProxy;
  private final List<LocationEngineCallback<LocationEngineResult>> callbacks;

  private LocationUpdatesBroadcastReceiver broadcastReceiver;

  BackgroundLocationEngine(@NonNull LocationEngineImpl locationEngineImpl,
                           @NonNull BroadcastReceiverProxy broadcastReceiverProxy) {
    super(locationEngineImpl);
    this.broadcastReceiverProxy = broadcastReceiverProxy;
    this.callbacks = new CopyOnWriteArrayList<>();
  }

  @Override
  public void requestLocationUpdates(@NonNull LocationEngineRequest request,
                                     @NonNull LocationEngineCallback<LocationEngineResult> callback,
                                     @Nullable Looper looper) throws SecurityException {
    if (broadcastReceiver == null) {
      broadcastReceiver = (LocationUpdatesBroadcastReceiver) broadcastReceiverProxy.createReceiver(this);
      broadcastReceiverProxy.registerReceiver(broadcastReceiver,
              LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
    }

    callbacks.add(callback);
    locationEngineImpl.requestLocationUpdates(request,
            broadcastReceiverProxy.getPendingIntent(LocationUpdatesBroadcastReceiver.class,
                    LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES));
  }

  @Override
  public void removeLocationUpdates(@NonNull LocationEngineCallback<LocationEngineResult> callback) {
    callbacks.remove(callback);
    locationEngineImpl.removeLocationUpdates(
            broadcastReceiverProxy.getPendingIntent(LocationUpdatesBroadcastReceiver.class,
                    LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES));

    if (callbacks.isEmpty()) {
      broadcastReceiverProxy.unregisterReceiver(broadcastReceiver);
      broadcastReceiver = null;
    }
  }

  @Override
  public void handle(Intent intent) {
    if (intent == null) {
      return;
    }

    LocationEngineResult location = locationEngineImpl.extractResult(intent);
    for (LocationEngineCallback<LocationEngineResult> callback : callbacks) {
      callback.onSuccess(location);
    }
  }
}