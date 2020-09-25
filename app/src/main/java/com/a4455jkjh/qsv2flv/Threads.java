package com.a4455jkjh.qsv2flv;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class Threads {
	private static Handler handler;
	private static ThreadPoolExecutor threads;
	public static void init(Context ctx) {
		handler = new Handler();
		SharedPreferences sp = ctx.getSharedPreferences("path", Context.MODE_PRIVATE);
		int count = sp.getInt("threads", 2);
		threads = (ThreadPoolExecutor) Executors.newFixedThreadPool(count);
	}
	public static void post(Runnable runnable) {
		handler.post(runnable);
	}
	public static <T> Future<T> submit(Callable<T> call) {
		return threads.submit(call);
	}
	public static void setThreadsCount(Context ctx, int count) {
		SharedPreferences sp = ctx.getSharedPreferences("path", Context.MODE_PRIVATE);
		int oldCount = sp.getInt("threads", 2);
		if (oldCount == count)
			return;
		threads.setCorePoolSize(count);
		threads.setMaximumPoolSize(count);
		SharedPreferences.Editor edit = sp.edit();
		edit.putInt("threads", count);
		edit.commit();
	}
}
