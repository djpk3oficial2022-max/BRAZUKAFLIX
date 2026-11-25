package com.braflix.tv;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public final class M3uParser {
 private static final Pattern ATTR=Pattern.compile("([\\w-]+)\\s*=\\s*\"([^\"]*)\"");
 public static ArrayList<MediaEntry> parse(String content) {
  ArrayList<MediaEntry> list=new ArrayList<>(); if(content==null)return list;
  String name="",group="",logo="";boolean ready=false;
  for(String raw:content.split("\\r?\\n")) {
   String line=raw.trim();
   if(line.startsWith("#EXTINF")) { name="";group="";logo="";ready=true;
    Matcher m=ATTR.matcher(line);while(m.find()){switch(m.group(1).toLowerCase(Locale.ROOT)) {
     case "group-title":group=m.group(2);break;case "tvg-logo":logo=m.group(2);break;case "tvg-name":name=m.group(2);break;}}
    int comma=line.lastIndexOf(',');if(comma>=0&&comma+1<line.length())name=line.substring(comma+1).trim();
   } else if(line.startsWith("#EXTGRP:"))group=line.substring(8).trim();
   else if(ready&&(line.startsWith("http://")||line.startsWith("https://"))){
    list.add(new MediaEntry(name.isEmpty()?"Canal "+(list.size()+1):name,line,group,logo,"TV"));ready=false;
   }
  }
  return list;
 }
}
