package com.a4455jkjh.qsv2flv;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import android.widget.Toast;

public class Storage {
	private static String outDir;
int i;
	private static void rm(File a) {
		if (a.isDirectory()) {
			for (File f:a.listFiles())
				rm(f);
		}
		a.delete();
	}
	public static void clear() {
		File a = new File(outDir);
		if (a.exists())
			rm(a);
		a.mkdirs();
	}

	public static void setOutDir(Context ctx,File outDirFile) {
		String outDir = outDirFile.getAbsolutePath();
		Storage.outDir = outDir;
		SharedPreferences.Editor editor = ctx.getSharedPreferences("path", Context.MODE_PRIVATE).edit();
		editor.putString("out_dir", outDir);
		editor.commit();
	}
	public static File getDataDir(){
		File converted = new File(outDir);
		return converted.getParentFile().getParentFile().getParentFile();
	}
	public static String getOutDir() {
		return outDir;
	}
	public static void init(Context ctx) {
		SharedPreferences sp = ctx.getSharedPreferences("path", Context.MODE_PRIVATE);
		outDir =sp.getString("out_dir", ctx.getExternalFilesDir(CONVERTED).getAbsolutePath());
	}
	public static final String CONVERTED = "converted";
}
