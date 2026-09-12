package com.duplasena.campea25;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int REQ_FILE=91;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private java.util.List<MotorCore.Contest> history;
    private String fileName="";
    private MotorCore.GroupStats champion, delayed;
    private MotorCore.GameResult game1, game2;

    private TextView fileInfo, historyInfo, championText, delayedText, result1Text, result2Text, status;
    private ProgressBar progress;
    private Button studyChampionBtn, game1Btn, delayedBtn, pdfBtn;

    private final int RED=Color.rgb(132,0,29);
    private final int DARK=Color.rgb(91,0,20);
    private final int LIGHT=Color.rgb(252,241,244);
    private final int GREEN=Color.rgb(31,132,74);
    private final int PALE_RED=Color.rgb(255,220,225);

    @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private GradientDrawable border(int c,int s){GradientDrawable g=bg(Color.WHITE,16);g.setStroke(dp(s),c);return g;}
    private TextView text(String s,int sp,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.rgb(36,36,36));t.setPadding(0,dp(5),0,dp(5));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(border(Color.rgb(228,202,209),1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(14),dp(8),dp(14),dp(8));c.setLayoutParams(lp);return c;}
    private Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(bg(color,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.setMargins(0,dp(7),0,dp(7));b.setLayoutParams(lp);return b;}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(253,251,252));scroll.addView(root,new ScrollView.LayoutParams(-1,-2));

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.HORIZONTAL);hero.setGravity(Gravity.CENTER_VERTICAL);hero.setPadding(dp(20),dp(22),dp(20),dp(22));hero.setBackgroundColor(DARK);
        ImageView clover=new ImageView(this);clover.setImageResource(R.drawable.ic_clover_white);hero.addView(clover,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.setPadding(dp(14),0,0,0);
        TextView h1=text("DUPLA SENA CAMPEÃ",24,true);h1.setTextColor(Color.WHITE);TextView h2=text("Grupo 25 • campeão + atrasado • perímetro duplo",13,false);h2.setTextColor(Color.rgb(245,214,222));ht.addView(h1);ht.addView(h2);hero.addView(ht,new LinearLayout.LayoutParams(0,-2,1));root.addView(hero);

        LinearLayout base=card();base.addView(text("1. BASE DE RESULTADOS",18,true));Button select=button("SELECIONAR TXT / CSV DA DUPLA SENA",RED);select.setOnClickListener(v->selectFile());base.addView(select);fileInfo=text("Nenhum arquivo selecionado.",14,false);historyInfo=text("",14,true);base.addView(fileInfo);base.addView(historyInfo);root.addView(base);

        LinearLayout champ=card();champ.addView(text("2. GRUPO CAMPEÃO DE 25",18,true));champ.addView(text("Pesquisa um grupo de 25 dezenas que mais vezes conteve as 6 dezenas completas, somando 1º e 2º sorteios. O grupo fica salvo no aparelho até você mandar estudar novamente.",13,false));
        studyChampionBtn=button("ESTUDAR HISTÓRICO E FIXAR CAMPEÃO 25",RED);studyChampionBtn.setEnabled(false);studyChampionBtn.setOnClickListener(v->studyChampion());champ.addView(studyChampionBtn);championText=text("Grupo ainda não estudado.",13,false);champ.addView(championText);game1Btn=button("GERAR JOGO 1 — GRUPO CAMPEÃO",DARK);game1Btn.setEnabled(false);game1Btn.setOnClickListener(v->generateChampionGame());champ.addView(game1Btn);result1Text=text("",14,false);champ.addView(result1Text);root.addView(champ);

        LinearLayout del=card();del.addView(text("3. GRUPO FORTE MAIS ATRASADO",18,true));del.addView(text("Procura, entre grupos historicamente fortes, outro grupo de 25 que está há mais sorteios sem fechar 6/6. Em seguida percorre as 177.100 combinações de 6.",13,false));
        delayedBtn=button("PROCURAR ATRASADO + GERAR JOGO 2",RED);delayedBtn.setEnabled(false);delayedBtn.setOnClickListener(v->searchDelayedAndGenerate());del.addView(delayedBtn);delayedText=text("Ainda não pesquisado.",13,false);del.addView(delayedText);result2Text=text("",14,false);del.addView(result2Text);root.addView(del);

        LinearLayout prog=card();prog.addView(text("PROCESSAMENTO",17,true));progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgress(0);prog.addView(progress,new LinearLayout.LayoutParams(-1,dp(20)));status=text("Aguardando base.",13,true);prog.addView(status);pdfBtn=button("GERAR PDF DOS JOGOS",RED);pdfBtn.setEnabled(false);pdfBtn.setOnClickListener(v->savePdf());prog.addView(pdfBtn);root.addView(prog);

        LinearLayout motor=card();motor.addView(text("MOTOR DUPLO",17,true));motor.addView(text("Cada grupo de 25 gera exatamente C(25,6)=177.100 jogos. O motor percorre todos, aprende filtros estruturais da base e depois aprofunda os candidatos fortes em um perímetro que mede separadamente 1º sorteio e 2º sorteio: duques, ternos, quadras e quinas. Jogo que já fez sena no perímetro é eliminado. Frequência recente, atraso, tendência e repetição em relação aos dois últimos sorteios entram no ranking.",13,false));root.addView(motor);
        setContentView(scroll);
    }

    private void selectFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/*");try{startActivityForResult(i,REQ_FILE);}catch(Exception e){i.setType("*/*");startActivityForResult(i,REQ_FILE);}}

    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req!=REQ_FILE||res!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        fileName=queryName(uri);fileInfo.setText("Arquivo: "+fileName);status.setText("Lendo os dois sorteios por concurso...");disableWork();
        executor.submit(()->{try(InputStream in=getContentResolver().openInputStream(uri)){byte[] bytes=readAll(in);java.util.List<MotorCore.Contest> h=MotorCore.parseHistory(bytes);if(h.size()<40)throw new IllegalArgumentException("Foram encontrados apenas "+h.size()+" concursos válidos.");history=h;MotorCore.Contest last=h.get(h.size()-1);MotorCore.GroupStats saved=loadChampion();if(saved!=null)saved=MotorCore.evaluateGroup(h,saved.group);final MotorCore.GroupStats fs=saved;champion=saved;delayed=null;game1=game2=null;runOnUiThread(()->{historyInfo.setText("Concursos: "+h.size()+" | Último: "+last.contest+"\n1º: "+MotorCore.fmt(last.draw1)+"\n2º: "+MotorCore.fmt(last.draw2));studyChampionBtn.setEnabled(true);if(fs!=null){championText.setText(groupReport("CAMPEÃO SALVO",fs));game1Btn.setEnabled(true);delayedBtn.setEnabled(true);}else{championText.setText("Nenhum campeão salvo. Toque em estudar histórico.");}delayedText.setText("Ainda não pesquisado.");result1Text.setText("");result2Text.setText("");status.setText("Base pronta.");progress.setProgress(0);pdfBtn.setEnabled(false);});}catch(Exception e){runOnUiThread(()->{status.setText("Erro ao ler: "+e.getMessage());historyInfo.setText("");studyChampionBtn.setEnabled(false);});}});
    }

    private void disableWork(){studyChampionBtn.setEnabled(false);game1Btn.setEnabled(false);delayedBtn.setEnabled(false);pdfBtn.setEnabled(false);progress.setProgress(0);}
    private String queryName(Uri u){String s=u.getLastPathSegment();if(s==null)s="dupla-sena.txt";int k=s.lastIndexOf('/');return k>=0?s.substring(k+1):s;}
    private byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);return b.toByteArray();}

    private void studyChampion(){if(history==null)return;disableDuringRun();status.setText("Iniciando busca do campeão...");executor.submit(()->{try{MotorCore.GroupStats s=MotorCore.searchChampion25(history,6000,(p,m)->uiProgress(p,m));champion=s;saveChampion(s);runOnUiThread(()->{championText.setText(groupReport("GRUPO CAMPEÃO FIXADO",s));game1Btn.setEnabled(true);delayedBtn.setEnabled(true);studyChampionBtn.setEnabled(true);status.setText("Campeão fixado. Agora gere o Jogo 1 ou pesquise o atrasado.");progress.setProgress(100);});}catch(Exception e){runOnUiThread(()->finishError(e));}});}

    private void generateChampionGame(){if(history==null||champion==null)return;disableDuringRun();status.setText("Gerando 177.100 jogos do campeão...");executor.submit(()->{try{MotorCore.GameResult r=MotorCore.generateGame(history,champion,(p,m)->uiProgress(p,m));game1=r;runOnUiThread(()->{result1Text.setText(gameReport("JOGO 1 — CAMPEÃO",r));finishButtons();});}catch(Exception e){runOnUiThread(()->finishError(e));}});}

    private void searchDelayedAndGenerate(){if(history==null||champion==null)return;disableDuringRun();status.setText("Procurando grupo forte mais atrasado...");executor.submit(()->{try{MotorCore.GroupStats d=MotorCore.searchDelayedStrong25(history,champion,7000,(p,m)->uiProgress(p,m));delayed=d;MotorCore.GameResult r=MotorCore.generateGame(history,d,(p,m)->uiProgress(p,m));game2=r;runOnUiThread(()->{delayedText.setText(groupReport("GRUPO FORTE MAIS ATRASADO ENCONTRADO",d));result2Text.setText(gameReport("JOGO 2 — ATRASADO",r));finishButtons();});}catch(Exception e){runOnUiThread(()->finishError(e));}});}

    private void disableDuringRun(){studyChampionBtn.setEnabled(false);game1Btn.setEnabled(false);delayedBtn.setEnabled(false);pdfBtn.setEnabled(false);progress.setProgress(0);}
    private void finishButtons(){studyChampionBtn.setEnabled(true);game1Btn.setEnabled(champion!=null);delayedBtn.setEnabled(champion!=null);pdfBtn.setEnabled(game1!=null||game2!=null);progress.setProgress(100);status.setText("Análise concluída.");}
    private void finishError(Exception e){finishButtons();status.setText("Erro: "+e.getMessage());Toast.makeText(this,"Erro: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    private void uiProgress(int p,String m){runOnUiThread(()->{progress.setProgress(Math.max(0,Math.min(100,p)));status.setText(m);});}

    private String groupReport(String title,MotorCore.GroupStats s){return title+"\n"+MotorCore.fmt(s.group)+"\n6/6 no 1º sorteio: "+s.hits1+" | 6/6 no 2º: "+s.hits2+" | Total: "+s.hitsTotal+"\nFalhou agora: "+s.delayEvents+" sorteio(s) • "+s.delayContests+" concurso(s) | Último fechamento: "+(s.lastHitContest<0?"nenhum":s.lastHitContest)+" | Pressão: "+String.format(Locale.US,"%.2f",s.pressure);}
    private String gameReport(String title,MotorCore.GameResult r){MotorCore.GameMetrics g=r.best;return "\n"+title+"\n"+MotorCore.fmt(g.game)+"\nFiltro: "+r.filter+"\nCombinações: "+fmtNum(r.totalCombinations)+" | Classificadas: "+fmtNum(r.classified)+" | Perímetro: "+r.perimeterContests+" concursos\n1º sorteio — 2: "+g.q2_1+" | 3: "+g.q3_1+" | 4: "+g.q4_1+" | 5: "+g.q5_1+"\n2º sorteio — 2: "+g.q2_2+" | 3: "+g.q3_2+" | 4: "+g.q4_2+" | 5: "+g.q5_2+"\nRepete último 1º: "+g.repeatLast1+" | último 2º: "+g.repeatLast2+" | união: "+g.repeatLastUnion+"\nPares: "+g.even+" | Primos: "+g.prime+" | Fibonacci: "+g.fib+" | 01-25: "+g.low+" | Soma: "+g.sum+" | Score: "+String.format(Locale.US,"%.2f",g.score);}
    private String fmtNum(long n){return String.format(Locale.US,"%,d",n).replace(',','.');}

    private void saveChampion(MotorCore.GroupStats s){StringBuilder b=new StringBuilder();for(int i=0;i<s.group.length;i++){if(i>0)b.append(',');b.append(s.group[i]);}getSharedPreferences("dupla_sena_campea",MODE_PRIVATE).edit().putString("group25",b.toString()).apply();}
    private MotorCore.GroupStats loadChampion(){String s=getSharedPreferences("dupla_sena_campea",MODE_PRIVATE).getString("group25","");if(s.isEmpty())return null;try{String[] p=s.split(",");if(p.length!=25)return null;int[] g=new int[25];for(int i=0;i<25;i++)g[i]=Integer.parseInt(p[i]);return new MotorCore.GroupStats(g,0,0,0,0,-1,0);}catch(Exception e){return null;}}

    private void savePdf(){if(game1==null&&game2==null)return;try{android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();int pageNo=0;if(game1!=null)drawPdfPage(pdf,++pageNo,"JOGO 1 — GRUPO CAMPEÃO",game1);if(game2!=null)drawPdfPage(pdf,++pageNo,"JOGO 2 — GRUPO ATRASADO",game2);int next=history.get(history.size()-1).contest+1;String name="DUPLA_SENA_CAMPEA_GRUPO25_CONCURSO_"+next+".pdf";Uri u=writePdf(pdf,name);pdf.close();Toast.makeText(this,u!=null?"PDF salvo: "+name:"Não foi possível salvar o PDF.",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Erro no PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}}

    private void drawPdfPage(android.graphics.pdf.PdfDocument pdf,int pageNo,String title,MotorCore.GameResult r){android.graphics.pdf.PdfDocument.PageInfo info=new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,pageNo).create();android.graphics.pdf.PdfDocument.Page page=pdf.startPage(info);Canvas c=page.getCanvas();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(DARK);c.drawRect(0,0,595,88,p);p.setColor(Color.WHITE);p.setTextSize(23);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("DUPLA SENA CAMPEÃ",30,38,p);p.setTextSize(13);p.setTypeface(Typeface.DEFAULT);c.drawText(title,30,63,p);int y=112;p.setColor(Color.DKGRAY);p.setTextSize(12);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("Grupo 25: "+MotorCore.fmt(r.groupStats.group),28,y,p);y+=22;c.drawText("Jogo: "+MotorCore.fmt(r.best.game),28,y,p);y+=20;p.setTypeface(Typeface.DEFAULT);c.drawText("1º sorteio 2/3/4/5: "+r.best.q2_1+" / "+r.best.q3_1+" / "+r.best.q4_1+" / "+r.best.q5_1,28,y,p);y+=18;c.drawText("2º sorteio 2/3/4/5: "+r.best.q2_2+" / "+r.best.q3_2+" / "+r.best.q4_2+" / "+r.best.q5_2,28,y,p);y+=18;c.drawText("Falhou agora: "+r.groupStats.delayEvents+" sorteios | 6/6 histórico total: "+r.groupStats.hitsTotal+" | Score: "+String.format(Locale.US,"%.2f",r.best.score),28,y,p);y+=34;
        HashSet<Integer> group=new HashSet<>(),game=new HashSet<>();for(int x:r.groupStats.group)group.add(x);for(int x:r.best.game)game.add(x);int cellW=49,cellH=42,startX=50;for(int row=0;row<5;row++){for(int col=0;col<10;col++){int n=row*10+col+1;int x=startX+col*cellW;int yy=y+row*cellH;if(game.contains(n))p.setColor(Color.rgb(207,244,218));else if(!group.contains(n))p.setColor(PALE_RED);else p.setColor(Color.WHITE);p.setStyle(Paint.Style.FILL);c.drawRoundRect(x,yy,x+42,yy+34,7,7,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.4f);p.setColor(game.contains(n)?GREEN:(!group.contains(n)?RED:Color.LTGRAY));c.drawRoundRect(x,yy,x+42,yy+34,7,7,p);p.setStyle(Paint.Style.FILL);p.setColor(game.contains(n)?GREEN:(!group.contains(n)?RED:Color.DKGRAY));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(13);c.drawText(String.format(Locale.US,"%02d",n),x+11,yy+22,p);}}
        y+=5*cellH+28;p.setTextSize(10);p.setTypeface(Typeface.DEFAULT);p.setColor(GREEN);c.drawText("VERDE = 6 dezenas do jogo",28,y,p);p.setColor(RED);c.drawText("VERMELHO = 25 dezenas fora do grupo",190,y,p);p.setColor(Color.DKGRAY);c.drawText("BRANCO = outras 19 do grupo",405,y,p);y+=24;p.setColor(Color.GRAY);p.setTextSize(9);c.drawText("Estudo estatístico do histórico carregado. Não representa garantia de premiação.",28,y,p);pdf.finishPage(page);}

    private Uri writePdf(android.graphics.pdf.PdfDocument pdf,String name)throws Exception{if(Build.VERSION.SDK_INT>=29){ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,name);v.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");v.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/DuplaSenaCampea");Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(uri==null)return null;try(OutputStream out=getContentResolver().openOutputStream(uri)){pdf.writeTo(out);}return uri;}File dir=getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);if(dir==null)dir=getFilesDir();File f=new File(dir,name);try(OutputStream out=new FileOutputStream(f)){pdf.writeTo(out);}return Uri.fromFile(f);}

    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
