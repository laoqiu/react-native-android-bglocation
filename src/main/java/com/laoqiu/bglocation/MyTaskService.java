package com.laoqiu.bglocation;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.facebook.react.HeadlessJsTaskService;


public class MyTaskService extends HeadlessJsTaskService {

  @Override
  protected @Nullable HeadlessJsTaskConfig getTaskConfig(Intent intent) {
    Bundle extras = intent.getExtras();
    if (extras != null) {
      return new HeadlessJsTaskConfig(
          "BackgroundLocationTask",
          Arguments.fromBundle(extras),
          5000, // timeout for the task
          false // optional: defines whether or not  the task is allowed in foreground. Default is false
        );
    }
    return null;
  }
}