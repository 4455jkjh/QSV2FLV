package com.a4455jkjh.qsv2flv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAdapter<T> extends BaseAdapter implements AdapterView.OnItemClickListener {
  private final List<T> lists;

  private final LayoutInflater inflater;
  protected AbstractAdapter(LayoutInflater inflater) {
    this.inflater = inflater;
    lists = new ArrayList<>();
  }
  @Override
  public int getCount() {
    return lists.size();
  }

  @Override
  public T getItem(int p1) {
    return lists.get(p1);
  }

  @Override
  public long getItemId(int p1) {
    return p1;
  }
  public void bind(ListView list) {
    list.setAdapter(this);
    list.setOnItemClickListener(this);
  }

  @Override
  public View getView(int position, View view, ViewGroup parent) {
    if (view == null)
      view = inflater.inflate(getLayoutId(), parent, false);
    bindView(view, lists.get(position));
    return view;
  }
  public void refresh() {
    lists.clear();
    addData(lists);
    sort(lists);
    notifyDataSetChanged();
  }

  @Override
  public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
    click(lists.get(p3), p2);
  }


  protected void sort(List<T> lists) {}

  protected abstract int getLayoutId();
  protected abstract void bindView(View view, T item);
  protected abstract void addData(List<T> lists);
  protected abstract void click(T item, View itemView);
}
