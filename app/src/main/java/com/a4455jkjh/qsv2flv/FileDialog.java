package com.a4455jkjh.qsv2flv;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.a4455jkjh.qsv2flv.R;
import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileDialog extends Dialog implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
	private ListView files;
	protected TextView path;
	private final MainActivity act;
	private final FilesAdapter adapter;
  private boolean showHidden;
	public FileDialog(MainActivity context) {
		super(context);
		act = context;
		adapter = new FilesAdapter(getLayoutInflater());
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.file);
		setCancelable(false);
		files = findViewById(R.id.files);
		path = findViewById(R.id.path);
    CheckBox showHidden = findViewById(R.id.showHidden);
    Context ctx = getContext();
    SharedPreferences perf = PreferenceManager.getDefaultSharedPreferences(ctx);
    boolean hidden = perf.getBoolean("hidden", false);
    showHidden.setOnCheckedChangeListener(this);
		findViewById(R.id.back).
			setOnClickListener(this);
		findViewById(R.id.hide).
			setOnClickListener(this);
		adapter.bind(files);
		adapter.init(savedInstanceState);
    showHidden.setChecked(hidden);
    this.showHidden = hidden;
	}

  @Override
  public void onCheckedChanged(CompoundButton widget, boolean value) {
    Context ctx = getContext();
    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
    editor.putBoolean("hidden", value);
    editor.commit();
    this.showHidden = value;
    adapter.refresh();
  }

	@Override
	public void onClick(View p1) {
		switch (p1.getId()) {
			case R.id.back:
				if (adapter.goBack())
					return;
				dismiss();
				break;
			case R.id.hide:
				dismiss();
				break;
		}
	}

	private class FilesAdapter extends AbstractAdapter<File> {
		private File curDir;
		FilesAdapter(LayoutInflater inflater) {
			super(inflater);
		}


		public boolean goBack() {
			if (curDir.equals(rootDir))
				return false;
			refresh(curDir.getParentFile());
			return true;
		}
		void init(Bundle savedInstanceState) {
			File rootDir = FileDialog.rootDir;
			if (savedInstanceState != null) {
				String cur_dir = savedInstanceState.getString("CUR_DIR_PATH", rootDir.getAbsolutePath());
				refresh(new File(cur_dir));
			} else {
				refresh(rootDir);
			}
		}
		void save(Bundle outState) {
			outState.putString("CUR_DIR_PATH", curDir.getAbsolutePath());
		}
		private void refresh(File f) {
			path.setText(f.getAbsolutePath());
			curDir = f;
			refresh();
		}
		@Override
		protected int getLayoutId() {
			return R.layout.file_entry;
		}

		@Override
		protected void click(File file, View itemView) {
			if (file.isDirectory()) {
				refresh(file);
				return;
			}
			act.open(file.getAbsolutePath());
		}

		@Override
		protected void bindView(View view, File file) {
			TextView name = view.findViewById(R.id.name);
			ImageView icon = view.findViewById(R.id.icon);
			name.setText(file.getName());
			if (file.isDirectory())
				icon.setImageResource(R.drawable.folder);
			else
				icon.setImageResource(R.drawable.file);
		}

		@Override
		protected void addData(List<File> lists) {
      if (curDir.canRead())
        for (File f : curDir.listFiles(nameFilter)) {
          lists.add(f);
        }
		}

		@Override
		protected void sort(List<File> lists) {
			Collections.sort(lists, comp);
		}
	}
	private FileFilter nameFilter = new FileFilter(){
		@Override
		public boolean accept(File p1) {
			String name = p1.getName().toLowerCase();
      if ((!showHidden) && name.charAt(0) == '.') {
        return false;
      }
      return p1.isDirectory() ||
				name.endsWith(".qsv");
		}
	};

	private static final Comparator<File> comp = new Comparator<File>(){
		@Override
		public int compare(File f1, File f2) {
			if (f1.isDirectory() && f2.isFile())
				return -1;
			if (f2.isDirectory() && f1.isFile())
				return 1;
			return f1.getName().toLowerCase().
				compareTo(f2.getName().toLowerCase());
		}
	};
	private static final File rootDir = Environment.getExternalStorageDirectory();
}
