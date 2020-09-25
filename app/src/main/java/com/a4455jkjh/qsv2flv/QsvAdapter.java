package com.a4455jkjh.qsv2flv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.widget.ListView;

public class QsvAdapter extends BaseAdapter implements AdapterView.OnItemClickListener,Comparator<QsvAdapter.QsvItem> {
	private final Callback callback;
	private final List<QsvItem> items;
	private final List<QsvGroup> groups;
	public boolean inChild;

	public QsvAdapter(Callback callback) {
		this.callback = callback;
		items = new ArrayList<>(100);
		groups = new ArrayList<>(100);
		refresh();
	}

	public void convertAll(ListView content) {
		List<QsvItem> items = this.items;
		int size = items.size();
		for (int i = 0; i < size; i++) {
			items.get(i).convert(false);
		}
	}
	public void refresh() {
		reset();
		File data = Storage.getDataDir();
		add(new File(data, "com.qiyi.video/files/app/download/video"));
		add(new File(data, "com.qiyi.video.pad/files/app/download/video"));
		items.addAll(groups);
		notifyDataSetChanged();
		for (QsvGroup group:groups)
			group.sort(this);
	}
	private void add(File downloadDir) {
		if (!(downloadDir.exists()&&downloadDir.canRead()))
			return;
		for (File f:downloadDir.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					File f = new File(dir, String.format("%s/%s.qsv", name, name));
					File f2 = new File(dir, String.format("%s/%s.qiyicfg", name, name));
					return f.exists() && f2.exists();
				}
			})) {
			try {
				String name = f.getName();
				Reader cfg = new FileReader(f + "/" + name + ".qiyicfg");
				Properties p = new Properties();
				p.load(cfg);
				Object clm = p.get("clm");
				String k;
				if (clm == null || clm.equals("")) {
					k = "其他";
					p.put("clm", k);
				} else
					k = clm.toString();
				QsvGroup group = findGroup(k);
				group.add(p);
			} catch (IOException e) {
				//empty
			}
		}
	}

	private QsvGroup findGroup(String clm) {
		for (QsvGroup group :groups) {
			if (group.getTitle().equals(clm))
				return group;
		}
		QsvGroup group = new QsvGroup(clm);
		groups.add(group);
		return group;
	}
	public void reset() {
		for (QsvItem item:groups)
			item.close();
		items.clear();
		groups.clear();
		inChild = false;
	}
	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Object getItem(int p1) {
		return items.get(p1);
	}

	@Override
	public long getItemId(int p1) {
		return p1;
	}

	@Override
	public View getView(int index, View view, ViewGroup parent) {
		if (view == null)
			view = callback.getLayoutInflater().
				inflate(R.layout.entry, parent, false);
		TextView title = view.findViewById(R.id.title);
		TextView state = view.findViewById(R.id.state);
		QsvItem item = items.get(index);
		if(item instanceof QSV){
		  ((QSV)item).setView(view);
		}
		title.setText(item.getTitle());
		if (item instanceof QsvGroup)
			state.setVisibility(View.GONE);
		else {
			state.setVisibility(View.VISIBLE);
			state.setText(((QSV)item).getState());
		}
		return view;
	}

	@Override
	public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
		QsvItem item = items.get(p3);
		if (item.click())
			return;
		callback.setTitle(item.getTitle());
		inChild = true;
		items.clear();
		items.addAll(((QsvGroup)item).getSubItems());
		notifyDataSetChanged();
	}
	public boolean goBack() {
		if (inChild) {
			inChild = false;
			items.clear();
			items.addAll(groups);
			callback.setTitle("QSV转换工具");
			notifyDataSetChanged();
			return true;
		}
		return false;
	}
	@Override
	public int compare(QsvItem p1, QsvItem p2) {
		String text1 = p1.getTitle().toString();
		String text2 = p2.getTitle().toString();
		Pattern p = Pattern.compile("第(\\d+)集");
		Matcher m1 = p.matcher(text1);
		Matcher m2 = p.matcher(text2);
		if (m1.find() && m2.find()) {
			int i1 = Integer.parseInt(m1.group(1));
			int i2 = Integer.parseInt(m2.group(1));
			return i1 - i2;
		}
		return text1.compareTo(text2);
	}
	public interface Callback {
		LayoutInflater getLayoutInflater();
		void setTitle(CharSequence s);
	}
	public interface QsvItem {
		CharSequence getTitle();
		boolean click();
		void close();
		void convert(boolean force);
	}
}
