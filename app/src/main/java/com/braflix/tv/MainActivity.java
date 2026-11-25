package com.braflix.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.content.pm.PackageInfo;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import java.security.MessageDigest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
 private final int BLACK=0xFF09090B, PANEL=0xFF19191E, RED=0xFFE50914, MUTED=0xFFB6B6BD;
 private final ArrayList<MediaEntry> channels=new ArrayList<>();
 private final Handler ui=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final ExecutorService images=Executors.newFixedThreadPool(3);
 private LinearLayout root,content,nav;private EditText search;
 private String screen="Início";private final Set<String> favorites=new HashSet<>();
 private static final int PICK_FILE=300;
 private static final int INSTALL_PERMISSION_REQUEST=301;
 private File pendingUpdateFile;
 private boolean updateCheckInProgress=false;
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BLACK);getWindow().setNavigationBarColor(BLACK);
  String fav=getPreferences(0).getString("favorites", "");if(!fav.isEmpty())favorites.addAll(Arrays.asList(fav.split("\n")));
  String cached=readCache();if(!cached.isEmpty())channels.addAll(M3uParser.parse(cached));show("Início");
  String source=getPreferences(0).getString("playlistUrl", "");if(!source.isEmpty())fetchList(source);
  if(!getPreferences(0).getString("updateUrl", BuildConfig.UPDATE_FEED_URL).isEmpty())checkUpdates(false);
 }
 private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
 private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 private void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
 private void show(String page){screen=page;root=column();root.setBackgroundColor(BLACK);setContentView(root);
  LinearLayout header=row();header.setPadding(dp(20),dp(19),dp(20),dp(12));TextView brand=text("BRAFLIX",28,RED,true);
  header.addView(brand,new LinearLayout.LayoutParams(0,dp(46),1));brand.setGravity(Gravity.CENTER_VERTICAL);
  TextView settings=text("⚙",27,Color.WHITE,false);settings.setGravity(Gravity.CENTER);settings.setOnClickListener(v->show("Ajustes"));header.addView(settings);root.addView(header);
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);content=column();content.setPadding(dp(16),dp(3),dp(16),dp(12));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  nav=row();nav.setPadding(dp(7),dp(8),dp(7),dp(8));nav.setBackgroundColor(0xFF141416);
  for(String item:new String[]{"Início","Canais","Favoritos","Ajustes"}){
   TextView t=text((item.equals("Início")?"⌂  ":item.equals("Canais")?"▣  ":item.equals("Favoritos")?"♥  ":"⚙  ")+item,11,item.equals(page)?RED:MUTED,item.equals(page));
   t.setGravity(Gravity.CENTER);nav.addView(t,new LinearLayout.LayoutParams(0,dp(44),1));t.setOnClickListener(v->show(item));
  }root.addView(nav);
  if(page.equals("Ajustes"))renderSettings();else renderCatalog();
 }
 private void renderCatalog(){
  if(screen.equals("Início")){
   TextView heading=text("Sua TV. Do seu jeito.",22,Color.WHITE,true);content.addView(heading);gap(content,8);
   TextView subtitle=text("Canais ao vivo organizados em um só lugar.",13,MUTED,false);content.addView(subtitle);gap(content,20);
   LinearLayout hero=column();hero.setPadding(dp(20),dp(24),dp(20),dp(23));hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF571018,0xFF19191E,0xFF101014}));
   hero.addView(text("● AO VIVO",12,0xFFFF7F87,true));gap(hero,14);hero.addView(text("O melhor da TV\nna sua tela",27,Color.WHITE,true));gap(hero,16);
   TextView watch=text("▶  EXPLORAR CANAIS",14,Color.WHITE,true);watch.setPadding(dp(14),dp(12),dp(14),dp(12));watch.setBackground(bg(RED,9));watch.setOnClickListener(v->show("Canais"));hero.addView(watch);content.addView(hero);gap(content,24);
  }
  TextView count=text(channels.size()+" canais carregados",13,MUTED,false);content.addView(count);gap(content,12);
  if(channels.isEmpty()){content.addView(text("Ainda não há uma lista de canais. Abra Ajustes para importar seu arquivo M3U ou adicionar uma URL.",16,Color.WHITE,false));gap(content,16);makeButton(content,"CONFIGURAR MINHA LISTA",()->show("Ajustes"));return;}
  search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(MUTED);search.setHint("⌕  Buscar canais ou categorias");search.setTextSize(15);search.setPadding(dp(15),dp(7),dp(15),dp(7));search.setBackground(bg(PANEL,12));content.addView(search);gap(content,18);
  LinearLayout results=column();content.addView(results);
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){populate(results,s.toString());}public void afterTextChanged(Editable e){}});
  populate(results,"");
 }
 private void populate(LinearLayout target,String q){target.removeAllViews();String query=q.trim().toLowerCase(Locale.ROOT);
  LinkedHashMap<String,ArrayList<MediaEntry>> groups=new LinkedHashMap<>();int matched=0;
  for(MediaEntry e:channels){if(screen.equals("Favoritos")&&!favorites.contains(e.url))continue;
   if(!e.name.toLowerCase(Locale.ROOT).contains(query)&&!e.group.toLowerCase(Locale.ROOT).contains(query))continue;
   String group=e.group.isEmpty()?"Outros canais":e.group.replace("CANAIS:","").replace("✨","").trim();
   groups.computeIfAbsent(group,k->new ArrayList<>()).add(e);matched++;
  }
  if(matched==0){target.addView(text(screen.equals("Favoritos")?"Toque no coração de um canal para salvá-lo aqui.":"Nenhum canal encontrado.",15,MUTED,false));return;}
  int max=0;
  for(Map.Entry<String,ArrayList<MediaEntry>> pair:groups.entrySet()){
   TextView title=text(pair.getKey(),19,Color.WHITE,true);target.addView(title);gap(target,11);
   // Horizontally scrolling channels, suitable for phone portrait mode.
   HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);LinearLayout shelf=row();
   for(MediaEntry e:pair.getValue()){
    if(max++>=300)break;
    LinearLayout card=column();card.setPadding(dp(13),dp(13),dp(13),dp(12));card.setBackground(bg(PANEL,12));
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(150),dp(152));cp.setMargins(0,0,dp(10),0);shelf.addView(card,cp);
    TextView logo=text("▶",29,RED,true);logo.setGravity(Gravity.CENTER);logo.setBackground(bg(0xFF28282E,10));card.addView(logo,new LinearLayout.LayoutParams(-1,dp(65)));
    if(!e.logo.isEmpty())loadLogo(logo,e.logo);
    gap(card,9);TextView name=text(e.name,13,Color.WHITE,true);name.setMaxLines(2);card.addView(name,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout footer=row();TextView live=text("● LIVE",10,0xFFFF797E,true);footer.addView(live,new LinearLayout.LayoutParams(0,-2,1));
    TextView heart=text(favorites.contains(e.url)?"♥":"♡",25,favorites.contains(e.url)?RED:MUTED,false);footer.addView(heart);
    heart.setOnClickListener(v->{if(!favorites.add(e.url))favorites.remove(e.url);getPreferences(0).edit().putString("favorites",String.join("\n",favorites)).apply();heart.setText(favorites.contains(e.url)?"♥":"♡");heart.setTextColor(favorites.contains(e.url)?RED:MUTED);});
    card.addView(footer);
    card.setOnClickListener(v->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra("url",e.url);i.putExtra("title",e.name);startActivity(i);});
   }
   hsv.addView(shelf);target.addView(hsv);gap(target,24);if(max>=300)break;
  }
  if(max>=300){TextView note=text("Exibindo os primeiros 300 canais. Use a busca para encontrar outros.",12,MUTED,false);target.addView(note);}
 }
 private void loadLogo(TextView target,String url){ // Small in-memory images; no external UI dependencies.
  images.execute(()->{try{URLConnection con=new URL(url).openConnection();con.setConnectTimeout(5000);con.setReadTimeout(5000);
   if(con.getContentLength()>500000)return;
   try(InputStream in=con.getInputStream()){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] a=new byte[4096];int n;while((n=in.read(a))!=-1){b.write(a,0,n);if(b.size()>500000)return;}
    byte[] bytes=b.toByteArray();android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    if(bitmap!=null)ui.post(()->{target.setText("");android.graphics.drawable.BitmapDrawable d=new android.graphics.drawable.BitmapDrawable(getResources(),bitmap);d.setGravity(Gravity.CENTER);target.setBackground(d);});}
  }catch(Exception ignored){} });
 }
 private void renderSettings(){content.addView(text("Configurações",24,Color.WHITE,true));gap(content,8);content.addView(text("Gerencie seus canais e as atualizações do BRAFLIX.",14,MUTED,false));gap(content,24);
  content.addView(text("LISTA DE CANAIS",15,Color.WHITE,true));gap(content,10);
  makeButton(content,"Adicionar lista por URL",()->prompt("URL da lista M3U",getPreferences(0).getString("playlistUrl",""),value->{
   if(!value.startsWith("http://")&&!value.startsWith("https://")){toast("URL inválida");return;}
   getPreferences(0).edit().putString("playlistUrl",value).apply();fetchList(value);
  }));gap(content,9);
  makeButton(content,"Importar arquivo M3U do celular",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_FILE);});gap(content,9);
  makeButton(content,"Atualizar lista agora",()->{String u=getPreferences(0).getString("playlistUrl","");if(u.isEmpty())toast("Adicione primeiro uma URL M3U");else fetchList(u);});gap(content,26);
  content.addView(text("ATUALIZAÇÕES DO APP",15,Color.WHITE,true));gap(content,10);
  makeButton(content,"Configurar endereço de atualizações",()->prompt("Endereço HTTPS do arquivo update.json",getPreferences(0).getString("updateUrl",BuildConfig.UPDATE_FEED_URL),s->{if(!s.startsWith("https://")){toast("Use um endereço HTTPS");return;}getPreferences(0).edit().putString("updateUrl",s).apply();toast("Endereço salvo");}));gap(content,9);
  makeButton(content,"Verificar nova versão",()->checkUpdates(true));gap(content,9);
  makeButton(content,"Ajuda: como publicar atualizações",()->new AlertDialog.Builder(this)
   .setTitle("Atualizações BRAFLIX").setMessage("1. Gere um novo APK com versionCode maior.\n2. Assine com a MESMA chave da versão instalada.\n3. Publique o APK e update.json em HTTPS.\n4. O BRAFLIX avisa quando houver uma nova versão.\n5. Toque em Atualizar e confirme no Android.\n\nCanais M3U podem ser atualizados separadamente, sem novo APK.")
   .setPositiveButton("Entendi",null).show());gap(content,18);
  content.addView(text("BRAFLIX " + BuildConfig.VERSION_NAME + "  •  Android",13,MUTED,false));gap(content,8);
  content.addView(text("Este aplicativo é um reprodutor de listas M3U e não fornece canais. Use somente transmissões autorizadas.",12,MUTED,false));
 }
 private void makeButton(LinearLayout parent,String value,Runnable action){TextView t=text(value,14,Color.WHITE,true);t.setPadding(dp(15),dp(16),dp(15),dp(16));t.setBackground(bg(PANEL,10));parent.addView(t,new LinearLayout.LayoutParams(-1,-2));t.setOnClickListener(v->action.run());}
 private interface Result{void done(String s);}
 private void prompt(String title,String initial,Result cb){EditText e=new EditText(this);e.setText(initial);e.setSingleLine();e.setTextColor(Color.BLACK);
  LinearLayout wrapper=column();wrapper.setPadding(dp(22),0,dp(22),0);wrapper.addView(e);
  new AlertDialog.Builder(this).setTitle(title).setView(wrapper).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->cb.done(e.getText().toString().trim())).show();}
 private void fetchList(String url){toast("Carregando lista...");worker.execute(()->{try{String m=download(url,15_000_000);ArrayList<MediaEntry> fresh=M3uParser.parse(m);
   if(fresh.isEmpty())throw new IOException("Nenhum canal M3U encontrado");
   try(FileOutputStream out=openFileOutput("channels.m3u",MODE_PRIVATE)){out.write(m.getBytes("UTF-8"));}
   ui.post(()->{channels.clear();channels.addAll(fresh);toast(fresh.size()+" canais carregados");show("Canais");});
  }catch(Exception ex){ui.post(()->toast("Erro na lista: "+ex.getMessage()));}});}
 private String readCache(){try(InputStream in=openFileInput("channels.m3u")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);return b.toString("UTF-8");}catch(Exception ex){return "";}}
 private String download(String url,int limit)throws Exception{URLConnection c=new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","BRAFLIX/1.0");
  try(InputStream in=c.getInputStream()){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1){b.write(bytes,0,n);if(b.size()>limit)throw new IOException("Arquivo grande demais");}return b.toString("UTF-8");}}
 private void checkUpdates(boolean manual){
  String url=getPreferences(0).getString("updateUrl", BuildConfig.UPDATE_FEED_URL);
  if(url.isEmpty()){if(manual)toast("Configure primeiro o endereço HTTPS de atualizações");return;}
  if(updateCheckInProgress)return;
  updateCheckInProgress=true;
  if(manual)toast("Verificando versão...");
  worker.execute(()->{
   try{
    JSONObject j=new JSONObject(download(url,100000));
    int remoteCode=j.getInt("versionCode");
    int currentCode=BuildConfig.VERSION_CODE;
    if(remoteCode<=currentCode){if(manual)ui.post(()->toast("Seu BRAFLIX já está atualizado"));return;}
    String apk=j.getString("apkUrl");
    String sha=j.getString("sha256").toLowerCase(Locale.ROOT);
    String versionName=j.optString("versionName",String.valueOf(remoteCode));
    String notes=j.optString("notes","Melhorias e correções.");
    if(!apk.startsWith("https://"))throw new IOException("O endereço do APK deve usar HTTPS");
    if(!sha.matches("[0-9a-f]{64}"))throw new IOException("SHA-256 inválido no update.json");
    ui.post(()->new AlertDialog.Builder(this)
     .setTitle("BRAFLIX "+versionName+" disponível")
     .setMessage(notes+"\n\nBaixar e instalar a nova versão? Seus favoritos e configurações serão mantidos.")
     .setNegativeButton("Agora não",null)
     .setPositiveButton("Atualizar",(d,w)->downloadUpdate(apk,sha,remoteCode)).show());
   }catch(Exception e){if(manual)ui.post(()->toast("Não foi possível verificar: "+e.getMessage()));}
   finally{updateCheckInProgress=false;}
  });
 }
 // Follow HTTPS-only redirects because GitHub Releases serves assets from a separate HTTPS host.
 // A finite redirect limit prevents redirect loops and HTTP downgrade.
 private HttpURLConnection openHttpsWithRedirects(String address) throws IOException {
  URL current=new URL(address);
  for(int redirects=0;redirects<=6;redirects++){
   if(!"https".equalsIgnoreCase(current.getProtocol()))throw new IOException("Atualização exige HTTPS");
   HttpURLConnection connection=(HttpURLConnection)current.openConnection();
   connection.setConnectTimeout(15000);connection.setReadTimeout(30000);
   connection.setInstanceFollowRedirects(false);
   int status=connection.getResponseCode();
   if(status==301||status==302||status==303||status==307||status==308){
    String location=connection.getHeaderField("Location");
    connection.disconnect();
    if(location==null)throw new IOException("Redirecionamento sem destino");
    current=new URL(current,location);
    continue;
   }
   if(status!=200){connection.disconnect();throw new IOException("Servidor retornou HTTP "+status);}
   return connection;
  }
  throw new IOException("Redirecionamentos demais no download");
 }
 private void downloadUpdate(String url,String expectedSha,int expectedVersion){
  Toast.makeText(this,"Baixando atualização do BRAFLIX...",Toast.LENGTH_LONG).show();
  worker.execute(()->{
   File tmp=null;
   try{
    File dir=new File(getFilesDir(),"updates");
    if(!dir.exists()&&!dir.mkdirs())throw new IOException("Pasta de atualizações indisponível");
    tmp=new File(dir,"BRAFLIX-update.part");
    File apk=new File(dir,"BRAFLIX-update.apk");
    HttpURLConnection conn=openHttpsWithRedirects(url);
    if(conn.getResponseCode()!=200)throw new IOException("Servidor retornou HTTP "+conn.getResponseCode());
    int maxSize=250*1024*1024;
    long contentLength=conn.getContentLengthLong();
    if(contentLength>maxSize)throw new IOException("APK excede 250 MB");
    MessageDigest digest=MessageDigest.getInstance("SHA-256");long count=0;
    try(InputStream in=conn.getInputStream();FileOutputStream out=new FileOutputStream(tmp)){
     byte[] buffer=new byte[32768];int n;
     while((n=in.read(buffer))!=-1){count+=n;if(count>maxSize)throw new IOException("APK excede 250 MB");out.write(buffer,0,n);digest.update(buffer,0,n);}
    }finally{conn.disconnect();}
    StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",b & 0xff));
    if(!hash.toString().equals(expectedSha))throw new IOException("Integridade do APK não confere (SHA-256)");
    if(apk.exists()&&!apk.delete())throw new IOException("Não foi possível substituir arquivo antigo");
    if(!tmp.renameTo(apk))throw new IOException("Não foi possível finalizar download");
    PackageInfo packageInfo=getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),0);
    if(packageInfo==null||!getPackageName().equals(packageInfo.packageName)){apk.delete();throw new IOException("APK não pertence ao BRAFLIX");}
    long code=Build.VERSION.SDK_INT>=28?packageInfo.getLongVersionCode():packageInfo.versionCode;
    if(code!=expectedVersion||code<=BuildConfig.VERSION_CODE){apk.delete();throw new IOException("Versão do arquivo APK é incompatível");}
    ui.post(()->{pendingUpdateFile=apk;requestInstall();});
   }catch(Exception e){if(tmp!=null)tmp.delete();ui.post(()->new AlertDialog.Builder(this)
      .setTitle("Erro no download").setMessage(e.getMessage()).setPositiveButton("OK",null).show());}
  });
 }
 private void requestInstall(){
  if(pendingUpdateFile==null||!pendingUpdateFile.exists()){toast("APK de atualização não encontrado");return;}
  if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
   new AlertDialog.Builder(this).setTitle("Permitir instalação")
    .setMessage("O Android precisa que você autorize o BRAFLIX a instalar atualizações. Depois da autorização, volte ao aplicativo para continuar.")
    .setNegativeButton("Cancelar",null)
    .setPositiveButton("Abrir configurações",(d,w)->{
     Intent i=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));
     startActivityForResult(i,INSTALL_PERMISSION_REQUEST);
    }).show();return;
  }
  Uri contentUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",pendingUpdateFile);
  Intent intent=new Intent(Intent.ACTION_VIEW);
  intent.setDataAndType(contentUri,"application/vnd.android.package-archive");
  intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
  try{startActivity(intent);}catch(Exception e){toast("Não foi possível abrir o instalador: "+e.getMessage());}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
  if(request==INSTALL_PERMISSION_REQUEST){if(Build.VERSION.SDK_INT<26||getPackageManager().canRequestPackageInstalls())requestInstall();return;}
  if(request==PICK_FILE&&result==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();worker.execute(()->{
   try(InputStream in=getContentResolver().openInputStream(uri)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>15000000)throw new IOException("Lista maior que 15 MB");}
    String m=out.toString("UTF-8");ArrayList<MediaEntry> resultList=M3uParser.parse(m);if(resultList.isEmpty())throw new IOException("Nenhum canal encontrado");
    try(FileOutputStream file=openFileOutput("channels.m3u",MODE_PRIVATE)){file.write(m.getBytes("UTF-8"));}
    getPreferences(0).edit().remove("playlistUrl").apply();ui.post(()->{channels.clear();channels.addAll(resultList);toast(channels.size()+" canais importados");show("Canais");});
   }catch(Exception e){ui.post(()->toast("Falha na importação: "+e.getMessage()));}
  });}}
 private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
 @Override protected void onDestroy(){worker.shutdownNow();images.shutdownNow();super.onDestroy();}
}
