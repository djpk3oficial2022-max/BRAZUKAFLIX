package com.braflix.tv;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends Activity {
    private ExoPlayer player;
    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_player);
        ((TextView)findViewById(R.id.title)).setText(getIntent().getStringExtra("title"));
        String url=getIntent().getStringExtra("url");
        if(url==null||url.isEmpty()){finish();return;}
        player=new ExoPlayer.Builder(this).build();
        ((PlayerView)findViewById(R.id.player)).setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();player.play();
    }
    @Override protected void onStop(){super.onStop();if(player!=null){player.release();player=null;}}
}
