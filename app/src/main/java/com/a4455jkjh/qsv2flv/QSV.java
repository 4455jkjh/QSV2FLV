package com.a4455jkjh.qsv2flv;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import fr.noop.subtitle.srt.SrtObject;
import fr.noop.subtitle.srt.SrtParser;
import fr.noop.subtitle.srt.SrtWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class QSV implements QsvAdapter.QsvItem {
  private final CharSequence title;
  private final String outFile,qsv;
  private long header;
  private String out_file = "unknown";
  private long size;
  private int type;
  private CharSequence info = null;
  public boolean isConverting;
  private String out_srt;
  public byte[] srt;


  private int len;
  public QSV(String qsv) {
    this.qsv = qsv;
    int s = qsv.lastIndexOf('/');
    int e = qsv.lastIndexOf('.');
    title = qsv.subSequence(s + 1, e);
    outFile = String.format("%s/%s", Storage.getOutDir(), title);
    init();
  }
  public QSV(Properties p, CharSequence parent) {
    title = getName(p, parent);
    qsv = String.format("%s/%s", p.get("downloadFileDir"), p.get("fileName"));
    outFile = String.format("%s/%s/%s", Storage.getOutDir(), parent, title);
    init();
  }

  public void play(Context ctx) {
    play(ctx, Uri.parse("file://"+out_file));
  }

  public void setView(View view) {
    this.view = view;
  }

  public View getView() {
    return view;
  }

  private void init() {
    isConverting = false;
    size = native_init(qsv);
    srt = getSrt();
  }

  private byte[] getSrt() {
    byte[] array = readInfo(qsv, header);
    String info = new String(array, 8, array.length - 8);
    try {
      JSONObject qsv_info = new JSONObject(info).getJSONObject("qsv_info");
      JSONArray sub_srt = qsv_info.getJSONArray("sub_srt");
      if (sub_srt != null) {
        String srt = sub_srt.getJSONObject(0).getString("data");
        byte[] decode = Base64.decode(srt, Base64.NO_WRAP);
        return decode;
      }
    } catch (JSONException e) {
    }
    return null;
  }

  public void showInfo(Context context) throws JSONException {
    View v = LayoutInflater.from(context).
      inflate(R.layout.dialog, null);
    TextView t = v.findViewById(R.id.message);
    t.setMovementMethod(LinkMovementMethod.getInstance());
    t.setText(getInfo());

    t.setTextAppearance(android.R.attr.textAppearanceLarge);
    new AlertDialog.Builder(context).
      setTitle("QSV信息").
      setView(v).
      setPositiveButton("确定", null).
      show();
  }
  public CharSequence getInfo() throws JSONException {
    CharSequence i = this.info;
    if (i == null) {
      byte[] info = readInfo(qsv, header);
      String s = new String(info, 8, info.length - 8);
      i = read(s, size, out_file);
      this.info = i;
    }
    return i;
  }

  private static CharSequence getName(Properties p, CharSequence parent) {
    String name = p.get("text").toString();
    String group = parent.toString();
    if (!group.equals("其他") && name.startsWith(group)) {
      int e = group.length();
      name = String.format("%s %s", name.substring(e), p.get("subTitle"));
    }
    return name;
  }
  @Override
  public CharSequence getTitle() {
    return title;
  }
  private View view;

  @Override
  public void convert(boolean force) {
    if (isConverted() && !force)
      return;
    if (view == null)
      return;
    ConvertTask task = new ConvertTask(view, null);
    task.execute(this);
  }

  private class Item implements AdapterView.OnItemClickListener {
    Context ctx;
    View view;
    ListPopupWindow pop;
    public Item(Context ctx, View view, ListPopupWindow pop) {
      this.ctx = ctx;
      this.view = view;
      this.pop = pop;
    }
    @Override
    public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
      Context context = p2.getContext();
      switch (p3) {
        case 0:

          try {
            showInfo(context);
          } catch (JSONException e) {}

          break;
        case 1:
          convert(true);
          break;
        case 2:
          play(ctx);
          break;
      }
      if (pop != null)
        pop.dismiss();
    }
  }
  @Override
  public boolean click() {
    if (isConverting) {
      Toast.makeText(view.getContext(), "正在转换中……", 0).show();
      return true;
    }
    final Context ctx = view.getContext();
    String[] text = ctx.getResources().getStringArray(isConverted() ?R.array.converted: R.array.unconverted);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, text);
    final ListPopupWindow pop = new ListPopupWindow(ctx);
    pop.setAnchorView(view);
    pop.setModal(true);
    pop.setAdapter(adapter);
    int w = view.getWidth() >> 1;
    pop.setWidth(w);
    pop.setOnItemClickListener(new Item(ctx, view, pop));
    pop.show();
    return true;
  }
  public boolean convert(ConvertTask task) {
    isConverting = true;
    task.setMax(len);
    File dir = new File(out_file).getParentFile();
    if (!dir.exists())
      if (!dir.mkdirs()) {
        task.showMessage("无法创建输出目录！");
        return false;
      }
    byte[] srt_array = this.srt;
    if (srt_array != null) {
      try {
        OutputStream o = new FileOutputStream(out_srt);
        ByteArrayInputStream baos = new ByteArrayInputStream(srt_array);
        SrtParser p = new SrtParser("utf-8");
        SrtObject srt = p.parse(baos);
        SrtWriter w = new SrtWriter("utf-8");
        w.write(srt, o);
        o.close();
      } catch (Exception e) {}
    }
    boolean b = convert(qsv, type, header, task);
    isConverting = false;
    return b;
  }
  @Override
  public void close() {
    native_close(header);
  }
  public boolean isConverted() {
    return new File(out_file).exists();
  }
  public String getState() {
    if (isConverted())
      return "已转换";
    return "未转换";
  }
  protected void setType(int type) {
    this.type = type;
    switch (type) {
      case 1:
        out_file = outFile + ".flv";
        break;
      case 2:
        out_file = outFile + ".ts";
        break;
    }
    out_srt = outFile + ".srt";
  }
  protected native long native_init(String qsv);
  protected native boolean convert(String qsv, int type, long header, ConvertTask task);
  protected static native void native_close(long header);
  protected static native byte[] readInfo(String qsv, long header);
  static{
    System.loadLibrary("convert");
  }
  private static CharSequence read(String info, long size, String out_file) throws JSONException {
    SpannableStringBuilder sb = new SpannableStringBuilder();
    JSONObject qsv_info = new JSONObject(info).getJSONObject("qsv_info");
    JSONObject video_info = new JSONObject(qsv_info.getString("vi"));
    sb.append("标题：").append(getString(video_info, "shortTitle"));
    sb.append("\n地区：").append(getString(video_info, "ar"));
    sb.append("\n时长：").append(time(qsv_info));
    sb.append("\n下载日期：").append(getString(video_info, "up"));
    sb.append("\n看点：").append(getString(video_info, "tvFocuse"));
    sb.append("\n简述：").append(getString(video_info, "subt"));
    sb.append("\n演职员：").append(getString(video_info, "ma"));
    sb.append("\n标签：").append(getString(video_info, "tg"));
    String url = getString(video_info, "vu");
    URLSpan span = new URLSpan(url);
    SpannableString vu = new SpannableString(url);
    vu.setSpan(span, 0, url.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    sb.append("\n在线播放地址：").append(vu);
    sb.append("\n详情：").append(getString(video_info, "info"));
    find_subtitle(sb, qsv_info);
    sb.append("\n文件大小：").append(toSize(size, 0, 0));
    FileSpan of = new FileSpan("file://" + out_file);
    int l=sb.length();
    sb.append("\n目标文件：").append(out_file);
    sb.setSpan(of, l + 6, sb.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    return sb;
  }
  
  private static void find_subtitle(SpannableStringBuilder sb, JSONObject qsv_info) {
    try {
      JSONArray sub = qsv_info.getJSONArray("sub_srt");
      sb.append("\nsrt字幕：");
      sb.append(sub == null ?"无": "有");
      sub = qsv_info.getJSONArray("sub_webvtt");
      sb.append("\nwebvtt字幕：");
      sb.append(sub == null ?"无": "有");
    } catch (JSONException e) {
      sb.append("\n无字幕");
    }
  }
  private static String getString(JSONObject json, String key) {
    try {
      return json.getString(key);
    } catch (JSONException e) {
      return "N/A";
    }
  }
  private static CharSequence time(JSONObject qsv_info) {
    try {
      JSONArray array = qsv_info.getJSONObject("vd").getJSONObject("seg").getJSONArray("duration");
      int time = 0;
      int l = array.length();
      for (int i=0;i < l;i++)
        time += Integer.parseInt(array.getString(i));
      int ms = time % 1000;
      time /= 1000;
      int s = time % 60;
      time /= 60;
      int m = time % 60;
      int h = time / 60;
      return String.format("%d:%02d:%02d.%d", h, m, s, ms);
    } catch (JSONException e) {
      return "N/A";
    }
  }
  private static final String[] n = {"B", "KB", "MB", "GB", "TB"};
  public static final String toSize(long size, long dec, int depth) {
    if (size > 1024) {
      return toSize(size / 1024, size % 1024, depth + 1);
    }
    double d = size + dec / 1024.0;
    return String.format("%.02f%s", d, n[depth]);
  }
  private static void play(Context context, Uri data) {
    String mime;
    String str = data.getPath();
    if (str.endsWith(".ts"))
      mime = "video/mp2ts";
    else if (str.endsWith(".flv"))
      mime = "video/x-flv";
    else
      mime = "video/xxx";
    Intent intent = new Intent();
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    intent.setDataAndType(data, mime);
    context.startActivity(intent);
  }
  protected void setHeader(long header, int len) {
    this.header = header;
    this.len = len;
  }
  protected void showMessage(ConvertTask task, int cur, int total) {
    String msg = String.format("正在处理第%d/%d段……", cur, total);
    task.showMessage(msg);
  }
  protected void showProgress(ConvertTask task, int progress) {
    Log.i("QSV2FLV", String.format("cur:%d size:%d", progress, size));
    task.showProgress(progress);
  }
  protected String getOutFile() {
    return out_file;
  }
  static class FileSpan extends ClickableSpan {
    String uri;

    public FileSpan(String uri) {
      this.uri = uri;
    }
    public int getSpanTypeId() {
      return 0;
    }
    @Override
    public void onClick(View widget) {
      Uri uri = Uri.parse(this.uri);
      Context context = widget.getContext();
      play(context, uri);
    }
    
    
  }
}
