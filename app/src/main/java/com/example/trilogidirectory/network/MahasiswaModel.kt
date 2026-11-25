package com.example.trilogidirectory.network

import com.google.gson.annotations.SerializedName

data class Mahasiswa(
    val id: String = "",
    @SerializedName("name")
    val nama: String = "",
    val nim: String = "",
    val prodi: String? = "Sistem Informasi",
    val foto: String? = null,
    val semester: String? = "1"
)