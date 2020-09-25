package com.a4455jkjh.qsv2flv;
import android.view.View;
import java.util.Comparator;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class QsvGroup implements QsvAdapter.QsvItem {
	private final CharSequence title;
	private final List<QsvAdapter.QsvItem> qsv;

	public QsvGroup(CharSequence title) {
		this.title = title;
		qsv = new ArrayList<>(100);
	}
	
	@Override
	public void convert(boolean force) {
		// TODO: Implement this method
	}

	public void sort(Comparator<QsvAdapter.QsvItem> comparator) {
		Collections.sort(qsv, comparator);
	}

	public void add(Properties p) {
		qsv.add(new QSV(p,title));
	}
	public List<QsvAdapter.QsvItem> getSubItems() {
		return qsv;
	}

	@Override
	public CharSequence getTitle() {
		return title;
	}

	@Override
	public boolean click() {
		return false;
	}

	@Override
	public void close() {
		for (QsvAdapter.QsvItem item:qsv)
			item.close();
		qsv.clear();
	}

}
