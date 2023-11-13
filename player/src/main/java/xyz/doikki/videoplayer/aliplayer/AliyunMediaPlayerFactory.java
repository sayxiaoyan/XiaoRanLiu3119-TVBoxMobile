package xyz.doikki.videoplayer.aliplayer;

import android.content.Context;

import xyz.doikki.videoplayer.player.PlayerFactory;

public class AliyunMediaPlayerFactory extends PlayerFactory<AliMediaPlayer> {

    public static AliyunMediaPlayerFactory create() {
        return new AliyunMediaPlayerFactory();
    }

    @Override
    public AliMediaPlayer createPlayer(Context context) {
        return new AliMediaPlayer(context);
    }
}