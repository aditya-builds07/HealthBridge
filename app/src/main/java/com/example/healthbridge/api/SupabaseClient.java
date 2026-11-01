package com.example.healthbridge.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client configured for Supabase REST API.
 * All requests carry the required apikey and Authorization headers.
 */
public class SupabaseClient {

    private static final String BASE_URL = "https://awegpsbewzbzazsrrnbr.supabase.co/rest/v1/";
    private static final String SUPABASE_KEY = "sb_publishable_yaAm59lZzOYZahj0y_NRYw_3z8PSKL6";

    // Storage base URL for file uploads
    public static final String STORAGE_URL = "https://awegpsbewzbzazsrrnbr.supabase.co/storage/v1/object/";
    public static final String STORAGE_PUBLIC_URL = "https://awegpsbewzbzazsrrnbr.supabase.co/storage/v1/object/public/";
    public static final String BUCKET_NAME = "healthbridge-files";

    private static Retrofit retrofit = null;
    private static OkHttpClient okHttpClient = null;

    public static String getSupabaseKey() {
        return SUPABASE_KEY;
    }

    public static OkHttpClient getHttpClient() {
        if (okHttpClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("apikey", SUPABASE_KEY)
                            .header("Authorization", "Bearer " + SUPABASE_KEY)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=representation")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                }
            };

            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .build();
        }
        return okHttpClient;
    }

    public static SupabaseApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SupabaseApiService.class);
    }
}
