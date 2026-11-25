package com.example.trilogidirectory.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("mahasiswa")
    suspend fun getAllMahasiswa(): Response<List<Mahasiswa>>

    @POST("mahasiswa")
    suspend fun tambahMahasiswa(
        @Body mahasiswa: Mahasiswa
    ): Response<Mahasiswa>

    @PUT("mahasiswa/{id}")
    suspend fun updateMahasiswa(
        @Path("id") id: String,
        @Body mahasiswa: Mahasiswa
    ): Response<Mahasiswa>

    @DELETE("mahasiswa/{id}")
    suspend fun hapusMahasiswa(
        @Path("id") id: String
    ): Response<Mahasiswa>
}