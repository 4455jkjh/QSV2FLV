package com.a4455jkjh.qsv2flv;
import android.app.Application;

public class QSV2FLVApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		Threads.init(this);
		Storage.init(this);
	}

}
