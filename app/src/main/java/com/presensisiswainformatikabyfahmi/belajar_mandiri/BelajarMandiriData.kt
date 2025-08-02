package com.presensisiswainformatikabyfahmi.belajar_mandiri

import com.google.gson.annotations.SerializedName

class BelajarMandiriData(
    @SerializedName("kelas10") val kelas10: List<Bab>,
    @SerializedName("kelas11") val kelas11: List<Bab>,
    @SerializedName("kelas12") val kelas12: List<Bab>
)