package com.example.healthbridge.api;

import android.content.Context;

import com.example.healthbridge.utils.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client configured for Supabase REST and GoTrue Auth APIs.
 * Automatically injects the user's Supabase JWT access token when logged in,
 * and falls back to the publishable anon key.
 */
public class SupabaseClient {

    public static final String SUPABASE_URL = "https://awegpsbewzbzazsrrnbr.supabase.co";
    private static final String BASE_REST_URL = "https://awegpsbewzbzazsrrnbr.supabase.co/rest/v1/";
    private static final String BASE_AUTH_URL = "https://awegpsbewzbzazsrrnbr.supabase.co/auth/v1/";
    private static final String SUPABASE_KEY = "sb_publishable_yaAm59lZzOYZahj0y_NRYw_3z8PSKL6";

    // Storage URLs for document vault
    public static final String STORAGE_URL = "https://awegpsbewzbzazsrrnbr.supabase.co/storage/v1/object/";
    public static final String STORAGE_PUBLIC_URL = "https://awegpsbewzbzazsrrnbr.supabase.co/storage/v1/object/public/";
    public static final String BUCKET_NAME = "healthbridge-files";

    private static Context appContext = null;
    private static OkHttpClient okHttpClient = null;
    private static Retrofit restRetrofit = null;
    private static Retrofit authRetrofit = null;

    /**
     * Optional initialization with application context to enable dynamic JWT bearer tokens.
     */
    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static String getSupabaseKey() {
        return SUPABASE_KEY;
    }

    public static synchronized OkHttpClient getHttpClient() {
        if (okHttpClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder()
                            .header("apikey", SUPABASE_KEY)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=representation");

                    // If a user is logged in, attach their user JWT access token.
                    // Otherwise, attach the default publishable/anon key.
                    String bearerToken = SUPABASE_KEY;
                    if (appContext != null) {
                        String userJwt = SessionManager.getInstance(appContext).getAccessToken();
                        if (userJwt != null && !userJwt.trim().isEmpty()) {
                            bearerToken = userJwt;
                        }
                    }

                    builder.header("Authorization", "Bearer " + bearerToken);
                    builder.method(original.method(), original.body());
                    return chain.proceed(builder.build());
                }
            };

            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .build();
        }
        return okHttpClient;
    }

    /**
     * Returns the PostgREST API service for database queries (/rest/v1/).
     */
    public static synchronized SupabaseApiService getApiService() {
        if (restRetrofit == null) {
            restRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_REST_URL)
                    .client(getHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return restRetrofit.create(SupabaseApiService.class);
    }

    /**
     * Returns the GoTrue Auth API service for login, signup, and tokens (/auth/v1/).
     */
    public static synchronized SupabaseAuthService getAuthService() {
        if (authRetrofit == null) {
            authRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_AUTH_URL)
                    .client(getHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit.create(SupabaseAuthService.class);
    }
}
