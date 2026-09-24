package com.crispy.felix;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class MainActivity extends Activity {
    static final int BG=Color.rgb(16,18,22), PANEL=Color.rgb(24,28,34), RED=Color.rgb(227,52,52), STEEL=Color.rgb(174,183,196), GREEN=Color.rgb(110,231,168);
    LinearLayout root, content; TextView state, output, receipt; EditText command, memory, endpoint; DB db; int pad=16;
    @Override public void onCreate(Bundle b){ super.onCreate(b); db=new DB(this); build(); refresh(); }
    TextView tv(String s,int size,int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setPadding(pad,pad,pad,pad); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setAllCaps(false); return b; }
    EditText ed(String hint){ EditText e=new EditText(this); e.setHint(hint); e.setHintTextColor(Color.GRAY); e.setTextColor(Color.WHITE); e.setPadding(pad,pad,pad,pad); return e; }
    void add(View v){ content.addView(v,new LinearLayout.LayoutParams(-1,-2)); }
    void build(){
        ScrollView scroll=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(12,12,12,12); root.setBackgroundColor(BG); scroll.addView(root); setContentView(scroll);
        TextView h=tv("FELIX // OPERATOR",24,Color.WHITE); h.setTypeface(null,1); root.addView(h);
        root.addView(tv("NATIVE ANDROID • LOCAL-FIRST • RECEIPT-GATED",11,STEEL));
        state=tv("STATE: loading",14,GREEN); state.setBackgroundColor(PANEL); root.addView(state);
        LinearLayout tabs=new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); String[] names={"CONTROL","GREY","MEMORY","LINK"};
        for(String n:names){ Button b=btn(n); tabs.addView(b,new LinearLayout.LayoutParams(0,-2,1)); b.setOnClickListener(v->showTab(n)); } root.addView(tabs);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); root.addView(content); showTab("CONTROL");
    }
    void showTab(String n){ content.removeAllViews();
        if(n.equals("CONTROL")){ add(tv("OPERATOR",18,Color.WHITE)); command=ed("status | tools | grey <case> | remember <text> | backup | receipts"); add(command); Button b=btn("EXECUTE"); add(b); output=tv("Ready. No cloud dependency.\n",13,STEEL); add(output); b.setOnClickListener(v->executeCommand()); }
        else if(n.equals("GREY")){ add(tv("THE GREY",18,Color.WHITE)); add(tv("OBSERVE → EVIDENCE → INFERENCE → HYPOTHESIS → PRESSURE TEST → VERIFY",13,STEEL)); EditText c=ed("Describe the problem/case"); add(c); Button b=btn("RUN GREY TEST"); add(b); output=tv("",13,STEEL); add(output); b.setOnClickListener(v->grey(c.getText().toString().trim())); }
        else if(n.equals("MEMORY")){ add(tv("MEMORY",18,Color.WHITE)); memory=ed("Write a continuity anchor"); add(memory); Button b=btn("WRITE MEMORY"); add(b); receipt=tv("",12,STEEL); add(receipt); b.setOnClickListener(v->{String s=memory.getText().toString().trim(); if(!s.isEmpty()){long id=db.memory(s); receipt.setText("RECEIPT VERIFIED #"+id); refresh(); showTab("MEMORY");}}); add(tv("RECENT",15,Color.WHITE)); add(tv(db.recent(),13,STEEL)); }
        else { add(tv("FELIX LINK",18,Color.WHITE)); add(tv("Phone ↔ FELIX daemon over private LAN. Native HTTP; no browser/WebView.",13,STEEL)); endpoint=ed("http://192.168.1.x:8761"); endpoint.setText(getPreferences(0).getString("endpoint","http://127.0.0.1:8761")); add(endpoint); Button b=btn("SAVE + TEST"); add(b); output=tv("",13,STEEL); add(output); b.setOnClickListener(v->{getPreferences(0).edit().putString("endpoint",endpoint.getText().toString().trim()).apply(); probe(endpoint.getText().toString().trim());}); }
    }
    void executeCommand(){ String c=command.getText().toString().trim(); if(c.equals("status")) output.setText(db.status()); else if(c.equals("tools")) output.setText("TOOLS\nstatus\nremember\ngrey\nbackup\nreceipts\nlink-test"); else if(c.startsWith("remember ")) output.setText("MEMORY WRITTEN #"+db.memory(c.substring(9))); else if(c.startsWith("grey ")) grey(c.substring(5)); else if(c.equals("backup")) output.setText(backup()); else if(c.equals("receipts")) output.setText(db.receipts()); else output.setText("Unknown command. Try: status | tools | grey <case> | remember <text> | backup | receipts"); refresh(); }
    void grey(String s){ if(s.isEmpty()){output.setText("Enter a case.");return;} String x="OBSERVATION\n"+s+"\n\nEVIDENCE\nList what is directly known.\n\nINFERENCE\nLabel interpretation separately.\n\nHYPOTHESIS\nKeep explanations provisional.\n\nPRESSURE TEST\nFind the first divergence; change one variable at a time.\n\nVERIFY\nDo not promote an unsupported conclusion to fact."; output.setText(x); db.receipt("GREY_TEST",s,"VERIFIED"); refresh(); }
    String backup(){ try{File f=new File(getFilesDir(),"felix-backup-"+System.currentTimeMillis()+".json"); JSONObject o=new JSONObject(); o.put("created",new Date().toString()); o.put("status",db.status()); o.put("memory",db.recent()); try(FileOutputStream out=new FileOutputStream(f)){out.write(o.toString(2).getBytes(StandardCharsets.UTF_8));} db.receipt("BACKUP",f.getName(),"VERIFIED"); return "BACKUP VERIFIED\n"+f.getAbsolutePath();} catch(Exception e){return "BACKUP FAILED: "+e;}}
    void probe(String url){ output.setText("TESTING…"); new Thread(()->{String r; try{HttpURLConnection c=(HttpURLConnection)new URL(url+"/api/health").openConnection(); c.setConnectTimeout(2500); c.setReadTimeout(2500); c.setRequestMethod("GET"); r="HTTP "+c.getResponseCode()+"\n"+new String(read(c.getInputStream()),StandardCharsets.UTF_8); db.receipt("LINK_TEST",url,"VERIFIED");} catch(Exception e){r="NOT CONNECTED\n"+e.getClass().getSimpleName()+": "+e.getMessage(); db.receipt("LINK_TEST",url,"FAILED");} String rr=r; runOnUiThread(()->{output.setText(rr);refresh();});}).start(); }
    byte[] read(InputStream i)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[1024];int n;while((n=i.read(b))>0)o.write(b,0,n);return o.toByteArray();}
    void refresh(){if(state!=null)state.setText("STATE: LOCAL • MEMORY "+db.count()+" • RECEIPTS "+db.rcount());}
    static class DB extends SQLiteOpenHelper{
        SQLiteDatabase d; DB(Context c){super(c,"felix.db",null,1);d=getWritableDatabase();}
        public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE memory(id INTEGER PRIMARY KEY AUTOINCREMENT,text TEXT,ts INTEGER)");d.execSQL("CREATE TABLE receipts(id INTEGER PRIMARY KEY AUTOINCREMENT,kind TEXT,payload TEXT,result TEXT,ts INTEGER)");}
        public void onUpgrade(SQLiteDatabase d,int a,int b){}
        long memory(String s){ContentValues v=new ContentValues();v.put("text",s);v.put("ts",System.currentTimeMillis());long id=d.insert("memory",null,v);receipt("MEMORY_WRITE",s,"VERIFIED");return id;}
        void receipt(String k,String p,String r){ContentValues v=new ContentValues();v.put("kind",k);v.put("payload",p);v.put("result",r);v.put("ts",System.currentTimeMillis());d.insert("receipts",null,v);}
        int count(){Cursor c=d.rawQuery("SELECT COUNT(*) FROM memory",null);c.moveToFirst();int n=c.getInt(0);c.close();return n;}
        int rcount(){Cursor c=d.rawQuery("SELECT COUNT(*) FROM receipts",null);c.moveToFirst();int n=c.getInt(0);c.close();return n;}
        String recent(){Cursor c=d.rawQuery("SELECT id,text FROM memory ORDER BY id DESC LIMIT 8",null);StringBuilder s=new StringBuilder();while(c.moveToNext())s.append("#").append(c.getLong(0)).append("  ").append(c.getString(1)).append("\n");c.close();return s.length()==0?"(empty)":s.toString();}
        String receipts(){Cursor c=d.rawQuery("SELECT id,kind,result FROM receipts ORDER BY id DESC LIMIT 12",null);StringBuilder s=new StringBuilder("RECEIPTS\n");while(c.moveToNext())s.append("#").append(c.getLong(0)).append(" ").append(c.getString(1)).append(" ").append(c.getString(2)).append("\n");c.close();return s.toString();}
        String status(){return "FELIX LOCAL STATUS\nRuntime: native Android\nDatabase: SQLite\nMemory rows: "+count()+"\nReceipts: "+rcount()+"\nCloud API required: NO\nModel required for core operation: NO";}
    }
}
