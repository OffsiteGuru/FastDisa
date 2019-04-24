package guru.offsite.fastdisa;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class PostForm {
    private final OkHttpClient client = new OkHttpClient();

    public String run(String URL, String CallerId, String Dest, String Passwd) throws Exception {
        RequestBody formBody = new FormBody.Builder()
                .add("myCallerId", CallerId)
                .add("myDest", Dest)
                .add("password", Passwd)
                .build();
        Request request = new Request.Builder()
                .url(URL)
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            ResponseBody responseBody = response.body();
            String output = responseBody.string();
            Log.d("response string: ", output);
            return(output);
        }
    }
}