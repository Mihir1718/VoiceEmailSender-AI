package com.example.voicemailsender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Header;

public interface OpenAIApi {

    @POST("v1/chat/completions")
    Call<OpenAIResponse> getAIMessage(
            @Header("Authorization") String authHeader,
            @Body OpenAIRequest request
    );
}
