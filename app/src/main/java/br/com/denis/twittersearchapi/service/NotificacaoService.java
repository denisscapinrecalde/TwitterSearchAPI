package br.com.denis.twittersearchapi.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import br.com.denis.twittersearchapi.R;
import br.com.denis.twittersearchapi.TweetActivity;
import br.com.denis.twittersearchapi.util.HttpRequest;


public class NotificacaoService extends Service {

    private String accessToken;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void criarNotificacao(String usuario,
                                  String texto, int id) {
        int icone = R.drawable.search_twitter;
        String aviso = getString(R.string.aviso);
        String titulo = usuario + " " + getString(R.string.titulo);
        Context context = getApplicationContext();
        Intent intent = new Intent(context, TweetActivity.class);
        intent.putExtra(TweetActivity.USUARIO, usuario.toString());
        intent.putExtra(TweetActivity.TEXTO, texto.toString());
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder notification = new Notification.Builder(this)
                                                .setSmallIcon(icone)
                                                .setContentTitle(titulo)
                                                .setContentText(aviso)
                                                .setAutoCancel(true)
                                                .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
        String ns = Context.NOTIFICATION_SERVICE;
        notification.setContentIntent(pendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(ns);
        notificationManager.notify(id, notification.build());
    }

    private boolean estaConectado() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info.isConnected();
    }

    @Override
    public int onStartCommand(Intent intent, int flags,
                              int startId) {
        ScheduledThreadPoolExecutor pool =
                new ScheduledThreadPoolExecutor(1);
        long delayInicial = 0;
        long periodo = 1;
        TimeUnit unit = TimeUnit.HOURS;
        pool.scheduleAtFixedRate(new NotificacaoTask(),
                delayInicial, periodo, unit);
        new AutenticacaoTask().execute();
        return START_STICKY;
    }

    private class NotificacaoTask implements Runnable {

        private String baseUrl =
                "https://api.twitter.com/1.1/search/tweets.json";
        private String refreshUrl = "?q=@android";

        @Override
        public void run() {
            if (!estaConectado()) {
                return;
            }
            try {
                String conteudo =
                        HttpRequest.get(baseUrl + refreshUrl)
                                .authorization("Bearer " + accessToken)
                                .body();
                JSONObject jsonObject = new JSONObject(conteudo);
                refreshUrl = jsonObject.getJSONObject("search_metadata").getString("next_results");
                JSONArray resultados =
                        jsonObject.getJSONArray("statuses");
                if(resultados.length()>0) {
                    for (int i = 0; i < 1; i++) {
                        JSONObject tweet = resultados.getJSONObject(i);
                        String texto = tweet.getString("text");
                        String usuario = tweet.getJSONObject("user")
                                .getString("screen_name");
                        criarNotificacao(usuario, texto, i);
                    }
                }
            } catch (Exception e) {
                Log.e(getPackageName(), e.getMessage(), e);
            }
        }
    }

    private class AutenticacaoTask  extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Map<String, String> data =
                        new HashMap<String, String>();
                data.put("grant_type", "client_credentials");
                String json = HttpRequest
                        .post("https://api.twitter.com/oauth2/token")
                        .authorization("Basic "+ gerarChave())
                        .form(data)
                        .body();
                JSONObject token = new JSONObject(json);
                accessToken = token.getString("access_token");
            } catch (Exception e) {
                return null;
            }
            return null;
        }

        private String gerarChave()
                throws UnsupportedEncodingException {
            String key = "yjAPAvaWgkXinaIygISWSEZei";
            String secret = "QUiZe73Wqh9dgJ06zLBA0TIW3bch81YhTqFAcfhCssEaUVff5s";
            String token = key + ":" + secret;
            String base64 = Base64.encodeToString(token.getBytes(),
                    Base64.NO_WRAP);
            return base64;
        }
    }
}
