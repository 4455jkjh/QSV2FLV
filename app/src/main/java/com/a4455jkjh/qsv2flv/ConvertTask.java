package com.a4455jkjh.qsv2flv;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.concurrent.Callable;

public class ConvertTask {
    private final TextView message;
    private final ProgressBar progress;
	public Callback callback;

	public void execute(final QSV qsv) {
		Threads.submit(new Callable<Boolean>(){
				@Override
				public Boolean call() throws Exception {
					Boolean bool = qsv.convert(ConvertTask.this);
					onPostExecute(bool);
					return bool;
				}
			});
	}
	public interface Callback {
		void call();
	}

    public ConvertTask(View view, Callback callback) {
		this.message = view.findViewById(R.id.state);
		progress = view.findViewById(R.id.progress);
		this.callback = callback;
		onPreExecute();
	}

	protected void publishProgress(final Msg value) {
		Threads.post(new Runnable(){
				@Override
				public void run() {
					if (value.message != null) {
						message.setText(value.message);
					} else {
						progress.setProgress(value.progress);
					}
				}
			});
	}
	protected void onPostExecute(final Boolean result) {
        Threads.post(new Runnable(){
				@Override
				public void run() {
					if (result.booleanValue()) {
						message.setText("已转换");
					} else {
						message.setText("未转换");
					}
					progress.setVisibility(View.GONE);
					count--;
					if (callback != null)
						callback.call();
				}
			});
	}

	protected void onPreExecute() {
		progress.setVisibility(View.VISIBLE);
		message.setText("等待中……");
		count++;
	}
    public void setMax(int i) {
		progress.setMax(i);
		progress.setProgress(0);
    }
	public void showMessage(String str) {
		Msg msg =  new Msg();
		msg.message = str;
        publishProgress(msg);
    }

    public void showProgress(int i) {
        Msg msg = new Msg();
		msg.progress = i;
        publishProgress(msg);
    }
	public static class Msg {
		String message = null;
		int progress = 0;
	}
	public static int count = 0;
}
