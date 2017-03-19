package br.com.denis.twittersearchapi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import br.com.denis.twittersearchapi.service.NotificacaoService;
import br.com.denis.twittersearchapi.util.HttpRequest;

public class MainActivity extends AppCompatActivity {

    private ListView lista;
    private EditText texto;

    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lista = (ListView) findViewById(R.id.lista);
        texto = (EditText) findViewById(R.id.texto);

        new AutenticacaoTask().execute();
    }

    public void buscar(View view) {
        String filtro = texto.getText().toString();
        if(accessToken == null){
            Toast.makeText(this, "Token não disponível",
                    Toast.LENGTH_SHORT).show();
        }else{
            //startService(new Intent(this, NotificacaoService.class));
            new TwitterTask().execute(filtro);
        }
    }

    private class TwitterTask  extends AsyncTask<String, Void, Spanned[]> {

        ProgressDialog dialog;
        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.show();
        }

        @Override
        protected Spanned[] doInBackground(String... params) {
            try {
                String filtro = params[0];
                if(TextUtils.isEmpty(filtro)){
                    return null;
                }
                String urlTwitter =
                        "https://api.twitter.com/1.1/search/tweets.json?q=";
                String url = Uri.parse(urlTwitter + filtro).toString();
                String conteudo = HttpRequest.get(url)
                        .authorization("Bearer " + accessToken)
                        .body();
                if(!conteudo.isEmpty()){
                    JSONObject jsonObject = new JSONObject(conteudo);
                    if(jsonObject.has("statuses")) {
                        JSONArray resultados = jsonObject.getJSONArray("statuses");
                        Spanned[] tweets = new Spanned[resultados.length()];
                        for (int i = 0; i < resultados.length(); i++) {
                            JSONObject tweet = resultados.getJSONObject(i);
                            String texto = tweet.getString("text");
                            String usuario = tweet.getJSONObject("user")
                                    .getString("screen_name");
                            String linha = "<b>@" + usuario + "</b> <br>" + texto;
                            tweets[i] = Html.fromHtml(linha);
                        }
                        return tweets;
                    }
                }
                return null;
            } catch (Exception e) {
                Log.e(getPackageName(), e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPostExecute(Spanned[] result) {
            ArrayAdapter<Spanned> adapter;
            if(result != null){
                adapter =
                        new ArrayAdapter<Spanned>(getBaseContext(),
                                android.R.layout.simple_list_item_1, result);

            }else{
                adapter  =
                        new ArrayAdapter<Spanned>(getBaseContext(),
                                android.R.layout.simple_list_item_1, new Spanned[]{});
            }
            lista.setAdapter(adapter);
            dialog.dismiss();
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
