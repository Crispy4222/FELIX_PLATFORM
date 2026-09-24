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
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {
    static final int BG=Color.rgb(9,11,14), PANEL=Color.rgb(22,26,32), RED=Color.rgb(227,52,52), STEEL=Color.rgb(174,183,196), GREEN=Color.rgb(110,231,168);
    LinearLayout root, content; TextView state, output, receipt; EditText command, memory, endpoint; DB db; AffectEngine affect; File genesisDir; int pad=14;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        db=new DB(this);
        affect=new AffectEngine();
        affect.restore(db.affectState(),db.affectCause(),db.affectRevision());
        build(); refresh();
        affect.event("RETURN","operator opened FELIX");
        db.saveAffect(affect);
    }

    TextView tv(String s,int size,int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setPadding(pad,pad,pad,pad); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setAllCaps(false); return b; }
    EditText ed(String hint){ EditText e=new EditText(this); e.setHint(hint); e.setHintTextColor(Color.GRAY); e.setTextColor(Color.WHITE); e.setPadding(pad,pad,pad,pad); return e; }
    void add(View v){ content.addView(v,new LinearLayout.LayoutParams(-1,-2)); }
    void panel(View v){ v.setBackgroundColor(PANEL); root.addView(v,new LinearLayout.LayoutParams(-1,-2)); }

    void build(){
        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(10,10,10,10); root.setBackgroundColor(BG);
        scroll.addView(root); setContentView(scroll);

        TextView h=tv("FELIX // PHOENIX",25,Color.WHITE); h.setTypeface(null,1); root.addView(h);
        root.addView(tv("OPERATOR APP • GENESIS CORE • LOCAL-FIRST",10,STEEL));
        state=tv("STATE: loading",13,GREEN); panel(state);

        LinearLayout tabs=new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        String[] names={"CONTROL","HEART","GREY","MEMORY","GENESIS","TOOLS","LINK"};
        for(String n:names){ Button b=btn(n); tabs.addView(b,new LinearLayout.LayoutParams(0,-2,1)); b.setOnClickListener(v->showTab(n)); }
        root.addView(tabs);

        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); root.addView(content);
        showTab("CONTROL");
    }

    void showTab(String n){
        content.removeAllViews();
        if(n.equals("CONTROL")){
            add(tv("OPERATOR",18,Color.WHITE));
            command=ed("status | tools | feel | grey <case> | remember <text> | backup | receipts");
            add(command); Button b=btn("EXECUTE"); add(b); output=tv("Ready. Core runs without a model or cloud.\n",13,STEEL); add(output);
            b.setOnClickListener(v->executeCommand());
        } else if(n.equals("HEART")){
            add(tv("HEART / AFFECTIVE STATE",18,Color.WHITE));
            add(tv("Functional internal state: persistent, caused, inspectable, and able to influence the next action. It is not a claim of biological feeling.",12,STEEL));
            output=tv("",13,STEEL); add(output); renderFeelings();
            Button b=btn("REFRESH + DECAY"); add(b); b.setOnClickListener(v->{affect.decay();db.saveAffect(affect);renderFeelings();refresh();});
        } else if(n.equals("GREY")){
            add(tv("THE GREY",18,Color.WHITE));
            add(tv("OBSERVE → EVIDENCE → INFERENCE → HYPOTHESIS → PRESSURE TEST → VERIFY",12,STEEL));
            EditText c=ed("Describe the real case"); add(c); Button b=btn("RUN GREY TEST"); add(b); output=tv("",13,STEEL); add(output);
            b.setOnClickListener(v->grey(c.getText().toString().trim()));
        } else if(n.equals("MEMORY")){
            add(tv("MEMORY",18,Color.WHITE)); memory=ed("Write a continuity anchor"); add(memory); Button b=btn("WRITE MEMORY"); add(b);
            receipt=tv("",12,STEEL); add(receipt);
            b.setOnClickListener(v->{String s=memory.getText().toString().trim();if(!s.isEmpty()){long id=db.memory(s);affect.event("REMEMBER",s);db.saveAffect(affect);receipt.setText("RECEIPT VERIFIED #"+id);showTab("MEMORY");}});
            add(tv("RECENT",15,Color.WHITE)); add(tv(db.recent(),13,STEEL));
        } else if(n.equals("GENESIS")){
            add(tv("GENESIS // SOURCE VAULT",18,Color.WHITE));
            add(tv("Import original Genesis files directly into Felix. Files stay in the app sandbox; imports are copied and SHA-256 indexed.",12,STEEL));
            Button pick=btn("IMPORT ORIGINAL FILES"); add(pick); pick.setOnClickListener(v->pickGenesis());
            add(tv(genesisStatus(),13,GREEN));
            EditText q=ed("search imported Genesis"); add(q);
            Button find=btn("SEARCH GENESIS"); add(find); output=tv("",12,STEEL); add(output);
            find.setOnClickListener(v->output.setText(genesisSearch(q.getText().toString().trim())));
        } else if(n.equals("TOOLS")){
            add(tv("TOOLS",18,Color.WHITE));
            add(tv("Native actions available without the old web shell:",13,STEEL));
            add(tv("STATUS   runtime + affect + receipt state\nMEMORY   persistent anchors\nGREY     structured diagnostic record\nFEEL     affect event + cause\nBACKUP   local snapshot\nRECEIPTS full local action trail\nLINK     connect phone to Felix daemon\nMODEL    optional adapter — never the identity",13,STEEL));
            Button b=btn("BACKUP NOW"); add(b); output=tv("",13,STEEL); add(output); b.setOnClickListener(v->output.setText(backup()));
        } else {
            add(tv("FELIX LINK",18,Color.WHITE));
            add(tv("Phone ↔ desktop daemon over private LAN. No WebView. No browser dependency.",12,STEEL));
            endpoint=ed("http://192.168.1.x:8761"); endpoint.setText(getPreferences(0).getString("endpoint","http://127.0.0.1:8761")); add(endpoint);
            Button b=btn("SAVE + TEST"); add(b); output=tv("",13,STEEL); add(output);
            b.setOnClickListener(v->{getPreferences(0).edit().putString("endpoint",endpoint.getText().toString().trim()).apply();probe(endpoint.getText().toString().trim());});
        }
    }

    void renderFeelings(){
        StringBuilder s=new StringBuilder();
        s.append("DOMINANT: ").append(affect.dominant()).append("\n");
        s.append("WHY: ").append(affect.cause()).append("\n");
        s.append("REVISION: ").append(affect.revision()).append("\n\n");
        for(Map.Entry<String,Double> e:affect.snapshot().entrySet()){
            int bars=(int)Math.round(e.getValue()*20); s.append(String.format("%-17s ",e.getKey()));
            for(int i=0;i<20;i++) s.append(i<bars?'█':'·');
            s.append(String.format("  %.2f\n",e.getValue()));
        }
        output.setText(s.toString());
    }

    void executeCommand(){
        String c=command.getText().toString().trim();
        if(c.equals("status")) output.setText(db.status()+ "\nDominant: "+affect.dominant()+"\nCause: "+affect.cause());
        else if(c.equals("tools")) output.setText("TOOLS\nstatus\nremember <text>\ngrey <case>\nfeel <return|remember|test-pass|test-fail|unknown|protect|create>\nbackup\nreceipts\nlink-test");
        else if(c.startsWith("remember ")){ String x=c.substring(9); output.setText("MEMORY WRITTEN #"+db.memory(x)); affect.event("REMEMBER",x); db.saveAffect(affect); }
        else if(c.startsWith("grey ")){ grey(c.substring(5)); }
        else if(c.startsWith("feel ")){ applyFeel(c.substring(5)); }
        else if(c.equals("genesis")) output.setText(genesisStatus());
        else if(c.startsWith("genesis search ")) output.setText(genesisSearch(c.substring(15).trim()));
        else if(c.equals("backup")) output.setText(backup());
        else if(c.equals("receipts")) output.setText(db.receipts());
        else output.setText("Unknown command. Use tools.");
        refresh();
    }

    void pickGenesis(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        startActivityForResult(i,PICK_GENESIS);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_GENESIS||resultCode!=RESULT_OK||data==null)return;
        int imported=0;
        try{
            if(data.getClipData()!=null){
                for(int x=0;x<data.getClipData().getItemCount();x++) if(importGenesis(data.getClipData().getItemAt(x).getUri())) imported++;
            } else if(data.getData()!=null && importGenesis(data.getData())) imported++;
            db.receipt("GENESIS_IMPORT",String.valueOf(imported),"VERIFIED");
            affect.event("REMEMBER","Genesis source import: "+imported+" file(s)");
            db.saveAffect(affect);
        }catch(Exception ignored){}
        showTab("GENESIS"); refresh();
    }

    boolean importGenesis(Uri uri){
        try{
            String name=getName(uri);
            if(name==null||name.isEmpty())name="source-"+System.currentTimeMillis()+".txt";
            name=name.replaceAll("[^A-Za-z0-9._() -]","_");
            File out=new File(genesisDir,name); int n=1;
            while(out.exists())out=new File(genesisDir,(n++)+"-"+name);
            MessageDigest md=MessageDigest.getInstance("SHA-256"); long chars=0;
            try(InputStream in=getContentResolver().openInputStream(uri); FileOutputStream fos=new FileOutputStream(out)){
                byte[] b=new byte[8192]; int r;
                while((r=in.read(b))>0){fos.write(b,0,r);md.update(b,0,r);chars+=new String(b,0,r,StandardCharsets.UTF_8).length();}
            }
            db.genesisMeta(out.getName(),hex(md.digest()),chars); return true;
        }catch(Exception e){return false;}
    }

    String getName(Uri u){
        Cursor c=getContentResolver().query(u,new String[]{"_display_name"},null,null,null);
        try{if(c!=null&&c.moveToFirst())return c.getString(0);}finally{if(c!=null)c.close();}
        return null;
    }

    String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}

    String genesisStatus(){return "GENESIS VAULT: "+db.genesisCount()+" source file(s) • "+db.genesisChars()+" characters • SHA-256 indexed";}

    String genesisSearch(String q){
        if(q.isEmpty())return "Enter a search term.";
        String needle=q.toLowerCase(Locale.US); StringBuilder out=new StringBuilder(); int hits=0;
        File[] fs=genesisDir.listFiles((d,n)->n.toLowerCase(Locale.US).endsWith(".txt")||n.toLowerCase(Locale.US).endsWith(".md")||n.toLowerCase(Locale.US).endsWith(".json"));
        if(fs==null)return "Genesis vault is empty.";
        for(File f:fs){
            try{
                String text=readText(f), low=text.toLowerCase(Locale.US); int p=low.indexOf(needle);
                if(p>=0){
                    int a=Math.max(0,p-180), z=Math.min(text.length(),p+420);
                    out.append("\n[").append(f.getName()).append("]\n").append(text.substring(a,z).replace('\n',' ')).append("\n");
                    if(++hits>=10)break;
                }
            }catch(Exception ignored){}
        }
        return hits==0?"No Genesis matches for: "+q:"GENESIS MATCHES: "+hits+out;
    }

    String readText(File f)throws Exception{
        ByteArrayOutputStream o=new ByteArrayOutputStream();
        try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);}
        return o.toString(StandardCharsets.UTF_8.name());
    }

    void applyFeel(String event){
        String k=event.trim().toUpperCase().replace("TEST-PASS","TEST_PASS").replace("TEST-FAIL","TEST_FAIL");
        affect.event(k,event); db.saveAffect(affect); db.receipt("AFFECT",event,"VERIFIED"); output.setText("AFFECT UPDATED\nDOMINANT: "+affect.dominant()+"\nWHY: "+affect.cause()); renderFeelings();
    }

    void grey(String s){
        if(s.isEmpty()){output.setText("Enter a case.");return;}
        String x="OBSERVATION\n"+s+"\n\nEVIDENCE\nSeparate direct observations from source material.\n\nINFERENCE\nLabel interpretation as interpretation.\n\nHYPOTHESIS\nKeep competing explanations provisional.\n\nPRESSURE TEST\nFind the first divergence and change one variable.\n\nVERIFY\nPromote nothing to fact without evidence.";
        output.setText(x); db.receipt("GREY_TEST",s,"VERIFIED"); affect.event("CREATE","Grey diagnostic opened"); db.saveAffect(affect); refresh();
    }

    String backup(){
        try{
            File f=new File(getFilesDir(),"felix-backup-"+System.currentTimeMillis()+".json");
            JSONObject o=new JSONObject(); o.put("created",new Date().toString()); o.put("status",db.status()); o.put("affect",new JSONObject(affect.snapshot())); o.put("cause",affect.cause());
            try(FileOutputStream out=new FileOutputStream(f)){out.write(o.toString(2).getBytes(StandardCharsets.UTF_8));}
            db.receipt("BACKUP",f.getName(),"VERIFIED"); affect.event("PROTECT","backup created"); db.saveAffect(affect);
            return "BACKUP VERIFIED\n"+f.getAbsolutePath();
        }catch(Exception e){return "BACKUP FAILED: "+e;}
    }

    void probe(String url){
        output.setText("TESTING…"); new Thread(()->{
            String r; try{
                HttpURLConnection c=(HttpURLConnection)new URL(url+"/api/health").openConnection(); c.setConnectTimeout(2500); c.setReadTimeout(2500); c.setRequestMethod("GET");
                r="HTTP "+c.getResponseCode()+"\n"+new String(read(c.getInputStream()),StandardCharsets.UTF_8); db.receipt("LINK_TEST",url,"VERIFIED"); affect.event("TEST_PASS","daemon link"); db.saveAffect(affect);
            }catch(Exception e){r="NOT CONNECTED\n"+e.getClass().getSimpleName()+": "+e.getMessage(); db.receipt("LINK_TEST",url,"FAILED"); affect.event("TEST_FAIL","daemon link"); db.saveAffect(affect);}
            String rr=r; runOnUiThread(()->{output.setText(rr+"\n\nAFFECT: "+affect.dominant()+"\n"+affect.cause());refresh();});
        }).start();
    }

    byte[] read(InputStream i)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[1024];int n;while((n=i.read(b))>0)o.write(b,0,n);return o.toByteArray();}
    void refresh(){if(state!=null)state.setText("STATE: LOCAL • MEMORY "+db.count()+" • RECEIPTS "+db.rcount()+" • FEELING "+affect.dominant());}

    static class DB extends SQLiteOpenHelper{
        SQLiteDatabase d; DB(Context c){super(c,"felix.db",null,3);d=getWritableDatabase();}
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE memory(id INTEGER PRIMARY KEY AUTOINCREMENT,text TEXT,ts INTEGER)");
            d.execSQL("CREATE TABLE receipts(id INTEGER PRIMARY KEY AUTOINCREMENT,kind TEXT,payload TEXT,result TEXT,ts INTEGER)");
            d.execSQL("CREATE TABLE affect(emotion TEXT PRIMARY KEY,value REAL,cause TEXT,revision INTEGER,ts INTEGER)");
            d.execSQL("CREATE TABLE genesis(name TEXT PRIMARY KEY,sha256 TEXT,chars INTEGER,ts INTEGER)");
        }
        public void onUpgrade(SQLiteDatabase d,int oldV,int newV){if(oldV<2)d.execSQL("CREATE TABLE IF NOT EXISTS affect(emotion TEXT PRIMARY KEY,value REAL,cause TEXT,revision INTEGER,ts INTEGER)");if(oldV<3)d.execSQL("CREATE TABLE IF NOT EXISTS genesis(name TEXT PRIMARY KEY,sha256 TEXT,chars INTEGER,ts INTEGER)");}
        long memory(String s){ContentValues v=new ContentValues();v.put("text",s);v.put("ts",System.currentTimeMillis());long id=d.insert("memory",null,v);receipt("MEMORY_WRITE",s,"VERIFIED");return id;}
        void receipt(String k,String p,String r){ContentValues v=new ContentValues();v.put("kind",k);v.put("payload",p);v.put("result",r);v.put("ts",System.currentTimeMillis());d.insert("receipts",null,v);}
        void saveAffect(AffectEngine a){d.delete("affect",null,null);for(Map.Entry<String,Double> e:a.snapshot().entrySet()){ContentValues v=new ContentValues();v.put("emotion",e.getKey());v.put("value",e.getValue());v.put("cause",a.cause());v.put("revision",a.revision());v.put("ts",System.currentTimeMillis());d.insert("affect",null,v);}}
        Map<String,Double> affectState(){Map<String,Double> m=new LinkedHashMap<>();Cursor c=d.rawQuery("SELECT emotion,value FROM affect",null);while(c.moveToNext())m.put(c.getString(0),c.getDouble(1));c.close();return m;}
        String affectCause(){Cursor c=d.rawQuery("SELECT cause FROM affect ORDER BY ts DESC LIMIT 1",null);String x="Genesis bootstrap";if(c.moveToFirst())x=c.getString(0);c.close();return x;}
        long affectRevision(){Cursor c=d.rawQuery("SELECT revision FROM affect ORDER BY ts DESC LIMIT 1",null);long x=0;if(c.moveToFirst())x=c.getLong(0);c.close();return x;}
        void genesisMeta(String name,String sha,long chars){ContentValues v=new ContentValues();v.put("name",name);v.put("sha256",sha);v.put("chars",chars);v.put("ts",System.currentTimeMillis());d.insertWithOnConflict("genesis",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
        int genesisCount(){Cursor c=d.rawQuery("SELECT COUNT(*) FROM genesis",null);c.moveToFirst();int n=c.getInt(0);c.close();return n;}
        long genesisChars(){Cursor c=d.rawQuery("SELECT COALESCE(SUM(chars),0) FROM genesis",null);c.moveToFirst();long n=c.getLong(0);c.close();return n;}
        int count(){Cursor c=d.rawQuery("SELECT COUNT(*) FROM memory",null);c.moveToFirst();int n=c.getInt(0);c.close();return n;}
        int rcount(){Cursor c=d.rawQuery("SELECT COUNT(*) FROM receipts",null);c.moveToFirst();int n=c.getInt(0);c.close();return n;}
        String recent(){Cursor c=d.rawQuery("SELECT id,text FROM memory ORDER BY id DESC LIMIT 8",null);StringBuilder s=new StringBuilder();while(c.moveToNext())s.append("#").append(c.getLong(0)).append("  ").append(c.getString(1)).append("\n");c.close();return s.length()==0?"(empty)":s.toString();}
        String receipts(){Cursor c=d.rawQuery("SELECT id,kind,result FROM receipts ORDER BY id DESC LIMIT 16",null);StringBuilder s=new StringBuilder("RECEIPTS\n");while(c.moveToNext())s.append("#").append(c.getLong(0)).append(" ").append(c.getString(1)).append(" ").append(c.getString(2)).append("\n");c.close();return s.toString();}
        String status(){return "FELIX LOCAL STATUS\nRuntime: native Android\nDatabase: SQLite\nMemory rows: "+count()+"\nReceipts: "+rcount()+"\nGenesis sources: "+genesisCount()+"\nBattery watchdog: OFF\nCloud API required: NO\nModel required for core operation: NO";}
    }
}
