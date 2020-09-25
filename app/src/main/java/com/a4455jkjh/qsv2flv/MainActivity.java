package com.a4455jkjh.qsv2flv;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.a4455jkjh.qsv2flv.MainActivity;
import java.io.File;
import org.json.JSONException;
import android.os.Environment;

public class MainActivity extends Activity implements QsvAdapter.Callback {
	private ListView items;
	private QsvAdapter adapter;
	private FileDialog fileDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    int sdk = Build.VERSION.SDK_INT;
    if(sdk>=30){
      init();
    }
   else if (sdk < 23 ||
        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0)
			init();
		else
			requestPermissions(new String[]{
                           Manifest.permission.WRITE_EXTERNAL_STORAGE,
                         }, hashCode());
  }
	private void init() {
		setContentView(R.layout.main);
		fileDialog = new FileDialog(this);
		items = findViewById(R.id.items);
		QsvAdapter adapter = new QsvAdapter(this);
		items.setAdapter(adapter);
		items.setOnItemClickListener(adapter);
		this.adapter = adapter;
		onNewIntent(getIntent());
	}

	@Override
	public void finish() {
		if (adapter != null)
			adapter.notifyDataSetInvalidated();
		super.finish();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri data = intent.getData();
		if (data == null)
			return;
		open(data.getPath());
	}

	public void open(String path) {
		final QSV qsv = new QSV(path);
		CharSequence info;
    try {
      info = qsv.getInfo();
    } catch (JSONException e) {
      info = null;
    }
		DialogInterface.OnClickListener click = new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface p1, int p2) {
				switch (p2) {
					case DialogInterface.BUTTON_POSITIVE:
						convert(qsv);
						return;
					case DialogInterface.BUTTON_NEUTRAL:
						open(qsv);
						break;
					default:
						break;
				}
				qsv.close();
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this).
			setTitle(qsv.getTitle()).
			setMessage(info).
			setPositiveButton(qsv.isConverted() ?"重新转换": "开始转换", click).
			setNegativeButton("关闭", click);
		if (qsv.isConverted())
			builder.setNeutralButton("打开", click);
		builder.show();
	}
	private void open(QSV qsv) {
		qsv.play(this);
	}
	private void convert(final QSV qsv) {
		View v = getLayoutInflater().
			inflate(R.layout.entry, null);
		v.<TextView>findViewById(R.id.title).
			setText(qsv.getTitle());
		final AlertDialog dialog = new AlertDialog.Builder(this).
			setTitle("转换中……").
			setView(v).
			setCancelable(false).
			show();
		ConvertTask task = new ConvertTask(v, new ConvertTask.Callback(){
				@Override
				public void call() {
					finish(dialog, qsv);
				}
			});
		task.execute(qsv);
	}
	public void finish(AlertDialog dialog, final QSV qsv) {
		dialog.dismiss();
		qsv.close();
		new AlertDialog.Builder(this).
			setTitle("已完成").
			setMessage("输出文件为：" + qsv.getOutFile()).
			setPositiveButton("确定", null).
			setNeutralButton("打开", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface p1, int p2) {
					qsv.play(MainActivity.this);
				}
			}).
			show();
	}
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == hashCode() && grantResults[0] == 0)
			init();
		else
			error();
	}

	private void error() {
		new AlertDialog.Builder(this).
			setTitle("权限不足").
			setMessage("无法读取存储空间！").
			setPositiveButton("退出", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface p1, int p2) {
					finish();
				}
			}).
			setCancelable(false).
			show();
	}

	private long last = 0;

	@Override
	public void onBackPressed() {
		if (isConvert())return;
		if (adapter.goBack())return;
		long time = System.currentTimeMillis();
		if (time - last > 2000) {
			last = time;
			Toast.makeText(this, "再按一次返回键退出", 0).show();
			return;
		}
		super.onBackPressed();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().
			inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onDestroy() {
		adapter.reset();
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
			case R.id.about:
				about();
				break;
			case R.id.refresh:
				if (isConvert())
					break;
				adapter.refresh();
				break;
			case R.id.convert_all:
				adapter.convertAll(items);
				break;
			case R.id.exit:
				if (isConvert())
					break;
				finish();
				break;
			case R.id.storage:
				selectStorage();
				break;
			case R.id.clear:
				clear();
				break;
			case R.id.threads:
				setThreads();
				break;
			case R.id.file:
				fileDialog.show();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void setThreads() {
		View v = getLayoutInflater().
			inflate(R.layout.threads, null);
		SharedPreferences sp = getSharedPreferences("path", Context.MODE_PRIVATE);
		int count = sp.getInt("threads", 2);
		final EditText threads = v.findViewById(R.id.threadsCount);
		threads.setText(count + "");
		new AlertDialog.Builder(this).
			setTitle("同时转换视频数").
			setView(v).
			setPositiveButton("确定", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface p1, int p2) {
					int count = Integer.parseInt(threads.getText().toString());
					Threads.setThreadsCount(MainActivity.this, count);
				}
			}).
			setNegativeButton("取消", null).
			show();
	}
	private void clear() {
		new AlertDialog.Builder(this).
			setTitle("清理输出目录").
			setMessage("执行此操作会删除当前存储位置下所有已转换是视频，是否继续？").
			setPositiveButton("是", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface p1, int p2) {
					Storage.clear();
					Toast.makeText(MainActivity.this, "清理完成", 0).show();
				}
			}).
			setNegativeButton("否", null).
			show();
	}
	private void about() {
		StringBuilder sb = new StringBuilder();
		help(sb);
		new AlertDialog.Builder(this).
			setTitle("关于").
			setMessage(sb).
			setPositiveButton("确定", null).
			show();

	}
	private void selectStorage() {
		final File[] dirs = getExternalFilesDirs(Storage.CONVERTED);
		final CharSequence[] items = new CharSequence[dirs.length];
		String c = Storage.getOutDir();
		int id = 0;
		for (int i=0;i < dirs.length;i++) {
			String p=dirs[i].getAbsolutePath();
			if (c.equals(p))
				id = i;
			if (p.startsWith("/storage/emulated"))
				items[i] = "内置存储"; 
			else
				items[i] = "SD卡";
		}
		final int idx = id;
		DialogInterface.OnClickListener l = new DialogInterface.OnClickListener(){
			int id = idx;
			@Override
			public void onClick(DialogInterface p1, int p2) {
				switch (p2) {
					case DialogInterface.BUTTON_POSITIVE:
						Storage.setOutDir(MainActivity.this, dirs[id]);
						adapter.refresh();
						break;
					default:
						if (p2 >= 0)
							id = p2;
						break;
				}
			}
		};
		new AlertDialog.Builder(this).
			setTitle("选择存储位置").
			setSingleChoiceItems(items, id, l).
			setPositiveButton("确定", l).
			show();
	}
	private void help(StringBuilder sb) {
		sb.append("本软件的用途是将爱奇艺的qsv文件转换为通用的flv或ts文件。");
		sb.append("\n\n本软件可以通过以下两种方式使用：");
		sb.append("\n1、手机爱奇艺下载的视频：由于手机版爱奇艺下载是视频存放位置是固定的");
		sb.append("（内置存储或内存卡/Android/data/com.qiyi.video/files/app/download/video），");
		sb.append("本软件直接打开后会读取此目录下所有已下载视频，点击可查看详情并开始转换。");
		sb.append("\n2、从其他设备复制的qsv文件：可在文件管理器中找到并选择使用QSV2FLV打开");
		sb.append("\n注1：本软件转换后的flv或ts仅供个人播放使用，如用作其他用途");
		sb.append("（包括但不限于分享给他人、上传到其他网站）本人概不负责。");
		sb.append("\n注2：本软件输出文件夹为：内置存储或内存卡/Android/data/com.a4455jkjh.qsv2flv/files/converted。");
		sb.append("\n\n开发者：4455jkjh");
		sb.append("\nQQ：375411971");
	}
	private boolean isConvert() {
		if (ConvertTask.count > 0) {
			Toast.makeText(this, "请等待转换完成！", 0).show();
			return true;
		}
		return false;
	}

	@Override
	public void setTitle(CharSequence title) {
		invalidateOptionsMenu();
		super.setTitle(title);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem all = menu.findItem(R.id.convert_all);
		QsvAdapter a = adapter;
		all.setEnabled(a == null ? false : a.inChild);
		if (Build.VERSION.SDK_INT < 19)
			menu.findItem(R.id.storage).
				setVisible(false);
		return super.onPrepareOptionsMenu(menu);
	}

}
