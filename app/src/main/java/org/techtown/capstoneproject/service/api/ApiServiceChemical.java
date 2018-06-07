package org.techtown.capstoneproject.service.api;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/*
 * Created by ShimPiggy on 2018-05-21.
 */

public interface ApiServiceChemical {
    /*
    @GET("api주소")
     Call<ResponseBody>함수이름(@Query("변수이름")int 안드로이드에서 보낼 변수);

          @GET("repos/{owner}/{repo}/contributors")
    Call<List<Contributor>> repoContributors(
            @Path("owner") String owner,
            @Path("repo") String repo);

            @Path : 동적 바인딩
            @Query : 원하는 것 받기
            @Body : Http request body
     */

    //화학성분
    //화학성분 이름 list
    @GET("chemical/getnamelist")
    Call<ResponseBody> getNameList(@Query("postId") String postId);

    //화학성분 하나 -> 정보 얻기
    @GET("chemical/getinfo/{name}")
    Call<ResponseBody> getInfo(@Path("name") String name);

    //제품명: 화학성분 여러개
    @GET("product/ingradient/{productname}")
    Call<ResponseBody> getProductChemicalList(@Path("productname") String productname);

    //화학성분: 화학성분 여러개
    @GET("chemical/getinfolist")
    Call<ResponseBody> getChemicalChemicalList(@Query("list") List<String> list);
}
